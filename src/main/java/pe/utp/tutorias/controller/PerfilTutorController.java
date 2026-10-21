package pe.utp.tutorias.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pe.utp.tutorias.dao.CursoDAO;
import pe.utp.tutorias.model.Usuario;
import pe.utp.tutorias.service.PerfilTutorService;
import pe.utp.tutorias.service.UsuarioService;

import java.math.BigDecimal;
import java.util.List;

/** BPM "Perfiles y Busqueda". */
@Controller
public class PerfilTutorController {

    private final PerfilTutorService perfilTutorService;
    private final UsuarioService usuarioService;
    private final CursoDAO cursoDAO;

    public PerfilTutorController(PerfilTutorService perfilTutorService, UsuarioService usuarioService, CursoDAO cursoDAO) {
        this.perfilTutorService = perfilTutorService;
        this.usuarioService = usuarioService;
        this.cursoDAO = cursoDAO;
    }

    @GetMapping("/tutor/perfil/nuevo")
    public String formularioPerfil(HttpSession session, Model model) {
        Usuario tutor = AuthController.usuarioRequerido(session, usuarioService);
        model.addAttribute("cursos", cursoDAO.findAll());
        model.addAttribute("misPerfiles", perfilTutorService.misPerfiles(tutor.getIdUsuario()));
        return "perfil-tutor-form";
    }

    @PostMapping("/tutor/perfil")
    public String publicarPerfil(HttpSession session,
                                  @RequestParam Integer idCurso,
                                  @RequestParam BigDecimal tarifaHora,
                                  @RequestParam(required = false) List<String> dias,
                                  @RequestParam(required = false) String horaInicio,
                                  @RequestParam(required = false) String horaFin,
                                  Model model) {
        Usuario tutor = AuthController.usuarioRequerido(session, usuarioService);
        try {
            String disponibilidad = construirDisponibilidad(dias, horaInicio, horaFin);
            perfilTutorService.publicarPerfil(tutor, idCurso, tarifaHora, disponibilidad);
            model.addAttribute("mensaje", "Perfil publicado correctamente.");
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        model.addAttribute("cursos", cursoDAO.findAll());
        model.addAttribute("misPerfiles", perfilTutorService.misPerfiles(tutor.getIdUsuario()));
        return "perfil-tutor-form";
    }

    /**
     * Convierte los dias seleccionados (checkboxes) y el rango de horario
     * (inputs "time") en el texto legible que se guarda en PerfilTutor.disponibilidad,
     * por ejemplo: "Lunes, Miercoles, Viernes de 18:00 a 21:00".
     */
    private String construirDisponibilidad(List<String> dias, String horaInicio, String horaFin) {
        if (dias == null || dias.isEmpty()) {
            return null;
        }
        String texto = String.join(", ", dias);
        boolean tieneHorario = horaInicio != null && !horaInicio.isBlank()
                && horaFin != null && !horaFin.isBlank();
        if (tieneHorario) {
            texto += " de " + horaInicio + " a " + horaFin;
        }
        return texto;
    }

    @GetMapping("/buscar")
    public String buscarTutores(@RequestParam(required = false) String curso,
                                 @RequestParam(required = false) BigDecimal tarifaMax,
                                 @RequestParam(required = false) BigDecimal calificacionMin,
                                 Model model) {
        model.addAttribute("resultados", perfilTutorService.buscar(curso, tarifaMax, calificacionMin));
        model.addAttribute("curso", curso);
        model.addAttribute("tarifaMax", tarifaMax);
        model.addAttribute("calificacionMin", calificacionMin);
        return "buscar-tutores";
    }
}
