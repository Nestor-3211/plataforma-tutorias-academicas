package pe.utp.tutorias.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.utp.tutorias.dao.PagoDAO;
import pe.utp.tutorias.dao.ReservaDAO;
import pe.utp.tutorias.model.EstadoReserva;
import pe.utp.tutorias.model.Pago;
import pe.utp.tutorias.model.Reserva;
import pe.utp.tutorias.model.Usuario;
import pe.utp.tutorias.service.impl.PagoServiceImpl;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Complementa a PagoServiceImplTest: validaciones del monto, redondeo de la
 * comision y reserva inexistente.
 */
class PagoServiceMontosTest {

    private PagoDAO pagoDAO;
    private ReservaDAO reservaDAO;
    private PagoServiceImpl pagoService;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        pagoDAO = mock(PagoDAO.class);
        reservaDAO = mock(ReservaDAO.class);
        pagoService = new PagoServiceImpl(pagoDAO, reservaDAO);

        Usuario alumno = new Usuario();
        alumno.setIdUsuario(2);
        reserva = new Reserva();
        reserva.setIdReserva(100);
        reserva.setAlumno(alumno);
        reserva.setEstado(EstadoReserva.COMPLETADA);
    }

    @Test
    void laComisionSeRedondeaADosDecimales() {
        when(reservaDAO.findById(100)).thenReturn(Optional.of(reserva));
        when(pagoDAO.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Pago pago = pagoService.registrarPago(100, 2, new BigDecimal("33.33"));

        // 33.33 * 0.15 = 4.9995 -> 5.00
        assertEquals(new BigDecimal("5.00"), pago.getComision());
        assertEquals(new BigDecimal("28.33"), pago.getMontoNeto());
    }

    @Test
    void noSePermiteUnMontoCeroONegativo() {
        assertThrows(IllegalArgumentException.class,
                () -> pagoService.registrarPago(100, 2, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> pagoService.registrarPago(100, 2, new BigDecimal("-5")));
        verify(pagoDAO, never()).save(any(Pago.class));
    }

    @Test
    void avisaCuandoLaReservaNoExiste() {
        when(reservaDAO.findById(100)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> pagoService.registrarPago(100, 2, new BigDecimal("80.00")));
        verify(pagoDAO, never()).save(any(Pago.class));
    }
}
