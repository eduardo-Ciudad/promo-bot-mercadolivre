package com.eduar.promobot.domain.port.out;

import com.eduar.promobot.domain.model.MensagemOutbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MensagemOutboxRepository {

    MensagemOutbox salvar(MensagemOutbox mensagemOutbox);

    List<MensagemOutbox> buscarPendentesParaEnvio(int limite);

    void marcarComoEnviada(UUID id, String providerMessageId, Instant enviadoEm);

    void marcarComoFalha(UUID id, String motivo, Instant proximaTentativaEm);

    void incrementarTentativa(UUID id);
}
