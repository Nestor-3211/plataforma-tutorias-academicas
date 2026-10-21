package pe.utp.tutorias.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.utp.tutorias.model.Usuario;

import java.util.Optional;

/**
 * Acceso a la tabla usuario. Los metodos findBy/existsBy los genera
 * Spring Data solo con el nombre del metodo, no hay que implementarlos.
 */
public interface UsuarioDAO extends JpaRepository<Usuario, Integer> {

    /** Lo uso en el login, para buscar por correo en vez de por id. */
    Optional<Usuario> findByCorreo(String correo);

    /** Para validar en el registro si el correo ya esta en uso. */
    boolean existsByCorreo(String correo);
}
