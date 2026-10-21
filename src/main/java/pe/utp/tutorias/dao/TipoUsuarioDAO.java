package pe.utp.tutorias.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.tutorias.model.TipoUsuario;

import java.util.Optional;

/**
 * Acceso a la tabla tipo_usuario (catalogo de roles).
 */
public interface TipoUsuarioDAO extends JpaRepository<TipoUsuario, Integer> {

    /** Para el registro, cuando llega el rol como texto ("Tutor", "Alumno"). */
    Optional<TipoUsuario> findByNombreTipo(String nombreTipo);
}
