package pe.utp.tutorias.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pe.utp.tutorias.model.Reserva;
import pe.utp.tutorias.model.Usuario;
import pe.utp.tutorias.service.ExcelService;
import pe.utp.tutorias.service.PagoService;
import pe.utp.tutorias.service.ReservaService;
import pe.utp.tutorias.service.UsuarioService;

import java.math.BigDecimal;
import java.time.LocalDate;

/** BPM "Registrar pago de sesion" (y el resumen de ingresos del tutor). */
@Controller
public class PagoController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final PagoService pagoService;
    private final ReservaService reservaService;
    private final UsuarioService usuarioService;
    private final ExcelService excelService;

    public PagoController(PagoService pagoService, ReservaService reservaService,
                          UsuarioService usuarioService, ExcelService excelService) {
        this.pagoService = pagoService;
        this.reservaService = reservaService;
        this.usuarioService = usuarioService;
        this.excelService = excelService;
    }

    @GetMapping("/pagar/{idReserva}")
    public String formularioPago(@PathVariable Integer idReserva, HttpSession session, Model model) {
        Usuario alumno = AuthController.usuarioRequerido(session, usuarioService);
        Reserva reserva = reservaService.obtener(idReserva);
        if (!reserva.getAlumno().getIdUsuario().equals(alumno.getIdUsuario())) {
            throw new IllegalStateException("Esta reserva no te pertenece.");
        }
        if (pagoService.yaPagada(idReserva)) {
            return "redirect:/historial-pagos";
        }
        model.addAttribute("reserva", reserva);
        return "pagar";
    }

    @PostMapping("/pagar")
    public String registrarPago(@RequestParam Integer idReserva,
                                 @RequestParam BigDecimal montoTotal,
                                 HttpSession session, Model model) {
        Usuario alumno = AuthController.usuarioRequerido(session, usuarioService);
        try {
            pagoService.registrarPago(idReserva, alumno.getIdUsuario(), montoTotal);
            return "redirect:/historial-pagos";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("reserva", reservaService.obtener(idReserva));
            return "pagar";
        }
    }

    @GetMapping("/historial-pagos")
    public String historialPagos(HttpSession session, Model model) {
        Usuario alumno = AuthController.usuarioRequerido(session, usuarioService);
        model.addAttribute("pagos", pagoService.historialPorAlumno(alumno.getIdUsuario()));
        return "historial-pagos";
    }

    @GetMapping("/tutor/ingresos")
    public String ingresosTutor(HttpSession session, Model model) {
        Usuario tutor = AuthController.usuarioRequerido(session, usuarioService);
        model.addAttribute("pagos", pagoService.ingresosPorTutor(tutor.getIdUsuario()));
        model.addAttribute("totalNeto", pagoService.totalIngresosNetos(tutor.getIdUsuario()));
        return "ingresos-tutor";
    }

    /** Descarga en Excel el historial de pagos del alumno (boton en historial-pagos.html). */
    @GetMapping("/historial-pagos/excel")
    public ResponseEntity<byte[]> historialPagosExcel(HttpSession session) {
        Usuario alumno = AuthController.usuarioRequerido(session, usuarioService);
        byte[] archivo = excelService.exportarHistorialAlumno(pagoService.historialPorAlumno(alumno.getIdUsuario()));
        return descargaExcel(archivo, "historial_pagos");
    }

    /** Descarga en Excel los ingresos del tutor (boton en ingresos-tutor.html). */
    @GetMapping("/tutor/ingresos/excel")
    public ResponseEntity<byte[]> ingresosTutorExcel(HttpSession session) {
        Usuario tutor = AuthController.usuarioRequerido(session, usuarioService);
        byte[] archivo = excelService.exportarIngresosTutor(pagoService.ingresosPorTutor(tutor.getIdUsuario()));
        return descargaExcel(archivo, "ingresos_tutor");
    }

    private ResponseEntity<byte[]> descargaExcel(byte[] archivo, String nombreBase) {
        String nombre = nombreBase + "_" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(XLSX)
                .body(archivo);
    }
}
