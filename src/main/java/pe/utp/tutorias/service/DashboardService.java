package pe.utp.tutorias.service;

import pe.utp.tutorias.dto.KpiDashboard;

/** Calcula los indicadores (KPIs) que ve el administrador en su dashboard. */
public interface DashboardService {

    /**
     * Lee reservas, pagos y perfiles de tutor y devuelve todos los KPIs ya
     * calculados. Solo lee, no modifica nada.
     */
    KpiDashboard calcularKpis();
}
