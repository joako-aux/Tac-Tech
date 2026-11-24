package cl.duoc.serviciotecnico.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.ui.components.BannerConexion
import cl.duoc.serviciotecnico.ui.components.BarraSuperior
import cl.duoc.serviciotecnico.ui.components.Contador
import cl.duoc.serviciotecnico.ui.components.DialogoMotivo
import cl.duoc.serviciotecnico.ui.components.EstadoVacio
import cl.duoc.serviciotecnico.ui.components.EtiquetaEstado
import cl.duoc.serviciotecnico.ui.components.EtiquetaSolicitud
import cl.duoc.serviciotecnico.ui.components.FilaDato
import cl.duoc.serviciotecnico.ui.theme.colorDeSolicitud
import cl.duoc.serviciotecnico.ui.viewmodel.ClienteViewModel
import cl.duoc.serviciotecnico.ui.viewmodel.SolicitudConTicket
import java.time.format.DateTimeFormatter

private val formatoFecha = DateTimeFormatter.ofPattern("dd-MM-yyyy · HH:mm")

/**
 * Panel del Cliente: crea solicitudes, ve su estado y el avance del ticket que genero
 * cada una, y puede cancelar las que siguen "Por confirmar".
 */
@Composable
fun ClienteHomeScreen(
    onNuevaSolicitud: () -> Unit,
    onSesionCerrada: () -> Unit,
    vm: ClienteViewModel = viewModel(factory = ClienteViewModel.Factory)
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val lista = rememberLazyListState()
    val fabExpandido by remember { derivedStateOf { lista.firstVisibleItemIndex == 0 } }
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.mensajeMostrado()
        }
    }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = "Mis solicitudes",
                subtitulo = estado.usuario?.let { u -> listOfNotNull(u.nombre, u.empresaNombre).joinToString(" · ") },
                acciones = {
                    IconButton(onClick = { confirmarSalida = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesion")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevaSolicitud,
                expanded = fabExpandido,
                icon = { Icon(Icons.Filled.Add, contentDescription = "Nueva solicitud") },
                text = { Text("Nueva solicitud") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        if (estado.cargando) {
            Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            state = lista,
            modifier = Modifier.fillMaxSize().padding(relleno),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "conexion") { BannerConexion(estado.sync, onSincronizar = vm::sincronizarAhora) }
            item(key = "resumen") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Contador("Por confirmar", estado.porConfirmar, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    Contador("En atencion", estado.enAtencion, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    Contador("Atendidas", estado.atendidas, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                }
            }
            if (estado.solicitudes.isEmpty()) {
                item(key = "vacio") {
                    EstadoVacio(
                        icono = Icons.Outlined.Inbox,
                        titulo = "Aun no tienes solicitudes",
                        mensaje = "Crea una solicitud para tu equipo de frio. Funciona aunque no tengas internet.",
                        modifier = Modifier.padding(top = 24.dp).height(300.dp)
                    )
                }
            } else {
                items(estado.solicitudes, key = { it.solicitud.id }) { item ->
                    TarjetaSolicitudCliente(
                        item = item,
                        onCancelar = { vm.pedirCancelacion(item.solicitud.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    estado.cancelacion?.let { c ->
        DialogoMotivo(
            titulo = "Cancelar solicitud",
            explicacion = "Puedes anular la solicitud mientras el administrador no la procese.",
            icono = Icons.Outlined.Block,
            motivo = c.motivo,
            error = c.error,
            textoConfirmar = "Cancelar solicitud",
            onMotivoCambia = vm::cambiarMotivo,
            onConfirmar = vm::confirmarCancelacion,
            onCerrar = vm::cerrarCancelacion
        )
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            title = { Text("Cerrar sesion") },
            text = { Text("Tus solicitudes quedan guardadas. ¿Quieres salir?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    vm.cerrarSesion(onSesionCerrada)
                }) { Text("Salir") }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Volver") } }
        )
    }
}

@Composable
private fun TarjetaSolicitudCliente(item: SolicitudConTicket, onCancelar: () -> Unit, modifier: Modifier = Modifier) {
    val s = item.solicitud
    val t = item.ticket
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, colorDeSolicitud(s.estado).copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                EtiquetaSolicitud(s.estado)
                if (s.pendienteSync) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = "Pendiente de enviar", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                }
            }
            Text(s.titulo, style = MaterialTheme.typography.titleMedium)
            FilaDato(Icons.Outlined.AcUnit, "Equipo", "${s.equipoNombre} · ${s.equipoModelo} · ${s.equipoSerie}")
            FilaDato(Icons.Outlined.Place, "Direccion", s.direccion)
            Text(
                "Creada el ${s.fechaCreacion.format(formatoFecha)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Sincronizacion multipanel: el cliente ve el avance que registra el tecnico
            if (t != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ConfirmationNumber, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ticket N° ${t.numero}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    EtiquetaEstado(t.estado)
                }
                if (t.tecnicoCorreo.isNotBlank()) {
                    FilaDato(Icons.Outlined.Engineering, "Tecnico asignado", t.tecnicoCorreo)
                }
            }
            s.motivo?.let { FilaDato(Icons.Outlined.Info, "Motivo", it) }

            if (s.estado.clientePuedeCancelar) {
                OutlinedButton(onClick = onCancelar, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.Outlined.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cancelar solicitud")
                }
            }
        }
    }
}
