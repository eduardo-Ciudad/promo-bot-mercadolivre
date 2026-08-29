package com.eduar.promobot.scraperlocal.ingestao;

import com.eduar.promobot.scraperlocal.model.PromocaoEncontrada;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class IngestaoClient {
    private static final Logger LOG = Logger.getLogger(IngestaoClient.class.getName());
    private static final String HEADER_API_KEY = "X-Ingestao-Api-Key";
    private static final int LIMITE_CORPO_LOG = 500;

    private final HttpClient httpClient;
    private final URI endpointUrl;
    private final String apiKey;
    private final Duration timeout;

    public IngestaoClient(HttpClient httpClient, URI endpointUrl, String apiKey, Duration timeout) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient e obrigatorio");
        this.endpointUrl = Objects.requireNonNull(endpointUrl, "endpointUrl e obrigatorio");
        this.timeout = Objects.requireNonNull(timeout, "timeout e obrigatorio");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey e obrigatoria");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout deve ser maior que zero");
        }
        this.apiKey = apiKey;
    }

    public ResultadoIngestao enviar(PromocaoEncontrada promocao) {
        Objects.requireNonNull(promocao, "promocao e obrigatoria");

        HttpRequest request = HttpRequest.newBuilder(endpointUrl)
                .timeout(timeout)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .header(HEADER_API_KEY, apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(
                        JsonSerializer.serializar(promocao), StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return interpretarResposta(response, promocao.idExterno());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.log(Level.WARNING, "Envio interrompido para " + promocao.idExterno(), e);
            return ResultadoIngestao.ERRO_TRANSITORIO;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Falha de comunicacao ao enviar " + promocao.idExterno(), e);
            return ResultadoIngestao.ERRO_TRANSITORIO;
        }
    }

    private ResultadoIngestao interpretarResposta(HttpResponse<String> response, String idExterno) {
        int status = response.statusCode();
        return switch (status) {
            case 201 -> {
                LOG.info(() -> "Promocao aceita: " + idExterno);
                yield ResultadoIngestao.ACEITA;
            }
            case 200 -> {
                LOG.info(() -> "Promocao duplicada: " + idExterno);
                yield ResultadoIngestao.DUPLICATA;
            }
            case 400 -> {
                LOG.severe(() -> "Payload invalido para " + idExterno + ": " + corpoSeguro(response.body()));
                yield ResultadoIngestao.PAYLOAD_INVALIDO;
            }
            case 403 -> {
                LOG.severe(() -> "API key de ingestao rejeitada ao enviar " + idExterno);
                yield ResultadoIngestao.NAO_AUTORIZADA;
            }
            case 429 -> {
                LOG.warning(() -> "Limite de requisicoes atingido ao enviar " + idExterno);
                yield ResultadoIngestao.ERRO_TRANSITORIO;
            }
            default -> {
                if (status >= 500 && status <= 599) {
                    LOG.warning(() -> "VPS indisponivel ao enviar " + idExterno + ": HTTP " + status);
                    yield ResultadoIngestao.ERRO_TRANSITORIO;
                }
                LOG.severe(() -> "Resposta inesperada para " + idExterno + ": HTTP " + status
                        + " - " + corpoSeguro(response.body()));
                yield ResultadoIngestao.ERRO_INESPERADO;
            }
        };
    }

    private String corpoSeguro(String corpo) {
        if (corpo == null || corpo.isBlank()) {
            return "(sem corpo)";
        }
        String normalizado = corpo.replace('\r', ' ').replace('\n', ' ');
        return normalizado.length() <= LIMITE_CORPO_LOG
                ? normalizado
                : normalizado.substring(0, LIMITE_CORPO_LOG) + "...";
    }
}

