package com.eduar.promobot.adapter.out.persistence;

import com.eduar.promobot.domain.model.MensagemOutbox;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * Prova, contra um Postgres real (Testcontainers), que duas "instancias" do worker chamando
 * reivindicarPendentes() ao mesmo tempo nunca recebem a mesma linha — o SELECT FOR UPDATE SKIP LOCKED
 * garante exclusividade entre elas, evitando entrega duplicada.
 *
 * Requer Docker disponivel no ambiente de execucao dos testes.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
// Desliga o rollback automatico do @DataJpaTest: cada chamada de repositorio precisa
// commitar de fato para ser visivel entre as duas threads/conexoes concorrentes.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
class MensagemOutboxConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MensagemOutboxJpaRepository outboxJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final List<UUID> idsInseridos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        idsInseridos.clear();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            UUID promocaoId = UUID.randomUUID();
            UUID destinoId = UUID.randomUUID();

            entityManager.createNativeQuery("""
                    INSERT INTO promocao (id, produto_nome, preco_original, preco_promocional, percentual_desconto,
                                           link_original, status, id_externo, criado_em, atualizado_em)
                    VALUES (:id, 'Produto Teste', 100, 50, 50, 'https://exemplo.com', 'ENVIADA', :idExterno, now(), now())
                    """)
                    .setParameter("id", promocaoId)
                    .setParameter("idExterno", "ext-" + promocaoId)
                    .executeUpdate();

            entityManager.createNativeQuery("""
                    INSERT INTO destino_distribuicao (id, canal, external_id, tipo, ativo, criado_em)
                    VALUES (:id, 'TELEGRAM', '999', 'USUARIO', true, now())
                    """)
                    .setParameter("id", destinoId)
                    .executeUpdate();

            for (int i = 0; i < 20; i++) {
                UUID id = UUID.randomUUID();
                idsInseridos.add(id);
                entityManager.createNativeQuery("""
                        INSERT INTO mensagem_outbox (id, promocao_id, destino_id, canal, status, tentativas, criado_em)
                        VALUES (:id, :promocaoId, :destinoId, 'TELEGRAM', 'PENDENTE', 0, now())
                        """)
                        .setParameter("id", id)
                        .setParameter("promocaoId", promocaoId)
                        .setParameter("destinoId", destinoId)
                        .executeUpdate();
            }
        });
    }

    @Test
    void naoDeveEntregarAMesmaMensagemParaDuasInstanciasConcorrentes() throws InterruptedException {
        List<UUID> claimedThreadA = new CopyOnWriteArrayList<>();
        List<UUID> claimedThreadB = new CopyOnWriteArrayList<>();

        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            aguardar(largada);
            List<MensagemOutbox> lote = outboxJpaRepository.reivindicarPendentes(10);
            lote.forEach(m -> claimedThreadA.add(m.getId()));
        });
        executor.submit(() -> {
            aguardar(largada);
            List<MensagemOutbox> lote = outboxJpaRepository.reivindicarPendentes(10);
            lote.forEach(m -> claimedThreadB.add(m.getId()));
        });

        largada.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        List<UUID> intersecao = claimedThreadA.stream()
                .filter(claimedThreadB::contains)
                .collect(Collectors.toList());

        assertThat(intersecao).as("nenhuma mensagem pode ser reivindicada por ambas as instancias").isEmpty();
        assertThat(claimedThreadA.size() + claimedThreadB.size()).isEqualTo(20);
    }

    private void aguardar(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
