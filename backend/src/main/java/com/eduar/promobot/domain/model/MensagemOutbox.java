package com.eduar.promobot.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mensagem_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MensagemOutbox {

    @Id
    private UUID id;

    @Column(name = "promocao_id", nullable = false)
    private UUID promocaoId;

    @Column(name = "destino_id", nullable = false)
    private UUID destinoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 30)
    private CanalDistribuicao canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusMensagemOutbox status;

    @Column(name = "tentativas", nullable = false)
    private int tentativas;

    @Column(name = "proxima_tentativa_em")
    private Instant proximaTentativaEm;

    @Column(name = "reivindicado_em")
    private Instant reivindicadoEm;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "ultimo_erro", columnDefinition = "TEXT")
    private String ultimoErro;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    @Builder
    private MensagemOutbox(UUID promocaoId, UUID destinoId, CanalDistribuicao canal) {
        this.id = UUID.randomUUID();
        this.promocaoId = promocaoId;
        this.destinoId = destinoId;
        this.canal = canal;
        this.status = StatusMensagemOutbox.PENDENTE;
        this.tentativas = 0;
        this.criadoEm = Instant.now();
    }

    public void marcarComoEnviada(String providerMessageId, Instant enviadoEm) {
        this.status = StatusMensagemOutbox.ENVIADA;
        this.providerMessageId = providerMessageId;
        this.enviadoEm = enviadoEm;
        this.ultimoErro = null;
        this.reivindicadoEm = null;
    }

    public void marcarComoFalha(String motivo, Instant proximaTentativaEm) {
        this.status = proximaTentativaEm != null ? StatusMensagemOutbox.PENDENTE : StatusMensagemOutbox.FALHA;
        this.ultimoErro = motivo;
        this.proximaTentativaEm = proximaTentativaEm;
        this.reivindicadoEm = null;
    }

    public void incrementarTentativa() {
        this.tentativas++;
    }
}
