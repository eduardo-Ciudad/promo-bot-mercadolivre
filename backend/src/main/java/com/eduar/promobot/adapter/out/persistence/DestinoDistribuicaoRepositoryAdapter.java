package com.eduar.promobot.adapter.out.persistence;

import com.eduar.promobot.domain.model.CanalDistribuicao;
import com.eduar.promobot.domain.model.DestinoDistribuicao;
import com.eduar.promobot.domain.port.out.DestinoDistribuicaoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DestinoDistribuicaoRepositoryAdapter implements DestinoDistribuicaoRepository {

    private final DestinoDistribuicaoJpaRepository jpaRepository;

    public DestinoDistribuicaoRepositoryAdapter(DestinoDistribuicaoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<DestinoDistribuicao> buscarPorExternalIdECanal(String externalId, CanalDistribuicao canal) {
        return jpaRepository.findByExternalIdAndCanal(externalId, canal);
    }

    @Override
    public Optional<DestinoDistribuicao> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public DestinoDistribuicao salvar(DestinoDistribuicao destino) {
        return jpaRepository.save(destino);
    }

    @Override
    public void marcarComoBloqueado(UUID id, Instant bloqueadoEm) {
        jpaRepository.findById(id).ifPresent(destino -> {
            destino.marcarComoBloqueado(bloqueadoEm);
            jpaRepository.save(destino);
        });
    }
}
