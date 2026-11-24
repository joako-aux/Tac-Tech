package cl.duoc.serviciotecnico.ui.screens

import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.testutil.PruebaDePantalla
import cl.duoc.serviciotecnico.testutil.empresaDto
import cl.duoc.serviciotecnico.testutil.usuarioDto
import cl.duoc.serviciotecnico.ui.viewmodel.EmpresasViewModel
import cl.duoc.serviciotecnico.ui.viewmodel.UsuariosViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/** Listados de empresas y usuarios del administrador (datos del microservicio). */
@RunWith(RobolectricTestRunner::class)
class EmpresasYUsuariosAdminScreenTest : PruebaDePantalla() {

    private var volvio = false
    private var nuevo = false
    private var editado: Long? = null

    // ---------- Empresas ----------

    private fun abrirEmpresas() {
        val vm = EmpresasViewModel(e.empresaRepository)
        mostrar {
            EmpresasAdminScreen(
                onVolver = { volvio = true },
                onNuevaEmpresa = { nuevo = true },
                onEditarEmpresa = { editado = it },
                vm = vm
            )
        }
    }

    @Test
    fun empresasSeEditanYEliminan() {
        e.api.empresas += empresaDto(id = 1)
        e.api.empresas += empresaDto(id = 2, nombre = "Hospital Norte")
        abrirEmpresas()
        esperarTexto("Hospital Norte")
        existe("2 registradas")

        tocarIcono("Editar Clinica Las Condes")
        assertEquals(1L, editado)

        tocarIcono("Eliminar Hospital Norte")
        existe("Eliminar empresa")
        tocar("Cancelar")
        tocarIcono("Eliminar Hospital Norte")
        tocar("Eliminar")
        esperar { e.api.llamadas.contains("eliminarEmpresa(2)") }

        tocarIcono("Volver")
        assertTrue(volvio)
    }

    @Test
    fun empresasConErrorYSinRegistros() {
        e.api.fallaRed = IOException("sin red")
        abrirEmpresas()
        esperarTexto("No se pudo cargar")
        e.api.fallaRed = null
        tocar("Reintentar")
        esperarTexto("Sin empresas")
    }

    // ---------- Usuarios ----------

    private fun abrirUsuarios() {
        val vm = UsuariosViewModel(e.usuarioRepository)
        mostrar {
            UsuariosAdminScreen(
                onVolver = { volvio = true },
                onNuevoUsuario = { nuevo = true },
                onEditarUsuario = { editado = it },
                vm = vm
            )
        }
    }

    @Test
    fun usuariosSeFiltranEditanYEliminan() {
        e.api.usuarios += usuarioDto(id = 1)
        e.api.usuarios += usuarioDto(id = 2, nombre = "Ana Rojas", correo = "ana@clinica.cl", rol = Rol.CLIENTE, activo = false)
        abrirUsuarios()
        esperarTexto("Ana Rojas")
        existe("Inactivo", parcial = true)

        tocar("Cliente (", parcial = true)
        tocar("Todos (", parcial = true)

        tocarIcono("Editar Ana Rojas")
        assertEquals(2L, editado)

        tocarIcono("Eliminar Ana Rojas")
        existe("Eliminar usuario")
        tocar("Cancelar")
        tocarIcono("Eliminar Ana Rojas")
        tocar("Eliminar")
        esperar { e.api.llamadas.contains("eliminarUsuario(2)") }

        tocarIcono("Volver")
        assertTrue(volvio)
    }

    @Test
    fun usuariosConErrorYSinRegistros() {
        e.api.fallaRed = IOException("sin red")
        abrirUsuarios()
        esperarTexto("No se pudo cargar")
        e.api.fallaRed = null
        tocar("Reintentar")
        esperarTexto("Sin usuarios")
    }
}