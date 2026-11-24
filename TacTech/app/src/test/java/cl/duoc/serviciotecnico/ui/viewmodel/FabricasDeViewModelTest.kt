package cl.duoc.serviciotecnico.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.core.app.ApplicationProvider
import cl.duoc.serviciotecnico.ServicioTecnicoApp
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Cada pantalla obtiene su ViewModel desde una factory que toma los repositorios de
 * ServicioTecnicoApp (inyeccion manual). Se verifica que cada factory arme su ViewModel.
 * (Las factories cuyo ViewModel llama al servidor al crearse se prueban en sus pantallas.)
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = ServicioTecnicoApp::class)
class FabricasDeViewModelTest {

    private val app = ApplicationProvider.getApplicationContext<ServicioTecnicoApp>()
    private val extras = MutableCreationExtras().apply { set(APPLICATION_KEY, app) }

    private fun <T : ViewModel> crear(fabrica: ViewModelProvider.Factory, clase: Class<T>): T =
        fabrica.create(clase, extras)

    @Test
    fun cadaFactoryArmaSuViewModel() {
        assertNotNull(crear(LoginViewModel.Factory, LoginViewModel::class.java))
        assertNotNull(crear(ClienteViewModel.Factory, ClienteViewModel::class.java))
        assertNotNull(crear(ListadoTicketsViewModel.Factory, ListadoTicketsViewModel::class.java))
        assertNotNull(crear(SolicitudesAdminViewModel.Factory, SolicitudesAdminViewModel::class.java))
        assertNotNull(crear(AdminViewModel.factory(), AdminViewModel::class.java))
        assertNotNull(crear(EmpresasViewModel.Factory, EmpresasViewModel::class.java))
        assertNotNull(crear(UsuariosViewModel.Factory, UsuariosViewModel::class.java))
        assertNotNull(crear(FormularioSolicitudViewModel.Factory, FormularioSolicitudViewModel::class.java))
        assertNotNull(crear(FormularioEmpresaViewModel.factory(null), FormularioEmpresaViewModel::class.java))
        assertNotNull(crear(DetalleTicketViewModel.factory("no-existe"), DetalleTicketViewModel::class.java))
        assertNotNull(crear(DetalleGuiaViewModel.factory("no-existe"), DetalleGuiaViewModel::class.java))
        assertNotNull(crear(FormularioGuiaViewModel.factory("no-existe"), FormularioGuiaViewModel::class.java))
    }

    @Test
    fun laAppArmaTodosSusRepositorios() {
        assertNotNull(app.ticketRepository)
        assertNotNull(app.equipoRepository)
        assertNotNull(app.guiaRepository)
        assertNotNull(app.sesionRepository)
        assertNotNull(app.solicitudRepository)
        assertNotNull(app.usuarioRepository)
        assertNotNull(app.empresaRepository)
        assertNotNull(app.geocodificacionRepository)
        assertNotNull(app.sincronizacionRepository)
    }
}
