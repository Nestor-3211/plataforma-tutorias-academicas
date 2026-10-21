package pe.utp.tutorias.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import pe.utp.tutorias.dao.CursoDAO;
import pe.utp.tutorias.dao.TipoUsuarioDAO;
import pe.utp.tutorias.dao.UniversidadDAO;
import pe.utp.tutorias.dao.UsuarioDAO;
import pe.utp.tutorias.model.Curso;
import pe.utp.tutorias.model.TipoUsuario;
import pe.utp.tutorias.model.Universidad;
import pe.utp.tutorias.service.UsuarioService;

/**
 * Carga los datos catalogo minimos (roles, universidad piloto y cursos
 * de ejemplo) al arrancar la aplicacion, si aun no existen.
 * Tambien crea la cuenta de administrador, con los datos que se ponen en
 * application.properties (nunca escritos aqui en el codigo).
 */
@Configuration
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final TipoUsuarioDAO tipoUsuarioDAO;
    private final UniversidadDAO universidadDAO;
    private final CursoDAO cursoDAO;
    private final UsuarioDAO usuarioDAO;
    private final UsuarioService usuarioService;
    private final String adminNombre;
    private final String adminCorreo;
    private final String adminContrasena;

    public DataSeeder(TipoUsuarioDAO tipoUsuarioDAO, UniversidadDAO universidadDAO, CursoDAO cursoDAO,
                      UsuarioDAO usuarioDAO, UsuarioService usuarioService,
                      @Value("${tutorias.admin.nombre:Administrador}") String adminNombre,
                      @Value("${tutorias.admin.correo:}") String adminCorreo,
                      @Value("${tutorias.admin.contrasena:}") String adminContrasena) {
        this.tipoUsuarioDAO = tipoUsuarioDAO;
        this.universidadDAO = universidadDAO;
        this.cursoDAO = cursoDAO;
        this.usuarioDAO = usuarioDAO;
        this.usuarioService = usuarioService;
        this.adminNombre = adminNombre;
        this.adminCorreo = adminCorreo;
        this.adminContrasena = adminContrasena;
    }

    @Override
    public void run(String... args) {
        if (tipoUsuarioDAO.count() == 0) {
            tipoUsuarioDAO.save(new TipoUsuario(TipoUsuario.TUTOR));
            tipoUsuarioDAO.save(new TipoUsuario(TipoUsuario.ALUMNO));
            tipoUsuarioDAO.save(new TipoUsuario(TipoUsuario.ADMINISTRADOR));
        }
        if (universidadDAO.count() == 0) {
            universidadDAO.save(new Universidad("Universidad Tecnologica del Peru", "UTP", "Peru"));
            universidadDAO.save(new Universidad("Pontificia Universidad Catolica del Peru", "PUCP", "Peru"));
            universidadDAO.save(new Universidad("Universidad Nacional Mayor de San Marcos", "UNMSM", "Peru"));
            universidadDAO.save(new Universidad("Universidad Nacional de Ingenieria", "UNI", "Peru"));
            universidadDAO.save(new Universidad("Universidad de Lima", "ULIMA", "Peru"));
            universidadDAO.save(new Universidad("Universidad Peruana de Ciencias Aplicadas", "UPC", "Peru"));
            universidadDAO.save(new Universidad("Universidad San Ignacio de Loyola", "USIL", "Peru"));
            universidadDAO.save(new Universidad("Universidad del Pacifico", "UP", "Peru"));
            universidadDAO.save(new Universidad("Universidad Esan", "ESAN", "Peru"));
            universidadDAO.save(new Universidad("Universidad Cesar Vallejo", "UCV", "Peru"));
            universidadDAO.save(new Universidad("Universidad Nacional de Trujillo", "UNITRU", "Peru"));
            universidadDAO.save(new Universidad("Universidad Nacional de San Agustin de Arequipa", "UNSA", "Peru"));
            universidadDAO.save(new Universidad("Universidad Continental", "UC", "Peru"));
            universidadDAO.save(new Universidad("Universidad Catolica San Pablo", "UCSP", "Peru"));
            universidadDAO.save(new Universidad("Universidad Ricardo Palma", "URP", "Peru"));
        }
        if (cursoDAO.count() == 0) {
            cursoDAO.save(new Curso("Matematica I", "Ciencias basicas"));
            cursoDAO.save(new Curso("Programacion Orientada a Objetos", "Ingenieria de Software"));
            cursoDAO.save(new Curso("Base de Datos", "Ingenieria de Software"));
            cursoDAO.save(new Curso("Contabilidad General", "Administracion"));
        }
        crearAdministrador();
    }

    /**
     * Crea el administrador solo si hay correo y contrasena configurados y
     * si ese correo todavia no existe. Pasa por UsuarioService.registrar,
     * asi la contrasena tambien se guarda con BCrypt.
     */
    private void crearAdministrador() {
        if (StringUtils.isAnyBlank(adminCorreo, adminContrasena)) {
            log.warn("No se creo el administrador: faltan tutorias.admin.correo y tutorias.admin.contrasena en application.properties");
            return;
        }
        String correo = StringUtils.lowerCase(StringUtils.trim(adminCorreo));
        if (usuarioDAO.existsByCorreo(correo)) {
            return;
        }
        usuarioService.registrar(adminNombre, correo, adminContrasena, TipoUsuario.ADMINISTRADOR, null);
        log.info("Administrador creado: {}", correo);
    }
}
