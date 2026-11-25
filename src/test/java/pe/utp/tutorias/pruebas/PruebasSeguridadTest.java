package pe.utp.tutorias.pruebas;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
// AJUSTA este import al paquete real de tu AdminInterceptor (config, security, web...)
import pe.utp.tutorias.config.AdminInterceptor;
import pe.utp.tutorias.dao.*;
import pe.utp.tutorias.model.*;
import pe.utp.tutorias.service.impl.UsuarioServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Informe de pruebas de seguridad: PSEG-01, PSEG-02 y PSEG-03.
 * PSEG-01 y PSEG-02 prueban las contrasenas (BCrypt); PSEG-03 prueba el
 * control de acceso por rol de AdminInterceptor.
 */
class PruebasSeguridadTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private UsuarioServiceImpl servicio(UsuarioDAO usuarioDAO) {
        TipoUsuarioDAO tipoDAO = mock(TipoUsuarioDAO.class);
        when(tipoDAO.findByNombreTipo(TipoUsuario.ALUMNO))
                .thenReturn(Optional.of(new TipoUsuario(TipoUsuario.ALUMNO)));
        return new UsuarioServiceImpl(usuarioDAO, tipoDAO, mock(UniversidadDAO.class), encoder);
    }

    /** PSEG-01: la contrasena nunca se guarda en texto plano. */
    @Test
    @DisplayName("PSEG-01 La contrasena se guarda como hash BCrypt")
    void pseg01ContrasenaSeGuardaComoHash() {
        UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
        when(usuarioDAO.existsByCorreo(any())).thenReturn(false);
        when(usuarioDAO.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        UsuarioServiceImpl servicio = servicio(usuarioDAO);

        servicio.registrar("Ana", "ana@utp.edu.pe", "Clave123", TipoUsuario.ALUMNO, null);
        servicio.registrar("Beto", "beto@utp.edu.pe", "Clave123", TipoUsuario.ALUMNO, null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioDAO, times(2)).save(captor.capture());
        String hash1 = captor.getAllValues().get(0).getContrasenaHash();
        String hash2 = captor.getAllValues().get(1).getContrasenaHash();

        assertNotEquals("Clave123", hash1);              // no es texto plano
        assertTrue(hash1.startsWith("$2"));              // formato BCrypt
        assertNotEquals(hash1, hash2);                   // salt distinto por usuario
        assertTrue(encoder.matches("Clave123", hash1));  // pero verifica bien
    }

    /** PSEG-02: credenciales incorrectas o con payload SQL no autentican. */
    @Test
    @DisplayName("PSEG-02 Credenciales incorrectas no inician sesion")
    void pseg02CredencialesIncorrectasSonRechazadas() {
        UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
        Usuario usuario = new Usuario();
        usuario.setCorreo("ana@utp.edu.pe");
        usuario.setContrasenaHash(encoder.encode("Clave123"));
        when(usuarioDAO.findByCorreo("ana@utp.edu.pe")).thenReturn(Optional.of(usuario));
        when(usuarioDAO.findByCorreo("no@existe.com")).thenReturn(Optional.empty());
        UsuarioServiceImpl servicio = servicio(usuarioDAO);

        assertTrue(servicio.autenticar("ana@utp.edu.pe", "Clave123").isPresent());       // control: la correcta entra
        assertTrue(servicio.autenticar("ana@utp.edu.pe", "otraClave").isEmpty());        // clave equivocada
        assertTrue(servicio.autenticar("no@existe.com", "Clave123").isEmpty());          // correo inexistente
        assertTrue(servicio.autenticar("ana@utp.edu.pe", "' OR '1'='1").isEmpty());      // payload SQL como clave
    }

    /** PSEG-03: solo el administrador abre las rutas /admin. */
    @Test
    @DisplayName("PSEG-03 Control de acceso por rol en AdminInterceptor")
    void pseg03SoloElAdministradorAccedeARutasAdmin() throws Exception {
        AdminInterceptor interceptor = new AdminInterceptor();
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        // a) sin sesion iniciada
        MockHttpServletRequest sinSesion = new MockHttpServletRequest("GET", "/admin/dashboard");
        assertThrows(IllegalStateException.class,
                () -> interceptor.preHandle(sinSesion, respuesta, new Object()));

        // b) alumno o tutor con sesion pero sin rol de administrador
        MockHttpServletRequest comoAlumno = new MockHttpServletRequest("GET", "/admin/dashboard");
        HttpSession sesionAlumno = new MockHttpSession();
        sesionAlumno.setAttribute("idUsuarioActual", 2);
        sesionAlumno.setAttribute("esAdminActual", false);
        comoAlumno.setSession(sesionAlumno);
        RuntimeException denegado = assertThrows(RuntimeException.class,
                () -> interceptor.preHandle(comoAlumno, respuesta, new Object()));
        assertEquals("AccesoDenegadoException", denegado.getClass().getSimpleName());

        // c) administrador: pasa
        MockHttpServletRequest comoAdmin = new MockHttpServletRequest("GET", "/admin/dashboard");
        HttpSession sesionAdmin = new MockHttpSession();
        sesionAdmin.setAttribute("idUsuarioActual", 1);
        sesionAdmin.setAttribute("esAdminActual", true);
        comoAdmin.setSession(sesionAdmin);
        assertTrue(interceptor.preHandle(comoAdmin, respuesta, new Object()));
    }
}
