package cl.duoc.serviciotecnico.testutil

import cl.duoc.serviciotecnico.data.local.dao.EquipoDao
import cl.duoc.serviciotecnico.data.local.dao.FotoGuiaDao
import cl.duoc.serviciotecnico.data.local.dao.GuiaDao
import cl.duoc.serviciotecnico.data.local.dao.SolicitudDao
import cl.duoc.serviciotecnico.data.local.dao.TicketDao
import cl.duoc.serviciotecnico.data.local.entity.Equipo
import cl.duoc.serviciotecnico.data.local.entity.FotoGuia
import cl.duoc.serviciotecnico.data.local.entity.Guia
import cl.duoc.serviciotecnico.data.local.entity.Solicitud
import cl.duoc.serviciotecnico.data.local.entity.Ticket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime

/*
 * DAO falsos (fakes) en memoria.
 *
 * Reemplazan a Room en las pruebas unitarias: guardan las filas en un MutableStateFlow, asi
 * las consultas que devuelven Flow reaccionan a los cambios igual que Room, pero sin SQLite
 * ni dispositivo. Cada fake respeta el contrato de su consulta SQL (filtros y orden).
 */

/** Tabla generica: filas indexadas por id. */
open class TablaEnMemoria<T>(private val idDe: (T) -> String) {
    val filas = MutableStateFlow<Map<String, T>>(emptyMap())
    val todas: List<T> get() = filas.value.values.toList()

    fun guardar(fila: T) = filas.update { it + (idDe(fila) to fila) }
    fun guardarTodas(lista: List<T>) = filas.update { it + lista.associateBy(idDe) }
    fun buscar(id: String): T? = filas.value[id]
    fun <R> observar(transformar: (Collection<T>) -> R): Flow<R> = filas.map { transformar(it.values) }
}

class TicketDaoEnMemoria : TablaEnMemoria<Ticket>({ it.id }), TicketDao {
    override fun observarTodos() = observar { l -> l.sortedByDescending { it.fechaActualizacion } }
    override fun observarPorTecnico(correo: String) =
        observar { l -> l.filter { it.tecnicoCorreo == correo }.sortedByDescending { it.numero } }
    override fun observarPorId(id: String) = observar { l -> l.firstOrNull { it.id == id } }
    override suspend fun obtenerPorId(id: String) = buscar(id)
    override suspend fun ultimoNumero() = todas.maxOfOrNull { it.numero }
    override suspend fun insertar(ticket: Ticket) = guardar(ticket)
    override suspend fun actualizar(ticket: Ticket) = guardar(ticket)
    override suspend fun pendientes() = todas.filter { it.pendienteSync }
    override suspend fun idsPendientes() = pendientes().map { it.id }
    override fun contarPendientes() = observar { l -> l.count { it.pendienteSync } }
    override suspend fun marcarSincronizado(id: String, fechaEnviada: LocalDateTime) {
        val t = buscar(id) ?: return
        if (t.fechaActualizacion == fechaEnviada) guardar(t.copy(pendienteSync = false))
    }
    override suspend fun upsertTodos(tickets: List<Ticket>) = guardarTodas(tickets)
}

class EquipoDaoEnMemoria : TablaEnMemoria<Equipo>({ it.id }), EquipoDao {
    override fun observarPorId(id: String) = observar { l -> l.firstOrNull { it.id == id } }
    override suspend fun obtenerPorId(id: String) = buscar(id)
    override suspend fun buscarPorSerie(serie: String) = todas.firstOrNull { it.serie == serie }
    override suspend fun insertar(equipo: Equipo) = guardar(equipo)
    override suspend fun actualizar(equipo: Equipo) = guardar(equipo)
    override suspend fun pendientes() = todas.filter { it.pendienteSync }
    override suspend fun idsPendientes() = pendientes().map { it.id }
    override fun contarPendientes() = observar { l -> l.count { it.pendienteSync } }
    override suspend fun marcarSincronizado(id: String) {
        buscar(id)?.let { guardar(it.copy(pendienteSync = false)) }
    }
    override suspend fun upsertTodos(equipos: List<Equipo>) = guardarTodas(equipos)
}

class GuiaDaoEnMemoria : TablaEnMemoria<Guia>({ it.id }), GuiaDao {
    override fun observarPorTicket(ticketId: String) =
        observar { l -> l.filter { it.ticketId == ticketId }.sortedByDescending { it.numero } }
    override fun observarPorId(id: String) = observar { l -> l.firstOrNull { it.id == id } }
    override suspend fun contarPorTicket(ticketId: String) = todas.count { it.ticketId == ticketId }
    override suspend fun ultimoNumero() = todas.maxOfOrNull { it.numero }
    override suspend fun insertar(guia: Guia) = guardar(guia)
    override suspend fun pendientes() = todas.filter { it.pendienteSync }
    override suspend fun idsPendientes() = pendientes().map { it.id }
    override fun contarPendientes() = observar { l -> l.count { it.pendienteSync } }
    override suspend fun marcarSincronizado(id: String) {
        buscar(id)?.let { guardar(it.copy(pendienteSync = false)) }
    }
    override suspend fun upsertTodos(guias: List<Guia>) = guardarTodas(guias)
}

class FotoGuiaDaoEnMemoria : TablaEnMemoria<FotoGuia>({ it.id }), FotoGuiaDao {
    override fun observarPorGuia(guiaId: String) =
        observar { l -> l.filter { it.guiaId == guiaId }.sortedBy { it.fechaCaptura } }
    override suspend fun obtenerPorGuia(guiaId: String) =
        todas.filter { it.guiaId == guiaId }.sortedBy { it.fechaCaptura }
    override suspend fun insertarTodas(fotos: List<FotoGuia>) = guardarTodas(fotos)
    override suspend fun upsertTodas(fotos: List<FotoGuia>) = guardarTodas(fotos)
}

class SolicitudDaoEnMemoria : TablaEnMemoria<Solicitud>({ it.id }), SolicitudDao {
    override fun observarDeCliente(correo: String) =
        observar { l -> l.filter { it.clienteCorreo == correo }.sortedByDescending { it.fechaCreacion } }
    override fun observarTodas() = observar { l -> l.sortedByDescending { it.fechaCreacion } }
    override fun observarPorId(id: String) = observar { l -> l.firstOrNull { it.id == id } }
    override suspend fun obtenerPorId(id: String) = buscar(id)
    override suspend fun insertar(solicitud: Solicitud) = guardar(solicitud)
    override suspend fun actualizar(solicitud: Solicitud) = guardar(solicitud)
    override suspend fun pendientes() = todas.filter { it.pendienteSync }
    override suspend fun idsPendientes() = pendientes().map { it.id }
    override fun contarPendientes() = observar { l -> l.count { it.pendienteSync } }
    override suspend fun marcarSincronizado(id: String, fechaEnviada: LocalDateTime) {
        val s = buscar(id) ?: return
        if (s.fechaActualizacion == fechaEnviada) guardar(s.copy(pendienteSync = false))
    }
    override suspend fun upsertTodas(solicitudes: List<Solicitud>) = guardarTodas(solicitudes)
}
