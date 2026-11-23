package cl.duoc.serviciotecnico.testutil

import cl.duoc.serviciotecnico.data.remote.ApiService
import cl.duoc.serviciotecnico.data.remote.dto.CambioEstadoRequest
import cl.duoc.serviciotecnico.data.remote.dto.ConfirmarSolicitudRequest
import cl.duoc.serviciotecnico.data.remote.dto.EmpresaDto
import cl.duoc.serviciotecnico.data.remote.dto.EquipoDto
import cl.duoc.serviciotecnico.data.remote.dto.GuiaDto
import cl.duoc.serviciotecnico.data.remote.dto.LoginRequest
import cl.duoc.serviciotecnico.data.remote.dto.MotivoRequest
import cl.duoc.serviciotecnico.data.remote.dto.ResumenDto
import cl.duoc.serviciotecnico.data.remote.dto.SolicitudDto
import cl.duoc.serviciotecnico.data.remote.dto.TicketDto
import cl.duoc.serviciotecnico.data.remote.dto.UsuarioDto
import cl.duoc.serviciotecnico.data.remote.dto.UsuarioRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/**
 * Microservicio tactech-backend falso, en memoria.
 *
 * Simula las respuestas del servidor sin red: guarda lo que recibe, devuelve listas filtradas
 * y permite forzar fallas para probar los caminos de error:
 * - [fallaRed]: toda llamada lanza esta excepcion (ej: IOException = sin internet).
 * - [conflictos]: ids que responden 409 al subirlos (el servidor ya los proceso).
 * - [llamadas]: registro de lo que la app pidio, para verificar el orden y los filtros.
 */
class ApiServiceFalsa : ApiService {

    var fallaRed: Exception? = null
    var conflictos: Set<String> = emptySet()
    val llamadas = mutableListOf<String>()

    // ---------- "Base de datos" del servidor ----------
    var usuarioLogin: UsuarioDto? = null
    val usuarios = mutableListOf<UsuarioDto>()
    val empresas = mutableListOf<EmpresaDto>()
    val solicitudes = mutableListOf<SolicitudDto>()
    val equipos = mutableListOf<EquipoDto>()
    val tickets = mutableListOf<TicketDto>()
    val guias = mutableListOf<GuiaDto>()

    /** Respuesta de POST /confirmar (el ticket que crea el servidor). */
    var ticketConfirmado: TicketDto? = null

    private fun registrar(llamada: String) {
        llamadas += llamada
        fallaRed?.let { throw it }
    }

    private fun conflicto(id: String) {
        if (id in conflictos) throw errorHttp(409, """{"estado":409,"mensaje":"Ya fue procesada"}""")
    }

    private fun noEncontrado(): Response<Unit> =
        Response.error(404, """{"estado":404,"mensaje":"No existe"}""".toResponseBody("application/json".toMediaType()))

    // ---------- Autenticacion y usuarios ----------

    override suspend fun login(datos: LoginRequest): UsuarioDto {
        registrar("login")
        return usuarioLogin ?: throw errorHttp(401, """{"estado":401,"mensaje":"Credenciales invalidas"}""")
    }

    override suspend fun usuarios(rol: String?): List<UsuarioDto> {
        registrar("usuarios(rol=$rol)")
        return usuarios.filter { rol == null || it.rol == rol }
    }

    override suspend fun crearUsuario(datos: UsuarioRequest): UsuarioDto {
        registrar("crearUsuario")
        val nuevo = UsuarioDto((usuarios.size + 1).toLong(), datos.nombre, datos.correo, datos.rol, datos.activo, datos.empresaId)
        usuarios += nuevo
        return nuevo
    }

    override suspend fun actualizarUsuario(id: Long, datos: UsuarioRequest): UsuarioDto {
        registrar("actualizarUsuario($id)")
        val actualizado = UsuarioDto(id, datos.nombre, datos.correo, datos.rol, datos.activo, datos.empresaId)
        usuarios.replaceAll { if (it.id == id) actualizado else it }
        return actualizado
    }

    override suspend fun eliminarUsuario(id: Long): Response<Unit> {
        registrar("eliminarUsuario($id)")
        return if (usuarios.removeIf { it.id == id }) Response.success(Unit) else noEncontrado()
    }

    // ---------- Empresas ----------

    override suspend fun empresas(): List<EmpresaDto> {
        registrar("empresas")
        return empresas.toList()
    }

    override suspend fun crearEmpresa(datos: EmpresaDto): EmpresaDto {
        registrar("crearEmpresa")
        val nueva = datos.copy(id = (empresas.size + 1).toLong())
        empresas += nueva
        return nueva
    }

