package com.eduar.promobot.domain.model;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Produto {

    @Column(name = "produto_nome", nullable = false, length = 500)
    private String nome;

    @Column(name = "produto_imagem_url", length = 1000)
    private String imagemUrl;

    @Column(name = "produto_categoria", length = 150)
    private String categoria;



}
