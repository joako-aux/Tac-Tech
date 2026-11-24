package cl.duoc.serviciotecnico.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.ui.components.BannerConexion
import cl.duoc.serviciotecnico.ui.components.BarraSuperior
import cl.duoc.serviciotecnico.ui.components.Contador
import cl.duoc.serviciotecnico.ui.components.EstadoVacio
import cl.duoc.serviciotecnico.ui.components.IconoCircular
import cl.duoc.serviciotecnico.ui.viewmodel.AdminViewModel
import cl.duoc.serviciotecnico.ui.viewmodel.FiltroAdmin
import cl.duoc.serviciotecnico.ui.viewmodel.ResumenTecnico

/**
 * Panel del Administrador: resumen de tareas pendientes y terminadas de todos los
 * tecnicos y avance de cada uno. Desde aqui se entra a la gestion de tickets.
 */
@Composable
fun AdminHomeScreen(
    onGestionarTickets: (FiltroAdmin, String?) -> Unit,
    onSolicitudes: () -> Unit,
    onUsuarios: () -> Unit,
    onEmpresas: () -> Unit,
    onSesionCerrada: () -> Unit,
    vm: AdminViewModel = viewModel(factory = AdminViewModel.factory())
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = "Panel administrador",
                subtitulo = estado.usuario?.nombre,
                acciones = {
                    IconButton(onClick = { confirmarSalida = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesion")
                    }
                }
            )
        }
    ) { relleno ->
        if (estado.cargando) {
            Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(relleno),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "conexion") { BannerConexion(estado.sync, onSincronizar = vm::sincronizarAhora) }

            // Solicitudes que esperan al administrador: la tarea principal del panel
            item(key = "porConfirmar") {
                AccesoPanel(
                    icono = Icons.Outlined.MarkEmailUnread,
                    titulo = "Solicitudes por confirmar",
                    detalle = if (estado.solicitudesPorConfirmar == 0) "No hay solicitudes nuevas"
                    else "Revisa y crea el ticket asignando un tecnico",
                    contador = estado.solicitudesPorConfirmar,
                    destacado = estado.solicitudesPorConfirmar > 0,
                    onClick = onSolicitudes
                )
            }

            item(key = "tituloTareas") {
                Text("Estado de las tareas", style = MaterialTheme.typography.titleMedium)
            }
            item(key = "contadores") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Contador(
                            "Pendientes", estado.pendientes, MaterialTheme.colorScheme.secondary, Modifier.weight(1f),
                            onClick = { onGestionarTickets(FiltroAdmin.PENDIENTES, null) }
                        )
                        Contador(
                            "En proceso", estado.enProceso, MaterialTheme.colorScheme.primary, Modifier.weight(1f),
                            onClick = { onGestionarTickets(FiltroAdmin.EN_PROCESO, null) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Contador(
                            "Terminadas", estado.terminados, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f),
                            onClick = { onGestionarTickets(FiltroAdmin.TERMINADOS, null) }
                        )
                        Contador(
                            "Canceladas", estado.cancelados, MaterialTheme.colorScheme.error, Modifier.weight(1f),
                            onClick = { onGestionarTickets(FiltroAdmin.CANCELADOS, null) }
                        )
                    }
                }
            }

            item(key = "tituloGestion") {
                Text("Gestion", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            }
            item(key = "accesos") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AccesoPanel(Icons.Outlined.Dashboard, "Tickets", "Supervisar y cambiar estados", estado.tickets.size) {
                        onGestionarTickets(FiltroAdmin.TODOS, null)
                    }
                    AccesoPanel(Icons.Outlined.Group, "Usuarios", "Tecnicos, clientes y administradores", null, onClick = onUsuarios)
                    AccesoPanel(Icons.Outlined.Business, "Empresas", "Empresas cliente", null, onClick = onEmpresas)
                }
            }

            item(key = "tituloTecnicos") {
                Text("Avance por tecnico", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            }

            if (estado.resumenPorTecnico.isEmpty()) {
                item(key = "vacio") {
                    EstadoVacio(
                        icono = Icons.Outlined.Inbox,
                        titulo = "Sin tareas registradas",
                        mensaje = "Cuando los tecnicos sincronicen sus tickets apareceran aqui.",
                        modifier = Modifier.height(320.dp)
                    )
                }
            } else {
                items(estado.resumenPorTecnico, key = { it.correo }) { resumen ->
                    TarjetaTecnico(
                        resumen = resumen,
                        onClick = { onGestionarTickets(FiltroAdmin.TODOS, resumen.correo) },
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
            text = { Text("¿Quieres salir del panel de administrador?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    vm.cerrarSesion(onSesionCerrada)
                }) { Text("Salir") }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun AccesoPanel(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    contador: Int?,
    destacado: Boolean = false,
    onClick: () -> Unit
) {
    // El acceso destacado (hay solicitudes nuevas) cambia de color con animacion
    val fondo by animateColorAsState(
        if (destacado) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        tween(400),
        label = "fondoAcceso"
    )
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = fondo),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconoCircular(icono)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (contador != null) {
                Text("$contador", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun TarjetaTecnico(resumen: ResumenTecnico, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // La barra crece animada hasta el porcentaje de tareas terminadas
    val avance by animateFloatAsState(resumen.avance, animationSpec = tween(700), label = "avance${resumen.correo}")

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconoCircular(Icons.Outlined.Engineering)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(resumen.correo, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${resumen.pendientes} pendientes · ${resumen.terminados} terminados",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { avance },
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small)
                )
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = "Ver tickets del tecnico")
        }
    }
}
