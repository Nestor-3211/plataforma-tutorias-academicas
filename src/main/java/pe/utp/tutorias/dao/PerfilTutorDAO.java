package pe.utp.tutorias.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.utp.tutorias.model.PerfilTutor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Acceso a la tabla perfil_tutor.
 */
public interface PerfilTutorDAO extends JpaRepository<PerfilTutor, Integer> {

    /** Perfiles publicados por un tutor (pantalla "Mis perfiles"). */
    List<PerfilTutor> findByUsuario_IdUsuario(Integer idUsuario);

    /** Para no dejar que un tutor publique dos perfiles del mismo curso. */
    boolean existsByUsuario_IdUsuarioAndCurso_IdCurso(Integer idUsuario, Integer idCurso);

    /**
     * Busqueda de tutores para la pantalla "Buscar tutores": filtra por
     * nombre de curso (parcial, sin importar mayusculas) y, si se manda,
     * por tarifa maxima y calificacion minima. Use @Query aqui porque
     * el filtro tiene varias condiciones opcionales y ya no me alcanzaba
     * con un metodo derivado del nombre.
     */
    @Query("""
           SELECT p FROM PerfilTutor p
           WHERE LOWER(p.curso.nombre) LIKE LOWER(CONCAT('%', :curso, '%'))
             AND (:tarifaMax IS NULL OR p.tarifaHora <= :tarifaMax)
             AND (:calificacionMin IS NULL OR p.calificacionProm >= :calificacionMin)
           ORDER BY p.calificacionProm DESC
           """)
    List<PerfilTutor> buscar(@Param("curso") String curso,
                              @Param("tarifaMax") BigDecimal tarifaMax,
                              @Param("calificacionMin") BigDecimal calificacionMin);
}
