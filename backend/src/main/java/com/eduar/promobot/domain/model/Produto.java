package com.eduar.promobot.domain.model;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
