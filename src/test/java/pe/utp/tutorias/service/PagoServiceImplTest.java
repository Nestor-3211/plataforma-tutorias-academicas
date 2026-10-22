package pe.utp.tutorias.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.utp.tutorias.dao.PagoDAO;
import pe.utp.tutorias.dao.ReservaDAO;
import pe.utp.tutorias.model.*;
import pe.utp.tutorias.service.impl.PagoServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias (TDD) del proceso BPM "Registrar pago de sesion":
 * validan el calculo de la comision (15%) y las reglas de negocio del
 * gateway (solo se paga una sesion COMPLETADA, y solo una vez).
 */
class PagoServiceImplTest {

    private PagoDAO pagoDAO;
    private ReservaDAO reservaDAO;
    private PagoServiceImpl pagoService;

    private Usuario tutor;
    private Usuario alumno;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        pagoDAO = mock(PagoDAO.class);
        reservaDAO = mock(ReservaDAO.class);
        pagoService = new PagoServiceImpl(pagoDAO, reservaDAO);

        tutor = new Usuario();
        tutor.setIdUsuario(1);

        alumno = new Usuario();
        alumno.setIdUsuario(2);

        PerfilTutor perfil = new PerfilTutor();
        perfil.setIdPerfil(10);
        perfil.setUsuario(tutor);

        reserva = new Reserva();
        reserva.setIdReserva(100);
        reserva.setPerfilTutor(perfil);
        reserva.setAlumno(alumno);
        reserva.setHorario(LocalDateTime.now().minusDays(1));
        reserva.setEstado(EstadoReserva.COMPLETADA);

        when(reservaDAO.findById(100)).thenReturn(Optional.of(reserva));
        when(pagoDAO.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registrarPagoCalculaComisionYMontoNetoCorrectamente() {
        Pago pago = pagoService.registrarPago(100, 2, new BigDecimal("100.00"));

        assertEquals(new BigDecimal("15.00"), pago.getComision());
        assertEquals(new BigDecimal("85.00"), pago.getMontoNeto());
    }

    @Test
    void noSePuedePagarUnaReservaQueNoEstaCompletada() {
        reserva.setEstado(EstadoReserva.CONFIRMADA);

        assertThrows(IllegalStateException.class,
                () -> pagoService.registrarPago(100, 2, new BigDecimal("100.00")));
        verify(pagoDAO, never()).save(any(Pago.class));
    }

    @Test
    void noSePuedePagarDosVecesLaMismaReserva() {
        when(pagoDAO.existsByReserva_IdReserva(100)).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> pagoService.registrarPago(100, 2, new BigDecimal("100.00")));
        verify(pagoDAO, never()).save(any(Pago.class));
    }

    @Test
    void noSePuedePagarUnaReservaDeOtroUsuario() {
        assertThrows(IllegalStateException.class,
                () -> pagoService.registrarPago(100, 999, new BigDecimal("100.00")));
        verify(pagoDAO, never()).save(any(Pago.class));
    }
}
