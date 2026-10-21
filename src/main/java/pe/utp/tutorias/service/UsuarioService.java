package pe.utp.tutorias.service;

import pe.utp.tutorias.model.Usuario;

import java.util.Optional;

/**
 * Logica de registro y login de usuarios.
 */
public interface UsuarioService {

    /**
     * Registra un nuevo usuario (Tutor o Alumno).
     *
     * @throws IllegalArgumentException si el correo ya esta registrado o el rol no es valido
     */
    Usuario registrar(String nombre, String correo, String contrasenaPlana, String nombreTipoUsuario, Integer idUniversidad);

    /** Verifica correo y contrasena. Vacio si no coinciden. */
    Optional<Usuario> autenticar(String correo, String contrasenaPlana);

    Optional<Usuario> buscarPorId(Integer idUsuario);
}
