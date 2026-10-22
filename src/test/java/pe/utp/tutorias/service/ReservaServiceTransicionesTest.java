package pe.utp.tutorias.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.utp.tutorias.dao.PerfilTutorDAO;
import pe.utp.tutorias.dao.ReservaDAO;
import pe.utp.tutorias.model.EstadoReserva;
import pe.utp.tutorias.model.PerfilTutor;
import pe.utp.tutorias.model.Reserva;
import pe.utp.tutorias.model.Usuario;
import pe.utp.tutorias.service.impl.ReservaServiceImpl;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Complementa a ReservaServiceImplTest: cambios de estado que hace el tutor
 * (confirmar, cancelar, completar) y casos de datos inexistentes.
 */
class ReservaServiceTransicionesTest {

    private ReservaDAO reservaDAO;
    private PerfilTutorDAO perfilTutorDAO;
    private ReservaServiceImpl reservaService;

    private Usuario alumno;
    private PerfilTutor perfil;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        reservaDAO = mock(ReservaDAO.class);
        perfilTutorDAO = mock(PerfilTutorDAO.class);
        reservaService = new ReservaServiceImpl(reservaDAO, perfilTutorDAO);

        Usuario tutor = new Usuario();
        tutor.setIdUsuario(1);
        alumno = new Usuario();
        alumno.setIdUsuario(2);
        perfil = new PerfilTutor();
        perfil.setIdPerfil(10);
        perfil.setUsuario(tutor);

        reserva = new Reserva();
        reserva.setIdReserva(100);
        reserva.setPerfilTutor(perfil);
        reserva.setAlumno(alumno);
        reserva.setEstado(EstadoReserva.PENDIENTE);

        when(reservaDAO.findById(100)).thenReturn(Optional.of(reserva));
        when(reservaDAO.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void elTutorDuenoConfirmaLaReserva() {
        assertEquals(EstadoReserva.CONFIRMADA, reservaService.confirmar(100, 1).getEstado());
    }

    @Test
    void elTutorDuenoCancelaLaReserva() {
        assertEquals(EstadoReserva.CANCELADA, reservaService.cancelar(100, 1).getEstado());
    }

    @Test
    void otroTutorNoPuedeConfirmarUnaReservaAjena() {
        assertThrows(IllegalStateException.class, () -> reservaService.confirmar(100, 99));
        verify(reservaDAO, never()).save(any(Reserva.class));
    }

    @Test
    void otroTutorNoPuedeCancelarUnaReservaAjena() {
        assertThrows(IllegalStateException.class, () -> reservaService.cancelar(100, 99));
        verify(reservaDAO, never()).save(any(Reserva.class));
    }

    @Test
    void unaReservaConfirmadaPuedeMarcarseCompletada() {
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        assertEquals(EstadoReserva.COMPLETADA, reservaService.marcarCompletada(100, 1).getEstado());
    }

    @Test
    void unaReservaPendienteNoPuedeMarcarseCompletada() {
        assertThrows(IllegalStateException.class, () -> reservaService.marcarCompletada(100, 1));
        verify(reservaDAO, never()).save(any(Reserva.class));
    }

    @Test
    void noSePuedeReservarSinHorario() {
        assertThrows(IllegalArgumentException.class, () -> reservaService.reservar(alumno, 10, null));
    }

    @Test
    void avisaCuandoElPerfilDelTutorNoExiste() {
        when(perfilTutorDAO.findById(10)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> reservaService.reservar(alumno, 10, LocalDateTime.now().plusDays(1)));
    }

    @Test
    void obtenerAvisaCuandoLaReservaNoExiste() {
        when(reservaDAO.findById(555)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> reservaService.obtener(555));
    }
}
