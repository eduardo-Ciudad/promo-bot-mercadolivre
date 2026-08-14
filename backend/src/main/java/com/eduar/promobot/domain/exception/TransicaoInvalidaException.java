package com.eduar.promobot.domain.exception;

import com.eduar.promobot.domain.model.StatusPromocao;

import java.util.UUID;

public class TransicaoInvalidaException extends RuntimeException {

    private final UUID promocaoId;
    private final StatusPromocao statusAtual;
    private final StatusPromocao statusEsperado;

    public TransicaoInvalidaException(UUID promocaoId, String acao,
                                      StatusPromocao statusAtual, StatusPromocao statusEsperado) {
        super("Nao e possivel %s a promocao %s: status atual e %s, esperado %s"
                .formatted(acao, promocaoId, statusAtual, statusEsperado));
        this.promocaoId = promocaoId;
        this.statusAtual = statusAtual;
        this.statusEsperado = statusEsperado;
    }

    public UUID getPromocaoId() {
        return promocaoId;
    }

    public StatusPromocao getStatusAtual() {
        return statusAtual;
    }

    public StatusPromocao getStatusEsperado() {
        return statusEsperado;
    }
}