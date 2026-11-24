package cl.duoc.serviciotecnico.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.domain.model.Propiedad
import cl.duoc.serviciotecnico.domain.validation.CampoSolicitud
import cl.duoc.serviciotecnico.ui.components.BarraSuperior
import cl.duoc.serviciotecnico.ui.components.BotonGuardar
import cl.duoc.serviciotecnico.ui.components.CampoTextoValidado
import cl.duoc.serviciotecnico.ui.components.SelectorDeOpciones
import cl.duoc.serviciotecnico.ui.components.TarjetaSeccion
import cl.duoc.serviciotecnico.ui.viewmodel.FormularioSolicitudViewModel

/** El cliente describe el problema de su equipo de frio. La solicitud nace "Por confirmar". */
@Composable
fun FormularioSolicitudScreen(
    onVolver: () -> Unit,
    vm: FormularioSolicitudViewModel = viewModel(factory = FormularioSolicitudViewModel.Factory)
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val progreso by animateFloatAsState(estado.progreso, tween(400), label = "progresoSolicitud")

    LaunchedEffect(estado.guardado) { if (estado.guardado) onVolver() }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.mensajeMostrado()
        }
    }
    val habilitado = !estado.guardando

    Scaffold(
        topBar = { BarraSuperior("Nueva solicitud", subtitulo = estado.empresa, onVolver = onVolver) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Campos obligatorios", style = MaterialTheme.typography.labelMedium)
                    Text("${estado.completos} de ${CampoSolicitud.entries.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small))
            }

            TarjetaSeccion(titulo = "¿Que esta pasando?", icono = Icons.Outlined.SupportAgent, descripcion = "Describe el problema del equipo") {
                CampoTextoValidado(
                    valor = estado.titulo, onValorCambia = { vm.cambiarCampo(CampoSolicitud.TITULO, it) },
                    etiqueta = "Titulo", icono = Icons.Outlined.Title, obligatorio = true, largoMaximo = 60,
                    ayuda = "Ej: El equipo no enfria",
                    error = estado.error(CampoSolicitud.TITULO), valido = estado.valido(CampoSolicitud.TITULO), habilitado = habilitado
                )
                CampoTextoValidado(
                    valor = estado.descripcion, onValorCambia = { vm.cambiarCampo(CampoSolicitud.DESCRIPCION, it) },
                    etiqueta = "Descripcion del problema", icono = Icons.Outlined.Description, obligatorio = true,
                    lineasMaximas = 4, largoMaximo = 500,
                    error = estado.error(CampoSolicitud.DESCRIPCION), valido = estado.valido(CampoSolicitud.DESCRIPCION), habilitado = habilitado
                )
                CampoTextoValidado(
                    valor = estado.direccion, onValorCambia = { vm.cambiarCampo(CampoSolicitud.DIRECCION, it) },
                    etiqueta = "Direccion del equipo", icono = Icons.Outlined.Place, obligatorio = true,
                    ayuda = "Calle, numero y comuna",
                    error = estado.error(CampoSolicitud.DIRECCION), valido = estado.valido(CampoSolicitud.DIRECCION), habilitado = habilitado
                )
            }

            TarjetaSeccion(titulo = "Equipo de frio", icono = Icons.Filled.AcUnit) {
                CampoTextoValidado(
                    valor = estado.nombreEquipo, onValorCambia = { vm.cambiarCampo(CampoSolicitud.NOMBRE_EQUIPO, it) },
                    etiqueta = "Nombre del equipo", icono = Icons.Outlined.AcUnit, obligatorio = true,
                    ayuda = "Ej: Split muro oficina 2",
                    error = estado.error(CampoSolicitud.NOMBRE_EQUIPO), valido = estado.valido(CampoSolicitud.NOMBRE_EQUIPO), habilitado = habilitado
                )
                CampoTextoValidado(
                    valor = estado.modelo, onValorCambia = { vm.cambiarCampo(CampoSolicitud.MODELO, it) },
                    etiqueta = "Modelo", icono = Icons.Outlined.Category, obligatorio = true,
                    error = estado.error(CampoSolicitud.MODELO), valido = estado.valido(CampoSolicitud.MODELO), habilitado = habilitado
                )
                CampoTextoValidado(
                    valor = estado.serie, onValorCambia = { vm.cambiarCampo(CampoSolicitud.SERIE, it) },
                    etiqueta = "Numero de serie", icono = Icons.Outlined.QrCode2, obligatorio = true,
                    ayuda = "Letras, numeros y guion", largoMaximo = 20,
                    capitalizacion = KeyboardCapitalization.Characters, accionDelTeclado = ImeAction.Done,
                    error = estado.error(CampoSolicitud.SERIE), valido = estado.valido(CampoSolicitud.SERIE), habilitado = habilitado
                )
                Text("Propiedad del equipo", style = MaterialTheme.typography.labelLarge)
                SelectorDeOpciones(
                    opciones = Propiedad.entries,
                    seleccionada = estado.propiedad,
                    etiqueta = { it.etiqueta },
                    onSeleccionar = vm::cambiarPropiedad,
                    habilitado = habilitado,
                    icono = { if (it == Propiedad.ARRENDADO) Icons.Outlined.Key else Icons.Outlined.Home }
                )
            }

            BotonGuardar(onClick = vm::enviar, texto = "Enviar solicitud", icono = Icons.AutoMirrored.Filled.Send, guardando = estado.guardando)
        }
    }
}
