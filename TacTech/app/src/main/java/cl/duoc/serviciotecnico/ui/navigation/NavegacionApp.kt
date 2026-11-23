package cl.duoc.serviciotecnico.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.serviciotecnico.ServicioTecnicoApp
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.ui.screens.AdminHomeScreen
import cl.duoc.serviciotecnico.ui.screens.ClienteHomeScreen
import cl.duoc.serviciotecnico.ui.screens.ConfirmarSolicitudScreen
import cl.duoc.serviciotecnico.ui.screens.EmpresasAdminScreen
import cl.duoc.serviciotecnico.ui.screens.FormularioEmpresaScreen
import cl.duoc.serviciotecnico.ui.screens.FormularioSolicitudScreen
import cl.duoc.serviciotecnico.ui.screens.FormularioUsuarioScreen
import cl.duoc.serviciotecnico.ui.screens.SolicitudesAdminScreen
import cl.duoc.serviciotecnico.ui.screens.UsuariosAdminScreen
import cl.duoc.serviciotecnico.ui.screens.DetalleGuiaScreen
import cl.duoc.serviciotecnico.ui.screens.DetalleTicketScreen
import cl.duoc.serviciotecnico.ui.screens.FormularioGuiaScreen
import cl.duoc.serviciotecnico.ui.screens.GestionTicketsAdminScreen
import cl.duoc.serviciotecnico.ui.screens.ListadoTicketsScreen
import cl.duoc.serviciotecnico.ui.screens.LoginScreen
import cl.duoc.serviciotecnico.ui.viewmodel.FiltroAdmin

private const val DURACION = 320

/** Pantalla de inicio segun la sesion guardada en DataStore. */
private sealed interface Inicio {
    data object Cargando : Inicio
    data class Listo(val ruta: String) : Inicio
}

/** Cada rol tiene su propio panel. */
private fun rutaDeInicio(rol: Rol): String = when (rol) {
    Rol.ADMINISTRADOR -> Rutas.ADMIN_HOME
    Rol.TECNICO -> Rutas.LISTADO_TICKETS
    Rol.CLIENTE -> Rutas.CLIENTE_HOME
}

@Composable
fun NavegacionApp() {
    val app = LocalContext.current.applicationContext as ServicioTecnicoApp

    // DataStore se lee de forma asincrona: mientras tanto se muestra un indicador
    val inicio by produceState<Inicio>(initialValue = Inicio.Cargando) {
        val usuario = app.sesionRepository.usuarioActual()
        value = Inicio.Listo(usuario?.let { rutaDeInicio(it.rol) } ?: Rutas.LOGIN)
    }

    when (val i = inicio) {
        Inicio.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is Inicio.Listo -> Grafo(rememberNavController(), i.ruta)
    }
}

