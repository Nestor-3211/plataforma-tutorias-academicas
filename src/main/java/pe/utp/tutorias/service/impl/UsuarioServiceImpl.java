package pe.utp.tutorias.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.tutorias.dao.TipoUsuarioDAO;
import pe.utp.tutorias.dao.UniversidadDAO;
import pe.utp.tutorias.dao.UsuarioDAO;
import pe.utp.tutorias.model.TipoUsuario;
import pe.utp.tutorias.model.Usuario;
import pe.utp.tutorias.service.UsuarioService;

import java.util.Optional;

/**
 * Implementacion del servicio de Usuario. Depende de las DAO por
 * interfaz (inversion de dependencias - SOLID) y nunca de JPA directamente
 * fuera de la capa dao.
 */
@Service
public class UsuarioServiceImpl implements UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioServiceImpl.class);

    private final UsuarioDAO usuarioDAO;
    private final TipoUsuarioDAO tipoUsuarioDAO;
    private final UniversidadDAO universidadDAO;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioDAO usuarioDAO, TipoUsuarioDAO tipoUsuarioDAO,
                               UniversidadDAO universidadDAO, PasswordEncoder passwordEncoder) {
        this.usuarioDAO = usuarioDAO;
        this.tipoUsuarioDAO = tipoUsuarioDAO;
        this.universidadDAO = universidadDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Usuario registrar(String nombre, String correo, String contrasenaPlana,
                              String nombreTipoUsuario, Integer idUniversidad) {
        // Commons Lang3:
        // normalizeSpace
        String nombreLimpio = StringUtils.normalizeSpace(nombre);
        String correoLimpio = StringUtils.lowerCase(StringUtils.trim(correo));
        if (StringUtils.isAnyBlank(nombreLimpio, correoLimpio, contrasenaPlana)) {
            throw new IllegalArgumentException("Nombre, correo y contrasena son obligatorios.");
        }

        if (usuarioDAO.existsByCorreo(correoLimpio)) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con ese correo.");
        }
        TipoUsuario tipo = tipoUsuarioDAO.findByNombreTipo(nombreTipoUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de usuario invalido: " + nombreTipoUsuario));

        Usuario usuario = new Usuario();
        usuario.setNombre(nombreLimpio);
        usuario.setCorreo(correoLimpio);
        // Seguridad: jamas se persiste la contrasena en texto plano.
        usuario.setContrasenaHash(passwordEncoder.encode(contrasenaPlana));
        usuario.setTipoUsuario(tipo);
        if (idUniversidad != null) {
            universidadDAO.findById(idUniversidad).ifPresent(usuario::setUniversidad);
        }
        Usuario guardado = usuarioDAO.save(usuario);
        log.info("Usuario registrado: id={}, tipo={}", guardado.getIdUsuario(), nombreTipoUsuario);
        return guardado;
    }

    @Override
    public Optional<Usuario> autenticar(String correo, String contrasenaPlana) {
        // Commons Lang3:
        // isAnyBlank
        String correoLimpio = StringUtils.lowerCase(StringUtils.trim(correo));
        if (StringUtils.isAnyBlank(correoLimpio, contrasenaPlana)) {
            return Optional.empty();
        }
        return usuarioDAO.findByCorreo(correoLimpio)
                .filter(u -> passwordEncoder.matches(contrasenaPlana, u.getContrasenaHash()));
    }

    @Override
    public Optional<Usuario> buscarPorId(Integer idUsuario) {
        return usuarioDAO.findById(idUsuario);
    }
}
