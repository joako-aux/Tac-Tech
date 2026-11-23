package cl.duoc.serviciotecnico.data.repository

import cl.duoc.serviciotecnico.data.local.dao.GuiaDao
import cl.duoc.serviciotecnico.data.local.dao.TicketDao
import cl.duoc.serviciotecnico.data.local.entity.Ticket
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Toda escritura queda primero en Room con pendienteSync = true.
 * SincronizacionRepository se encarga de enviarla al backend cuando haya conexion.
 */
class TicketRepository(
    private val ticketDao: TicketDao,
    private val guiaDao: GuiaDao
) {
    // Los tickets los crea el administrador en el servidor al confirmar una solicitud;
    // llegan al telefono por la sincronizacion.

    /** Todos los tickets: vista del administrador. */
    fun observarTickets(): Flow<List<Ticket>> = ticketDao.observarTodos()

    /** Solo los asignados al tecnico con sesion. */
    fun observarTicketsDe(correo: String): Flow<List<Ticket>> = ticketDao.observarPorTecnico(correo)

    fun observarTicket(id: String): Flow<Ticket?> = ticketDao.observarPorId(id)

    suspend fun obtenerTicket(id: String): Ticket? = ticketDao.obtenerPorId(id)

    suspend fun actualizar(ticket: Ticket) {
        ticketDao.actualizar(ticket.copy(fechaActualizacion = LocalDateTime.now(), pendienteSync = true))
    }

    /**
     * Aplica un cambio de estado. Si el nuevo estado es final (Cerrado o Cancelado) se
     * registra la fecha de cierre; si es Cerrado, el comentario queda como la solucion.
     * Devuelve false si la regla de negocio lo impide (cerrar sin guias).
     */
    suspend fun cambiarEstado(ticketId: String, nuevoEstado: EstadoTicket, comentario: String): Boolean {
        val ticket = ticketDao.obtenerPorId(ticketId) ?: return false
        if (!ticket.editable) return false
        if (nuevoEstado == EstadoTicket.CERRADO && guiaDao.contarPorTicket(ticketId) == 0) return false

        val ahora = LocalDateTime.now()
        ticketDao.actualizar(
            ticket.copy(
                estado = nuevoEstado,
                comentarioEstado = comentario,
                fechaActualizacion = ahora,
                fechaCierre = if (nuevoEstado.esFinal) ahora else ticket.fechaCierre,
                solucion = if (nuevoEstado == EstadoTicket.CERRADO) comentario else ticket.solucion,
                pendienteSync = true
            )
        )
        return true
    }
}
