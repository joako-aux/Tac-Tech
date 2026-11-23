package cl.duoc.serviciotecnico.data.repository

import cl.duoc.serviciotecnico.data.local.dao.SolicitudDao
import cl.duoc.serviciotecnico.data.local.entity.Solicitud
import cl.duoc.serviciotecnico.data.remote.ApiService
import cl.duoc.serviciotecnico.data.remote.Resultado
import cl.duoc.serviciotecnico.data.remote.dto.ConfirmarSolicitudRequest
import cl.duoc.serviciotecnico.data.remote.dto.MotivoRequest
import cl.duoc.serviciotecnico.data.remote.dto.aEntidad
import cl.duoc.serviciotecnico.data.remote.llamar
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.Prioridad
import cl.duoc.serviciotecnico.domain.model.Propiedad
import cl.duoc.serviciotecnico.domain.model.Usuario
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Solicitudes de servicio.
 * - Cliente: crea y cancela offline (Room + pendienteSync); se envian al sincronizar.
 * - Administrador: confirmar y rechazar se hacen directo en el servidor, porque deben
 *   ser atomicas (crear ticket + equipo + cambiar la solicitud en una sola transaccion).
 */
class SolicitudRepository(
    private val solicitudDao: SolicitudDao,
    private val api: ApiService
) {
    fun observarDeCliente(correo: String): Flow<List<Solicitud>> = solicitudDao.observarDeCliente(correo)

    fun observarTodas(): Flow<List<Solicitud>> = solicitudDao.observarTodas()

    fun observarSolicitud(id: String): Flow<Solicitud?> = solicitudDao.observarPorId(id)

    /** Toda solicitud nueva nace en POR_CONFIRMAR. */
    suspend fun crear(
        cliente: Usuario,
        titulo: String,
        descripcion: String,
        direccion: String,
        equipoNombre: String,
        equipoModelo: String,
        equipoSerie: String,
        propiedad: Propiedad
    ): Solicitud {
        val solicitud = Solicitud(
            clienteCorreo = cliente.correo,
            clienteNombre = cliente.nombre,
            empresaNombre = cliente.empresaNombre,
            titulo = titulo,
            descripcion = descripcion,
            direccion = direccion,
            equipoNombre = equipoNombre,
            equipoModelo = equipoModelo,
            equipoSerie = equipoSerie,
            propiedad = propiedad,
            estado = EstadoSolicitud.POR_CONFIRMAR,
            pendienteSync = true
        )
        solicitudDao.insertar(solicitud)
        return solicitud
    }

    /**
     * El cliente anula su solicitud. Solo se permite mientras siga POR_CONFIRMAR.
     * Si el administrador ya la proceso en el servidor, al sincronizar gana el servidor (409).
     */
    suspend fun cancelar(id: String, motivo: String): Boolean {
        val solicitud = solicitudDao.obtenerPorId(id) ?: return false
        if (!solicitud.estado.clientePuedeCancelar) return false
        solicitudDao.actualizar(
            solicitud.copy(
                estado = EstadoSolicitud.CANCELADA,
                motivo = motivo,
                fechaActualizacion = LocalDateTime.now(),
                pendienteSync = true
            )
        )
        return true
    }

    /** Administrador: crea el ticket formal y lo asigna. Devuelve el numero del ticket creado. */
    suspend fun confirmar(id: String, tecnicoCorreo: String, prioridad: Prioridad): Resultado<Int> {
        val r = llamar { api.confirmarSolicitud(id, ConfirmarSolicitudRequest(tecnicoCorreo, prioridad.name)) }
        if (r is Resultado.Exito) {
            solicitudDao.obtenerPorId(id)?.let {
                solicitudDao.actualizar(
                    it.copy(
                        estado = EstadoSolicitud.CONFIRMADA,
                        ticketId = r.dato.id,
                        fechaActualizacion = LocalDateTime.now(),
                        pendienteSync = false
                    )
                )
            }
            return Resultado.Exito(r.dato.numero)
        }
        return r as Resultado.Error
    }

    /** Administrador: rechaza la solicitud con un motivo. */
    suspend fun rechazar(id: String, motivo: String): Resultado<Unit> {
        val r = llamar { api.rechazarSolicitud(id, MotivoRequest(motivo)) }
        return when (r) {
            is Resultado.Exito -> {
                solicitudDao.upsertTodas(listOf(r.dato.aEntidad()))
                Resultado.Exito(Unit)
            }
            is Resultado.Error -> r
        }
    }
}
