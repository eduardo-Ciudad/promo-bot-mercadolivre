package com.eduar.promobot.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "destino_distribuicao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DestinoDistribuicao {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 30)
    private CanalDistribuicao canal;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoDestino tipo;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "bloqueado_em")
    private Instant bloqueadoEm;

    @Builder
    private DestinoDistribuicao(CanalDistribuicao canal, String externalId, TipoDestino tipo) {
        this.id = UUID.randomUUID();
        this.canal = canal;
        this.externalId = externalId;
        this.tipo = tipo;
        this.ativo = true;
        this.criadoEm = Instant.now();
    }

    public void marcarComoBloqueado(Instant bloqueadoEm) {
        this.ativo = false;
        this.bloqueadoEm = bloqueadoEm;
    }

    public void reativar() {
        this.ativo = true;
        this.bloqueadoEm = null;
    }
}
