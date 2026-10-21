package pe.utp.tutorias.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.utp.tutorias.exception.AccesoDenegadoException;

import java.util.NoSuchElementException;

/**
 * Expone el usuario de la sesion actual a TODAS las vistas (para el
 * fragmento de navegacion), sin que cada controlador tenga que repetirlo,
 * y centraliza el manejo de errores que no cada controlador captura por su
 * cuenta, para no mostrar nunca la pagina de error generica de Spring
 * (Whitelabel Error Page) al usuario final.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("nombreUsuarioActual")
    public String nombreUsuarioActual(HttpSession session) {
        return (String) session.getAttribute("nombreUsuarioActual");
    }

    @ModelAttribute("esTutorActual")
    public Boolean esTutorActual(HttpSession session) {
        Object valor = session.getAttribute("esTutorActual");
        return valor != null && (Boolean) valor;
    }

    /** Para que los fragmentos de navegacion sepan si mostrar el enlace del dashboard. */
    @ModelAttribute("esAdminActual")
    public Boolean esAdminActual(HttpSession session) {
        Object valor = session.getAttribute("esAdminActual");
        return valor != null && (Boolean) valor;
    }

    /** Alguien con sesion intento entrar a una seccion que no es de su rol (ej. /admin/...). */
    @ExceptionHandler(AccesoDenegadoException.class)
    public String manejarAccesoDenegado(AccesoDenegadoException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/";
    }

    /**
     * Se lanza, por ejemplo, cuando se abre una pagina que requiere sesion
     * (usuarioRequerido) sin haber iniciado sesion, o cuando una regla de
     * negocio impide la accion. En vez del error generico de Spring,
     * redirige a login mostrando el motivo.
     */
    @ExceptionHandler(IllegalStateException.class)
    public String manejarSesionInvalida(IllegalStateException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/login";
    }

    /** Recurso no encontrado (ej. una reserva o un perfil inexistente): vuelve al inicio. */
    @ExceptionHandler(NoSuchElementException.class)
    public String manejarNoEncontrado(NoSuchElementException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/";
    }
}
