-- Demo data: five events, one in each status, with fictitious people and fictitious CPFs.
-- The dates are relative to the moment the migration runs, so the open events stay open for weeks.
DO $$
DECLARE
    today DATE := (now() AT TIME ZONE 'America/Sao_Paulo')::date;
    lunch BIGINT;
    breakfast BIGINT;
    barbecue BIGINT;
    snack BIGINT;
    happy_hour BIGINT;
BEGIN
    -- People (the CPFs are valid by the check digits, but belong to nobody)
    INSERT INTO collaborators (name, cpf) VALUES
        ('Ana Souza', '28451697364'),
        ('Bruno Lima', '37582914619'),
        ('Carla Mendes', '46193728546'),
        ('Diego Alves', '53826471946'),
        ('Elisa Rocha', '69248153755'),
        ('Fábio Nunes', '71935826409'),
        ('Gabriela Torres', '84617293582'),
        ('Hugo Pereira', '95362741837');

    INSERT INTO collaborator_restrictions (collaborator_id, restriction)
    SELECT c.id, v.restriction
    FROM (VALUES
        ('Ana Souza', 'CELIAC_GLUTEN_FREE'),
        ('Bruno Lima', 'VEGAN'),
        ('Carla Mendes', 'LACTOSE_INTOLERANCE'),
        ('Diego Alves', 'HYPERTENSIVE_LOW_SODIUM'),
        ('Diego Alves', 'DIABETIC_NO_SUGAR'),
        ('Elisa Rocha', 'PEANUT_TREE_NUT_ALLERGY'),
        ('Hugo Pereira', 'VEGAN'),
        ('Hugo Pereira', 'DIABETIC_NO_SUGAR'),
        ('Hugo Pereira', 'HYPERTENSIVE_LOW_SODIUM')
    ) AS v(person, restriction)
    JOIN collaborators c ON c.name = v.person;

    -- Events
    INSERT INTO events (name, description, type, date_time, change_deadline, time_zone, status)
    VALUES ('Almoço de fim de ano', 'Almoço da equipe para encerrar o ano.', 'LUNCH',
            today + 50 + TIME '12:00', today + 45 + TIME '18:00', 'America/Sao_Paulo', 'ABERTO')
    RETURNING id INTO lunch;
    INSERT INTO events (name, description, type, date_time, change_deadline, time_zone, status)
    VALUES ('Café da manhã de integração', 'Café para receber as pessoas novas do time.', 'BREAKFAST',
            today + 20 + TIME '08:30', today + 15 + TIME '18:00', 'America/Sao_Paulo', 'ABERTO')
    RETURNING id INTO breakfast;
    INSERT INTO events (name, description, type, date_time, change_deadline, time_zone, status)
    VALUES ('Churrasco de confraternização', 'Ainda em preparação: só o cardápio está montado.', 'GET_TOGETHER',
            today + 60 + TIME '13:00', today + 55 + TIME '18:00', 'America/Sao_Paulo', 'RASCUNHO')
    RETURNING id INTO barbecue;
    INSERT INTO events (name, description, type, date_time, change_deadline, time_zone, status)
    VALUES ('Lanche da tarde do time', 'Já aconteceu: todos levaram o combinado.', 'SNACK',
            today - 10 + TIME '15:30', today - 12 + TIME '18:00', 'America/Sao_Paulo', 'REALIZADO')
    RETURNING id INTO snack;
    INSERT INTO events (name, description, type, date_time, change_deadline, time_zone, status)
    VALUES ('Happy hour de aniversário', 'Cancelado por falta de data.', 'OTHER',
            today + 30 + TIME '18:00', today + 25 + TIME '12:00', 'America/Sao_Paulo', 'CANCELADO')
    RETURNING id INTO happy_hour;

    -- Categories (each one has its own unit and goal)
    INSERT INTO event_categories (event_id, name, unit, goal_quantity) VALUES
        (lunch, 'Pratos principais', 'PORTION', 30),
        (lunch, 'Acompanhamentos', 'PORTION', 20),
        (lunch, 'Bebidas', 'MILLILITER', 20000),
        (lunch, 'Sobremesas', 'PORTION', 40),
        (breakfast, 'Pães e frios', 'UNIT', 0),
        (breakfast, 'Doces', 'PORTION', 0),
        (breakfast, 'Bebidas', 'MILLILITER', 0),
        (breakfast, 'Frutas', 'UNIT', 0),
        (barbecue, 'Carnes', 'GRAM', 8000),
        (barbecue, 'Acompanhamentos', 'PORTION', 0),
        (barbecue, 'Bebidas', 'MILLILITER', 0),
        (snack, 'Salgados', 'UNIT', 0),
        (snack, 'Bebidas', 'MILLILITER', 0),
        (happy_hour, 'Petiscos', 'PORTION', 0);

    -- Items
    INSERT INTO event_items (event_id, category_id, name, required_quantity, mandatory)
    SELECT e.id, c.id, v.name, v.required_quantity, v.mandatory
    FROM (VALUES
        ('Almoço de fim de ano', 'Pratos principais', 'Lasanha de carne', 12, TRUE),
        ('Almoço de fim de ano', 'Pratos principais', 'Strogonoff de frango', 10, TRUE),
        ('Almoço de fim de ano', 'Pratos principais', 'Moqueca de peixe', 8, FALSE),
        ('Almoço de fim de ano', 'Pratos principais', 'Risoto de cogumelos', 6, FALSE),
        ('Almoço de fim de ano', 'Acompanhamentos', 'Arroz branco', 20, TRUE),
        ('Almoço de fim de ano', 'Acompanhamentos', 'Salada de folhas', 12, TRUE),
        ('Almoço de fim de ano', 'Acompanhamentos', 'Farofa', 10, FALSE),
        ('Almoço de fim de ano', 'Acompanhamentos', 'Batata gratinada', 8, FALSE),
        ('Almoço de fim de ano', 'Bebidas', 'Água mineral', 10000, TRUE),
        ('Almoço de fim de ano', 'Bebidas', 'Suco de laranja', 6000, TRUE),
        ('Almoço de fim de ano', 'Bebidas', 'Refrigerante', 8000, FALSE),
        ('Almoço de fim de ano', 'Sobremesas', 'Pudim', 12, FALSE),
        ('Almoço de fim de ano', 'Sobremesas', 'Mousse de maracujá', 10, FALSE),
        ('Almoço de fim de ano', 'Sobremesas', 'Salada de frutas', 8, FALSE),
        ('Café da manhã de integração', 'Pães e frios', 'Pão francês', 40, TRUE),
        ('Café da manhã de integração', 'Pães e frios', 'Queijo e presunto', 30, TRUE),
        ('Café da manhã de integração', 'Doces', 'Bolo de cenoura', 12, FALSE),
        ('Café da manhã de integração', 'Bebidas', 'Café', 3000, TRUE),
        ('Café da manhã de integração', 'Bebidas', 'Leite', 2000, TRUE),
        ('Café da manhã de integração', 'Frutas', 'Frutas da estação', 15, FALSE),
        ('Churrasco de confraternização', 'Carnes', 'Picanha', 3000, TRUE),
        ('Churrasco de confraternização', 'Carnes', 'Linguiça', 2000, TRUE),
        ('Churrasco de confraternização', 'Carnes', 'Frango', 3000, FALSE),
        ('Churrasco de confraternização', 'Acompanhamentos', 'Pão de alho', 20, FALSE),
        ('Churrasco de confraternização', 'Acompanhamentos', 'Vinagrete', 15, TRUE),
        ('Churrasco de confraternização', 'Bebidas', 'Refrigerante', 10000, FALSE),
        ('Lanche da tarde do time', 'Salgados', 'Coxinha', 40, TRUE),
        ('Lanche da tarde do time', 'Salgados', 'Pão de queijo', 30, FALSE),
        ('Lanche da tarde do time', 'Bebidas', 'Suco', 3000, TRUE),
        ('Happy hour de aniversário', 'Petiscos', 'Porção de batata frita', 10, FALSE)
    ) AS v(event, category, name, required_quantity, mandatory)
    JOIN events e ON e.name = v.event
    JOIN event_categories c ON c.event_id = e.id AND c.name = v.category;

    INSERT INTO item_allergens (item_id, allergen)
    SELECT i.id, v.allergen
    FROM (VALUES
        ('Almoço de fim de ano', 'Lasanha de carne', 'GLUTEN'),
        ('Almoço de fim de ano', 'Lasanha de carne', 'LACTOSE'),
        ('Almoço de fim de ano', 'Lasanha de carne', 'EGG'),
        ('Almoço de fim de ano', 'Strogonoff de frango', 'LACTOSE'),
        ('Almoço de fim de ano', 'Moqueca de peixe', 'FISH'),
        ('Almoço de fim de ano', 'Risoto de cogumelos', 'LACTOSE'),
        ('Almoço de fim de ano', 'Batata gratinada', 'LACTOSE'),
        ('Almoço de fim de ano', 'Pudim', 'LACTOSE'),
        ('Almoço de fim de ano', 'Pudim', 'EGG'),
        ('Almoço de fim de ano', 'Mousse de maracujá', 'LACTOSE'),
        ('Café da manhã de integração', 'Pão francês', 'GLUTEN'),
        ('Café da manhã de integração', 'Queijo e presunto', 'LACTOSE'),
        ('Café da manhã de integração', 'Bolo de cenoura', 'GLUTEN'),
        ('Café da manhã de integração', 'Bolo de cenoura', 'EGG'),
        ('Café da manhã de integração', 'Bolo de cenoura', 'LACTOSE'),
        ('Café da manhã de integração', 'Leite', 'LACTOSE'),
        ('Churrasco de confraternização', 'Pão de alho', 'GLUTEN'),
        ('Churrasco de confraternização', 'Pão de alho', 'LACTOSE'),
        ('Lanche da tarde do time', 'Coxinha', 'GLUTEN'),
        ('Lanche da tarde do time', 'Pão de queijo', 'LACTOSE'),
        ('Lanche da tarde do time', 'Pão de queijo', 'EGG')
    ) AS v(event, item, allergen)
    JOIN events e ON e.name = v.event
    JOIN event_items i ON i.event_id = e.id AND i.name = v.item;

    INSERT INTO item_characteristics (item_id, characteristic)
    SELECT i.id, v.characteristic
    FROM (VALUES
        ('Almoço de fim de ano', 'Lasanha de carne', 'ANIMAL_MEAT'),
        ('Almoço de fim de ano', 'Strogonoff de frango', 'ANIMAL_MEAT'),
        ('Almoço de fim de ano', 'Moqueca de peixe', 'ANIMAL_MEAT'),
        ('Almoço de fim de ano', 'Farofa', 'HIGH_SODIUM'),
        ('Almoço de fim de ano', 'Suco de laranja', 'ADDED_SUGAR'),
        ('Almoço de fim de ano', 'Refrigerante', 'ADDED_SUGAR'),
        ('Almoço de fim de ano', 'Pudim', 'ADDED_SUGAR'),
        ('Almoço de fim de ano', 'Mousse de maracujá', 'ADDED_SUGAR'),
        ('Café da manhã de integração', 'Queijo e presunto', 'ANIMAL_MEAT'),
        ('Café da manhã de integração', 'Queijo e presunto', 'HIGH_SODIUM'),
        ('Café da manhã de integração', 'Bolo de cenoura', 'ADDED_SUGAR'),
        ('Churrasco de confraternização', 'Picanha', 'ANIMAL_MEAT'),
        ('Churrasco de confraternização', 'Linguiça', 'ANIMAL_MEAT'),
        ('Churrasco de confraternização', 'Linguiça', 'HIGH_SODIUM'),
        ('Churrasco de confraternização', 'Frango', 'ANIMAL_MEAT'),
        ('Churrasco de confraternização', 'Refrigerante', 'ADDED_SUGAR'),
        ('Lanche da tarde do time', 'Coxinha', 'ANIMAL_MEAT'),
        ('Happy hour de aniversário', 'Porção de batata frita', 'HIGH_SODIUM')
    ) AS v(event, item, characteristic)
    JOIN events e ON e.name = v.event
    JOIN event_items i ON i.event_id = e.id AND i.name = v.item;

    -- Enrollments
    INSERT INTO event_participants (event_id, collaborator_id)
    SELECT e.id, c.id
    FROM (VALUES
        ('Almoço de fim de ano', 'Ana Souza'),
        ('Almoço de fim de ano', 'Bruno Lima'),
        ('Almoço de fim de ano', 'Carla Mendes'),
        ('Almoço de fim de ano', 'Diego Alves'),
        ('Almoço de fim de ano', 'Elisa Rocha'),
        ('Almoço de fim de ano', 'Fábio Nunes'),
        ('Almoço de fim de ano', 'Gabriela Torres'),
        ('Almoço de fim de ano', 'Hugo Pereira'),
        ('Café da manhã de integração', 'Ana Souza'),
        ('Café da manhã de integração', 'Diego Alves'),
        ('Café da manhã de integração', 'Fábio Nunes'),
        ('Café da manhã de integração', 'Gabriela Torres'),
        ('Lanche da tarde do time', 'Ana Souza'),
        ('Lanche da tarde do time', 'Bruno Lima'),
        ('Lanche da tarde do time', 'Carla Mendes'),
        ('Lanche da tarde do time', 'Fábio Nunes'),
        ('Happy hour de aniversário', 'Carla Mendes'),
        ('Happy hour de aniversário', 'Hugo Pereira')
    ) AS v(event, person)
    JOIN events e ON e.name = v.event
    JOIN collaborators c ON c.name = v.person;

    -- Contributions (partial in the lunch, almost complete in the breakfast, complete in the snack)
    INSERT INTO contributions (participant_id, item_id, quantity, note)
    SELECT p.id, i.id, v.quantity, v.note
    FROM (VALUES
        ('Almoço de fim de ano', 'Fábio Nunes', 'Lasanha de carne', 8, 'Receita da família'),
        ('Almoço de fim de ano', 'Gabriela Torres', 'Strogonoff de frango', 10, NULL),
        ('Almoço de fim de ano', 'Elisa Rocha', 'Risoto de cogumelos', 3, NULL),
        ('Almoço de fim de ano', 'Carla Mendes', 'Arroz branco', 10, NULL),
        ('Almoço de fim de ano', 'Diego Alves', 'Arroz branco', 10, 'Sem sal'),
        ('Almoço de fim de ano', 'Bruno Lima', 'Salada de folhas', 12, NULL),
        ('Almoço de fim de ano', 'Fábio Nunes', 'Farofa', 4, NULL),
        ('Almoço de fim de ano', 'Ana Souza', 'Água mineral', 6000, NULL),
        ('Almoço de fim de ano', 'Elisa Rocha', 'Água mineral', 4000, NULL),
        ('Almoço de fim de ano', 'Gabriela Torres', 'Suco de laranja', 3000, NULL),
        ('Almoço de fim de ano', 'Elisa Rocha', 'Pudim', 6, NULL),
        ('Almoço de fim de ano', 'Hugo Pereira', 'Salada de frutas', 8, 'Sem açúcar'),
        ('Café da manhã de integração', 'Fábio Nunes', 'Pão francês', 40, NULL),
        ('Café da manhã de integração', 'Gabriela Torres', 'Queijo e presunto', 30, NULL),
        ('Café da manhã de integração', 'Diego Alves', 'Bolo de cenoura', 12, NULL),
        ('Café da manhã de integração', 'Gabriela Torres', 'Café', 3000, NULL),
        ('Café da manhã de integração', 'Fábio Nunes', 'Leite', 2000, NULL),
        ('Café da manhã de integração', 'Ana Souza', 'Frutas da estação', 10, NULL),
        ('Lanche da tarde do time', 'Fábio Nunes', 'Coxinha', 40, NULL),
        ('Lanche da tarde do time', 'Fábio Nunes', 'Pão de queijo', 20, NULL),
        ('Lanche da tarde do time', 'Ana Souza', 'Pão de queijo', 10, NULL),
        ('Lanche da tarde do time', 'Bruno Lima', 'Suco', 3000, NULL)
    ) AS v(event, person, item, quantity, note)
    JOIN events e ON e.name = v.event
    JOIN collaborators c ON c.name = v.person
    JOIN event_participants p ON p.event_id = e.id AND p.collaborator_id = c.id
    JOIN event_items i ON i.event_id = e.id AND i.name = v.item;
END $$;
