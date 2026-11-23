package cl.duoc.serviciotecnico.data.repository

import cl.duoc.serviciotecnico.data.local.dao.FotoGuiaDao
import cl.duoc.serviciotecnico.data.local.dao.GuiaDao
import cl.duoc.serviciotecnico.data.local.entity.FotoGuia
import cl.duoc.serviciotecnico.data.local.entity.Guia
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

class GuiaRepository(
    private val guiaDao: GuiaDao,
    private val fotoGuiaDao: FotoGuiaDao
) {
    fun observarGuiasDeTicket(ticketId: String): Flow<List<Guia>> =
        guiaDao.observarPorTicket(ticketId)

    fun observarGuia(id: String): Flow<Guia?> = guiaDao.observarPorId(id)

    fun observarFotos(guiaId: String): Flow<List<FotoGuia>> =
        fotoGuiaDao.observarPorGuia(guiaId)

    suspend fun crear(
        ticketId: String,
        equipoId: String,
        direccion: String,
        tecnico: String,
        trabajoRealizado: String,
        repuestos: String,
        observaciones: String,
        recibidoPor: String,
        latitud: Double?,
        longitud: Double?,
        urisDeFotos: List<String>
    ): Guia {
        val guia = Guia(
            numero = (guiaDao.ultimoNumero() ?: 0) + 1,
            ticketId = ticketId,
            equipoId = equipoId,
            direccion = direccion,
            latitud = latitud,
            longitud = longitud,
            fecha = LocalDateTime.now(),
            tecnico = tecnico,
            trabajoRealizado = trabajoRealizado,
            repuestos = repuestos,
            observaciones = observaciones,
            recibidoPor = recibidoPor,
            pendienteSync = true
        )
        guiaDao.insertar(guia)
        if (urisDeFotos.isNotEmpty()) {
            fotoGuiaDao.insertarTodas(urisDeFotos.map { uri -> FotoGuia(guiaId = guia.id, uri = uri) })
        }
        return guia
    }
}
