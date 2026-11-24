package cl.duoc.serviciotecnico.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.data.local.entity.Solicitud
import cl.duoc.serviciotecnico.domain.model.EstadoSolicitud
import cl.duoc.serviciotecnico.ui.components.BannerConexion
import cl.duoc.serviciotecnico.ui.components.BarraSuperior
import cl.duoc.serviciotecnico.ui.components.EstadoVacio
import cl.duoc.serviciotecnico.ui.components.EtiquetaSolicitud
import cl.duoc.serviciotecnico.ui.components.FilaDato
import cl.duoc.serviciotecnico.ui.viewmodel.SolicitudesAdminViewModel
import java.time.format.DateTimeFormatter

private val formatoFecha = DateTimeFormatter.ofPattern("dd-MM-yyyy · HH:mm")

/** Bandeja del administrador: solicitudes de los clientes, por defecto las "Por confirmar". */
@Composable
fun SolicitudesAdminScreen(
    onVolver: () -> Unit,
    onAbrirSolicitud: (String) -> Unit,
    vm: SolicitudesAdminViewModel = viewModel(factory = SolicitudesAdminViewModel.Factory)
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val opciones: List<EstadoSolicitud?> = listOf(null) + EstadoSolicitud.entries

    Scaffold(topBar = { BarraSuperior("Solicitudes de clientes", onVolver = onVolver) }) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno)) {
            BannerConexion(estado.sync, vm::sincronizarAhora, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(opciones) { opcion ->
                    FilterChip(
                        selected = estado.filtro == opcion,
                        onClick = { vm.cambiarFiltro(opcion) },
                        label = { Text("${opcion?.etiqueta ?: "Todas"} (${estado.cantidad(opcion)})") }
                    )
                }
            }
            if (estado.filtradas.isEmpty()) {
                EstadoVacio(
                    icono = Icons.Outlined.MarkEmailRead,
                    titulo = "No hay solicitudes en este estado",
                    mensaje = "Las solicitudes nuevas de los clientes llegan aqui como \"Por confirmar\"."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(estado.filtradas, key = { it.id }) { s ->
                        TarjetaSolicitudAdmin(s, onClick = { onAbrirSolicitud(s.id) }, modifier = Modifier.animateItem())
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaSolicitudAdmin(s: Solicitud, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EtiquetaSolicitud(s.estado)
                Text(s.titulo, style = MaterialTheme.typography.titleMedium)
                FilaDato(Icons.Outlined.Business, "Cliente", listOfNotNull(s.empresaNombre, s.clienteNombre ?: s.clienteCorreo).joinToString(" · "))
                FilaDato(Icons.Outlined.AcUnit, "Equipo", "${s.equipoNombre} · ${s.equipoSerie}")
                Text(
                    s.fechaCreacion.format(formatoFecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Revisar solicitud")
        }
    }
}
