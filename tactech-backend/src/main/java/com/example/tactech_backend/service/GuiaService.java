package com.example.tactech_backend.service;

import com.example.tactech_backend.dto.FotoGuiaDto;
import com.example.tactech_backend.dto.GuiaDto;
import com.example.tactech_backend.entity.FotoGuia;
import com.example.tactech_backend.entity.Guia;
import com.example.tactech_backend.exception.RecursoNoEncontradoException;
import com.example.tactech_backend.exception.ReglaNegocioException;
import com.example.tactech_backend.repository.GuiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuiaService {

    private final GuiaRepository guiaRepository;
    private final TicketService ticketService;
    private final EquipoService equipoService;

    /**
     * @param ticketId guias de un ticket (opcional)
     * @param tecnico  guias de los tickets asignados a un tecnico (opcional)
     */
    @Transactional(readOnly = true)
    public List<GuiaDto> listar(String ticketId, String tecnico) {
        List<Guia> guias;
        if (ticketId != null && !ticketId.isBlank()) {
            guias = guiaRepository.findByTicket_IdOrderByNumeroDesc(ticketId);
        } else if (tecnico != null && !tecnico.isBlank()) {
            guias = guiaRepository.findByTicket_TecnicoCorreoIgnoreCaseOrderByNumeroDesc(tecnico.trim());
        } else {
            guias = guiaRepository.findAll();
        }
        return guias.stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public GuiaDto obtener(String id) {
        return aDto(buscar(id));
    }

    @Transactional
    public GuiaDto crear(GuiaDto datos) {
        if (guiaRepository.existsById(datos.getId())) {
            throw new ReglaNegocioException("Ya existe la guia " + datos.getId());
        }
        return guardar(datos.getId(), datos);
    }

    /** Crea o actualiza la guia y reemplaza su lista de fotos (orphanRemoval borra las que sobran). */
    @Transactional
    public GuiaDto guardar(String id, GuiaDto datos) {
        Guia guia = guiaRepository.findById(id).orElseGet(Guia::new);
        guia.setId(id);
        guia.setNumero(datos.getNumero());
        guia.setTicket(ticketService.buscar(datos.getTicketId()));
        guia.setEquipo(equipoService.buscar(datos.getEquipoId()));
        guia.setDireccion(datos.getDireccion().trim());
        guia.setLatitud(datos.getLatitud());
        guia.setLongitud(datos.getLongitud());
        guia.setFecha(datos.getFecha() != null ? datos.getFecha() : LocalDateTime.now());
        guia.setTecnico(datos.getTecnico().trim());
        guia.setTrabajoRealizado(datos.getTrabajoRealizado().trim());
        guia.setRepuestos(textoOVacio(datos.getRepuestos()));
        guia.setObservaciones(textoOVacio(datos.getObservaciones()));
        guia.setRecibidoPor(datos.getRecibidoPor().trim());

        actualizarFotos(guia, datos.getFotos());
        return aDto(guiaRepository.save(guia));
    }

    @Transactional
    public void eliminar(String id) {
        guiaRepository.delete(buscar(id));
    }

    private Guia buscar(String id) {
        return guiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la guia " + id));
    }

    /**
     * Sincroniza la lista de fotos por id: quita las que ya no vienen y agrega las nuevas.
     * No se borra y reinserta todo porque reenviar la misma guia repetiria los mismos id.
     */
    private void actualizarFotos(Guia guia, List<FotoGuiaDto> recibidas) {
        List<FotoGuiaDto> lista = recibidas == null ? List.of() : recibidas;
        Set<String> idsRecibidos = lista.stream().map(FotoGuiaDto::getId).collect(Collectors.toSet());
        guia.getFotos().removeIf(f -> !idsRecibidos.contains(f.getId()));

        Set<String> idsActuales = guia.getFotos().stream().map(FotoGuia::getId).collect(Collectors.toSet());
        for (FotoGuiaDto f : lista) {
            if (!idsActuales.contains(f.getId())) {
                guia.getFotos().add(FotoGuia.builder()
                        .id(f.getId())
                        .guia(guia)
                        .uri(f.getUri())
                        .fechaCaptura(f.getFechaCaptura() != null ? f.getFechaCaptura() : LocalDateTime.now())
                        .build());
            }
        }
    }

    private static String textoOVacio(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private GuiaDto aDto(Guia g) {
        List<FotoGuiaDto> fotos = g.getFotos().stream()
                .map(f -> new FotoGuiaDto(f.getId(), f.getUri(), f.getFechaCaptura()))
                .toList();
        return new GuiaDto(
                g.getId(),
                g.getNumero(),
                g.getTicket().getId(),
                g.getEquipo().getId(),
                g.getDireccion(),
                g.getLatitud(),
                g.getLongitud(),
                g.getFecha(),
                g.getTecnico(),
                g.getTrabajoRealizado(),
                textoOVacio(g.getRepuestos()),
                textoOVacio(g.getObservaciones()),
                g.getRecibidoPor(),
                fotos
        );
    }
}
