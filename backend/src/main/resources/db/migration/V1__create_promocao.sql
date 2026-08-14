CREATE TABLE promocao (
    id                   UUID PRIMARY KEY,

    -- Produto (embedded)
    produto_nome         VARCHAR(500) NOT NULL,
    produto_imagem_url   VARCHAR(1000),
    produto_categoria    VARCHAR(150),

    preco_original       NUMERIC(12, 2) NOT NULL,
    preco_promocional    NUMERIC(12, 2) NOT NULL,
    percentual_desconto  INTEGER NOT NULL,

    link_original        VARCHAR(1000) NOT NULL,
    link_afiliado        VARCHAR(1000),

    descricao_gerada     TEXT,

    status               VARCHAR(20) NOT NULL,
    canal_envio          VARCHAR(20),

    id_externo           VARCHAR(100) NOT NULL,

    criado_em            TIMESTAMP NOT NULL,
    atualizado_em        TIMESTAMP NOT NULL,

    CONSTRAINT uq_promocao_id_externo UNIQUE (id_externo)
);

CREATE INDEX idx_promocao_status ON promocao (status);