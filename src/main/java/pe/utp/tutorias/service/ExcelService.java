package pe.utp.tutorias.service;

import pe.utp.tutorias.dto.KpiDashboard;
import pe.utp.tutorias.model.Pago;

import java.util.List;

/**
 * Exporta informacion de pagos a archivos Excel (.xlsx) con Apache POI.
 * Devuelve el archivo como bytes para que el controlador lo descargue.
 */
public interface ExcelService {

    /**
     * Excel para el alumno: una hoja con cada pago que hizo (fecha, curso,
     * tutor y monto pagado) y el total al final.
     */
    byte[] exportarHistorialAlumno(List<Pago> pagos);

    /**
     * Excel para el tutor: la hoja "Ingresos" con el detalle de cada pago
     * (monto total, comision y neto) y una hoja "Por curso" con lo que
     * gano en cada curso.
     */
    byte[] exportarIngresosTutor(List<Pago> pagos);

    /**
     * Excel para el administrador con los KPIs del dashboard: hoja "Resumen"
     * (indicador y valor) mas una hoja por cada grafico o ranking.
     */
    byte[] exportarKpis(KpiDashboard kpi);
}
