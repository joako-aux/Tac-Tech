package com.example.tactech_backend.service;

import com.example.tactech_backend.dto.SolicitudDto;
import com.example.tactech_backend.entity.EstadoSolicitud;
import com.example.tactech_backend.entity.Propiedad;
import com.example.tactech_backend.entity.Solicitud;
import com.example.tactech_backend.exception.ReglaNegocioException;
import com.example.tactech_backend.repository.EquipoRepository;
import com.example.tactech_backend.repository.SolicitudRepository;
import com.example.tactech_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Reglas del ciclo de vida de una solicitud, sin base de datos (Mockito). */
@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private TicketService ticketService;

    @InjectMocks
    private SolicitudService solicitudService;

    private Solicitud solicitud(EstadoSolicitud estado) {
        return Solicitud.builder()
                .id("s-1").clienteCorreo("cliente@tactech.cl").titulo("Equipo no enfria")
                .descripcion("El split no baja de 26 grados").direccion("Estoril 450, Las Condes")
                .equipoNombre("Split muro").equipoModelo("MSAFB-12").equipoSerie("MID-12345")
                .propiedad(Propiedad.PROPIO_DEL_CLIENTE).estado(estado)
                .fechaCreacion(LocalDateTime.now()).fechaActualizacion(LocalDateTime.now())
                .build();
    }

    @Test
    void clientePuedeCancelarMientrasEstaPorConfirmar() {
        when(solicitudRepository.findById("s-1")).thenReturn(Optional.of(solicitud(EstadoSolicitud.POR_CONFIRMAR)));
        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(i -> i.getArgument(0));

        SolicitudDto resultado = solicitudService.cancelar("s-1", "La cree por error");

        assertEquals(EstadoSolicitud.CANCELADA, resultado.getEstado());
    }

    @Test
    void noSePuedeCancelarUnaSolicitudYaConfirmada() {
        when(solicitudRepository.findById("s-1")).thenReturn(Optional.of(solicitud(EstadoSolicitud.CONFIRMADA)));
        assertThrows(ReglaNegocioException.class, () -> solicitudService.cancelar("s-1", "La cree por error"));
    }

    @Test
    void clienteNoPuedeConfirmarDesdeLaApp() {
        SolicitudDto datos = new SolicitudDto();
        datos.setEstado(EstadoSolicitud.CONFIRMADA);
        assertThrows(ReglaNegocioException.class, () -> solicitudService.guardarDesdeCliente("s-1", datos));
    }
}
