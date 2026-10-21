package pe.utp.tutorias.service.impl;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.utp.tutorias.dto.FilaConteo;
import pe.utp.tutorias.dto.KpiDashboard;
import pe.utp.tutorias.model.Pago;
import pe.utp.tutorias.service.ExcelService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera los Excel con Apache POI (XSSFWorkbook = formato .xlsx).
 *
 * Ojo: recorre relaciones LAZY (pago -> reserva -> perfil -> curso). Funciona
 * porque spring.jpa.open-in-view esta activo (viene asi por defecto en Spring
 * Boot) y esto se ejecuta dentro del mismo request. Si algun dia lo
 * desactivo, hay que traer esos datos con un fetch join en el DAO.
 */
@Service
public class ExcelServiceImpl implements ExcelService {

    private static final Logger log = LoggerFactory.getLogger(ExcelServiceImpl.class);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public byte[] exportarHistorialAlumno(List<Pago> pagos) {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Sheet hoja = libro.createSheet("Historial de pagos");
            CellStyle cabecera = estiloCabecera(libro);
            CellStyle moneda = estiloMoneda(libro, false);
            CellStyle monedaNegrita = estiloMoneda(libro, true);

            escribirCabecera(hoja, cabecera, "Fecha", "Curso", "Tutor", "Monto pagado (S/)");

            BigDecimal total = BigDecimal.ZERO;
            int fila = 1;
            for (Pago p : pagos) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(p.getFechaPago().format(FECHA));
                row.createCell(1).setCellValue(nombreCurso(p));
                row.createCell(2).setCellValue(p.getReserva().getPerfilTutor().getUsuario().getNombre());
                celdaMoneda(row, 3, p.getMontoTotal(), moneda);
                total = total.add(p.getMontoTotal());
            }

            Row filaTotal = hoja.createRow(fila);
            filaTotal.createCell(2).setCellValue("TOTAL PAGADO");
            celdaMoneda(filaTotal, 3, total, monedaNegrita);

