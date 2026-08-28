package com.eduar.promobot.adapter.in.web;

import com.eduar.promobot.adapter.out.persistence.PromocaoJpaRepository;
import com.eduar.promobot.domain.model.Promocao;
import com.eduar.promobot.domain.port.out.GeradorDeDescricao;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class TesteGeminiController {

    private final PromocaoJpaRepository promocaoJpaRepository;
    private final GeradorDeDescricao geradorDeDescricao;

    public TesteGeminiController(PromocaoJpaRepository promocaoJpaRepository, GeradorDeDescricao geradorDeDescricao) {
        this.promocaoJpaRepository = promocaoJpaRepository;
        this.geradorDeDescricao = geradorDeDescricao;
    }

    @PostMapping("/admin/teste/gemini/{promocaoId}")
    public String testar(@PathVariable UUID promocaoId) {
        Promocao promocao = promocaoJpaRepository.findById(promocaoId)
                .orElseThrow(() -> new RuntimeException("Promoção não encontrada"));

        return geradorDeDescricao.gerarDescricao(promocao);
    }
}
