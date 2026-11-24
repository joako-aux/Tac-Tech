package cl.duoc.serviciotecnico.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.ui.components.BannerConexion
import cl.duoc.serviciotecnico.ui.components.BarraSuperior
import cl.duoc.serviciotecnico.ui.components.EstadoVacio
import cl.duoc.serviciotecnico.ui.components.TarjetaTicket
import cl.duoc.serviciotecnico.ui.viewmodel.AdminViewModel
import cl.duoc.serviciotecnico.ui.viewmodel.FiltroAdmin

/**
 * Gestion de tickets del Administrador: filtra por pendientes o terminados, por tecnico
 * y por texto. Al tocar un ticket se abre su detalle, donde puede cambiar su estado.
 */
@Composable
fun GestionTicketsAdminScreen(
    filtroInicial: FiltroAdmin,
    tecnicoInicial: String?,
    onVolver: () -> Unit,
    onAbrirTicket: (String) -> Unit,
    vm: AdminViewModel = viewModel(factory = AdminViewModel.factory(filtroInicial, tecnicoInicial))
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val lista = estado.filtrados

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = "Gestion de tickets",
                subtitulo = "${lista.size} de ${estado.tickets.size} tickets",
                onVolver = onVolver
            )
        }
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno)) {
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BannerConexion(estado.sync, onSincronizar = vm::sincronizarAhora)

                OutlinedTextField(
                    value = estado.busqueda,
                    onValueChange = vm::cambiarBusqueda,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    placeholder = { Text("Buscar por N°, titulo o cliente") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (estado.busqueda.isNotEmpty()) {
                            IconButton(onClick = { vm.cambiarBusqueda("") }) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Limpiar busqueda")
                            }
                        }
                    }
                )
            }

            // Filtro principal: pendientes / terminados
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FiltroAdmin.entries) { filtro ->
                    val cantidad = estado.cantidad(filtro)
                    FilterChip(
                        selected = estado.filtro == filtro,
                        onClick = { vm.cambiarFiltro(filtro) },
                        label = { Text("${filtro.etiqueta} ($cantidad)") },
                        leadingIcon = if (estado.filtro == filtro) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }

            // Filtro por tecnico
            if (estado.tecnicos.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = estado.tecnico == null,
                            onClick = { vm.cambiarTecnico(null) },
                            label = { Text("Todos los tecnicos") }
                        )
                    }
                    items(estado.tecnicos) { correo ->
                        FilterChip(
                            selected = estado.tecnico == correo,
                            onClick = { vm.cambiarTecnico(correo) },
                            label = { Text(correo.substringBefore('@')) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Engineering, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                    }
                }
            }

            if (lista.isEmpty()) {
                EstadoVacio(
                    icono = Icons.Outlined.SearchOff,
                    titulo = "No hay tickets con estos filtros",
                    mensaje = "Prueba con otro estado, tecnico o texto de busqueda."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(lista, key = { it.id }) { ticket ->
                        TarjetaTicket(
                            ticket = ticket,
                            onClick = { onAbrirTicket(ticket.id) },
                            mostrarTecnico = true,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}
