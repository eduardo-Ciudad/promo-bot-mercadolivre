package com.eduar.promobot.adapter.in.web;

import com.eduar.promobot.adapter.in.web.dto.IngestaoPromocaoRequest;
import com.eduar.promobot.adapter.in.web.dto.IngestaoPromocaoResponse;
import com.eduar.promobot.application.IngestaoPromocaoService;
import com.eduar.promobot.application.ResultadoIngestao;
import com.eduar.promobot.config.IngestaoProperties;
import com.eduar.promobot.domain.model.Produto;
import com.eduar.promobot.domain.model.Promocao;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/promocoes/ingestao")
public class IngestaoPromocaoController {

    private static final Logger log = LoggerFactory.getLogger(IngestaoPromocaoController.class);
    private static final String HEADER_API_KEY = "X-Ingestao-Api-Key";

    private final IngestaoProperties properties;
    private final IngestaoPromocaoService ingestaoPromocaoService;

    public IngestaoPromocaoController(IngestaoProperties properties,
                                      IngestaoPromocaoService ingestaoPromocaoService) {
        this.properties = properties;
        this.ingestaoPromocaoService = ingestaoPromocaoService;
    }

    @PostMapping
    public ResponseEntity<IngestaoPromocaoResponse> ingerir(
            @RequestHeader(value = HEADER_API_KEY, required = false) String apiKey,
            @Valid @RequestBody IngestaoPromocaoRequest request) {

        if (!apiKeyValida(apiKey)) {
            log.warn("Ingestão de promoção recebida com API key ausente ou inválida");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Promocao promocao = paraPromocao(request);
        ResultadoIngestao resultado = ingestaoPromocaoService.ingerir(promocao);
        IngestaoPromocaoResponse response = new IngestaoPromocaoResponse(resultado);

        if (resultado == ResultadoIngestao.ACEITA) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    private Promocao paraPromocao(IngestaoPromocaoRequest request) {
        Produto produto = new Produto(
                request.produtoNome(),
                request.produtoImagemUrl(),
                request.produtoCategoria()
        );

        return Promocao.builder()
                .produto(produto)
                .precoOriginal(request.precoOriginal())
                .precoPromocional(request.precoPromocional())
                .linkOriginal(request.linkOriginal())
                .idExterno(request.idExterno())
                .build();
    }

    private boolean apiKeyValida(String recebida) {
        String esperada = properties.apiKey();
        if (esperada == null || esperada.isBlank() || recebida == null) {
            return false;
        }
        byte[] recebidaBytes = recebida.getBytes(StandardCharsets.UTF_8);
        byte[] esperadaBytes = esperada.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(recebidaBytes, esperadaBytes);
    }
}
