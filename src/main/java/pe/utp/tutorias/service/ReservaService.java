package pe.utp.tutorias.service;

import pe.utp.tutorias.model.Reserva;
import pe.utp.tutorias.model.Usuario;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Logica del proceso BPM "Reservar Sesion": crear la reserva y las
 * transiciones de estado que decide el tutor.
 */
public interface ReservaService {

    /** Crea la reserva. Valida que el horario este libre antes de guardar. */
    Reserva reservar(Usuario alumno, Integer idPerfilTutor, LocalDateTime horario);

    /** El tutor confirma la sesion. */
    Reserva confirmar(Integer idReserva, Integer idUsuarioTutor);

    /** El tutor cancela la sesion. */
    Reserva cancelar(Integer idReserva, Integer idUsuarioTutor);

    /**
     * El tutor marca la sesion como dictada. Con esto el alumno ya
     * puede registrar el pago.
     */
    Reserva marcarCompletada(Integer idReserva, Integer idUsuarioTutor);

    List<Reserva> misReservasComoAlumno(Integer idUsuario);

    List<Reserva> misReservasComoTutor(Integer idUsuarioTutor);

    Reserva obtener(Integer idReserva);
}
