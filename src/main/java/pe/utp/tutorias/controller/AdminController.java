package pe.utp.tutorias.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.utp.tutorias.dto.FilaConteo;
import pe.utp.tutorias.dto.KpiDashboard;
import pe.utp.tutorias.exception.AccesoDenegadoException;
import pe.utp.tutorias.model.Usuario;
import pe.utp.tutorias.service.DashboardService;
import pe.utp.tutorias.service.ExcelService;
import pe.utp.tutorias.service.UsuarioService;

import java.time.LocalDate;
import java.util.List;

/**
 * Pantallas del administrador. Todas las URLs /admin/** pasan antes por el
 * AdminInterceptor (config); aqui vuelvo a comprobar el rol por si acaso.
 */
@Controller
public class AdminController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final DashboardService dashboardService;
    private final ExcelService excelService;
    private final UsuarioService usuarioService;

    public AdminController(DashboardService dashboardService, ExcelService excelService, UsuarioService usuarioService) {
        this.dashboardService = dashboardService;
        this.excelService = excelService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(HttpSession session, Model model) {
        adminRequerido(session);
        KpiDashboard kpi = dashboardService.calcularKpis();

        model.addAttribute("kpi", kpi);
        // Datos de los graficos (Chart.js los lee desde la plantilla)
        model.addAttribute("labelsSemana", kpi.sesionesPorSemana().stream().map(FilaConteo::nombre).toList());
        model.addAttribute("valoresSemana", kpi.sesionesPorSemana().stream().map(FilaConteo::total).toList());
        model.addAttribute("labelsEstados", List.of(
                "Completadas (" + kpi.completadas() + ")",
                "Confirmadas (" + kpi.confirmadas() + ")",
                "Pendientes (" + kpi.pendientes() + ")",
                "Canceladas (" + kpi.canceladas() + ")"));
        model.addAttribute("valoresEstados", List.of(kpi.completadas(), kpi.confirmadas(), kpi.pendientes(), kpi.canceladas()));
        return "admin-dashboard";
    }

    /** Descarga los KPIs en Excel (boton "Exportar a Excel" del dashboard). */
    @GetMapping("/admin/dashboard/excel")
    public ResponseEntity<byte[]> dashboardExcel(HttpSession session) {
        adminRequerido(session);
        byte[] archivo = excelService.exportarKpis(dashboardService.calcularKpis());
        String nombre = "kpis_plataforma_" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(XLSX)
                .body(archivo);
    }

    private Usuario adminRequerido(HttpSession session) {
        Usuario usuario = AuthController.usuarioRequerido(session, usuarioService);
        if (!usuario.esAdmin()) {
            throw new AccesoDenegadoException("Esta seccion es solo para administradores.");
        }
        return usuario;
    }
}
