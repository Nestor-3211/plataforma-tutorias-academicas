package pe.utp.tutorias.model;

/**
 * Los estados posibles de una reserva, tal como salen en el diagrama
 * BPM de "Reservar Sesion".
 */
public enum EstadoReserva {

    /** Recien creada, esperando que el tutor la confirme. */
    PENDIENTE,

    /** El tutor la acepto. */
    CONFIRMADA,

    /** El tutor la rechazo o alguien la cancelo antes de la sesion. */
    CANCELADA,

    /** Ya se dicto la sesion, desde aqui se puede registrar el pago. */
    COMPLETADA
}
