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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.data.remote.dto.EmpresaDto
import cl.duoc.serviciotecnico.ui.components.BarraSuperior
import cl.duoc.serviciotecnico.ui.components.EstadoVacio
import cl.duoc.serviciotecnico.ui.components.IconoCircular
import cl.duoc.serviciotecnico.ui.viewmodel.EmpresasViewModel

/** CRUD de empresas cliente. */
@Composable
fun EmpresasAdminScreen(
    onVolver: () -> Unit,
    onNuevaEmpresa: () -> Unit,
    onEditarEmpresa: (Long) -> Unit,
    vm: EmpresasViewModel = viewModel(factory = EmpresasViewModel.Factory)
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.recargar() }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.mensajeMostrado()
        }
    }

    Scaffold(
        topBar = { BarraSuperior("Empresas", subtitulo = "${estado.empresas.size} registradas", onVolver = onVolver) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevaEmpresa,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nueva empresa") },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(relleno), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            estado.error != null -> EstadoVacio(
                icono = Icons.Outlined.CloudOff,
                titulo = "No se pudo cargar",
                mensaje = estado.error.orEmpty(),
                modifier = Modifier.padding(relleno)
            ) { Button(onClick = vm::recargar) { Text("Reintentar") } }
            estado.empresas.isEmpty() -> EstadoVacio(
                icono = IconoEmpresa,
                titulo = "Sin empresas",
                mensaje = "Registra la primera empresa cliente.",
                modifier = Modifier.padding(relleno)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(relleno),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(estado.empresas, key = { it.id ?: it.rut }) { e ->
                    TarjetaEmpresa(e, onEditar = { e.id?.let(onEditarEmpresa) }, onEliminar = { vm.pedirEliminar(e) }, modifier = Modifier.animateItem())
                }
            }
        }
    }

    estado.porEliminar?.let { e ->
        AlertDialog(
            onDismissRequest = vm::cancelarEliminar,
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Eliminar empresa") },
            text = { Text("¿Eliminar ${e.nombre}? No se puede si tiene usuarios cliente.") },
            confirmButton = { TextButton(onClick = vm::eliminar) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = vm::cancelarEliminar) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun TarjetaEmpresa(empresa: EmpresaDto, onEditar: () -> Unit, onEliminar: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onEditar,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconoCircular(IconoEmpresa)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(empresa.nombre, style = MaterialTheme.typography.titleSmall)
                Text("RUT ${empresa.rut}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    listOfNotNull(empresa.direccion, empresa.telefono).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEditar) { Icon(Icons.Outlined.Edit, contentDescription = "Editar ${empresa.nombre}") }
            IconButton(onClick = onEliminar) { Icon(Icons.Outlined.Delete, contentDescription = "Eliminar ${empresa.nombre}") }
        }
    }
}
