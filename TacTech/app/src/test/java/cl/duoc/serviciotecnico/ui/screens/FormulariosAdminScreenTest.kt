package cl.duoc.serviciotecnico.ui.screens

import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.testutil.empresaDto
import cl.duoc.serviciotecnico.testutil.usuarioDto
import cl.duoc.serviciotecnico.ui.viewmodel.FormularioEmpresaViewModel
import cl.duoc.serviciotecnico.ui.viewmodel.FormularioUsuarioViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Formularios del administrador: crear y editar empresas y usuarios. */
@RunWith(RobolectricTestRunner::class)
class FormulariosAdminScreenTest : PruebaDePantalla() {

    private var volvio = false

    // ---------- Empresa ----------

    private fun abrirEmpresa(id: Long?) {
        val vm = FormularioEmpresaViewModel(id, e.empresaRepository)
        mostrar { FormularioEmpresaScreen(empresaId = id, onVolver = { volvio = true }, vm = vm) }
    }

    @Test
    fun creaUnaEmpresaNueva() {
        abrirEmpresa(null)
        esperarTexto("Nueva empresa")

        tocar("Crear empresa")
        existeIcono("con error", parcial = true)

        escribir("Razon social o nombre", "Clinica Las Condes")
        escribir("RUT", "76.123.456-0")
        escribir("Direccion", "Av. Apoquindo 1234")
        escribir("Telefono", "+56912345678")
        tocar("Crear empresa")

        esperar { volvio }
    }

    @Test
    fun editaUnaEmpresaExistente() {
        e.api.empresas += empresaDto(id = 1)
        abrirEmpresa(1)
        esperarTexto("Editar empresa")
        esperarTexto("Clinica Las Condes")
        tocar("Guardar cambios")
        esperar { volvio }
    }

    // ---------- Usuario ----------

    private fun abrirUsuario(id: Long?) {
        val vm = FormularioUsuarioViewModel(id, e.usuarioRepository, e.empresaRepository)
        mostrar { FormularioUsuarioScreen(usuarioId = id, onVolver = { volvio = true }, vm = vm) }
    }

    @Test
    fun creaUnUsuarioClienteDeUnaEmpresa() {
        e.api.empresas += empresaDto(id = 1)
        abrirUsuario(null)
        esperarTexto("Nuevo usuario")

        tocar("Tecnico")
        tocar("Cliente")
        esperarTexto("Selecciona una opcion")
        tocar("Selecciona una opcion")
        tocar("Clinica Las Condes · 76123456-0")

        escribir("Nombre completo", "Ana Rojas")
        escribir("Correo", "ana@clinica.cl")
        escribir("Contraseña", "cliente123")
        compose.onAllNodes(isToggleable()).onFirst().performClick()
        compose.onAllNodes(isToggleable()).onFirst().performClick()
        tocar("Crear usuario")

        esperar { volvio }
    }

    @Test
    fun editaUnUsuarioSinCambiarSuClave() {
        e.api.usuarios += usuarioDto(id = 1)
        abrirUsuario(1)
        esperarTexto("Editar usuario")
        esperarTexto("Dejala vacia para no cambiarla")
        tocar("Guardar cambios")
        esperar { volvio }
    }
}
