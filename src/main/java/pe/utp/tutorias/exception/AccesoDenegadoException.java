package pe.utp.tutorias.exception;

/**
 * Se lanza cuando un usuario con sesion iniciada intenta entrar a una
 * seccion que no es para su rol (por ejemplo, un alumno que escribe
 * /admin/dashboard en la barra del navegador). GlobalModelAttributes la
 * captura y lo manda al inicio con un mensaje.
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
