CREATE TABLE destino_distribuicao (
    id                UUID PRIMARY KEY,
    canal             VARCHAR(30) NOT NULL,
    external_id       VARCHAR(255) NOT NULL,
    tipo              VARCHAR(30) NOT NULL,
    ativo             BOOLEAN NOT NULL DEFAULT true,
    criado_em         TIMESTAMPTZ NOT NULL,
    bloqueado_em      TIMESTAMPTZ,

    CONSTRAINT uq_destino_distribuicao_canal_external_id UNIQUE (canal, external_id)
);

CREATE TABLE mensagem_outbox (
    id                    UUID PRIMARY KEY,
    promocao_id           UUID NOT NULL REFERENCES promocao(id),
    destino_id            UUID NOT NULL REFERENCES destino_distribuicao(id),
    canal                 VARCHAR(30) NOT NULL,
    status                VARCHAR(30) NOT NULL,
    tentativas            INTEGER NOT NULL DEFAULT 0,
    proxima_tentativa_em  TIMESTAMPTZ,
    provider_message_id   VARCHAR(255),
    ultimo_erro           TEXT,
    criado_em             TIMESTAMPTZ NOT NULL,
    enviado_em            TIMESTAMPTZ,

    CONSTRAINT uq_mensagem_outbox_promocao_destino_canal UNIQUE (promocao_id, destino_id, canal)
);

CREATE INDEX idx_mensagem_outbox_status_proxima_tentativa
    ON mensagem_outbox (status, proxima_tentativa_em);

CREATE TABLE telegram_update_processado (
    update_id      BIGINT PRIMARY KEY,
    processado_em  TIMESTAMPTZ NOT NULL
);
