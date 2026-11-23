package cl.duoc.serviciotecnico.data.repository

import cl.duoc.serviciotecnico.data.local.dao.EquipoDao
import cl.duoc.serviciotecnico.data.local.entity.Equipo
import kotlinx.coroutines.flow.Flow

/** Los equipos se registran en el servidor al confirmar una solicitud; aqui solo se leen. */
class EquipoRepository(
    private val equipoDao: EquipoDao
) {
    fun observarEquipo(id: String): Flow<Equipo?> = equipoDao.observarPorId(id)

    suspend fun obtenerEquipo(id: String): Equipo? = equipoDao.obtenerPorId(id)
}
