package cl.duoc.serviciotecnico.ui.viewmodel

import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.testutil.ADMIN
import cl.duoc.serviciotecnico.testutil.EntornoDePrueba
import cl.duoc.serviciotecnico.testutil.MainDispatcherRule
import cl.duoc.serviciotecnico.testutil.observar
import cl.duoc.serviciotecnico.testutil.solicitud
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Capa VIEWMODEL: bandeja de solicitudes del administrador. */
class SolicitudesAdminViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun muestraPorDefectoLasPorConfirmarYPermiteVerTodas() = runTest {
        val e = EntornoDePrueba(backgroundScope)
        e.iniciarComo(ADMIN)
        e.solicitudes.guardar(solicitud(id = "s-1"))
        e.solicitudes.guardar(solicitud(id = "s-2", estado = EstadoSolicitud.RECHAZADA))
        e.solicitudes.guardar(solicitud(id = "s-3", estado = EstadoSolicitud.CONFIRMADA))

        val vm = SolicitudesAdminViewModel(e.solicitudRepository, e.sincronizacion)
        observar(vm.estado)

        assertTrue("solicitudes(cliente=null)" in e.api.llamadas)
        val s = vm.estado.value
        assertEquals(EstadoSolicitud.POR_CONFIRMAR, s.filtro)
        assertEquals(listOf("s-1"), s.filtradas.map { it.id })
        assertEquals(3, s.cantidad(null))
        assertEquals(1, s.cantidad(EstadoSolicitud.RECHAZADA))

        vm.cambiarFiltro(null)
        assertEquals(3, vm.estado.value.filtradas.size)

        vm.cambiarFiltro(EstadoSolicitud.CONFIRMADA)
        assertEquals(listOf("s-3"), vm.estado.value.filtradas.map { it.id })
    }
}
