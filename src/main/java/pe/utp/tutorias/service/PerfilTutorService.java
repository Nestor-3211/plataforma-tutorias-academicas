package pe.utp.tutorias.service;

import pe.utp.tutorias.model.PerfilTutor;
import pe.utp.tutorias.model.Usuario;

import java.math.BigDecimal;
import java.util.List;

/**
 * Logica de publicacion y busqueda de perfiles de tutor.
 */
public interface PerfilTutorService {

    /**
     * Publica el perfil de un tutor para un curso.
     *
     * @throws IllegalStateException si el usuario no es de tipo Tutor, o ya tiene perfil en ese curso
     */
    PerfilTutor publicarPerfil(Usuario tutor, Integer idCurso, BigDecimal tarifaHora, String disponibilidad);

    /** Usado en "Buscar tutores", con filtros opcionales de tarifa y calificacion. */
    List<PerfilTutor> buscar(String curso, BigDecimal tarifaMax, BigDecimal calificacionMin);

    List<PerfilTutor> misPerfiles(Integer idUsuarioTutor);

    PerfilTutor obtener(Integer idPerfil);
}
