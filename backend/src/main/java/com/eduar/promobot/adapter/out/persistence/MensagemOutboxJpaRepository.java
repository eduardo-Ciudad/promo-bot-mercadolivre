package com.eduar.promobot.adapter.out.persistence;

import com.eduar.promobot.domain.model.MensagemOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MensagemOutboxJpaRepository extends JpaRepository<MensagemOutbox, UUID> {

    /*
     * Reivindica um lote de mensagens pendentes de forma atomica: o SELECT ... FOR UPDATE SKIP LOCKED
     * seleciona linhas pendentes ignorando as ja travadas por outra instancia do worker, e o UPDATE
     * as move para PROCESSANDO na mesma transacao — evitando entrega duplicada entre instancias
     * concorrentes sem manter o lock aberto durante a chamada HTTP ao provider.
     */
    @Query(value = """
            UPDATE mensagem_outbox
            SET status = 'PROCESSANDO', reivindicado_em = now()
            WHERE id IN (
                SELECT id FROM mensagem_outbox
                WHERE (status = 'PENDENTE'
                       AND (proxima_tentativa_em IS NULL OR proxima_tentativa_em <= now()))
                   OR (status = 'PROCESSANDO'
                       AND reivindicado_em < now() - make_interval(secs => :leaseTimeoutSeconds))
                ORDER BY criado_em
                FOR UPDATE SKIP LOCKED
                LIMIT :limite
            )
            RETURNING *
            """, nativeQuery = true)
    List<MensagemOutbox> reivindicarPendentes(@Param("limite") int limite,
                                               @Param("leaseTimeoutSeconds") long leaseTimeoutSeconds);

    default List<MensagemOutbox> reivindicarPendentes(int limite) {
        return reivindicarPendentes(limite, 120);
    }
}
