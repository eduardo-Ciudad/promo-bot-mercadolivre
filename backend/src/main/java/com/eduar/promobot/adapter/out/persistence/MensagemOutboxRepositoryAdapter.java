package com.eduar.promobot.adapter.out.persistence;

import com.eduar.promobot.domain.model.MensagemOutbox;
import com.eduar.promobot.domain.port.out.MensagemOutboxRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class MensagemOutboxRepositoryAdapter implements MensagemOutboxRepository {

    private final MensagemOutboxJpaRepository jpaRepository;

    public MensagemOutboxRepositoryAdapter(MensagemOutboxJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MensagemOutbox salvar(MensagemOutbox mensagemOutbox) {
        return jpaRepository.save(mensagemOutbox);
    }

    @Override
    @Transactional
    public List<MensagemOutbox> buscarPendentesParaEnvio(int limite, long leaseTimeoutSeconds) {
        return jpaRepository.reivindicarPendentes(limite, leaseTimeoutSeconds);
    }

    @Override
    public void marcarComoEnviada(UUID id, String providerMessageId, Instant enviadoEm) {
        jpaRepository.findById(id).ifPresent(item -> {
            item.marcarComoEnviada(providerMessageId, enviadoEm);
            jpaRepository.save(item);
        });
    }

    @Override
    public void marcarComoFalha(UUID id, String motivo, Instant proximaTentativaEm) {
        jpaRepository.findById(id).ifPresent(item -> {
            item.marcarComoFalha(motivo, proximaTentativaEm);
            jpaRepository.save(item);
        });
    }

    @Override
    public void incrementarTentativa(UUID id) {
        jpaRepository.findById(id).ifPresent(item -> {
            item.incrementarTentativa();
            jpaRepository.save(item);
        });
    }
}
