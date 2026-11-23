package cl.duoc.serviciotecnico.domain.model

/** Ciclo de vida de una solicitud creada por el cliente. */
enum class EstadoSolicitud(val etiqueta: String) {
    /** Toda solicitud nace aqui. Unico estado en que el cliente puede cancelarla. */
    POR_CONFIRMAR("Por confirmar"),
    /** El administrador creo el ticket y lo asigno a un tecnico. */
    CONFIRMADA("Confirmada"),
    /** El administrador la rechazo con un motivo. */
    RECHAZADA("Rechazada"),
    /** El cliente la anulo antes de ser procesada. */
    CANCELADA("Cancelada");

    val clientePuedeCancelar: Boolean get() = this == POR_CONFIRMAR
}
