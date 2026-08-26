package com.eduar.promobot.adapter.out.telegram;

import com.eduar.promobot.config.TelegramProperties;
import com.eduar.promobot.domain.exception.DestinatarioIndisponivelException;
import com.eduar.promobot.domain.exception.EnvioFalhouException;
import com.eduar.promobot.domain.exception.EnvioInvalidoException;
import com.eduar.promobot.domain.exception.RateLimitedException;
import com.eduar.promobot.domain.model.CanalDistribuicao;
import com.eduar.promobot.domain.model.MensagemSaida;
import com.eduar.promobot.domain.model.ResultadoEnvio;
import com.eduar.promobot.domain.port.out.EnviadorDeMensagem;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

@Component
public class TelegramBotAdapter implements EnviadorDeMensagem {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotAdapter.class);
    private static final String PARSE_MODE = "HTML";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TelegramBotAdapter(RestClient.Builder restClientBuilder, TelegramProperties properties, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder
                .baseUrl(properties.apiBaseUrl() + "/bot" + properties.botToken())
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public CanalDistribuicao canal() {
        return CanalDistribuicao.TELEGRAM;
    }

    @Override
    public ResultadoEnvio enviar(MensagemSaida mensagem) {
        String conteudo = TelegramTextFormatter.montarConteudo(mensagem.titulo(), mensagem.texto(),
                mensagem.link() != null ? mensagem.link().toString() : null);

        try {
            TelegramApiResponse resposta = mensagem.imagemUrl() != null
                    ? enviarFoto(mensagem, conteudo)
                    : enviarTexto(mensagem, conteudo);
            return new ResultadoEnvio(String.valueOf(resposta.result().message_id()), Instant.now());
        } catch (RestClientResponseException e) {
            throw traduzirErro(e);
        } catch (ResourceAccessException e) {
            throw new EnvioFalhouException("Timeout ou falha de rede ao chamar a API do Telegram", e);
        } catch (RuntimeException e) {
            throw new EnvioFalhouException("Falha inesperada ao enviar mensagem via Telegram", e);
        }
    }

    private TelegramApiResponse enviarFoto(MensagemSaida mensagem, String conteudo) {
        TelegramSendPhotoRequest request = new TelegramSendPhotoRequest(
                mensagem.destinoExterno(),
                mensagem.imagemUrl().toString(),
                TelegramTextFormatter.truncar(conteudo, TelegramTextFormatter.LIMITE_CAPTION),
                PARSE_MODE,
                false
        );
        return restClient.post()
                .uri("/sendPhoto")
                .body(request)
                .retrieve()
                .body(TelegramApiResponse.class);
    }

    private TelegramApiResponse enviarTexto(MensagemSaida mensagem, String conteudo) {
        TelegramSendMessageRequest request = new TelegramSendMessageRequest(
                mensagem.destinoExterno(),
                TelegramTextFormatter.truncar(conteudo, TelegramTextFormatter.LIMITE_TEXTO),
                PARSE_MODE,
                false
        );
        return restClient.post()
                .uri("/sendMessage")
                .body(request)
                .retrieve()
                .body(TelegramApiResponse.class);
    }

    private RuntimeException traduzirErro(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        TelegramApiResponse corpo = lerCorpoErro(e);
        String descricao = corpo != null ? corpo.description() : e.getMessage();

        return switch (status) {
            case 429 -> {
                long retryAfter = corpo != null && corpo.parameters() != null && corpo.parameters().retry_after() != null
                        ? corpo.parameters().retry_after()
                        : 1L;
                yield new RateLimitedException(retryAfter);
            }
            case 403 -> new DestinatarioIndisponivelException(
                    "Bot bloqueado pelo usuario ou removido do grupo/canal: " + descricao);
            case 400 -> new EnvioInvalidoException("Requisicao invalida para a API do Telegram: " + descricao);
            default -> new EnvioFalhouException("Erro ao chamar a API do Telegram (status " + status + "): " + descricao, e);
        };
    }

    private TelegramApiResponse lerCorpoErro(RestClientResponseException e) {
        try {
            String corpo = e.getResponseBodyAsString();
            if (corpo == null || corpo.isBlank()) {
                return null;
            }
            return objectMapper.readValue(corpo, TelegramApiResponse.class);
        } catch (Exception parseException) {
            log.warn("Nao foi possivel interpretar o corpo de erro da API do Telegram", parseException);
            return null;
        }
    }
}
