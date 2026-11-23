package cl.duoc.serviciotecnico.ui.navigation

object Rutas {

    const val LOGIN = "login"

    // ---------- Cliente ----------

    const val CLIENTE_HOME = "cliente"
    const val NUEVA_SOLICITUD = "cliente/solicitudes/nueva"

    // ---------- Tecnico ----------

    const val LISTADO_TICKETS = "tickets"

    const val DETALLE_TICKET = "tickets/{ticketId}"
    fun detalleTicket(ticketId: String): String = "tickets/$ticketId"

    const val FORMULARIO_GUIA = "tickets/{ticketId}/guias/formulario"
    fun formularioGuia(ticketId: String): String = "tickets/$ticketId/guias/formulario"

    const val DETALLE_GUIA = "guias/{guiaId}"
    fun detalleGuia(guiaId: String): String = "guias/$guiaId"

    // ---------- Administrador ----------

    const val ADMIN_HOME = "admin"

    const val ADMIN_TICKETS = "admin/tickets?filtro={filtro}&tecnico={tecnico}"
    fun adminTickets(filtro: String, tecnico: String?): String =
        "admin/tickets?filtro=$filtro&tecnico=${android.net.Uri.encode(tecnico.orEmpty())}"

    const val ADMIN_SOLICITUDES = "admin/solicitudes"

    const val ADMIN_SOLICITUD = "admin/solicitudes/{solicitudId}"
    fun adminSolicitud(id: String): String = "admin/solicitudes/$id"

    const val ADMIN_USUARIOS = "admin/usuarios"

    /** id -1 = usuario nuevo. */
    const val ADMIN_USUARIO_FORM = "admin/usuarios/{usuarioId}"
    fun adminUsuarioForm(id: Long?): String = "admin/usuarios/${id ?: -1}"

    const val ADMIN_EMPRESAS = "admin/empresas"

    /** id -1 = empresa nueva. */
    const val ADMIN_EMPRESA_FORM = "admin/empresas/{empresaId}"
    fun adminEmpresaForm(id: Long?): String = "admin/empresas/${id ?: -1}"

    const val ARG_TICKET_ID = "ticketId"
    const val ARG_SOLICITUD_ID = "solicitudId"
    const val ARG_USUARIO_ID = "usuarioId"
    const val ARG_EMPRESA_ID = "empresaId"
    const val ARG_GUIA_ID = "guiaId"
    const val ARG_FILTRO = "filtro"
    const val ARG_TECNICO = "tecnico"
}
