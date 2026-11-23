package cl.duoc.serviciotecnico.data.repository

import cl.duoc.serviciotecnico.data.remote.ApiService
import cl.duoc.serviciotecnico.data.remote.Resultado
import cl.duoc.serviciotecnico.data.remote.dto.EmpresaDto
import cl.duoc.serviciotecnico.data.remote.llamar

/** CRUD de empresas cliente (administrador). Siempre contra el servidor. */
class EmpresaRepository(private val api: ApiService) {

    suspend fun listar(): Resultado<List<EmpresaDto>> = llamar { api.empresas() }

    suspend fun guardar(datos: EmpresaDto): Resultado<EmpresaDto> = llamar {
        val id = datos.id
        if (id == null) api.crearEmpresa(datos) else api.actualizarEmpresa(id, datos)
    }

    suspend fun eliminar(id: Long): Resultado<Unit> = llamar {
        val respuesta = api.eliminarEmpresa(id)
        if (!respuesta.isSuccessful) throw retrofit2.HttpException(respuesta)
    }
}
