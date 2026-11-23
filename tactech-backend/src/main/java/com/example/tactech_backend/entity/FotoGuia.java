package com.example.tactech_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Referencia a una fotografia de la guia. Se guarda la URI del archivo en el telefono
 * del tecnico; la imagen en si no viaja al servidor en esta version.
 */
@Entity
@Table(name = "fotos_guia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FotoGuia {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guia_id", nullable = false)
    @ToString.Exclude
    private Guia guia;

    @Column(nullable = false, length = 500)
    private String uri;

    @Column(nullable = false)
    private LocalDateTime fechaCaptura;
}
