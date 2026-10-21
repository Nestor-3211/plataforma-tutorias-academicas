package pe.utp.tutorias.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pe.utp.tutorias.model.Usuario;
import pe.utp.tutorias.service.PerfilTutorService;
import pe.utp.tutorias.service.ReservaService;
import pe.utp.tutorias.service.UsuarioService;

import java.time.LocalDateTime;

/** BPM "Reservar Sesion" (ramas Alumno y Tutor). */
@Controller
public class ReservaController {

    private final ReservaService reservaService;
    private final PerfilTutorService perfilTutorService;
    private final UsuarioService usuarioService;

    public ReservaController(ReservaService reservaService, PerfilTutorService perfilTutorService, UsuarioService usuarioService) {
        this.reservaService = reservaService;
        this.perfilTutorService = perfilTutorService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/reserva/nueva")
    public String formularioReserva(@RequestParam Integer idPerfil, HttpSession session, Model model) {
        AuthController.usuarioRequerido(session, usuarioService);
        model.addAttribute("perfil", perfilTutorService.obtener(idPerfil));
        return "reserva-form";
    }

    @PostMapping("/reserva")
    public String reservar(@RequestParam Integer idPerfil,
                            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime horario,
                            HttpSession session, Model model) {
        Usuario alumno = AuthController.usuarioRequerido(session, usuarioService);
        try {
            reservaService.reservar(alumno, idPerfil, horario);
            return "redirect:/mis-reservas";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("perfil", perfilTutorService.obtener(idPerfil));
            return "reserva-form";
        }
    }

    @GetMapping("/mis-reservas")
    public String misReservas(HttpSession session, Model model) {
        Usuario usuario = AuthController.usuarioRequerido(session, usuarioService);
        model.addAttribute("reservas", reservaService.misReservasComoAlumno(usuario.getIdUsuario()));
        return "mis-reservas";
    }

    @GetMapping("/tutor/reservas")
    public String reservasComoTutor(HttpSession session, Model model) {
        Usuario tutor = AuthController.usuarioRequerido(session, usuarioService);
        model.addAttribute("reservas", reservaService.misReservasComoTutor(tutor.getIdUsuario()));
        return "tutor-reservas";
    }

    @PostMapping("/tutor/reservas/{id}/confirmar")
    public String confirmar(@PathVariable("id") Integer idReserva, HttpSession session) {
        Usuario tutor = AuthController.usuarioRequerido(session, usuarioService);
        reservaService.confirmar(idReserva, tutor.getIdUsuario());
        return "redirect:/tutor/reservas";
    }

    @PostMapping("/tutor/reservas/{id}/cancelar")
    public String cancelar(@PathVariable("id") Integer idReserva, HttpSession session) {
        Usuario tutor = AuthController.usuarioRequerido(session, usuarioService);
        reservaService.cancelar(idReserva, tutor.getIdUsuario());
        return "redirect:/tutor/reservas";
    }

    @PostMapping("/tutor/reservas/{id}/completar")
    public String marcarCompletada(@PathVariable("id") Integer idReserva, HttpSession session) {
        Usuario tutor = AuthController.usuarioRequerido(session, usuarioService);
        reservaService.marcarCompletada(idReserva, tutor.getIdUsuario());
        return "redirect:/tutor/reservas";
    }
}
