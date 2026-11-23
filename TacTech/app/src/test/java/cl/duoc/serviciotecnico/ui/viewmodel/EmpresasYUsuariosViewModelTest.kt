package cl.duoc.serviciotecnico.ui.viewmodel

import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.testutil.EntornoDePrueba
import cl.duoc.serviciotecnico.testutil.MainDispatcherRule
import cl.duoc.serviciotecnico.testutil.empresaDto
import cl.duoc.serviciotecnico.testutil.usuarioDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Capa VIEWMODEL: listados CRUD del administrador (empresas y usuarios) contra el microservicio. */
class EmpresasYUsuariosViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sinRed = "Sin conexion con el servidor. Esta accion necesita internet."

    // ---------- Empresas ----------

    @Test
    fun empresasSeCarganYSeEliminan() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.api.empresas += listOf(empresaDto(id = 1, nombre = "Clinica"), empresaDto(id = 2, nombre = "Hospital"))
        val vm = EmpresasViewModel(e.empresaRepository)
        assertTrue(vm.estado.value.cargando)

        vm.recargar()
        assertFalse(vm.estado.value.cargando)
        assertEquals(2, vm.estado.value.empresas.size)

        vm.pedirEliminar(empresaDto(id = 1, nombre = "Clinica"))
        assertEquals(1L, vm.estado.value.porEliminar?.id)
        vm.eliminar()

        assertEquals("Empresa Clinica eliminada", vm.estado.value.mensaje)
        assertEquals(listOf(2L), vm.estado.value.empresas.map { it.id })
        assertNull(vm.estado.value.porEliminar)

        vm.mensajeMostrado()
        assertNull(vm.estado.value.mensaje)
    }

    @Test
    fun empresasConErrores() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        val vm = EmpresasViewModel(e.empresaRepository)

        e.api.fallaRed = IOException("sin red")
        vm.recargar()
        assertEquals(sinRed, vm.estado.value.error)

        e.api.fallaRed = null
        vm.pedirEliminar(empresaDto(id = 99))
        vm.eliminar()
        assertEquals("No existe", vm.estado.value.mensaje)

        vm.pedirEliminar(empresaDto(id = null))
        vm.eliminar() // sin id no se llama al servidor
        vm.cancelarEliminar()
        vm.eliminar() // sin empresa seleccionada no hace nada
        assertEquals(1, e.api.llamadas.count { it.startsWith("eliminarEmpresa") })
        assertNull(vm.estado.value.porEliminar)
    }

    // ---------- Usuarios ----------

    @Test
    fun usuariosSeFiltranPorRolYSeEliminan() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.api.usuarios += listOf(
            usuarioDto(id = 1, nombre = "Tecnico Uno"),
            usuarioDto(id = 2, nombre = "Cliente Uno", correo = "c@t.cl", rol = Rol.CLIENTE),
            usuarioDto(id = 3, nombre = "Admin", correo = "a@t.cl", rol = Rol.ADMINISTRADOR)
        )
        val vm = UsuariosViewModel(e.usuarioRepository)

        vm.recargar()
        assertEquals(3, vm.estado.value.filtrados.size)
        assertEquals(1, vm.estado.value.cantidad(Rol.CLIENTE))
        assertEquals(3, vm.estado.value.cantidad(null))

        vm.cambiarFiltro(Rol.TECNICO)
        assertEquals(listOf("Tecnico Uno"), vm.estado.value.filtrados.map { it.nombre })

        vm.pedirEliminar(usuarioDto(id = 1, nombre = "Tecnico Uno"))
        vm.eliminar()
        assertEquals("Usuario Tecnico Uno eliminado", vm.estado.value.mensaje)
        assertEquals(2, vm.estado.value.usuarios.size)

        vm.mensajeMostrado()
        assertNull(vm.estado.value.mensaje)
    }

    @Test
    fun usuariosConErrores() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        val vm = UsuariosViewModel(e.usuarioRepository)

        e.api.fallaRed = IOException("sin red")
        vm.recargar()
        assertEquals(sinRed, vm.estado.value.error)
        assertFalse(vm.estado.value.cargando)

        e.api.fallaRed = null
        vm.pedirEliminar(usuarioDto(id = 50))
        vm.eliminar()
        assertEquals("No existe", vm.estado.value.mensaje)

        vm.pedirEliminar(usuarioDto().copy(id = null))
        vm.eliminar()
        vm.cancelarEliminar()
        vm.eliminar()
        assertEquals(1, e.api.llamadas.count { it.startsWith("eliminarUsuario") })
    }
}
