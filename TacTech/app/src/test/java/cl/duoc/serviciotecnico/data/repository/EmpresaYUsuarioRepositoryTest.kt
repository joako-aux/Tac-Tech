package cl.duoc.serviciotecnico.data.repository

import cl.duoc.serviciotecnico.data.remote.ApiService
import cl.duoc.serviciotecnico.data.remote.Resultado
import cl.duoc.serviciotecnico.data.remote.dto.UsuarioRequest
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.testutil.empresaDto
import cl.duoc.serviciotecnico.testutil.usuarioDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

/**
 * Capa REPOSITORY con MockK: el CRUD del administrador va siempre contra el microservicio.
 * La API se simula con un mock y se verifica que el repositorio llame al endpoint correcto.
 */
class EmpresaYUsuarioRepositoryTest {

    private val api = mockk<ApiService>()
    private val empresas = EmpresaRepository(api)
    private val usuarios = UsuarioRepository(api)

    private val conflicto409 = """{"estado":409,"mensaje":"La empresa tiene clientes asociados"}"""
        .toResponseBody("application/json".toMediaType())

    // ---------- Empresas ----------

    @Test
    fun listarEmpresas() = runTest {
        coEvery { api.empresas() } returns listOf(empresaDto())
        assertEquals(Resultado.Exito(listOf(empresaDto())), empresas.listar())
    }

    @Test
    fun empresaSinIdSeCreaConPost() = runTest {
        coEvery { api.crearEmpresa(any()) } returns empresaDto(id = 5)

        val r = empresas.guardar(empresaDto(id = null))

        assertEquals(Resultado.Exito(empresaDto(id = 5)), r)
        coVerify(exactly = 1) { api.crearEmpresa(any()) }
        coVerify(exactly = 0) { api.actualizarEmpresa(any(), any()) }
    }

    @Test
    fun empresaConIdSeActualizaConPut() = runTest {
        coEvery { api.actualizarEmpresa(3, any()) } returns empresaDto(id = 3)

        empresas.guardar(empresaDto(id = 3))

        coVerify(exactly = 1) { api.actualizarEmpresa(3, any()) }
        coVerify(exactly = 0) { api.crearEmpresa(any()) }
    }

    @Test
    fun eliminarEmpresa() = runTest {
        coEvery { api.eliminarEmpresa(1) } returns Response.success(Unit)
        coEvery { api.eliminarEmpresa(2) } returns Response.error(409, conflicto409)

        assertEquals(Resultado.Exito(Unit), empresas.eliminar(1))
        assertEquals(Resultado.Error("La empresa tiene clientes asociados"), empresas.eliminar(2))
    }

    // ---------- Usuarios ----------

    @Test
    fun listarUsuariosPorRol() = runTest {
        coEvery { api.usuarios("TECNICO") } returns listOf(usuarioDto())
        coEvery { api.usuarios(null) } returns emptyList()

        assertEquals(Resultado.Exito(listOf(usuarioDto())), usuarios.listar(Rol.TECNICO))
        assertEquals(Resultado.Exito(emptyList<Any>()), usuarios.listar())
    }

    @Test
    fun usuarioNuevoSeCreaYExistenteSeActualiza() = runTest {
        val datos = UsuarioRequest("Ana Rojas", "ana@clinica.cl", "clave123", "CLIENTE", true, 1)
        coEvery { api.crearUsuario(datos) } returns usuarioDto(id = 10)
        coEvery { api.actualizarUsuario(10, datos) } returns usuarioDto(id = 10)

        usuarios.guardar(null, datos)
        usuarios.guardar(10, datos)

        coVerify(exactly = 1) { api.crearUsuario(datos) }
        coVerify(exactly = 1) { api.actualizarUsuario(10, datos) }
    }

    @Test
    fun eliminarUsuario() = runTest {
        coEvery { api.eliminarUsuario(1) } returns Response.success(Unit)
        coEvery { api.eliminarUsuario(9) } returns Response.error(
            404,
            """{"estado":404,"mensaje":"Usuario no encontrado"}""".toResponseBody("application/json".toMediaType())
        )

        assertEquals(Resultado.Exito(Unit), usuarios.eliminar(1))
        assertEquals(Resultado.Error("Usuario no encontrado"), usuarios.eliminar(9))
    }
}
