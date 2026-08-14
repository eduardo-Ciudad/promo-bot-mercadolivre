package com.eduar.promobot.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promocao")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Promocao {

    @Id
    private UUID id;

    @Embedded
    private Produto produto;

    @Column(name = "preco_original", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoOriginal;

    @Column(name = "preco_promocional", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoPromocional;

    @Column(name = "percentual_desconto", nullable = false)
    private Integer percentualDesconto;

    @Column(name = "link_original", nullable = false, length = 1000)
    private String linkOriginal;

    @Column(name = "link_afiliado", length = 1000)
    private String linkAfiliado;

    @Column(name = "descricao_gerada", columnDefinition = "TEXT")
    private String descricaoGerada;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPromocao status;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_envio", length = 20)
    private CanalDistribuicao canalEnvio;

    @Column(name = "id_externo", nullable = false, unique = true, length = 100)
    private String idExterno;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;


    @Builder
    private Promocao(Produto produto, BigDecimal precoOriginal, BigDecimal precoPromocional,
                     String linkOriginal, String idExterno) {
        this.id = UUID.randomUUID();
        this.produto = produto;
        this.precoOriginal = precoOriginal;
        this.precoPromocional = precoPromocional;
        this.percentualDesconto = calcularPercentualDesconto(precoOriginal, precoPromocional);
        this.linkOriginal = linkOriginal;
        this.idExterno = idExterno;
        this.status = StatusPromocao.ENCONTRADA;
    }

    private static Integer calcularPercentualDesconto(BigDecimal precoOriginal, BigDecimal precoPromocional) {
        if (precoOriginal == null || precoPromocional == null || precoOriginal.signum() <= 0) {
            return 0;
        }
        BigDecimal desconto = precoOriginal.subtract(precoPromocional);
        BigDecimal percentual = desconto
                .divide(precoOriginal, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return percentual.intValue();
    }

    public void definirLinkAfiliado(String linkAfiliado) {
        this.linkAfiliado = linkAfiliado;
    }

    public void enriquecerComDescricao(String descricaoGerada) {
        exigirStatus(StatusPromocao.ENCONTRADA, "enriquecer");
        this.descricaoGerada = descricaoGerada;
        this.status = StatusPromocao.ENRIQUECIDA;
    }

    public void marcarComoEnviada(CanalDistribuicao canal) {
        exigirStatus(StatusPromocao.ENRIQUECIDA, "enviar");
        this.canalEnvio = canal;
        this.status = StatusPromocao.ENVIADA;
    }

    public void marcarComoFalha() {
        this.status = StatusPromocao.FALHA;
    }

    private void exigirStatus(StatusPromocao esperado, String acao) {
        if (this.status != esperado) {
            throw new TransactionRequiredException(
                    "Nao e possivel %s a promocao %s: status atual e %s, esperado %s"
                            .formatted(acao, id, status, esperado));
        }
    }
}

