package pe.utp.tutorias.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Todos los numeros que muestra el dashboard del administrador (los KPIs del
 * Lean Canvas mas los rankings). Es un record: solo guarda datos ya calculados.
 *
 * Los campos que pueden no tener dato (tasaCompletadas, calificacionPromedio)
 * son null cuando todavia no hay informacion para calcularlos.
 */
public record KpiDashboard(
        // KPI 1: sesiones reservadas
        int totalReservas,
        List<FilaConteo> sesionesPorSemana,
        // KPI 2: tutores activos (con al menos una sesion dictada)
        int tutoresActivos,
        int totalTutores,
        // KPI 3: completadas vs canceladas
        int completadas,
        int confirmadas,
        int pendientes,
        int canceladas,
        BigDecimal tasaCompletadas,
        // KPI 4: calificacion promedio de los tutores
        BigDecimal calificacionPromedio,
        // KPI 5: ingreso por comision
        BigDecimal comisionTotal,
        BigDecimal montoProcesado,
        int pagosRegistrados,
        // Reportes del administrador
        List<FilaConteo> topTutores,
        List<FilaConteo> topCursos) {
}
