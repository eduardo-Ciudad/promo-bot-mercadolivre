package com.eduar.promobot.domain.port.out;

import com.eduar.promobot.domain.model.CanalDistribuicao;
import com.eduar.promobot.domain.model.DestinoDistribuicao;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DestinoDistribuicaoRepository {

    Optional<DestinoDistribuicao> buscarPorExternalIdECanal(String externalId, CanalDistribuicao canal);

    Optional<DestinoDistribuicao> buscarPorId(UUID id);

    DestinoDistribuicao salvar(DestinoDistribuicao destino);

    void marcarComoBloqueado(UUID id, Instant bloqueadoEm);
}
