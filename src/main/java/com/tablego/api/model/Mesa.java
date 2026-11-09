package com.tablego.api.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mesas", uniqueConstraints = @UniqueConstraint(columnNames = {"restaurante_id", "numero"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurante_id")
    private Restaurante restaurante;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false)
    private Integer capacidad;

    @Builder.Default
    @Column(nullable = false)
    private boolean activa = true;
}
