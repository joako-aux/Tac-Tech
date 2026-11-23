package cl.duoc.serviciotecnico.ui.viewmodel

import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.domain.validation.CampoEmpresa
import cl.duoc.serviciotecnico.domain.validation.CampoUsuario
import cl.duoc.serviciotecnico.testutil.EntornoDePrueba
import cl.duoc.serviciotecnico.testutil.MainDispatcherRule
import cl.duoc.serviciotecnico.testutil.empresaDto
import cl.duoc.serviciotecnico.testutil.usuarioDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Capa VIEWMODEL: formularios de empresa y usuario del administrador (crear y editar). */
class FormulariosAdminViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val sinRed = "Sin conexion con el servidor. Esta accion necesita internet."

    // ---------- Empresa ----------

    private fun FormularioEmpresaViewModel.llenar() {
        cambiarCampo(CampoEmpresa.NOMBRE, "Clinica Las Condes")
        cambiarCampo(CampoEmpresa.RUT, "10.000.013-k")
        cambiarCampo(CampoEmpresa.DIRECCION, "Av. Apoquindo 1234")
        cambiarCampo(CampoEmpresa.TELEFONO, "+56 9 1234 5678")
    }

    @Test
    fun empresaNuevaSeCrea() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        val vm = FormularioEmpresaViewModel(null, e.empresaRepository)
        assertTrue(vm.estado.value.esNueva)
        assertFalse(vm.estado.value.valido(CampoEmpresa.TELEFONO))

        vm.llenar()
        assertEquals("10.000.013-K", vm.estado.value.rut)
        assertTrue(vm.estado.value.valido(CampoEmpresa.TELEFONO))
        assertTrue(vm.estado.value.valido(CampoEmpresa.RUT))
        assertNull(vm.estado.value.error(CampoEmpresa.NOMBRE))
        vm.guardar()

        assertTrue(vm.estado.value.guardado)
        val creada = e.api.empresas.single()
        assertEquals("+56912345678", creada.telefono)
        assertTrue("crearEmpresa" in e.api.llamadas)
    }

    @Test
    fun empresaInvalidaNoSeGuarda() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        val vm = FormularioEmpresaViewModel(null, e.empresaRepository)
        vm.cambiarCampo(CampoEmpresa.RUT, "123")

        vm.guardar()

        assertEquals("Revisa los campos marcados en rojo", vm.estado.value.mensaje)
        assertNotNull(vm.estado.value.error(CampoEmpresa.RUT))
        assertTrue(e.api.empresas.isEmpty())
        vm.mensajeMostrado()
        assertNull(vm.estado.value.mensaje)
    }

    @Test
    fun empresaExistenteSeCargaYSeActualiza() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.api.empresas += empresaDto(id = 3, nombre = "Hospital Norte").copy(telefono = null)
        val vm = FormularioEmpresaViewModel(3, e.empresaRepository)
        assertFalse(vm.estado.value.esNueva)
        assertEquals("Hospital Norte", vm.estado.value.nombre)
        assertEquals("", vm.estado.value.telefono)

        vm.cambiarCampo(CampoEmpresa.NOMBRE, "Hospital Norte 2")
        vm.guardar()

        assertTrue("actualizarEmpresa(3)" in e.api.llamadas)
        assertEquals("Hospital Norte 2", e.api.empresas.single().nombre)
        assertNull(e.api.empresas.single().telefono)
    }

    @Test
    fun empresaSinInternet() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.api.fallaRed = IOException("sin red")
        val vmEditar = FormularioEmpresaViewModel(3, e.empresaRepository)
        assertEquals(sinRed, vmEditar.estado.value.mensaje)

        val vmNueva = FormularioEmpresaViewModel(null, e.empresaRepository)
        vmNueva.llenar()
        vmNueva.guardar()
        assertEquals(sinRed, vmNueva.estado.value.mensaje)
        assertFalse(vmNueva.estado.value.guardando)
    }

    // ---------- Usuario ----------

    @Test
    fun usuarioNuevoClienteExigeEmpresa() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.api.empresas += empresaDto(id = 1)
        val vm = FormularioUsuarioViewModel(null, e.usuarioRepository, e.empresaRepository)
        assertEquals(1, vm.estado.value.empresas.size)
        assertFalse(vm.estado.value.cargando)

        vm.cambiarCampo(CampoUsuario.NOMBRE, "Ana Rojas")
        vm.cambiarCampo(CampoUsuario.CORREO, " Ana@Clinica.cl ")
        vm.cambiarCampo(CampoUsuario.CLAVE, "cliente123")
        vm.cambiarCampo(CampoUsuario.EMPRESA, "ignorado")
        vm.cambiarRol(Rol.CLIENTE)
        assertTrue(vm.estado.value.pideEmpresa)
        vm.guardar()
        assertEquals("Revisa los campos marcados en rojo", vm.estado.value.mensaje)
        assertEquals("Un cliente debe pertenecer a una empresa", vm.estado.value.error(CampoUsuario.EMPRESA))

        vm.cambiarEmpresa(empresaDto(id = 1))
        assertEquals(1L, vm.estado.value.empresa?.id)
        vm.cambiarActivo(false)
        vm.guardar()

        assertTrue(vm.estado.value.guardado)
        val creado = e.api.usuarios.single()
        assertEquals("ana@clinica.cl", creado.correo)
        assertEquals("CLIENTE", creado.rol)
        assertEquals(1L, creado.empresaId)
        assertFalse(creado.activo)
        vm.mensajeMostrado()
        assertNull(vm.estado.value.mensaje)
    }

    @Test
    fun alDejarDeSerClienteSeQuitaLaEmpresa() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        val vm = FormularioUsuarioViewModel(null, e.usuarioRepository, e.empresaRepository)
        vm.cambiarRol(Rol.CLIENTE)
        vm.cambiarEmpresa(empresaDto(id = 1))

        vm.cambiarRol(Rol.TECNICO)

        assertNull(vm.estado.value.empresaId)
        assertFalse(vm.estado.value.pideEmpresa)
        assertTrue(vm.estado.value.valido(CampoUsuario.EMPRESA))
    }

    @Test
    fun usuarioExistenteSeEditaSinCambiarLaClave() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.api.usuarios += usuarioDto(id = 2, nombre = "Tecnico Dos", correo = "tecnico2@tactech.cl")
        val vm = FormularioUsuarioViewModel(2, e.usuarioRepository, e.empresaRepository)
        assertFalse(vm.estado.value.esNuevo)
        assertEquals("Tecnico Dos", vm.estado.value.nombre)
        assertEquals(Rol.TECNICO, vm.estado.value.rol)
        assertTrue(vm.estado.value.formulario.esValido)

        vm.cambiarCampo(CampoUsuario.NOMBRE, "Tecnico Dos Editado")
        vm.guardar()

        assertTrue("actualizarUsuario(2)" in e.api.llamadas)
        assertEquals("Tecnico Dos Editado", e.api.usuarios.single().nombre)
        assertTrue(vm.estado.value.guardado)
    }

    @Test
    fun usuarioSinInternet() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.api.fallaRed = IOException("sin red")
        val vmEditar = FormularioUsuarioViewModel(2, e.usuarioRepository, e.empresaRepository)
        assertEquals(sinRed, vmEditar.estado.value.mensaje)
        assertFalse(vmEditar.estado.value.cargando)

        val vmNuevo = FormularioUsuarioViewModel(null, e.usuarioRepository, e.empresaRepository)
        vmNuevo.cambiarCampo(CampoUsuario.NOMBRE, "Ana Rojas")
        vmNuevo.cambiarCampo(CampoUsuario.CORREO, "ana@clinica.cl")
        vmNuevo.cambiarCampo(CampoUsuario.CLAVE, "clave123")
        vmNuevo.guardar()
        assertEquals(sinRed, vmNuevo.estado.value.mensaje)
        assertFalse(vmNuevo.estado.value.guardado)
    }
}
