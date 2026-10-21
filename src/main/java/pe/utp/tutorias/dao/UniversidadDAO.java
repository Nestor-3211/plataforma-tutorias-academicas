package pe.utp.tutorias.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.tutorias.model.Universidad;

/**
 * Acceso a la tabla universidad. Por ahora no necesita busquedas
 * personalizadas, con lo que trae JpaRepository alcanza.
 */
public interface UniversidadDAO extends JpaRepository<Universidad, Integer> {
}
