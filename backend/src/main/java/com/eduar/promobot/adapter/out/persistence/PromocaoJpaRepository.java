package com.eduar.promobot.adapter.out.persistence;

import com.eduar.promobot.domain.model.Promocao;
import com.eduar.promobot.domain.model.StatusPromocao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromocaoJpaRepository extends JpaRepository<Promocao, UUID> {

    boolean existsByIdExterno(String idExterno);

    List<Promocao> findByStatus(StatusPromocao status);
}
