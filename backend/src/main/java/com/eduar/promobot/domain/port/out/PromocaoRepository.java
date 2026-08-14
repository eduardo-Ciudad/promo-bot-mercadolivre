package com.eduar.promobot.domain.port.out;

import com.eduar.promobot.domain.model.Promocao;
import com.eduar.promobot.domain.model.StatusPromocao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromocaoRepository {

    Promocao salvar(Promocao promocao);

    Optional<Promocao> buscarPorId(UUID id);

    boolean existePorIdExterno(String idExterno);

    List<Promocao> buscarPorStatus(StatusPromocao status);
}
