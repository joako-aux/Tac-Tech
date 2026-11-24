package cl.duoc.serviciotecnico.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.serviciotecnico.data.repository.SesionRepository
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.domain.validation.CampoLogin
import cl.duoc.serviciotecnico.ui.components.BotonGuardar
import cl.duoc.serviciotecnico.ui.components.CampoTextoValidado
import cl.duoc.serviciotecnico.ui.theme.PetroleoOscuro
import cl.duoc.serviciotecnico.ui.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onIngresado: (Rol) -> Unit,
    vm: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
) {
    val estado by vm.estado.collectAsStateWithLifecycle()

    // Segun el rol, la navegacion lleva a la vista del tecnico o al panel del administrador
    LaunchedEffect(estado.rolIngresado) {
        estado.rolIngresado?.let(onIngresado)
    }

    // Animacion de entrada: la tarjeta sube y aparece al abrir la pantalla
    val entrada = remember { MutableTransitionState(false).apply { targetState = true } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ---------- Encabezado de marca ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    brush = Brush.verticalGradient(listOf(PetroleoOscuro, MaterialTheme.colorScheme.primary)),
                    shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Box(
                    Modifier
                        .size(76.dp)
                        .background(Color.White.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AcUnit, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("TacTech", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text(
                    "Servicio tecnico en terreno",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        // ---------- Formulario ----------
        AnimatedVisibility(
            visibleState = entrada,
            enter = slideInVertically(tween(500)) { it / 4 } + fadeIn(tween(500))
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .offset(y = (-48).dp)
                    .fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Iniciar sesion", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Ingresa con tu cuenta de tecnico",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    CampoTextoValidado(
                        valor = estado.correo,
                        onValorCambia = vm::cambiarCorreo,
                        etiqueta = "Correo",
                        icono = Icons.Outlined.Email,
                        obligatorio = true,
                        tipoDeTeclado = KeyboardType.Email,
                        error = estado.formulario.error(CampoLogin.CORREO),
                        valido = estado.formulario.correcto(CampoLogin.CORREO),
                        habilitado = !estado.ingresando
                    )
                    CampoTextoValidado(
                        valor = estado.clave,
                        onValorCambia = vm::cambiarClave,
                        etiqueta = "Contraseña",
                        icono = Icons.Outlined.Lock,
                        obligatorio = true,
                        tipoDeTeclado = KeyboardType.Password,
                        accionDelTeclado = ImeAction.Done,
                        esClave = true,
                        error = estado.formulario.error(CampoLogin.CLAVE),
                        valido = estado.formulario.correcto(CampoLogin.CLAVE),
                        habilitado = !estado.ingresando
                    )

                    // Credenciales incorrectas: aviso animado
                    AnimatedVisibility(
                        visible = estado.mensaje != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.ReportProblem,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    estado.mensaje.orEmpty(),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    BotonGuardar(
                        onClick = vm::ingresar,
                        texto = "Ingresar",
                        icono = Icons.AutoMirrored.Filled.Login,
                        guardando = estado.ingresando
                    )
                }
            }
        }

        // ---------- Ayuda del prototipo ----------
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .offset(y = (-32).dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Administrador: ${SesionRepository.ADMIN_DEMO} / ${SesionRepository.CLAVE_ADMIN}\n" +
                    "Tecnico: ${SesionRepository.USUARIO_DEMO} / ${SesionRepository.CLAVE_DEMO}\n" +
                    "Cliente: ${SesionRepository.CLIENTE_DEMO} / ${SesionRepository.CLAVE_CLIENTE}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