    override suspend fun actualizarEmpresa(id: Long, datos: EmpresaDto): EmpresaDto {
        registrar("actualizarEmpresa($id)")
        empresas.replaceAll { if (it.id == id) datos else it }
        return datos
    }

    override suspend fun eliminarEmpresa(id: Long): Response<Unit> {
        registrar("eliminarEmpresa($id)")
        return if (empresas.removeIf { it.id == id }) Response.success(Unit) else noEncontrado()
    }

    // ---------- Solicitudes ----------

    override suspend fun solicitudes(estado: String?, cliente: String?): List<SolicitudDto> {
        registrar("solicitudes(cliente=$cliente)")
        return solicitudes.filter { (estado == null || it.estado == estado) && (cliente == null || it.clienteCorreo == cliente) }
    }

    override suspend fun guardarSolicitud(id: String, solicitud: SolicitudDto): SolicitudDto {
        registrar("guardarSolicitud($id)")
        conflicto(id)
        solicitudes.removeIf { it.id == id }
        solicitudes += solicitud
        return solicitud
    }

    override suspend fun rechazarSolicitud(id: String, datos: MotivoRequest): SolicitudDto {
        registrar("rechazarSolicitud($id)")
        conflicto(id)
        val actual = solicitudes.first { it.id == id }
        val rechazada = actual.copy(estado = "RECHAZADA", motivo = datos.motivo)
        solicitudes.replaceAll { if (it.id == id) rechazada else it }
        return rechazada
    }

    override suspend fun confirmarSolicitud(id: String, datos: ConfirmarSolicitudRequest): TicketDto {
        registrar("confirmarSolicitud($id,${datos.tecnicoCorreo},${datos.prioridad})")
        conflicto(id)
        val creado = ticketConfirmado ?: throw errorHttp(404, """{"estado":404,"mensaje":"Tecnico no encontrado"}""")
        // Igual que el backend: en una transaccion confirma la solicitud y crea el ticket
        solicitudes.replaceAll { if (it.id == id) it.copy(estado = "CONFIRMADA", ticketId = creado.id) else it }
        tickets.removeIf { it.id == creado.id }
        tickets += creado
        return creado
    }

    // ---------- Equipos ----------

    override suspend fun equipos(): List<EquipoDto> {
        registrar("equipos")
        return equipos.toList()
    }

    override suspend fun guardarEquipo(id: String, equipo: EquipoDto): EquipoDto {
        registrar("guardarEquipo($id)")
        conflicto(id)
        equipos.removeIf { it.id == id }
        equipos += equipo
        return equipo
    }

    // ---------- Tickets ----------

    override suspend fun tickets(estado: String?, tecnico: String?, cliente: String?): List<TicketDto> {
        registrar("tickets(tecnico=$tecnico,cliente=$cliente)")
        val idsDelCliente = solicitudes.filter { it.clienteCorreo == cliente }.map { it.id }.toSet()
        return tickets.filter {
            (tecnico == null || it.tecnicoCorreo == tecnico) && (cliente == null || it.solicitudId in idsDelCliente)
        }
    }

    override suspend fun resumen(): ResumenDto {
        registrar("resumen")
        return ResumenDto(total = tickets.size.toLong(), pendientes = 0, terminados = 0)
    }

    override suspend fun crearTicket(ticket: TicketDto): TicketDto {
        registrar("crearTicket")
        tickets += ticket
        return ticket
    }

    override suspend fun guardarTicket(id: String, ticket: TicketDto): TicketDto {
        registrar("guardarTicket($id)")
        conflicto(id)
        tickets.removeIf { it.id == id }
        tickets += ticket
        return ticket
    }

    override suspend fun cambiarEstado(id: String, cambio: CambioEstadoRequest): TicketDto {
        registrar("cambiarEstado($id)")
        val actualizado = tickets.first { it.id == id }.copy(estado = cambio.estado, comentarioEstado = cambio.comentario)
        tickets.replaceAll { if (it.id == id) actualizado else it }
        return actualizado
    }

    override suspend fun eliminarTicket(id: String): Response<Unit> {
        registrar("eliminarTicket($id)")
        return if (tickets.removeIf { it.id == id }) Response.success(Unit) else noEncontrado()
    }

    // ---------- Guias ----------

    override suspend fun guias(ticketId: String?, tecnico: String?): List<GuiaDto> {
        registrar("guias(tecnico=$tecnico)")
        return guias.filter { ticketId == null || it.ticketId == ticketId }
    }

    override suspend fun guardarGuia(id: String, guia: GuiaDto): GuiaDto {
        registrar("guardarGuia($id)")
        conflicto(id)
        guias.removeIf { it.id == id }
        guias += guia
        return guia
    }
}
