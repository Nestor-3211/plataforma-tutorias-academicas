package pe.utp.tutorias.service;

import pe.utp.tutorias.model.Pago;

import java.math.BigDecimal;
import java.util.List;

/**
 * Logica del proceso BPM "Registrar pago de sesion": calcula la
 * comision de la plataforma y el monto neto que le queda al tutor.
 */
public interface PagoService {

    /**
     * Registra el pago de una sesion ya completada.
     *
     * @throws IllegalStateException si la reserva no existe, no esta COMPLETADA o ya tiene pago
     */
    Pago registrarPago(Integer idReserva, Integer idUsuarioAlumno, BigDecimal montoTotal);

    /** Historial de pagos hechos por el alumno. */
    List<Pago> historialPorAlumno(Integer idUsuarioAlumno);

    /** Pagos recibidos por el tutor, para la pantalla de ingresos. */
    List<Pago> ingresosPorTutor(Integer idUsuarioTutor);

    /** Suma de los montos netos que ya recibio el tutor. */
    BigDecimal totalIngresosNetos(Integer idUsuarioTutor);

    boolean yaPagada(Integer idReserva);
}
