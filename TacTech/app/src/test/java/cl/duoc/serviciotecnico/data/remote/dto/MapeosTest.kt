package cl.duoc.serviciotecnico.data.remote.dto

import cl.duoc.serviciotecnico.data.local.entity.FotoGuia
import cl.duoc.serviciotecnico.domain.model.EstadoTicket
import cl.duoc.serviciotecnico.domain.model.Rol
import cl.duoc.serviciotecnico.testutil.FECHA_BASE
import cl.duoc.serviciotecnico.testutil.equipo
import cl.duoc.serviciotecnico.testutil.guia
import cl.duoc.serviciotecnico.testutil.solicitud
import cl.duoc.serviciotecnico.testutil.ticket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Capa de DATOS: conversion entre entidades de Room y los DTO que viajan al microservicio.
 * Lo que llega del servidor queda con pendienteSync = false (ya esta sincronizado).
 */
class MapeosTest {

    @Test
    fun ticketIdaYVuelta() {
        val original = ticket(estado = EstadoTicket.CERRADO, solicitudId = "s-9").copy(
            fechaCierre = FECHA_BASE.plusDays(1),
            solucion = "Cambio de capacitor",
            comentarioEstado = "Listo"
        )
        val dto = original.aDto()
        assertEquals("CERRADO", dto.estado)
        assertEquals("MEDIA", dto.prioridad)
        assertEquals("2025-11-02T10:30", dto.fechaCreacion)
        assertEquals(original.copy(pendienteSync = false), dto.aEntidad())
    }

    @Test
    fun ticketSinTecnicoViajaComoNull() {
        val dto = ticket(tecnicoCorreo = "").aDto()
        assertNull(dto.tecnicoCorreo)
        assertEquals("", dto.aEntidad().tecnicoCorreo)
        assertNull(dto.aEntidad().fechaCierre)
    }

    @Test
    fun equipoIdaYVuelta() {
        val original = equipo(pendienteSync = true)
        val dto = original.aDto()
        assertEquals("ARRENDADO", dto.propiedad)
        assertEquals(original.copy(pendienteSync = false), dto.aEntidad())
    }

    @Test
    fun guiaConFotosIdaYVuelta() {
        val original = guia(pendienteSync = true)
        val fotos = listOf(FotoGuia(id = "f-1", guiaId = original.id, uri = "content://foto1", fechaCaptura = FECHA_BASE))
        val dto = original.aDto(fotos)
        assertEquals(1, dto.fotos.size)
        assertEquals(original.copy(pendienteSync = false), dto.aEntidad())
        assertEquals(fotos, dto.fotosAEntidad())
    }

    @Test
    fun solicitudIdaYVuelta() {
        val original = solicitud(pendienteSync = true, ticketId = "t-1")
        val dto = original.aDto()
        assertEquals("POR_CONFIRMAR", dto.estado)
        val vuelta = dto.aEntidad()
        assertFalse(vuelta.pendienteSync)
        assertEquals(original.copy(pendienteSync = false), vuelta)
    }

    @Test
    fun usuarioDelLogin() {
        val dto = UsuarioDto(id = 4, nombre = "Cliente Demo", correo = "cliente@tactech.cl", rol = "CLIENTE", empresaNombre = "Clinica")
        val usuario = dto.aUsuario()
        assertEquals(Rol.CLIENTE, usuario.rol)
        assertEquals("Clinica", usuario.empresaNombre)
    }
}
