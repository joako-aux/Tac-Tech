package cl.duoc.serviciotecnico.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.domain.validation.CampoEmpresa
import cl.duoc.serviciotecnico.ui.components.BarraSuperior
import cl.duoc.serviciotecnico.ui.components.BotonGuardar
import cl.duoc.serviciotecnico.ui.components.CampoTextoValidado
import cl.duoc.serviciotecnico.ui.components.TarjetaSeccion
import cl.duoc.serviciotecnico.ui.viewmodel.FormularioEmpresaViewModel

@Composable
fun FormularioEmpresaScreen(
    empresaId: Long?,
    onVolver: () -> Unit,
    vm: FormularioEmpresaViewModel = viewModel(factory = FormularioEmpresaViewModel.factory(empresaId))
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val habilitado = !estado.guardando

    LaunchedEffect(estado.guardado) { if (estado.guardado) onVolver() }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.mensajeMostrado()
        }
    }

    Scaffold(
        topBar = { BarraSuperior(if (estado.esNueva) "Nueva empresa" else "Editar empresa", onVolver = onVolver) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        Column(
            modifier = Modifier.fillMaxSize().padding(relleno).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TarjetaSeccion(titulo = "Datos de la empresa", icono = Icons.Outlined.Business) {
                CampoTextoValidado(
                    valor = estado.nombre, onValorCambia = { vm.cambiarCampo(CampoEmpresa.NOMBRE, it) },
                    etiqueta = "Razon social o nombre", icono = Icons.Outlined.Business, obligatorio = true,
                    error = estado.error(CampoEmpresa.NOMBRE), valido = estado.valido(CampoEmpresa.NOMBRE), habilitado = habilitado
                )
                CampoTextoValidado(
                    valor = estado.rut, onValorCambia = { vm.cambiarCampo(CampoEmpresa.RUT, it) },
                    etiqueta = "RUT", icono = Icons.Outlined.Badge, obligatorio = true,
                    ayuda = "Se valida el digito verificador. Ej: 76.123.456-0",
                    error = estado.error(CampoEmpresa.RUT), valido = estado.valido(CampoEmpresa.RUT), habilitado = habilitado
                )
                CampoTextoValidado(
                    valor = estado.direccion, onValorCambia = { vm.cambiarCampo(CampoEmpresa.DIRECCION, it) },
                    etiqueta = "Direccion", icono = Icons.Outlined.Place, obligatorio = true,
                    error = estado.error(CampoEmpresa.DIRECCION), valido = estado.valido(CampoEmpresa.DIRECCION), habilitado = habilitado
                )
                CampoTextoValidado(
                    valor = estado.telefono, onValorCambia = { vm.cambiarCampo(CampoEmpresa.TELEFONO, it) },
                    etiqueta = "Telefono", icono = Icons.Outlined.Phone, ayuda = "Opcional · +56912345678",
                    tipoDeTeclado = KeyboardType.Phone, accionDelTeclado = ImeAction.Done,
                    error = estado.error(CampoEmpresa.TELEFONO), valido = estado.valido(CampoEmpresa.TELEFONO), habilitado = habilitado
                )
            }
            BotonGuardar(onClick = vm::guardar, texto = if (estado.esNueva) "Crear empresa" else "Guardar cambios", guardando = estado.guardando)
        }
    }
}
