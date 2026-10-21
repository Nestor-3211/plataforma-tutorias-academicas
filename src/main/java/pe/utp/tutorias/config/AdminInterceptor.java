package pe.utp.tutorias.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import pe.utp.tutorias.exception.AccesoDenegadoException;

/**
 * Filtro por rol para todo lo que empieza con /admin (ver WebConfig).
 * Antes de que llegue al controlador revisa la sesion:
 *  - sin sesion iniciada: manda al login (IllegalStateException, que ya
 *    maneja GlobalModelAttributes);
 *  - con sesion pero sin ser administrador: AccesoDenegadoException.
 *
 * Asi la regla "solo el admin entra aqui" esta en un solo lugar y no hay
 * que acordarse de repetirla en cada metodo de AdminController.
 */
public class AdminInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AdminInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("idUsuarioActual") == null) {
            throw new IllegalStateException("Debes iniciar sesion.");
        }
        if (!Boolean.TRUE.equals(session.getAttribute("esAdminActual"))) {
            log.warn("Acceso denegado a {} para el usuario {}", request.getRequestURI(), session.getAttribute("idUsuarioActual"));
            throw new AccesoDenegadoException("Esta seccion es solo para administradores.");
        }
        return true;
    }
}
