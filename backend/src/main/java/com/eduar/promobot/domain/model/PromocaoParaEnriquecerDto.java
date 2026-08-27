package com.eduar.promobot.domain.model;

import java.io.Serializable;
import java.util.UUID;

public class PromocaoParaEnriquecerDto implements Serializable {

    private UUID promocaoId;
    private String produtoNome;
    private String produtoCategoria;

    public PromocaoParaEnriquecerDto() {
    }

    public PromocaoParaEnriquecerDto(UUID promocaoId, String produtoNome, String produtoCategoria) {
        this.promocaoId = promocaoId;
        this.produtoNome = produtoNome;
        this.produtoCategoria = produtoCategoria;
    }

    public UUID getPromocaoId() {
        return promocaoId;
    }

    public void setPromocaoId(UUID promocaoId) {
        this.promocaoId = promocaoId;
    }

    public String getProdutoNome() {
        return produtoNome;
    }

    public void setProdutoNome(String produtoNome) {
        this.produtoNome = produtoNome;
    }

    public String getProdutoCategoria() {
        return produtoCategoria;
    }

    public void setProdutoCategoria(String produtoCategoria) {
        this.produtoCategoria = produtoCategoria;
    }
}
