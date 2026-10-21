package pe.utp.tutorias.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.utp.tutorias.dao.UniversidadDAO;
import pe.utp.tutorias.model.TipoUsuario;
import pe.utp.tutorias.model.Usuario;
import pe.utp.tutorias.service.UsuarioService;

/** Controlador MVC: registro, login y logout (BPM implicito de "alta de usuario"). */
@Controller
public class AuthController {

    private final UsuarioService usuarioService;
    private final UniversidadDAO universidadDAO;

    public AuthController(UsuarioService usuarioService, UniversidadDAO universidadDAO) {
        this.usuarioService = usuarioService;
        this.universidadDAO = universidadDAO;
    }

    @GetMapping("/registro")
    public String formularioRegistro(Model model) {
        model.addAttribute("universidades", universidadDAO.findAll());
        model.addAttribute("tipos", new String[]{TipoUsuario.TUTOR, TipoUsuario.ALUMNO});
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@RequestParam String nombre,
                             @RequestParam String correo,
                             @RequestParam String contrasena,
                             @RequestParam String tipoUsuario,
                             @RequestParam(required = false) Integer idUniversidad,
                             Model model) {
        try {
            usuarioService.registrar(nombre, correo, contrasena, tipoUsuario, idUniversidad);
            model.addAttribute("mensaje", "Cuenta creada correctamente. Ya puedes iniciar sesion.");
            return "login";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("universidades", universidadDAO.findAll());
            model.addAttribute("tipos", new String[]{TipoUsuario.TUTOR, TipoUsuario.ALUMNO});
            return "registro";
        }
    }

    @GetMapping("/login")
    public String formularioLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String correo, @RequestParam String contrasena,
                         HttpSession session, Model model) {
        return usuarioService.autenticar(correo, contrasena)
                .map(usuario -> {
                    session.setAttribute("idUsuarioActual", usuario.getIdUsuario());
                    session.setAttribute("nombreUsuarioActual", usuario.getNombre());
                    session.setAttribute("esTutorActual", usuario.esTutor());
                    session.setAttribute("esAdminActual", usuario.esAdmin());
                    // el administrador entra directo a su dashboard
                    return usuario.esAdmin() ? "redirect:/admin/dashboard" : "redirect:/";
                })
                .orElseGet(() -> {
                    model.addAttribute("error", "Correo o contrasena incorrectos.");
                    return "login";
                });
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    static Usuario usuarioRequerido(HttpSession session, UsuarioService usuarioService) {
        Integer id = (Integer) session.getAttribute("idUsuarioActual");
        if (id == null) {
            throw new IllegalStateException("Debes iniciar sesion.");
        }
        return usuarioService.buscarPorId(id)
                .orElseThrow(() -> new IllegalStateException("Sesion invalida, vuelve a iniciar sesion."));
    }
}
