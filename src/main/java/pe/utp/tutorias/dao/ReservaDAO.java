package pe.utp.tutorias.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.tutorias.model.EstadoReserva;
import pe.utp.tutorias.model.Reserva;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Acceso a la tabla reserva.
 */
public interface ReservaDAO extends JpaRepository<Reserva, Integer> {

    /** Para la pantalla "Mis reservas" del alumno. */
    List<Reserva> findByAlumno_IdUsuario(Integer idUsuario);

    /** Para "Reservas recibidas" del tutor. */
    List<Reserva> findByPerfilTutor_Usuario_IdUsuario(Integer idUsuarioTutor);

    /**
     * Este es el que revisa la regla "¿Horario disponible?" del BPM de
     * reservar sesion: si el tutor ya tiene una reserva (que no este
     * cancelada) exactamente en ese horario, no se puede reservar de nuevo.
     */
    boolean existsByPerfilTutor_IdPerfilAndHorarioAndEstadoNot(
            Integer idPerfil, LocalDateTime horario, EstadoReserva estadoExcluido);
}
