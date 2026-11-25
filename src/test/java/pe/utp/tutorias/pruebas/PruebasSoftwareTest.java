package pe.utp.tutorias.pruebas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.utp.tutorias.dao.*;
import pe.utp.tutorias.model.*;
import pe.utp.tutorias.service.impl.PagoServiceImpl;
import pe.utp.tutorias.service.impl.ReservaServiceImpl;
import pe.utp.tutorias.service.impl.UsuarioServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Informe de pruebas de software: PS-01, PS-02 y PS-03.
 * Los DAO se simulan con Mockito; la logica de los servicios es la real.
 */
class PruebasSoftwareTest {

    /** PS-01: flujo completo reservar -> confirmar -> completar -> pagar. */
    @Test
    @DisplayName("PS-01 Flujo completo: reservar, confirmar, completar y pagar")
    void ps01FlujoCompletoDeUnaSesion() {
        ReservaDAO reservaDAO = mock(ReservaDAO.class);
        PerfilTutorDAO perfilTutorDAO = mock(PerfilTutorDAO.class);
        PagoDAO pagoDAO = mock(PagoDAO.class);
        ReservaServiceImpl reservaService = new ReservaServiceImpl(reservaDAO, perfilTutorDAO);
        PagoServiceImpl pagoService = new PagoServiceImpl(pagoDAO, reservaDAO);

        Usuario tutor = new Usuario();
        tutor.setIdUsuario(1);
        Usuario alumno = new Usuario();
        alumno.setIdUsuario(2);
        PerfilTutor perfil = new PerfilTutor();
        perfil.setIdPerfil(10);
        perfil.setUsuario(tutor);

        when(perfilTutorDAO.findById(10)).thenReturn(Optional.of(perfil));
        when(reservaDAO.existsByPerfilTutor_IdPerfilAndHorarioAndEstadoNot(
                eq10(), any(LocalDateTime.class), eqCancelada())).thenReturn(false);
        // la reserva "guardada" queda disponible para las siguientes operaciones
        when(reservaDAO.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setIdReserva(100);
            when(reservaDAO.findById(100)).thenReturn(Optional.of(r));
            return r;
        });
        when(pagoDAO.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva reserva = reservaService.reservar(alumno, 10, LocalDateTime.now().plusDays(2));
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());

        assertEquals(EstadoReserva.CONFIRMADA, reservaService.confirmar(100, 1).getEstado());
        assertEquals(EstadoReserva.COMPLETADA, reservaService.marcarCompletada(100, 1).getEstado());

        Pago pago = pagoService.registrarPago(100, 2, new BigDecimal("100.00"));
        assertEquals(new BigDecimal("15.00"), pago.getComision());
        assertEquals(new BigDecimal("85.00"), pago.getMontoNeto());
    }

    /** PS-02: el total de ingresos netos del tutor suma sus pagos. */
    @Test
    @DisplayName("PS-02 Total de ingresos netos del tutor")
    void ps02TotalIngresosNetosSumaLosPagos() {
        PagoDAO pagoDAO = mock(PagoDAO.class);
        PagoServiceImpl pagoService = new PagoServiceImpl(pagoDAO, mock(ReservaDAO.class));

        Pago p1 = new Pago();
        p1.setMontoNeto(new BigDecimal("85.00"));
        Pago p2 = new Pago();
        p2.setMontoNeto(new BigDecimal("42.50"));
        when(pagoDAO.findByReserva_PerfilTutor_Usuario_IdUsuarioOrderByFechaPagoDesc(1))
                .thenReturn(List.of(p1, p2));
        when(pagoDAO.findByReserva_PerfilTutor_Usuario_IdUsuarioOrderByFechaPagoDesc(99))
                .thenReturn(List.of());

        assertEquals(new BigDecimal("127.50"), pagoService.totalIngresosNetos(1));
        assertEquals(BigDecimal.ZERO, pagoService.totalIngresosNetos(99));
    }

    /** PS-03: registro de un usuario y posterior inicio de sesion. */
    @Test
    @DisplayName("PS-03 Registro de usuario e inicio de sesion")
    void ps03RegistrarYAutenticarUnUsuario() {
        UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
        TipoUsuarioDAO tipoDAO = mock(TipoUsuarioDAO.class);
        UniversidadDAO universidadDAO = mock(UniversidadDAO.class);
        UsuarioServiceImpl servicio = new UsuarioServiceImpl(
                usuarioDAO, tipoDAO, universidadDAO, new BCryptPasswordEncoder());

        TipoUsuario alumnoTipo = new TipoUsuario(TipoUsuario.ALUMNO);
        Universidad utp = new Universidad();
        when(usuarioDAO.existsByCorreo("luis@utp.edu.pe")).thenReturn(false);
        when(tipoDAO.findByNombreTipo(TipoUsuario.ALUMNO)).thenReturn(Optional.of(alumnoTipo));
        when(universidadDAO.findById(1)).thenReturn(Optional.of(utp));
        when(usuarioDAO.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario creado = servicio.registrar("Luis Alumno", "luis@utp.edu.pe", "Clave123", TipoUsuario.ALUMNO, 1);

        assertEquals("luis@utp.edu.pe", creado.getCorreo());
        assertSame(alumnoTipo, creado.getTipoUsuario());
        assertSame(utp, creado.getUniversidad());

        when(usuarioDAO.findByCorreo("luis@utp.edu.pe")).thenReturn(Optional.of(creado));
        assertTrue(servicio.autenticar("luis@utp.edu.pe", "Clave123").isPresent());
    }

    // ayudantes para que los matchers de Mockito se lean claro
    private static Integer eq10() { return org.mockito.ArgumentMatchers.eq(10); }
    private static EstadoReserva eqCancelada() { return org.mockito.ArgumentMatchers.eq(EstadoReserva.CANCELADA); }
}
