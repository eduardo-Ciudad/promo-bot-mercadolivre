package com.eduar.promobot.adapter.out.persistence;

import com.eduar.promobot.domain.model.CanalDistribuicao;
import com.eduar.promobot.domain.model.DestinoDistribuicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DestinoDistribuicaoJpaRepository extends JpaRepository<DestinoDistribuicao, UUID> {

    Optional<DestinoDistribuicao> findByExternalIdAndCanal(String externalId, CanalDistribuicao canal);
}
