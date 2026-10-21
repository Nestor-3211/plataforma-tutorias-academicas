package pe.utp.tutorias.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.tutorias.model.Curso;

/**
 * Acceso a la tabla curso.
 */
public interface CursoDAO extends JpaRepository<Curso, Integer> {
}
