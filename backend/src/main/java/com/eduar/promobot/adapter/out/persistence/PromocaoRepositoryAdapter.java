package com.eduar.promobot.adapter.out.persistence;

import com.eduar.promobot.domain.model.Promocao;
import com.eduar.promobot.domain.model.StatusPromocao;
import com.eduar.promobot.domain.port.out.PromocaoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PromocaoRepositoryAdapter implements PromocaoRepository {

    private final PromocaoJpaRepository jpaRepository;

    public PromocaoRepositoryAdapter(PromocaoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Promocao salvar(Promocao promocao) {
        return jpaRepository.save(promocao);
    }

    @Override
    public Optional<Promocao> buscarPorId(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existePorIdExterno(String idExterno) {
        return jpaRepository.existsByIdExterno(idExterno);
    }

    @Override
    public List<Promocao> buscarPorStatus(StatusPromocao status) {
        return jpaRepository.findByStatus(status);
    }
}
