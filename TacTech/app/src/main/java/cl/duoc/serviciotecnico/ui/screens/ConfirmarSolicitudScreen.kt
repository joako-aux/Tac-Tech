package cl.duoc.serviciotecnico.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.domain.model.Prioridad
import cl.duoc.serviciotecnico.ui.components.BarraSuperior
import cl.duoc.serviciotecnico.ui.components.BotonGuardar
import cl.duoc.serviciotecnico.ui.components.DialogoMotivo
import cl.duoc.serviciotecnico.ui.components.EtiquetaSolicitud
import cl.duoc.serviciotecnico.ui.components.FilaDato
import cl.duoc.serviciotecnico.ui.components.SelectorDeOpciones
import cl.duoc.serviciotecnico.ui.components.SelectorDesplegable
import cl.duoc.serviciotecnico.ui.components.TarjetaSeccion
import cl.duoc.serviciotecnico.ui.viewmodel.ConfirmarSolicitudViewModel
import java.time.format.DateTimeFormatter

private val formatoFecha = DateTimeFormatter.ofPattern("dd-MM-yyyy · HH:mm")

/**
 * El administrador revisa la solicitud y crea el ticket formal asignandolo a un
 * soporte tecnico, o la rechaza con un motivo.
 */
@Composable
fun ConfirmarSolicitudScreen(
    solicitudId: String,
    onVolver: () -> Unit,
    vm: ConfirmarSolicitudViewModel = viewModel(factory = ConfirmarSolicitudViewModel.factory(solicitudId))
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val a = estado.asignacion
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(a.mensaje) {
        a.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.mensajeMostrado()
            if (a.terminado) onVolver()
        }
    }

    Scaffold(
        topBar = { BarraSuperior("Revisar solicitud", subtitulo = estado.solicitud?.empresaNombre, onVolver = onVolver) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        val s = estado.solicitud
        if (s == null) {
            Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TarjetaSeccion(titulo = s.titulo, icono = Icons.Outlined.SupportAgent) {
                EtiquetaSolicitud(s.estado)
                Text(s.descripcion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilaDato(Icons.Outlined.Business, "Cliente", listOfNotNull(s.empresaNombre, s.clienteNombre, s.clienteCorreo).joinToString(" · "))
                FilaDato(Icons.Outlined.Place, "Direccion", s.direccion)
                FilaDato(Icons.Outlined.Event, "Creada", s.fechaCreacion.format(formatoFecha))
                s.motivo?.let { FilaDato(Icons.Outlined.Info, "Motivo", it) }
            }
            TarjetaSeccion(titulo = "Equipo de frio", icono = Icons.Filled.AcUnit, descripcion = s.equipoNombre) {
                FilaDato(Icons.Outlined.Category, "Modelo", s.equipoModelo)
                FilaDato(Icons.Outlined.QrCode2, "N° de serie", s.equipoSerie)
                FilaDato(Icons.Outlined.Description, "Propiedad", s.propiedad.etiqueta)
            }

            if (s.estado == EstadoSolicitud.POR_CONFIRMAR) {
                // Gestionar solicitudes exige conexion: el servidor crea ticket y equipo en una transaccion
                AnimatedVisibility(visible = !estado.enLinea) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Necesitas conexion para crear o rechazar el ticket.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                TarjetaSeccion(
                    titulo = "Crear ticket y asignar",
                    icono = Icons.Outlined.AssignmentInd,
                    descripcion = "El ticket aparecera en el panel del tecnico elegido"
                ) {
                    SelectorDesplegable(
                        etiqueta = "Soporte tecnico *",
                        opciones = a.tecnicos,
                        seleccionado = a.tecnico,
                        textoDe = { "${it.nombre} · ${it.correo}" },
                        onSeleccionar = vm::elegirTecnico,
                        icono = Icons.Outlined.Engineering,
                        error = a.errorTecnico ?: a.errorTecnicos,
                        habilitado = !a.procesando && !a.cargandoTecnicos,
                        textoVacio = "No hay tecnicos activos. Crealos en Usuarios."
                    )
                    if (a.errorTecnicos != null) {
                        TextButton(onClick = vm::cargarTecnicos) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reintentar")
                        }
                    }
                    Text("Prioridad", style = MaterialTheme.typography.labelLarge)
                    SelectorDeOpciones(
                        opciones = Prioridad.entries,
                        seleccionada = a.prioridad,
                        etiqueta = { it.etiqueta },
                        onSeleccionar = vm::elegirPrioridad,
                        habilitado = !a.procesando
                    )
                    BotonGuardar(
                        onClick = vm::confirmar,
                        texto = "Crear ticket",
                        icono = Icons.Outlined.AssignmentInd,
                        habilitado = estado.enLinea,
                        guardando = a.procesando
                    )
                    OutlinedButton(
                        onClick = vm::abrirRechazo,
                        enabled = estado.enLinea && !a.procesando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Rechazar solicitud")
                    }
                }
            }
        }
    }

    a.motivoRechazo?.let { motivo ->
        DialogoMotivo(
            titulo = "Rechazar solicitud",
            explicacion = "El cliente vera este motivo en su panel.",
            icono = Icons.Outlined.Block,
            motivo = motivo,
            error = a.errorMotivo,
            textoConfirmar = "Rechazar",
            onMotivoCambia = vm::cambiarMotivo,
            onConfirmar = vm::rechazar,
            onCerrar = vm::cerrarRechazo,
            procesando = a.procesando
        )
    }
}
