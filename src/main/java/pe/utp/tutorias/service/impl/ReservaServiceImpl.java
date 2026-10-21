package pe.utp.tutorias.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.tutorias.dao.PerfilTutorDAO;
import pe.utp.tutorias.dao.ReservaDAO;
import pe.utp.tutorias.model.EstadoReserva;
import pe.utp.tutorias.model.PerfilTutor;
import pe.utp.tutorias.model.Reserva;
import pe.utp.tutorias.model.Usuario;
import pe.utp.tutorias.service.ReservaService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Implementa el proceso BPM "Reservar Sesion": valida disponibilidad de
 * horario antes de crear la reserva, y aplica las transiciones de estado
 * que decide el tutor (confirmar / cancelar).
 */
@Service
public class ReservaServiceImpl implements ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaServiceImpl.class);

    private final ReservaDAO reservaDAO;
    private final PerfilTutorDAO perfilTutorDAO;

    public ReservaServiceImpl(ReservaDAO reservaDAO, PerfilTutorDAO perfilTutorDAO) {
        this.reservaDAO = reservaDAO;
        this.perfilTutorDAO = perfilTutorDAO;
    }

    @Override
    @Transactional
    public Reserva reservar(Usuario alumno, Integer idPerfilTutor, LocalDateTime horario) {
        checkArgument(horario != null && !horario.isBefore(LocalDateTime.now()),
                "El horario de la reserva debe ser una fecha futura.");
        PerfilTutor perfil = perfilTutorDAO.findById(idPerfilTutor)
                .orElseThrow(() -> new NoSuchElementException("Perfil de tutor no encontrado: " + idPerfilTutor));

        checkState(!perfil.getUsuario().getIdUsuario().equals(alumno.getIdUsuario()),
                "Un tutor no puede reservarse una sesion a si mismo.");

        // Gateway BPM "¿Horario disponible?"
        boolean ocupado = reservaDAO.existsByPerfilTutor_IdPerfilAndHorarioAndEstadoNot(
                idPerfilTutor, horario, EstadoReserva.CANCELADA);
        checkState(!ocupado, "El horario seleccionado ya no esta disponible para este tutor.");

        Reserva reserva = new Reserva();
        reserva.setPerfilTutor(perfil);
        reserva.setAlumno(alumno);
        reserva.setHorario(horario);
        reserva.setEstado(EstadoReserva.PENDIENTE);
        Reserva guardada = reservaDAO.save(reserva);
        log.info("Reserva #{} creada: alumno={}, perfil={}, horario={}",
                guardada.getIdReserva(), alumno.getIdUsuario(), idPerfilTutor, horario);
        return guardada;
    }

    @Override
    @Transactional
    public Reserva confirmar(Integer idReserva, Integer idUsuarioTutor) {
        Reserva reserva = obtenerYValidarPropietario(idReserva, idUsuarioTutor);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        log.info("Reserva #{} confirmada por el tutor {}", idReserva, idUsuarioTutor);
        return reservaDAO.save(reserva);
    }

    @Override
    @Transactional
    public Reserva cancelar(Integer idReserva, Integer idUsuarioTutor) {
        Reserva reserva = obtenerYValidarPropietario(idReserva, idUsuarioTutor);
        reserva.setEstado(EstadoReserva.CANCELADA);
        log.info("Reserva #{} cancelada por el tutor {}", idReserva, idUsuarioTutor);
        return reservaDAO.save(reserva);
    }

    @Override
    @Transactional
    public Reserva marcarCompletada(Integer idReserva, Integer idUsuarioTutor) {
        Reserva reserva = obtenerYValidarPropietario(idReserva, idUsuarioTutor);
        checkState(reserva.getEstado() == EstadoReserva.CONFIRMADA,
                "Solo una reserva CONFIRMADA puede marcarse como completada.");
        reserva.setEstado(EstadoReserva.COMPLETADA);
        log.info("Reserva #{} marcada como completada por el tutor {}", idReserva, idUsuarioTutor);
        return reservaDAO.save(reserva);
    }

    private Reserva obtenerYValidarPropietario(Integer idReserva, Integer idUsuarioTutor) {
        Reserva reserva = reservaDAO.findById(idReserva)
                .orElseThrow(() -> new NoSuchElementException("Reserva no encontrada: " + idReserva));
        checkState(reserva.getPerfilTutor().getUsuario().getIdUsuario().equals(idUsuarioTutor),
                "Esta reserva no pertenece a un curso dictado por este tutor.");
        return reserva;
    }

    @Override
    public List<Reserva> misReservasComoAlumno(Integer idUsuario) {
        return reservaDAO.findByAlumno_IdUsuario(idUsuario);
    }

    @Override
    public List<Reserva> misReservasComoTutor(Integer idUsuarioTutor) {
        return reservaDAO.findByPerfilTutor_Usuario_IdUsuario(idUsuarioTutor);
    }

    @Override
    public Reserva obtener(Integer idReserva) {
        return reservaDAO.findById(idReserva)
                .orElseThrow(() -> new NoSuchElementException("Reserva no encontrada: " + idReserva));
    }
}
