package pe.utp.tutorias.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.tutorias.dao.PagoDAO;
import pe.utp.tutorias.dao.ReservaDAO;
import pe.utp.tutorias.model.EstadoReserva;
import pe.utp.tutorias.model.Pago;
import pe.utp.tutorias.model.Reserva;
import pe.utp.tutorias.service.PagoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Implementa el proceso BPM "Registrar pago de sesion": solo se puede pagar
 * una reserva COMPLETADA, una unica vez, y la plataforma retiene una
 * comision fija sobre el monto pagado por el alumno.
 */
@Service
public class PagoServiceImpl implements PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoServiceImpl.class);

    /** Comision de la plataforma sobre cada sesion pagada (15%). */
    private static final BigDecimal PORCENTAJE_COMISION = new BigDecimal("0.15");

    private final PagoDAO pagoDAO;
    private final ReservaDAO reservaDAO;

    public PagoServiceImpl(PagoDAO pagoDAO, ReservaDAO reservaDAO) {
        this.pagoDAO = pagoDAO;
        this.reservaDAO = reservaDAO;
    }

    @Override
    @Transactional
    public Pago registrarPago(Integer idReserva, Integer idUsuarioAlumno, BigDecimal montoTotal) {
        // Guava Preconditions:
        // (checkArgument -> IllegalArgumentException, checkState -> IllegalStateException)
        checkArgument(montoTotal != null && montoTotal.signum() > 0, "El monto a pagar debe ser mayor a 0.");
        Reserva reserva = reservaDAO.findById(idReserva)
                .orElseThrow(() -> new NoSuchElementException("Reserva no encontrada: " + idReserva));

        checkState(reserva.getAlumno().getIdUsuario().equals(idUsuarioAlumno),
                "Esta reserva no pertenece al usuario que intenta pagarla.");
        checkState(reserva.getEstado() == EstadoReserva.COMPLETADA,
                "Solo se puede pagar una sesion que el tutor ya marco como completada.");
        checkState(!pagoDAO.existsByReserva_IdReserva(idReserva),
                "Esta reserva ya tiene un pago registrado.");

        BigDecimal comision = montoTotal.multiply(PORCENTAJE_COMISION).setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoNeto = montoTotal.subtract(comision).setScale(2, RoundingMode.HALF_UP);

        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setMontoTotal(montoTotal);
        pago.setComision(comision);
        pago.setMontoNeto(montoNeto);
        Pago guardado = pagoDAO.save(pago);
        log.info("Pago #{} registrado para la reserva #{}: total={}, comision={}, neto tutor={}",
                guardado.getIdPago(), idReserva, montoTotal, comision, montoNeto);
        return guardado;
    }

    @Override
    public List<Pago> historialPorAlumno(Integer idUsuarioAlumno) {
        return pagoDAO.findByReserva_Alumno_IdUsuarioOrderByFechaPagoDesc(idUsuarioAlumno);
    }

    @Override
    public List<Pago> ingresosPorTutor(Integer idUsuarioTutor) {
        return pagoDAO.findByReserva_PerfilTutor_Usuario_IdUsuarioOrderByFechaPagoDesc(idUsuarioTutor);
    }

    @Override
    public BigDecimal totalIngresosNetos(Integer idUsuarioTutor) {
        return ingresosPorTutor(idUsuarioTutor).stream()
                .map(Pago::getMontoNeto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean yaPagada(Integer idReserva) {
        return pagoDAO.existsByReserva_IdReserva(idReserva);
    }
}
