package cl.duoc.serviciotecnico.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.domain.validation.CampoUsuario
import cl.duoc.serviciotecnico.ui.components.BarraSuperior
import cl.duoc.serviciotecnico.ui.components.BotonGuardar
import cl.duoc.serviciotecnico.ui.components.CampoTextoValidado
import cl.duoc.serviciotecnico.ui.components.SelectorDeOpciones
import cl.duoc.serviciotecnico.ui.components.SelectorDesplegable
import cl.duoc.serviciotecnico.ui.components.TarjetaSeccion
import cl.duoc.serviciotecnico.ui.viewmodel.FormularioUsuarioViewModel

/** Alta o edicion de un usuario. Al elegir "Cliente" aparece la empresa (obligatoria). */
@Composable
fun FormularioUsuarioScreen(
    usuarioId: Long?,
    onVolver: () -> Unit,
    vm: FormularioUsuarioViewModel = viewModel(factory = FormularioUsuarioViewModel.factory(usuarioId))
) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val habilitado = !estado.guardando && !estado.cargando

    LaunchedEffect(estado.guardado) { if (estado.guardado) onVolver() }
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.mensajeMostrado()
        }
    }

    Scaffold(
        topBar = { BarraSuperior(if (estado.esNuevo) "Nuevo usuario" else "Editar usuario", onVolver = onVolver) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { relleno ->
        Column(
            modifier = Modifier.fillMaxSize().padding(relleno).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (estado.cargando) LinearProgressIndicator(Modifier.fillMaxWidth())

            TarjetaSeccion(titulo = "Rol", icono = Icons.Outlined.ManageAccounts, descripcion = "Define que panel vera al ingresar") {
                SelectorDeOpciones(
                    opciones = Rol.entries,
                    seleccionada = estado.rol,
                    etiqueta = { it.etiqueta },
                    onSeleccionar = vm::cambiarRol,
                    habilitado = habilitado,
                    icono = { iconoDeRol(it.name) }
                )
                // La empresa solo aplica a clientes: aparece y desaparece con animacion
                AnimatedVisibility(visible = estado.pideEmpresa) {
                    SelectorDesplegable(
                        etiqueta = "Empresa *",
                        opciones = estado.empresas,
                        seleccionado = estado.empresa,
                        textoDe = { "${it.nombre} · ${it.rut}" },
                        onSeleccionar = vm::cambiarEmpresa,
                        icono = Icons.Outlined.Business,
                        error = estado.error(CampoUsuario.EMPRESA),
                        habilitado = habilitado,
                        textoVacio = "No hay empresas. Crea una en Empresas."
                    )
                }
            }

            TarjetaSeccion(titulo = "Datos de acceso", icono = Icons.Outlined.Badge) {
                CampoTextoValidado(
                    valor = estado.nombre, onValorCambia = { vm.cambiarCampo(CampoUsuario.NOMBRE, it) },
                    etiqueta = "Nombre completo", icono = Icons.Outlined.Badge, obligatorio = true,
                    capitalizacion = KeyboardCapitalization.Words,
                    error = estado.error(CampoUsuario.NOMBRE), valido = estado.valido(CampoUsuario.NOMBRE), habilitado = habilitado
                )
                CampoTextoValidado(
                    valor = estado.correo, onValorCambia = { vm.cambiarCampo(CampoUsuario.CORREO, it) },
                    etiqueta = "Correo", icono = Icons.Outlined.Email, obligatorio = true, tipoDeTeclado = KeyboardType.Email,
                    error = estado.error(CampoUsuario.CORREO), valido = estado.valido(CampoUsuario.CORREO), habilitado = habilitado
                )
                CampoTextoValidado(
                    valor = estado.clave, onValorCambia = { vm.cambiarCampo(CampoUsuario.CLAVE, it) },
                    etiqueta = if (estado.esNuevo) "Contraseña" else "Nueva contraseña", icono = Icons.Outlined.Lock,
                    obligatorio = estado.esNuevo, esClave = true, tipoDeTeclado = KeyboardType.Password,
                    ayuda = if (estado.esNuevo) "Minimo 6 caracteres" else "Dejala vacia para no cambiarla",
                    accionDelTeclado = ImeAction.Done,
                    error = estado.error(CampoUsuario.CLAVE), valido = estado.valido(CampoUsuario.CLAVE) && estado.clave.isNotEmpty(),
                    habilitado = habilitado
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Usuario activo", style = MaterialTheme.typography.bodyLarge)
                        Text("Un usuario inactivo no puede ingresar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = estado.activo, onCheckedChange = vm::cambiarActivo, enabled = habilitado)
                }
            }

            BotonGuardar(onClick = vm::guardar, texto = if (estado.esNuevo) "Crear usuario" else "Guardar cambios", habilitado = !estado.cargando, guardando = estado.guardando)
        }
    }
}
