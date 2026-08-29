package com.eduar.promobot.scraperlocal.ingestao;

import com.eduar.promobot.scraperlocal.model.PromocaoEncontrada;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestaoClientTest {
    private final AtomicInteger statusResposta = new AtomicInteger(201);
    private final AtomicReference<String> corpoRecebido = new AtomicReference<>();
    private final AtomicReference<String> apiKeyRecebida = new AtomicReference<>();
    private HttpServer servidor;
    private IngestaoClient client;

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/api/promocoes/ingestao", this::responder);
        servidor.start();

        URI endpoint = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort()
                + "/api/promocoes/ingestao");
        client = new IngestaoClient(HttpClient.newHttpClient(), endpoint, "chave-local", Duration.ofSeconds(2));
    }

    @AfterEach
    void pararServidor() {
        servidor.stop(0);
    }

    @Test
    void enviaContratoCorretoParaServidorLocal() {
        ResultadoIngestao resultado = client.enviar(promocao());

        assertEquals(ResultadoIngestao.ACEITA, resultado);
        assertEquals("chave-local", apiKeyRecebida.get());
        assertEquals("application/json; charset=UTF-8", ultimoContentType);
        String json = corpoRecebido.get();
        assertTrue(json.contains("\"produtoNome\":\"Notebook \\\"Pro\\\"\\n2026\""));
        assertTrue(json.contains("\"produtoImagemUrl\":null"));
        assertTrue(json.contains("\"precoOriginal\":100.00"));
        assertTrue(json.contains("\"precoPromocional\":79.90"));
        assertTrue(json.contains("\"idExterno\":\"MLB123\""));
    }

    @Test
    void mapeiaCodigosDeRespostaSemAcessarVps() {
        Map<Integer, ResultadoIngestao> casos = new LinkedHashMap<>();
        casos.put(201, ResultadoIngestao.ACEITA);
        casos.put(200, ResultadoIngestao.DUPLICATA);
        casos.put(400, ResultadoIngestao.PAYLOAD_INVALIDO);
        casos.put(403, ResultadoIngestao.NAO_AUTORIZADA);
        casos.put(429, ResultadoIngestao.ERRO_TRANSITORIO);
        casos.put(503, ResultadoIngestao.ERRO_TRANSITORIO);
        casos.put(418, ResultadoIngestao.ERRO_INESPERADO);

        casos.forEach((status, esperado) -> {
            statusResposta.set(status);
            assertEquals(esperado, client.enviar(promocao()), "HTTP " + status);
        });
    }

    private volatile String ultimoContentType;

    private void responder(HttpExchange exchange) throws IOException {
        corpoRecebido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        apiKeyRecebida.set(exchange.getRequestHeaders().getFirst("X-Ingestao-Api-Key"));
        ultimoContentType = exchange.getRequestHeaders().getFirst("Content-Type");

        byte[] resposta = "{\"teste\":true}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusResposta.get(), resposta.length);
        exchange.getResponseBody().write(resposta);
        exchange.close();
    }

    private PromocaoEncontrada promocao() {
        return new PromocaoEncontrada(
                "Notebook \"Pro\"\n2026",
                null,
                "informatica",
                new BigDecimal("100.00"),
                new BigDecimal("79.90"),
                "https://produto.mercadolivre.com.br/MLB-123",
                "MLB123");
    }
}
