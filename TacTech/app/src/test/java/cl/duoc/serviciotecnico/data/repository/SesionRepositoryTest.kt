package cl.duoc.serviciotecnico.data.repository

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.testutil.ApiServiceFalsa
import cl.duoc.serviciotecnico.testutil.DataStoreEnMemoria
import cl.duoc.serviciotecnico.testutil.errorHttp
import cl.duoc.serviciotecnico.testutil.usuarioDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Capa REPOSITORY: inicio de sesion contra el microservicio con respaldo offline,
 * y persistencia de la sesion en DataStore (en memoria para la prueba).
 */
class SesionRepositoryTest {

    private val almacen = DataStoreEnMemoria()
    private val api = ApiServiceFalsa()
    private val repo = SesionRepository(almacen, api)

    @Test
    fun loginContraElServidorGuardaLaSesion() = runTest {
        api.usuarioLogin = usuarioDto(nombre = "Cliente Demo", correo = "cliente@tactech.cl", rol = Rol.CLIENTE)
            .copy(empresaNombre = "Clinica Las Condes")

        val r = repo.iniciarSesion("  Cliente@Tactech.cl ", "cliente123")

        assertTrue(r is ResultadoLogin.Exito && !r.sinConexion)
        val sesion = repo.usuarioActual()!!
        assertEquals("cliente@tactech.cl", sesion.correo)
        assertEquals(Rol.CLIENTE, sesion.rol)
        assertEquals("Clinica Las Condes", sesion.empresaNombre)
    }

    @Test
    fun servidorRechazaCredenciales() = runTest {
        api.usuarioLogin = null // responde 401

        assertEquals(ResultadoLogin.CredencialesInvalidas, repo.iniciarSesion("tecnico@tactech.cl", "mala"))
        assertNull(repo.usuarioActual())
    }

    @Test
    fun sinRedEntraConElUsuarioLocal() = runTest {
        api.fallaRed = IOException("sin red")

        val r = repo.iniciarSesion(SesionRepository.ADMIN_DEMO, SesionRepository.CLAVE_ADMIN)

        assertTrue(r is ResultadoLogin.Exito && r.sinConexion)
        assertEquals(Rol.ADMINISTRADOR, repo.usuarioActual()?.rol)
    }

    @Test
    fun sinRedConClaveIncorrectaOCorreoDesconocidoNoEntra() = runTest {
        api.fallaRed = IOException("sin red")

        assertEquals(ResultadoLogin.CredencialesInvalidas, repo.iniciarSesion(SesionRepository.USUARIO_DEMO, "otra-clave"))
        assertEquals(ResultadoLogin.CredencialesInvalidas, repo.iniciarSesion("nadie@tactech.cl", "tecnico123"))
        assertNull(repo.usuarioActual())
    }

    @Test
    fun errorHttpDelServidorNoUsaElRespaldoLocal() = runTest {
        api.fallaRed = errorHttp(403)
        assertEquals(
            ResultadoLogin.CredencialesInvalidas,
            repo.iniciarSesion(SesionRepository.USUARIO_DEMO, SesionRepository.CLAVE_DEMO)
        )
    }

    @Test
    fun alCambiarDeUsuarioSeBorraLaEmpresaAnterior() = runTest {
        api.fallaRed = IOException("sin red")
        repo.iniciarSesion(SesionRepository.CLIENTE_DEMO, SesionRepository.CLAVE_CLIENTE)
        assertEquals("Clinica Las Condes", repo.usuarioActual()?.empresaNombre)

        repo.iniciarSesion(SesionRepository.USUARIO_DEMO, SesionRepository.CLAVE_DEMO)

        assertNull(repo.usuarioActual()?.empresaNombre)
        assertEquals(Rol.TECNICO, repo.usuarioActual()?.rol)
    }

    @Test
    fun cerrarSesionLaBorra() = runTest {
        api.fallaRed = IOException("sin red")
        repo.iniciarSesion(SesionRepository.USUARIO_DEMO, SesionRepository.CLAVE_DEMO)

        repo.cerrarSesion()

        assertNull(repo.usuarioActual())
    }

    @Test
    fun unRolGuardadoInvalidoSeLeeComoTecnico() = runTest {
        almacen.edit {
            it[stringPreferencesKey("correo")] = "x@tactech.cl"
            it[stringPreferencesKey("rol")] = "ROL_ANTIGUO"
        }
        val sesion = repo.usuarioActual()!!
        assertEquals(Rol.TECNICO, sesion.rol)
        assertEquals("", sesion.nombre)
    }
}
