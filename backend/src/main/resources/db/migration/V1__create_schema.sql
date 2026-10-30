CREATE TABLE events (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120)  NOT NULL,
    description     VARCHAR(1000),
    type            VARCHAR(30)   NOT NULL,
    date_time       TIMESTAMP     NOT NULL,
    change_deadline TIMESTAMP     NOT NULL,
    time_zone       VARCHAR(50)   NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT ck_events_type CHECK (type IN
        ('BREAKFAST', 'LUNCH', 'SNACK', 'GET_TOGETHER', 'OTHER')),
    CONSTRAINT ck_events_status CHECK (status IN
        ('RASCUNHO', 'ABERTO', 'ENCERRADO', 'REALIZADO', 'CANCELADO')),
    CONSTRAINT ck_events_deadline CHECK (change_deadline <= date_time)
);

CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_status_deadline ON events (status, change_deadline);

CREATE TABLE event_categories (
    id            BIGSERIAL PRIMARY KEY,
    event_id      BIGINT       NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    name          VARCHAR(80)  NOT NULL,
    unit          VARCHAR(20)  NOT NULL,
    goal_quantity INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT ck_categories_unit CHECK (unit IN
        ('UNIT', 'PORTION', 'GRAM', 'MILLILITER')),
    CONSTRAINT ck_categories_goal CHECK (goal_quantity >= 0)
);

CREATE UNIQUE INDEX uq_event_categories_name ON event_categories (event_id, lower(name));

CREATE TABLE event_items (
    id                BIGSERIAL PRIMARY KEY,
    event_id          BIGINT       NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    category_id       BIGINT       NOT NULL REFERENCES event_categories (id) ON DELETE RESTRICT,
    name              VARCHAR(120) NOT NULL,
    required_quantity INTEGER      NOT NULL,
    mandatory         BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_items_quantity CHECK (required_quantity > 0)
);

CREATE UNIQUE INDEX uq_items_category_name ON event_items (category_id, lower(name));
CREATE INDEX idx_event_items_event ON event_items (event_id);
CREATE INDEX idx_event_items_category ON event_items (category_id);

CREATE TABLE item_allergens (
    item_id  BIGINT      NOT NULL REFERENCES event_items (id) ON DELETE CASCADE,
    allergen VARCHAR(30) NOT NULL,
    PRIMARY KEY (item_id, allergen),
    CONSTRAINT ck_item_allergens_allergen CHECK (allergen IN
        ('GLUTEN', 'LACTOSE', 'PEANUT', 'TREE_NUTS', 'SHELLFISH', 'FISH',
         'EGG', 'SOY'))
);

CREATE TABLE item_characteristics (
    item_id        BIGINT      NOT NULL REFERENCES event_items (id) ON DELETE CASCADE,
    characteristic VARCHAR(30) NOT NULL,
    PRIMARY KEY (item_id, characteristic),
    CONSTRAINT ck_item_characteristics_characteristic CHECK (characteristic IN
        ('ANIMAL_MEAT', 'ADDED_SUGAR', 'HIGH_SODIUM'))
);

CREATE TABLE collaborators (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    cpf  CHAR(11)     NOT NULL,
    CONSTRAINT uq_collaborators_cpf UNIQUE (cpf),
    CONSTRAINT ck_collaborators_cpf CHECK (cpf ~ '^[0-9]{11}$')
);

CREATE TABLE collaborator_restrictions (
    collaborator_id BIGINT      NOT NULL REFERENCES collaborators (id) ON DELETE CASCADE,
    restriction     VARCHAR(40) NOT NULL,
    PRIMARY KEY (collaborator_id, restriction),
    CONSTRAINT ck_collaborator_restrictions_restriction CHECK (restriction IN
        ('LACTOSE_INTOLERANCE', 'CELIAC_GLUTEN_FREE', 'PEANUT_TREE_NUT_ALLERGY',
         'SHELLFISH_ALLERGY', 'FISH_ALLERGY', 'EGG_ALLERGY', 'SOY_ALLERGY',
         'DIABETIC_NO_SUGAR', 'HYPERTENSIVE_LOW_SODIUM', 'VEGETARIAN', 'VEGAN'))
);

CREATE TABLE event_participants (
    id              BIGSERIAL PRIMARY KEY,
    event_id        BIGINT    NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    collaborator_id BIGINT    NOT NULL REFERENCES collaborators (id) ON DELETE RESTRICT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_event_participants_collaborator UNIQUE (event_id, collaborator_id)
);

CREATE TABLE contributions (
    id             BIGSERIAL PRIMARY KEY,
    participant_id BIGINT       NOT NULL REFERENCES event_participants (id) ON DELETE CASCADE,
    item_id        BIGINT       NOT NULL REFERENCES event_items (id) ON DELETE CASCADE,
    quantity       INTEGER      NOT NULL,
    note           VARCHAR(255),
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_contributions_participant_item UNIQUE (participant_id, item_id),
    CONSTRAINT ck_contributions_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_contributions_item ON contributions (item_id);
