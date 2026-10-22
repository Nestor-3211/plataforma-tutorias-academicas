package pe.utp.tutorias.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.utp.tutorias.dao.PerfilTutorDAO;
import pe.utp.tutorias.dao.ReservaDAO;
import pe.utp.tutorias.model.*;
import pe.utp.tutorias.service.impl.ReservaServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias (TDD) del proceso BPM "Reservar Sesion": validan la
 * regla de negocio del gateway "Horario disponible?" implementada en
 * ReservaServiceImpl, sin necesidad de una base de datos real.
 */
class ReservaServiceImplTest {

    private ReservaDAO reservaDAO;
    private PerfilTutorDAO perfilTutorDAO;
    private ReservaServiceImpl reservaService;

    private Usuario alumno;
    private Usuario tutor;
    private PerfilTutor perfil;

    @BeforeEach
    void setUp() {
        reservaDAO = mock(ReservaDAO.class);
        perfilTutorDAO = mock(PerfilTutorDAO.class);
        reservaService = new ReservaServiceImpl(reservaDAO, perfilTutorDAO);

        tutor = new Usuario();
        tutor.setIdUsuario(1);
        tutor.setNombre("Ana Tutor");

        alumno = new Usuario();
        alumno.setIdUsuario(2);
        alumno.setNombre("Luis Alumno");

        perfil = new PerfilTutor();
        perfil.setIdPerfil(10);
        perfil.setUsuario(tutor);

        when(perfilTutorDAO.findById(10)).thenReturn(Optional.of(perfil));
        when(reservaDAO.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reservarConHorarioLibreCreaLaReservaEnEstadoPendiente() {
        LocalDateTime horario = LocalDateTime.now().plusDays(1);
        when(reservaDAO.existsByPerfilTutor_IdPerfilAndHorarioAndEstadoNot(10, horario, EstadoReserva.CANCELADA))
                .thenReturn(false);

        Reserva creada = reservaService.reservar(alumno, 10, horario);

        assertEquals(EstadoReserva.PENDIENTE, creada.getEstado());
        assertEquals(alumno, creada.getAlumno());
        verify(reservaDAO, times(1)).save(any(Reserva.class));
    }

    @Test
    void reservarConHorarioYaOcupadoLanzaExcepcion() {
        LocalDateTime horario = LocalDateTime.now().plusDays(1);
        when(reservaDAO.existsByPerfilTutor_IdPerfilAndHorarioAndEstadoNot(10, horario, EstadoReserva.CANCELADA))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> reservaService.reservar(alumno, 10, horario));
        verify(reservaDAO, never()).save(any(Reserva.class));
    }

    @Test
    void reservarConHorarioEnElPasadoLanzaExcepcion() {
        LocalDateTime horarioPasado = LocalDateTime.now().minusDays(1);

        assertThrows(IllegalArgumentException.class, () -> reservaService.reservar(alumno, 10, horarioPasado));
        verify(reservaDAO, never()).save(any(Reserva.class));
    }

    @Test
    void tutorNoPuedeReservarseASiMismo() {
        LocalDateTime horario = LocalDateTime.now().plusDays(1);

        assertThrows(IllegalStateException.class, () -> reservaService.reservar(tutor, 10, horario));
        verify(reservaDAO, never()).save(any(Reserva.class));
    }
}
