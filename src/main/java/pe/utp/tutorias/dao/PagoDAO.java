package pe.utp.tutorias.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.tutorias.model.Pago;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a la tabla pago.
 */
public interface PagoDAO extends JpaRepository<Pago, Integer> {

    Optional<Pago> findByReserva_IdReserva(Integer idReserva);

    /** Para no dejar pagar dos veces la misma reserva. */
    boolean existsByReserva_IdReserva(Integer idReserva);

    /** Historial de pagos del alumno (lo que pago por sus sesiones). */
    List<Pago> findByReserva_Alumno_IdUsuarioOrderByFechaPagoDesc(Integer idUsuarioAlumno);

    /** Ingresos del tutor (lo que le corresponde, ya descontada la comision). */
    List<Pago> findByReserva_PerfilTutor_Usuario_IdUsuarioOrderByFechaPagoDesc(Integer idUsuarioTutor);
}
