package com.eduar.promobot.adapter.out.ia;

import com.eduar.promobot.config.GeminiProperties;
import com.eduar.promobot.domain.model.Promocao;
import com.eduar.promobot.domain.port.out.GeradorDeDescricao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GeminiGeradorDeDescricao implements GeradorDeDescricao {

    private static final Logger log = LoggerFactory.getLogger(GeminiGeradorDeDescricao.class);

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiGeradorDeDescricao(RestClient.Builder restClientBuilder, GeminiProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }


    @Override
    public String gerarDescricao(Promocao promocao) {
        String nomeProduto = promocao.getProduto().getNome();
        String categoria = promocao.getProduto().getCategoria();


        String prompt = "Escreva uma descrição curta e chamativa, em português, "
                + "para uma promoção que será enviada em um grupo do Telegram, sobre o seguinte produto: "
                + nomeProduto + ", categoria " + categoria
                + ". Use no máximo 2 frases, tom animado e direto, sem inventar características do produto, "
                + "sem mencionar preços ou valores (isso já aparece separadamente na mensagem), "
                + "e sem mencionar WhatsApp ou qualquer outro aplicativo de mensagens.";

        Map<String, Object> corpo = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                                ))
                )
        );
        try {
            Map<String, Object> resposta = restClient.post()
                    .uri(properties.apiUrl() + "?key=" + properties.apiKey())
                    .body(corpo)
                    .retrieve()
                    .body(Map.class);

            return extrairTexto(resposta);
        } catch (Exception e) {
            log.error("Falha ao gerar descrição via Gemini para a promoção {}", promocao.getId(), e);
            return nomeProduto + " com desconto especial!";
        }
    }

    @SuppressWarnings("unchecked")
    private String extrairTexto(Map<String, Object> resposta) {
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) resposta.get("candidates");
        Map<String, Object> conteudo = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) conteudo.get("parts");
        return (String) parts.get(0).get("text");
    }
}
