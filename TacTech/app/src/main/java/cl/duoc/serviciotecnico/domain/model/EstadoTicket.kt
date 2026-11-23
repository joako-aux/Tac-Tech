package cl.duoc.serviciotecnico.domain.model

enum class EstadoTicket(val etiqueta: String) {
    ABIERTO("Abierto"),
    COORDINACION_EN_PROCESO("Coordinacion en proceso"),
    COORDINADO("Coordinado"),
    TRANSITO("Transito"),
    TRABAJO_EN_PROCESO("Trabajo en proceso"),
    PENDIENTE_POR_CLIENTE("Pendiente por cliente"),
    PENDIENTE_POR_TACTECH("Pendiente por TacTech"),
    CERRADO("Cerrado"),
    CANCELADO("Cancelado");

    val esFinal: Boolean
        get() = this == CERRADO || this == CANCELADO

    /** El tecnico esta en camino o trabajando en el equipo. */
    val enProceso: Boolean
        get() = this == TRANSITO || this == TRABAJO_EN_PROCESO

    companion object {
        val intermedios: List<EstadoTicket>
            get() = entries.filterNot { it.esFinal }
    }
}
