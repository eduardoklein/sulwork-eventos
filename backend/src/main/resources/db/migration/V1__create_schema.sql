CREATE TABLE eventos (
    id               BIGSERIAL PRIMARY KEY,
    nome             VARCHAR(120)  NOT NULL,
    descricao        VARCHAR(1000),
    tipo             VARCHAR(30)   NOT NULL,
    data_hora        TIMESTAMP     NOT NULL,
    prazo_alteracoes TIMESTAMP     NOT NULL,
    fuso             VARCHAR(50)   NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    criado_em        TIMESTAMP     NOT NULL DEFAULT now(),
    atualizado_em    TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT ck_eventos_tipo CHECK (tipo IN
        ('CAFE_DA_MANHA', 'ALMOCO', 'LANCHE', 'CONFRATERNIZACAO', 'OUTRO')),
    CONSTRAINT ck_eventos_status CHECK (status IN
        ('RASCUNHO', 'ABERTO', 'ENCERRADO', 'REALIZADO', 'CANCELADO')),
    CONSTRAINT ck_eventos_prazo CHECK (prazo_alteracoes <= data_hora)
);

CREATE INDEX idx_eventos_status ON eventos (status);
CREATE INDEX idx_eventos_status_prazo ON eventos (status, prazo_alteracoes);

CREATE TABLE categorias_evento (
    id              BIGSERIAL PRIMARY KEY,
    evento_id       BIGINT       NOT NULL REFERENCES eventos (id) ON DELETE CASCADE,
    nome            VARCHAR(80)  NOT NULL,
    unidade         VARCHAR(20)  NOT NULL,
    meta_quantidade INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT uq_categorias_evento_nome UNIQUE (evento_id, nome),
    CONSTRAINT ck_categorias_unidade CHECK (unidade IN
        ('UNIDADE', 'PORCAO', 'GRAMA', 'MILILITRO')),
    CONSTRAINT ck_categorias_meta CHECK (meta_quantidade >= 0)
);

CREATE TABLE itens_evento (
    id                    BIGSERIAL PRIMARY KEY,
    evento_id             BIGINT       NOT NULL REFERENCES eventos (id) ON DELETE CASCADE,
    categoria_id          BIGINT       NOT NULL REFERENCES categorias_evento (id) ON DELETE RESTRICT,
    nome                  VARCHAR(120) NOT NULL,
    quantidade_necessaria INTEGER      NOT NULL,
    obrigatorio           BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_itens_quantidade CHECK (quantidade_necessaria > 0)
);

CREATE INDEX idx_itens_evento_evento ON itens_evento (evento_id);
CREATE INDEX idx_itens_evento_categoria ON itens_evento (categoria_id);

CREATE TABLE item_alergenos (
    item_id  BIGINT      NOT NULL REFERENCES itens_evento (id) ON DELETE CASCADE,
    alergeno VARCHAR(30) NOT NULL,
    PRIMARY KEY (item_id, alergeno),
    CONSTRAINT ck_item_alergenos_alergeno CHECK (alergeno IN
        ('GLUTEN', 'LACTOSE', 'AMENDOIM', 'CASTANHAS', 'FRUTOS_DO_MAR', 'PEIXE',
         'OVO', 'SOJA'))
);

CREATE TABLE item_caracteristicas (
    item_id        BIGINT      NOT NULL REFERENCES itens_evento (id) ON DELETE CASCADE,
    caracteristica VARCHAR(30) NOT NULL,
    PRIMARY KEY (item_id, caracteristica),
    CONSTRAINT ck_item_caracteristicas_caracteristica CHECK (caracteristica IN
        ('CARNE_ORIGEM_ANIMAL', 'ACUCAR_ADICIONADO', 'ALTO_SODIO'))
);

CREATE TABLE colaboradores (
    id   BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    cpf  CHAR(11)     NOT NULL,
    CONSTRAINT uq_colaboradores_cpf UNIQUE (cpf),
    CONSTRAINT ck_colaboradores_cpf CHECK (cpf ~ '^[0-9]{11}$')
);

CREATE TABLE colaborador_restricoes (
    colaborador_id BIGINT      NOT NULL REFERENCES colaboradores (id) ON DELETE CASCADE,
    restricao      VARCHAR(40) NOT NULL,
    PRIMARY KEY (colaborador_id, restricao),
    CONSTRAINT ck_colaborador_restricoes_restricao CHECK (restricao IN
        ('INTOLERANCIA_LACTOSE', 'CELIACO_SEM_GLUTEN', 'ALERGIA_AMENDOIM_CASTANHAS',
         'ALERGIA_FRUTOS_DO_MAR', 'ALERGIA_PEIXE', 'ALERGIA_OVO', 'ALERGIA_SOJA',
         'DIABETICO_SEM_ACUCAR', 'HIPERTENSO_POUCO_SODIO', 'VEGETARIANO', 'VEGANO'))
);

CREATE TABLE participantes_evento (
    id             BIGSERIAL PRIMARY KEY,
    evento_id      BIGINT    NOT NULL REFERENCES eventos (id) ON DELETE CASCADE,
    colaborador_id BIGINT    NOT NULL REFERENCES colaboradores (id) ON DELETE RESTRICT,
    criado_em      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_participantes_evento_colaborador UNIQUE (evento_id, colaborador_id)
);

CREATE TABLE contribuicoes (
    id              BIGSERIAL PRIMARY KEY,
    participante_id BIGINT       NOT NULL REFERENCES participantes_evento (id) ON DELETE CASCADE,
    item_id         BIGINT       NOT NULL REFERENCES itens_evento (id) ON DELETE CASCADE,
    quantidade      INTEGER      NOT NULL,
    observacao      VARCHAR(255),
    criado_em       TIMESTAMP    NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_contribuicoes_participante_item UNIQUE (participante_id, item_id),
    CONSTRAINT ck_contribuicoes_quantidade CHECK (quantidade > 0)
);

CREATE INDEX idx_contribuicoes_item ON contribuicoes (item_id);
