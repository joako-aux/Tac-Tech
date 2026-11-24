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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.data.remote.dto.UsuarioDto
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.ui.components.BarraSuperior
import cl.duoc.serviciotecnico.ui.components.EstadoVacio
import cl.duoc.serviciotecnico.ui.components.IconoCircular
import cl.duoc.serviciotecnico.ui.viewmodel.UsuariosViewModel

fun iconoDeRol(rol: String): ImageVector = when (rol) {
    Rol.ADMINISTRADOR.name -> Icons.Outlined.AdminPanelSettings
    Rol.TECNICO.name -> Icons.Outlined.Engineering
    else -> Icons.Outlined.Person
}

/** Gestion total de usuarios: listar, crear, editar y eliminar (tecnicos, clientes y administradores). */
@Composable
fun UsuariosAdminScreen(
    onVolver: () -> Unit,
    onNuevoUsuario: () -> Unit,
    onEditarUsuario: (Long) -> Unit,
    vm: UsuariosViewModel = viewModel(factory = UsuariosViewModel.Factory)
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val filtros: List<Rol?> = listOf(null) + Rol.entries

    // Recarga cada vez que se vuelve a esta pantalla (por ejemplo, tras crear un usuario)
    LaunchedEffect(Unit) { vm.recargar() }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.mensajeMostrado()
        }
    }

    Scaffold(
        topBar = { BarraSuperior("Usuarios", subtitulo = "${estado.usuarios.size} registrados", onVolver = onVolver) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevoUsuario,
                icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                text = { Text("Nuevo usuario") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtros) { rol ->
                    FilterChip(
                        selected = estado.filtro == rol,
                        onClick = { vm.cambiarFiltro(rol) },
                        label = { Text("${rol?.etiqueta ?: "Todos"} (${estado.cantidad(rol)})") }
                    )
                }
            }
            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                estado.error != null -> EstadoVacio(
                    icono = Icons.Outlined.CloudOff,
                    titulo = "No se pudo cargar",
                    mensaje = estado.error.orEmpty()
                ) { Button(onClick = vm::recargar) { Text("Reintentar") } }
                estado.filtrados.isEmpty() -> EstadoVacio(
                    icono = Icons.Outlined.Group,
                    titulo = "Sin usuarios",
                    mensaje = "Crea el primero con el boton Nuevo usuario."
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(estado.filtrados, key = { it.id ?: it.correo }) { u ->
                        TarjetaUsuario(
                            usuario = u,
                            onEditar = { u.id?.let(onEditarUsuario) },
                            onEliminar = { vm.pedirEliminar(u) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    estado.porEliminar?.let { u ->
        AlertDialog(
            onDismissRequest = vm::cancelarEliminar,
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Eliminar usuario") },
            text = { Text("¿Eliminar a ${u.nombre}? Si tiene tickets o solicitudes, el sistema pedira desactivarlo en su lugar.") },
            confirmButton = { TextButton(onClick = vm::eliminar) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = vm::cancelarEliminar) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaUsuario(usuario: UsuarioDto, onEditar: () -> Unit, onEliminar: () -> Unit, modifier: Modifier = Modifier) {
    val rol = Rol.entries.firstOrNull { it.name == usuario.rol }
    Card(
        onClick = onEditar,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconoCircular(iconoDeRol(usuario.rol))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(usuario.nombre, style = MaterialTheme.typography.titleSmall)
                Text(usuario.correo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    listOfNotNull(rol?.etiqueta, usuario.empresaNombre, if (usuario.activo) null else "Inactivo").joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (usuario.activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = onEditar) { Icon(Icons.Outlined.Edit, contentDescription = "Editar ${usuario.nombre}") }
            IconButton(onClick = onEliminar) { Icon(Icons.Outlined.Delete, contentDescription = "Eliminar ${usuario.nombre}") }
        }
    }
}

/** Icono para la empresa (reutilizado en la lista de empresas). */
internal val IconoEmpresa: ImageVector get() = Icons.Outlined.Business