@Composable
private fun Grafo(navController: NavHostController, inicio: String) {
    // Al cerrar sesion se vacia la pila completa para que "atras" no vuelva a una pantalla privada
    val irAlLogin: () -> Unit = {
        navController.navigate(Rutas.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = inicio,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(DURACION)) +
                fadeIn(tween(DURACION))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(DURACION)) +
                fadeOut(tween(DURACION))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(DURACION)) +
                fadeIn(tween(DURACION))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(DURACION)) +
                fadeOut(tween(DURACION))
        }
    ) {

        composable(Rutas.LOGIN) {
            LoginScreen(
                onIngresado = { rol ->
                    // Se saca el login de la pila para que "atras" no vuelva a el
                    navController.navigate(rutaDeInicio(rol)) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // ---------- Cliente ----------

        composable(Rutas.CLIENTE_HOME) {
            ClienteHomeScreen(
                onNuevaSolicitud = { navController.navigate(Rutas.NUEVA_SOLICITUD) },
                onSesionCerrada = irAlLogin
            )
        }

        composable(Rutas.NUEVA_SOLICITUD) {
            FormularioSolicitudScreen(onVolver = { navController.popBackStack() })
        }

        // ---------- Tecnico ----------

        composable(Rutas.LISTADO_TICKETS) {
            ListadoTicketsScreen(
                onAbrirTicket = { ticketId -> navController.navigate(Rutas.detalleTicket(ticketId)) },
                onSesionCerrada = irAlLogin
            )
        }

        // Compartida: el tecnico la usa para trabajar y el administrador para gestionar el estado
        composable(
            route = Rutas.DETALLE_TICKET,
            arguments = listOf(navArgument(Rutas.ARG_TICKET_ID) { type = NavType.StringType })
        ) { entrada ->
            val ticketId = entrada.arguments?.getString(Rutas.ARG_TICKET_ID).orEmpty()
            DetalleTicketScreen(
                ticketId = ticketId,
                onVolver = { navController.popBackStack() },
                onNuevaGuia = { navController.navigate(Rutas.formularioGuia(ticketId)) },
                onAbrirGuia = { guiaId -> navController.navigate(Rutas.detalleGuia(guiaId)) }
            )
        }

        composable(
            route = Rutas.FORMULARIO_GUIA,
            arguments = listOf(navArgument(Rutas.ARG_TICKET_ID) { type = NavType.StringType })
        ) { entrada ->
            val ticketId = entrada.arguments?.getString(Rutas.ARG_TICKET_ID).orEmpty()
            FormularioGuiaScreen(
                ticketId = ticketId,
                onVolver = { navController.popBackStack() }
            )
        }

        composable(
            route = Rutas.DETALLE_GUIA,
            arguments = listOf(navArgument(Rutas.ARG_GUIA_ID) { type = NavType.StringType })
        ) { entrada ->
            val guiaId = entrada.arguments?.getString(Rutas.ARG_GUIA_ID).orEmpty()
            DetalleGuiaScreen(
                guiaId = guiaId,
                onVolver = { navController.popBackStack() }
            )
        }

        // ---------- Administrador ----------

        composable(Rutas.ADMIN_HOME) {
            AdminHomeScreen(
                onGestionarTickets = { filtro, tecnico ->
                    navController.navigate(Rutas.adminTickets(filtro.name, tecnico))
                },
                onSolicitudes = { navController.navigate(Rutas.ADMIN_SOLICITUDES) },
                onUsuarios = { navController.navigate(Rutas.ADMIN_USUARIOS) },
                onEmpresas = { navController.navigate(Rutas.ADMIN_EMPRESAS) },
                onSesionCerrada = irAlLogin
            )
        }

        composable(Rutas.ADMIN_SOLICITUDES) {
            SolicitudesAdminScreen(
                onVolver = { navController.popBackStack() },
                onAbrirSolicitud = { id -> navController.navigate(Rutas.adminSolicitud(id)) }
            )
        }

        composable(
            route = Rutas.ADMIN_SOLICITUD,
            arguments = listOf(navArgument(Rutas.ARG_SOLICITUD_ID) { type = NavType.StringType })
        ) { entrada ->
            ConfirmarSolicitudScreen(
                solicitudId = entrada.arguments?.getString(Rutas.ARG_SOLICITUD_ID).orEmpty(),
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Rutas.ADMIN_USUARIOS) {
            UsuariosAdminScreen(
                onVolver = { navController.popBackStack() },
                onNuevoUsuario = { navController.navigate(Rutas.adminUsuarioForm(null)) },
                onEditarUsuario = { id -> navController.navigate(Rutas.adminUsuarioForm(id)) }
            )
        }

        composable(
            route = Rutas.ADMIN_USUARIO_FORM,
            arguments = listOf(navArgument(Rutas.ARG_USUARIO_ID) { type = NavType.LongType })
        ) { entrada ->
            val id = entrada.arguments?.getLong(Rutas.ARG_USUARIO_ID) ?: -1L
            FormularioUsuarioScreen(
                usuarioId = id.takeIf { it > 0 },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(Rutas.ADMIN_EMPRESAS) {
            EmpresasAdminScreen(
                onVolver = { navController.popBackStack() },
                onNuevaEmpresa = { navController.navigate(Rutas.adminEmpresaForm(null)) },
                onEditarEmpresa = { id -> navController.navigate(Rutas.adminEmpresaForm(id)) }
            )
        }

        composable(
            route = Rutas.ADMIN_EMPRESA_FORM,
            arguments = listOf(navArgument(Rutas.ARG_EMPRESA_ID) { type = NavType.LongType })
        ) { entrada ->
            val id = entrada.arguments?.getLong(Rutas.ARG_EMPRESA_ID) ?: -1L
            FormularioEmpresaScreen(
                empresaId = id.takeIf { it > 0 },
                onVolver = { navController.popBackStack() }
            )
        }

        composable(
            route = Rutas.ADMIN_TICKETS,
            arguments = listOf(
                navArgument(Rutas.ARG_FILTRO) {
                    type = NavType.StringType
                    defaultValue = FiltroAdmin.PENDIENTES.name
                },
                navArgument(Rutas.ARG_TECNICO) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { entrada ->
            val filtro = entrada.arguments?.getString(Rutas.ARG_FILTRO)
                ?.let { nombre -> FiltroAdmin.entries.firstOrNull { it.name == nombre } }
                ?: FiltroAdmin.PENDIENTES
            val tecnico = entrada.arguments?.getString(Rutas.ARG_TECNICO).orEmpty().ifBlank { null }
            GestionTicketsAdminScreen(
                filtroInicial = filtro,
                tecnicoInicial = tecnico,
                onVolver = { navController.popBackStack() },
                onAbrirTicket = { ticketId -> navController.navigate(Rutas.detalleTicket(ticketId)) }
            )
        }
    }
}
