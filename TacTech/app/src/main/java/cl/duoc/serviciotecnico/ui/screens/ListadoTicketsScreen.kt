package cl.duoc.serviciotecnico.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.data.local.entity.Ticket
import cl.duoc.serviciotecnico.ui.components.BannerConexion
import cl.duoc.serviciotecnico.ui.components.Contador
import cl.duoc.serviciotecnico.ui.components.TarjetaTicket
import cl.duoc.serviciotecnico.ui.components.BarraSuperior
import cl.duoc.serviciotecnico.ui.components.EstadoVacio
import cl.duoc.serviciotecnico.ui.components.EtiquetaEstado
import cl.duoc.serviciotecnico.ui.components.EtiquetaPrioridad
import cl.duoc.serviciotecnico.ui.theme.colorDeEstado
import cl.duoc.serviciotecnico.ui.viewmodel.ListadoTicketsUiState
import cl.duoc.serviciotecnico.ui.viewmodel.ListadoTicketsViewModel
import java.time.format.DateTimeFormatter

private val formatoFecha = DateTimeFormatter.ofPattern("dd-MM-yyyy · HH:mm")

@Composable
fun ListadoTicketsScreen(
    onAbrirTicket: (String) -> Unit,
    onSesionCerrada: () -> Unit,
    vm: ListadoTicketsViewModel = viewModel(factory = ListadoTicketsViewModel.Factory)
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val lista = rememberLazyListState()
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }


    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = "Tickets asignados",
                subtitulo = estado.usuario?.let { "${it.nombre} · ${it.rol.etiqueta}" },
                acciones = {
                    IconButton(onClick = { confirmarSalida = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesion")
                    }
                }
            )
        },
    ) { relleno ->
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            estado.tickets.isEmpty() -> Column(Modifier.fillMaxSize().padding(relleno)) {
                BannerConexion(
                    sync = estado.sync,
                    onSincronizar = vm::sincronizarAhora,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                )
                EstadoVacio(
                    icono = Icons.Outlined.Inbox,
                    titulo = "No tienes tickets asignados",
                    mensaje = "Cuando el administrador te asigne un ticket aparecera aqui. Si estas sin internet, se descargara al recuperar la señal."
                )
            }

            else -> LazyColumn(
                state = lista,
                modifier = Modifier.fillMaxSize().padding(relleno),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "conexion") { BannerConexion(estado.sync, onSincronizar = vm::sincronizarAhora) }
                item(key = "resumen") { Resumen(estado) }
                item(key = "titulo") {
                    Text(
                        "Mis visitas",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                // animateItem: cada tarjeta aparece con fundido y se reacomoda con animacion
                items(estado.tickets, key = { it.id }) { ticket ->
                    TarjetaTicket(
                        ticket = ticket,
                        onClick = { onAbrirTicket(ticket.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            title = { Text("Cerrar sesion") },
            text = { Text("Tus tickets quedan guardados en el telefono. ¿Quieres salir?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    vm.cerrarSesion(onSesionCerrada)
                }) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun Resumen(estado: ListadoTicketsUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Contador("Activos", estado.activos, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        Contador("Cerrados", estado.cerrados, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
        Contador("Total", estado.total, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
    }
}
