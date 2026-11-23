package cl.duoc.serviciotecnico.data.remote

import cl.duoc.serviciotecnico.data.remote.dto.CambioEstadoRequest
import cl.duoc.serviciotecnico.data.remote.dto.ConfirmarSolicitudRequest
import cl.duoc.serviciotecnico.data.remote.dto.EmpresaDto
import cl.duoc.serviciotecnico.data.remote.dto.MotivoRequest
import cl.duoc.serviciotecnico.data.remote.dto.SolicitudDto
import cl.duoc.serviciotecnico.data.remote.dto.UsuarioRequest
import cl.duoc.serviciotecnico.data.remote.dto.EquipoDto
import cl.duoc.serviciotecnico.data.remote.dto.GuiaDto
import cl.duoc.serviciotecnico.data.remote.dto.LoginRequest
import cl.duoc.serviciotecnico.data.remote.dto.ResumenDto
import cl.duoc.serviciotecnico.data.remote.dto.TicketDto
import cl.duoc.serviciotecnico.data.remote.dto.UsuarioDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrato HTTP con el microservicio tactech-backend.
 *
 * Todas son funciones suspend: Retrofit las ejecuta fuera del hilo principal y la
 * corrutina espera la respuesta sin bloquear la interfaz.
 *
 * Para sincronizar se usa PUT /{id}: el id (UUID) lo genera la app, asi que enviar
 * el mismo registro dos veces no lo duplica (operacion idempotente).
 */
interface ApiService {

    // ---------- Autenticacion y usuarios ----------

    @POST("api/auth/login")
    suspend fun login(@Body datos: LoginRequest): UsuarioDto

    /** rol opcional: TECNICO, CLIENTE o ADMINISTRADOR. */
    @GET("api/usuarios")
    suspend fun usuarios(@Query("rol") rol: String? = null): List<UsuarioDto>

    @POST("api/usuarios")
    suspend fun crearUsuario(@Body datos: UsuarioRequest): UsuarioDto

    @PUT("api/usuarios/{id}")
    suspend fun actualizarUsuario(@Path("id") id: Long, @Body datos: UsuarioRequest): UsuarioDto

    @DELETE("api/usuarios/{id}")
    suspend fun eliminarUsuario(@Path("id") id: Long): Response<Unit>

    // ---------- Empresas ----------

    @GET("api/empresas")
    suspend fun empresas(): List<EmpresaDto>

    @POST("api/empresas")
    suspend fun crearEmpresa(@Body datos: EmpresaDto): EmpresaDto

    @PUT("api/empresas/{id}")
    suspend fun actualizarEmpresa(@Path("id") id: Long, @Body datos: EmpresaDto): EmpresaDto

    @DELETE("api/empresas/{id}")
    suspend fun eliminarEmpresa(@Path("id") id: Long): Response<Unit>

    // ---------- Solicitudes ----------

    @GET("api/solicitudes")
    suspend fun solicitudes(
        @Query("estado") estado: String? = null,
        @Query("cliente") cliente: String? = null
    ): List<SolicitudDto>

    /** Upsert desde la app del cliente. 409 si el administrador ya la proceso. */
    @PUT("api/solicitudes/{id}")
    suspend fun guardarSolicitud(@Path("id") id: String, @Body solicitud: SolicitudDto): SolicitudDto

    @PUT("api/solicitudes/{id}/rechazar")
    suspend fun rechazarSolicitud(@Path("id") id: String, @Body datos: MotivoRequest): SolicitudDto

    /** Crea el ticket formal asignado al tecnico. */
    @POST("api/solicitudes/{id}/confirmar")
    suspend fun confirmarSolicitud(@Path("id") id: String, @Body datos: ConfirmarSolicitudRequest): TicketDto

    // ---------- Equipos ----------

    @GET("api/equipos")
    suspend fun equipos(): List<EquipoDto>

    @PUT("api/equipos/{id}")
    suspend fun guardarEquipo(@Path("id") id: String, @Body equipo: EquipoDto): EquipoDto

    // ---------- Tickets ----------

    /** estado: PENDIENTES, TERMINADOS o el nombre de un estado. tecnico: correo. Ambos opcionales. */
    @GET("api/tickets")
    suspend fun tickets(
        @Query("estado") estado: String? = null,
        @Query("tecnico") tecnico: String? = null,
        @Query("cliente") cliente: String? = null
    ): List<TicketDto>

    @GET("api/tickets/resumen")
    suspend fun resumen(): ResumenDto

    @POST("api/tickets")
    suspend fun crearTicket(@Body ticket: TicketDto): TicketDto

    @PUT("api/tickets/{id}")
    suspend fun guardarTicket(@Path("id") id: String, @Body ticket: TicketDto): TicketDto

    @PUT("api/tickets/{id}/estado")
    suspend fun cambiarEstado(@Path("id") id: String, @Body cambio: CambioEstadoRequest): TicketDto

    @DELETE("api/tickets/{id}")
    suspend fun eliminarTicket(@Path("id") id: String): Response<Unit>

    // ---------- Guias ----------

    @GET("api/guias")
    suspend fun guias(
        @Query("ticketId") ticketId: String? = null,
        @Query("tecnico") tecnico: String? = null
    ): List<GuiaDto>

    @PUT("api/guias/{id}")
    suspend fun guardarGuia(@Path("id") id: String, @Body guia: GuiaDto): GuiaDto
}