            autoajustar(hoja, 4);
            log.info("Excel de historial generado: {} pagos, total={}", pagos.size(), total);
            return aBytes(libro);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel.", e);
        }
    }

    @Override
    public byte[] exportarIngresosTutor(List<Pago> pagos) {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            CellStyle cabecera = estiloCabecera(libro);
            CellStyle moneda = estiloMoneda(libro, false);
            CellStyle monedaNegrita = estiloMoneda(libro, true);

            // ---- Hoja 1: detalle de cada pago ----
            Sheet hoja = libro.createSheet("Ingresos");
            escribirCabecera(hoja, cabecera, "Fecha", "Curso", "Alumno", "Monto total (S/)", "Comision (S/)", "Neto tutor (S/)");

            BigDecimal totalNeto = BigDecimal.ZERO;
            int fila = 1;
            for (Pago p : pagos) {
                Row row = hoja.createRow(fila++);
                row.createCell(0).setCellValue(p.getFechaPago().format(FECHA));
                row.createCell(1).setCellValue(nombreCurso(p));
                row.createCell(2).setCellValue(p.getReserva().getAlumno().getNombre());
                celdaMoneda(row, 3, p.getMontoTotal(), moneda);
                celdaMoneda(row, 4, p.getComision(), moneda);
                celdaMoneda(row, 5, p.getMontoNeto(), moneda);
                totalNeto = totalNeto.add(p.getMontoNeto());
            }
            Row filaTotal = hoja.createRow(fila);
            filaTotal.createCell(4).setCellValue("TOTAL NETO");
            celdaMoneda(filaTotal, 5, totalNeto, monedaNegrita);
            autoajustar(hoja, 6);

            // ---- Hoja 2: lo ganado por curso ----
            // Guava: Multimaps.index agrupa la lista de pagos por el nombre del curso
            // (un curso -> varios pagos) sin tener que armar el Map a mano con ifs.
            ImmutableListMultimap<String, Pago> porCurso = Multimaps.index(pagos, ExcelServiceImpl::nombreCurso);

            Sheet hojaCursos = libro.createSheet("Por curso");
            escribirCabecera(hojaCursos, cabecera, "Curso", "Sesiones pagadas", "Neto tutor (S/)");
            int filaCurso = 1;
            for (String curso : porCurso.keySet()) {
                List<Pago> delCurso = porCurso.get(curso);
                BigDecimal netoCurso = delCurso.stream()
                        .map(Pago::getMontoNeto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                Row row = hojaCursos.createRow(filaCurso++);
                row.createCell(0).setCellValue(curso);
                row.createCell(1).setCellValue(delCurso.size());
                celdaMoneda(row, 2, netoCurso, moneda);
            }
            autoajustar(hojaCursos, 3);

            log.info("Excel de ingresos generado: {} pagos en {} cursos, neto={}",
                    pagos.size(), porCurso.keySet().size(), totalNeto);
            return aBytes(libro);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel.", e);
        }
    }

    @Override
    public byte[] exportarKpis(KpiDashboard kpi) {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            CellStyle cabecera = estiloCabecera(libro);
            CellStyle moneda = estiloMoneda(libro, false);

            // ---- Hoja 1: resumen de indicadores ----
            Sheet resumen = libro.createSheet("Resumen");
            escribirCabecera(resumen, cabecera, "Indicador", "Valor");
            int fila = 1;
            fila = filaTexto(resumen, fila, "Sesiones reservadas (total)", String.valueOf(kpi.totalReservas()));
            fila = filaTexto(resumen, fila, "Tutores activos (con al menos 1 sesion dictada)",
                    kpi.tutoresActivos() + " de " + kpi.totalTutores());
            fila = filaTexto(resumen, fila, "Sesiones completadas", String.valueOf(kpi.completadas()));
            fila = filaTexto(resumen, fila, "Sesiones confirmadas", String.valueOf(kpi.confirmadas()));
            fila = filaTexto(resumen, fila, "Sesiones pendientes", String.valueOf(kpi.pendientes()));
            fila = filaTexto(resumen, fila, "Sesiones canceladas", String.valueOf(kpi.canceladas()));
            fila = filaTexto(resumen, fila, "Tasa completadas vs canceladas",
                    kpi.tasaCompletadas() == null ? "Sin datos" : kpi.tasaCompletadas() + " %");
            fila = filaTexto(resumen, fila, "Calificacion promedio de tutores",
                    kpi.calificacionPromedio() == null ? "Sin resenas todavia" : kpi.calificacionPromedio().toString());
            fila = filaTexto(resumen, fila, "Pagos registrados", String.valueOf(kpi.pagosRegistrados()));
            Row filaMonto = resumen.createRow(fila++);
            filaMonto.createCell(0).setCellValue("Monto procesado (S/)");
            celdaMoneda(filaMonto, 1, kpi.montoProcesado(), moneda);
            Row filaComision = resumen.createRow(fila);
            filaComision.createCell(0).setCellValue("Ingreso por comision (S/)");
            celdaMoneda(filaComision, 1, kpi.comisionTotal(), moneda);
            autoajustar(resumen, 2);

            // ---- Hojas de graficos y rankings ----
            hojaConteos(libro, cabecera, "Sesiones por semana", "Semana (lunes)", "Sesiones", kpi.sesionesPorSemana());
            hojaConteos(libro, cabecera, "Top tutores", "Tutor", "Sesiones pedidas", kpi.topTutores());
            hojaConteos(libro, cabecera, "Top cursos", "Curso", "Sesiones pedidas", kpi.topCursos());

            log.info("Excel de KPIs generado: {} reservas, comision total={}", kpi.totalReservas(), kpi.comisionTotal());
            return aBytes(libro);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel.", e);
        }
    }

    // ------------------------- metodos de apoyo -------------------------

    private static int filaTexto(Sheet hoja, int fila, String indicador, String valor) {
        Row row = hoja.createRow(fila);
        row.createCell(0).setCellValue(indicador);
        row.createCell(1).setCellValue(valor);
        return fila + 1;
    }

    private static void hojaConteos(XSSFWorkbook libro, CellStyle cabecera, String nombreHoja,
                                    String titulo1, String titulo2, List<FilaConteo> filas) {
        Sheet hoja = libro.createSheet(nombreHoja);
        escribirCabecera(hoja, cabecera, titulo1, titulo2);
        int n = 1;
        for (FilaConteo f : filas) {
            Row row = hoja.createRow(n++);
            row.createCell(0).setCellValue(f.nombre());
            row.createCell(1).setCellValue(f.total());
        }
        autoajustar(hoja, 2);
    }

    private static String nombreCurso(Pago p) {
        return p.getReserva().getPerfilTutor().getCurso().getNombre();
    }

    private static void escribirCabecera(Sheet hoja, CellStyle estilo, String... titulos) {
        Row row = hoja.createRow(0);
        for (int i = 0; i < titulos.length; i++) {
            Cell celda = row.createCell(i);
            celda.setCellValue(titulos[i]);
            celda.setCellStyle(estilo);
        }
    }

    private static void celdaMoneda(Row row, int columna, BigDecimal valor, CellStyle estilo) {
        Cell celda = row.createCell(columna);
        celda.setCellValue(valor.doubleValue());
        celda.setCellStyle(estilo);
    }

    private static CellStyle estiloCabecera(XSSFWorkbook libro) {
        Font negrita = libro.createFont();
        negrita.setBold(true);
        negrita.setColor(IndexedColors.WHITE.getIndex());
        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(negrita);
        estilo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private static CellStyle estiloMoneda(XSSFWorkbook libro, boolean negrita) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));
        if (negrita) {
            Font font = libro.createFont();
            font.setBold(true);
            estilo.setFont(font);
        }
        return estilo;
    }

    private static void autoajustar(Sheet hoja, int columnas) {
        for (int i = 0; i < columnas; i++) {
            hoja.autoSizeColumn(i);
        }
    }

    private static byte[] aBytes(XSSFWorkbook libro) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        libro.write(salida);
        return salida.toByteArray();
    }
}
