# Sulwork Eventos

Aplicação para organizar eventos de alimentação da Sulwork: cafés da manhã, almoços, lanches e confraternizações em que cada pessoa leva alguma coisa.

## O problema

Em um evento coletivo, o organizador precisa saber o que falta levar. Cada participante precisa saber o que pode levar, sem repetir o que outra pessoa já combinou. Algumas pessoas têm restrições alimentares, e alguns itens contêm alérgenos. A aplicação junta essas informações em um só lugar: eventos, participantes, itens por categoria, contribuições e cobertura do que foi combinado.

## Decisões e ambiguidades

O enunciado deixa regras abertas de propósito. Esta seção lista cada ambiguidade e a direção escolhida. O detalhe de cada decisão entra neste README no passo em que ela vira código.

| # | Pergunta | Direção escolhida |
| :-- | :--- | :--- |
| 1 | Quais transições de status existem? | Fluxo em linha: `RASCUNHO → ABERTO → ENCERRADO → REALIZADO`. O `CANCELADO` é possível a partir de `RASCUNHO`, `ABERTO` e `ENCERRADO`. `REALIZADO` e `CANCELADO` são finais. Não há reabertura. |
| 2 | Como o prazo e o status se combinam? | A API confere o relógio em cada operação. Um job agendado muda `ABERTO` para `ENCERRADO` quando o prazo passa. O prazo não pode ser depois da data do evento. |
| 3 | Como uma pessoa participa de vários eventos sem cadastro duplicado? | Cadastro único da pessoa, com CPF único, guarda nome e restrições alimentares. Uma inscrição liga a pessoa ao evento. Um índice único impede a mesma pessoa duas vezes no mesmo evento. |
| 4 | Como calcular a cobertura? | A conta roda dentro de cada categoria, onde a unidade é uma só, e o geral é a média das categorias. Veja a seção "Cálculo da cobertura". |
| 5 | Como relacionar restrições alimentares e alérgenos? | Matriz fixa de conflitos, em listas fechadas. Veja a seção "Alérgenos, características e restrições". A visão "ver o cardápio como" um participante marca os itens em conflito. O sistema avisa e não bloqueia. |
| 6 | Qual será a funcionalidade livre? | Sugestões do que levar (itens com maior falta e seguros para a restrição da pessoa) e resumo em texto para compartilhar. |
| 7 | Como tratar o CPF? | A API aceita 11 dígitos ou o formato completo com máscara (`390.533.447-05`). Ela recusa letras e máscara pela metade. Ela guarda só os 11 dígitos, confere os dígitos verificadores (módulo 11) e recusa sequências repetidas. O formulário do frontend mostra a máscara. O sistema não consulta a Receita Federal: um CPF que passa na conta pode não pertencer a ninguém. Para identificar a pessoa e evitar duplicidade, isso basta. |
| 8 | O sistema tem login? | Não. É uma premissa: qualquer pessoa pode alterar a contribuição de outra. |
| 9 | O que cada status permite? | Só `ABERTO` aceita inscrições e contribuições. Em `RASCUNHO`, o organizador monta o cardápio. `ENCERRADO`, `REALIZADO` e `CANCELADO` ficam só para leitura. |
| 10 | Como definir o tipo do evento? | Lista fechada: `BREAKFAST`, `LUNCH`, `SNACK`, `GET_TOGETHER` e `OTHER`. |
| 11 | O item precisa ter categoria? | Sim. A categoria é obrigatória. O sistema bloqueia apagar uma categoria que ainda tem itens. Assim toda a cobertura por categoria é completa. |
| 12 | Em que unidade fica a meta da categoria? | A unidade pertence à categoria, e os itens dela usam essa unidade. Assim a soma dos itens e a meta são comparáveis. Quem precisa de outra unidade cria outra categoria. |
| 13 | A quantidade é inteira ou decimal? | Inteira. As unidades são uma lista fechada: `UNIT`, `PORTION`, `GRAM` e `MILLILITER`. Quem quer 1,5 kg escreve 1500 gramas. A soma e o limite ficam exatos, sem arredondamento. |
| 14 | Quem define a lista de restrições alimentares? | O enunciado não dá a lista. Nós definimos 11 restrições, escolhidas por serem comuns em eventos de empresa. |
| 15 | Todo item da lista de "alérgenos" é um alérgeno? | Não. Peixe é alérgeno, mas "carne de origem animal", "açúcar adicionado" e "rico em sal" não são. Separamos em dois campos: alérgenos e características alimentares. O enunciado só fala de alérgenos, e a interface mantém essa palavra. |
| 16 | O vegetariano aceita ovo e laticínios? | Sim. Quem evita ovo ou leite marca a restrição própria ou escolhe vegano. |
| 17 | O cardápio pode mudar com o evento `ABERTO`? | Sim, com duas proteções: não reduzir a quantidade de um item abaixo do total já contribuído, e não apagar um item que já tem contribuições. Assim o organizador corrige erros sem apagar o compromisso de alguém. |
| 18 | O prazo vale até quando? | Até o instante marcado, inclusive. No instante exato do prazo ainda é possível alterar. Um segundo depois, o prazo está vencido. |
| 19 | Como a API informa os erros? | Todo erro sai como `ProblemDetail` (RFC 7807), com a mensagem em `detail`. `400` é entrada malformada, `422` é regra de negócio quebrada, `409` é conflito com dado existente e `404` é recurso inexistente. No `400` de validação, o campo `errors` lista o campo e a mensagem de cada erro. Um erro inesperado sai como `500` com mensagem genérica, e o detalhe vai só para o log. |
| 20 | Em que fuso o prazo e a data do evento são lidos? | Cada evento guarda o seu fuso (`America/Sao_Paulo` por padrão). A data e o prazo são horários de parede nesse fuso. O relógio do servidor fica em UTC e só informa o instante real, então o prazo vence na hora certa em qualquer servidor. As datas de criação e atualização (`createdAt`, `updatedAt`) usam o fuso do servidor e são só informativas. |
| 21 | O que se pode editar em cada status? | `RASCUNHO` e `ABERTO` editam tudo. Em `ABERTO`, o prazo novo precisa ainda não ter passado, senão o evento se encerraria sozinho. `ENCERRADO` edita só nome, descrição, tipo e data e hora. O prazo e o fuso ficam travados, porque não há reabertura. `REALIZADO` e `CANCELADO` não editam nada. |
| 22 | Quais eventos podem ser excluídos? | Só `RASCUNHO`. Excluir um evento aberto apagaria, em cascata, inscrições e contribuições. Quem quer desfazer um evento aberto o cancela, e o cancelamento preserva o histórico. |
| 23 | Como o status muda, e quando um evento vira `REALIZADO`? | Pela rota `PATCH /api/events/{id}/status`, que respeita a máquina de estados. Um evento só vira `REALIZADO` na data e hora marcadas, lidas no fuso do evento, ou depois. O cancelamento é livre a qualquer momento. Ao abrir um rascunho, a chamada pode trazer um prazo novo. Nas outras mudanças, o prazo novo é recusado. Uma mudança inválida devolve 422 com as transições permitidas. |
| 24 | Como o prazo e o status ficam coerentes? | Dupla garantia, com uma regra só, escrita em Java (`Event.isDeadlineExpired`). **Regra síncrona:** toda operação confere o prazo na hora, então nada passa depois do prazo, mesmo que o status ainda diga `ABERTO`. **Sincronização:** ao ler, listar ou alterar um evento aberto com prazo vencido, ele vira `ENCERRADO` antes de responder. Um agendador a cada 60 segundos faz o mesmo sem ninguém acessar. Uma só garantia falharia: só o agendador deixa uma janela de até 60 segundos, e só a leitura deixa o banco desatualizado para relatórios. Escolhemos Java a SQL em lote, para a regra não existir em dois lugares e um nome de fuso estranho afetar só o evento dele. |
| 25 | Como funcionam as categorias de um evento? | Cada categoria tem nome, unidade e meta. O nome é único no evento, sem diferenciar maiúsculas nem espaços nas pontas: o banco garante isso com um índice em `lower(name)`. A unidade fica travada enquanto a categoria tem itens, para um item não mudar de significado. Uma categoria só se exclui quando está sem itens. O cardápio só muda em `RASCUNHO` e `ABERTO`. Cada categoria mostra o total de itens, a quantidade planejada (soma dos itens) e a quantidade contribuída (soma das contribuições). |
| 26 | Como funcionam os itens do cardápio? | O nome do item é único dentro da categoria, sem diferenciar maiúsculas nem espaços: o banco garante com um índice em `(category_id, lower(name))`. O mesmo nome pode existir em categorias diferentes. O item não muda de categoria depois de criado, porque a unidade vem da categoria. Para mudar, exclua e recrie, o que só é possível sem contribuições. Cada item mostra a quantidade contribuída, a pendente (nunca negativa) e o percentual coberto, arredondado para baixo: só mostra 100% quando está completo. Alérgenos e características são listas opcionais. |
| 27 | Como uma pessoa participa de vários eventos, e quem altera o cadastro dela? | O cadastro da pessoa (nome, CPF e restrições) é único e vale para todos os eventos. A inscrição só liga a pessoa ao evento. Inscrever uma pessoa já cadastrada não altera nada: nome e restrições do corpo são ignorados, e a resposta mostra o que está guardado. Um CPF novo exige o nome. O cadastro muda em um só lugar, `PUT /api/collaborators/{id}`, que exige nome e restrições, para que um erro de inscrição ou uma omissão não apague dados. Remover a inscrição apaga as contribuições da pessoa naquele evento e mantém o cadastro. |
| 28 | Quanto do CPF a API mostra? | O CPF sai mascarado (`***.533.447-**`): só os seis dígitos do meio, o bastante para separar homônimos. O CPF completo só entra na inscrição e na busca por CPF exato. Não existe listagem geral de colaboradores. Restrição alimentar é dado de saúde, e sem login (decisão 8) quem conhece um CPF consegue ver o nome e as restrições da pessoa. É um risco aceito. |
| 29 | Como funcionam as contribuições, e o que acontece com pedidos simultâneos? | `POST` cria a contribuição de uma pessoa para um item, e `PUT` altera a quantidade e a observação. Um segundo `POST` da mesma pessoa para o mesmo item devolve 409, e nada soma por acidente: um clique duplo não dobra o valor. A soma de todas as contribuições do item nunca passa da quantidade necessária (422, com o que ainda cabe). Só um evento aberto e dentro do prazo aceita incluir, alterar ou remover. **Concorrência:** antes de mudar uma contribuição, o sistema bloqueia a linha do item no banco (`SELECT ... FOR UPDATE`). Dois pedidos simultâneos não leem o mesmo saldo livre: o segundo espera o primeiro terminar e lê a soma correta. Editar a quantidade do item e excluí-lo passam pelo mesmo bloqueio. Teste: 20 pessoas disputam 1 unidade, e só uma contribuição entra. Sem o bloqueio, o mesmo teste aceita 7 contribuições. |
| 30 | Como o sistema mostra quem pode ficar sem comer? | `GET /api/events/{id}/dietary-analysis` cruza as restrições de cada participante com os alérgenos e as características de cada item, pela matriz da seção "Alérgenos, características e restrições". Mostra, por pessoa, quantos itens são seguros e quais não são, com o motivo. Mostra, por restrição, quantas pessoas a têm e quantos itens são compatíveis. Gera alertas: `CRITICAL` quando uma pessoa com restrição não tem nenhum item seguro, `WARNING` quando menos de 35% do cardápio é seguro para ela, e `INFO` ("Cardápio inclusivo") quando não há outro alerta, há pessoas com restrição e o cardápio tem itens. Com o cardápio vazio, não há alertas. O sistema só avisa e não bloqueia. Um item sem nenhuma marca conta como seguro, então o aviso vale tanto quanto o cadastro dos itens. |
| 31 | Qual é a funcionalidade livre, e como ela decide o que sugerir? | Sugestões do que levar: `GET /api/events/{id}/suggestions?participantId=X`. Veja a seção "Funcionalidade livre: sugestões do que levar". A prioridade vai de 0 a 100, em relação ao máximo que aquela pessoa poderia pontuar. Itens que a pessoa já contribui, itens completos e eventos que não aceitam contribuições ficam fora da lista. |
| 32 | O que entra no resumo para compartilhar? | `GET /api/events/{id}/summary` devolve um texto pronto para colar em um grupo (WhatsApp, Slack ou Teams): o nome, a data, o prazo, a situação, a cobertura, o que ainda falta (obrigatórios primeiro) e quem já vai levar o quê. O texto não leva CPF nem restrições alimentares: ele vai para um grupo, e restrição alimentar é dado de saúde. |

## Tecnologias escolhidas

- **Backend:** Java 21, Spring Boot 4.0.9, Maven, JPA/Hibernate, PostgreSQL 16, Flyway e springdoc-openapi 3.0.3 (Swagger). O Spring Boot 4 é a versão estável com suporte. A linha 3.4 já saiu do gerador oficial e não recebe mais correções de segurança.
- **Testes do backend:** JUnit 5 e Testcontainers. Os testes de integração usam um PostgreSQL real, o mesmo banco da produção.
- **Frontend:** React, TypeScript, Vite e Tailwind. Vitest e React Testing Library nos testes.
- **Execução:** Docker Compose, com frontend, backend e banco.
- **Publicação:** Render, definida no fim do projeto.

## Idioma

README, commits e interface ficam em português: o enunciado, quem avalia e quem usa o sistema falam português. O **código** (classes, métodos, campos, variáveis, nomes dos testes e comentários), o **banco** (tabelas e colunas) e a **API** (rotas e campos JSON) ficam em inglês. Duas exceções, de propósito:

- os cinco status do evento (`RASCUNHO`, `ABERTO`, `ENCERRADO`, `REALIZADO` e `CANCELADO`), porque o enunciado manda esses nomes;
- os textos que o usuário lê, como as mensagens de erro da API e as descrições dos enums (por exemplo, "Café da Manhã").

## Alérgenos, características e restrições

Cada item guarda dois conjuntos. Os **alérgenos** são `GLUTEN`, `LACTOSE`, `PEANUT`, `TREE_NUTS`, `SHELLFISH`, `FISH`, `EGG` e `SOY`. As **características alimentares** são `ANIMAL_MEAT`, `ADDED_SUGAR` e `HIGH_SODIUM`. Quem cadastra o item decide se ele é rico em sal. Esse dado é um aviso e não um aconselhamento médico.

Cada restrição de um participante conflita com alguns valores:

| Restrição | Conflita com (alérgenos) | Conflita com (características) |
| :--- | :--- | :--- |
| `LACTOSE_INTOLERANCE` | `LACTOSE` | |
| `CELIAC_GLUTEN_FREE` | `GLUTEN` | |
| `PEANUT_TREE_NUT_ALLERGY` | `PEANUT`, `TREE_NUTS` | |
| `SHELLFISH_ALLERGY` | `SHELLFISH` | |
| `FISH_ALLERGY` | `FISH` | |
| `EGG_ALLERGY` | `EGG` | |
| `SOY_ALLERGY` | `SOY` | |
| `DIABETIC_NO_SUGAR` | | `ADDED_SUGAR` |
| `HYPERTENSIVE_LOW_SODIUM` | | `HIGH_SODIUM` |
| `VEGETARIAN` | `SHELLFISH`, `FISH` | `ANIMAL_MEAT` |
| `VEGAN` | `SHELLFISH`, `FISH`, `LACTOSE`, `EGG` | `ANIMAL_MEAT` |

A matriz mora nos enums, em um só lugar. Mudar a matriz muda o sistema inteiro. Os `CHECK` do banco repetem as listas, e testes de integração garantem que enums e banco concordam.

## Status do evento

| Status | Pode ir para | Cardápio | Inscrições | Contribuições |
| :--- | :--- | :---: | :---: | :---: |
| `RASCUNHO` | `ABERTO`, `CANCELADO` | sim | não | não |
| `ABERTO` | `ENCERRADO`, `CANCELADO` | sim | sim | sim |
| `ENCERRADO` | `REALIZADO`, `CANCELADO` | não | não | não |
| `REALIZADO` | nenhum | não | não | não |
| `CANCELADO` | nenhum | não | não | não |

- **`RASCUNHO`:** o organizador monta o cardápio. Ninguém se inscreve nem contribui, porque o cardápio ainda pode mudar.
- **`ABERTO`:** o evento recebe inscrições e contribuições. O cardápio ainda pode ser ajustado, com as proteções da decisão 17.
- **`ENCERRADO`:** o prazo passou ou o organizador encerrou. Tudo fica só para leitura, até o evento acontecer ou ser cancelado.
- **`REALIZADO` e `CANCELADO`:** estados finais. Eles preservam o histórico.
- **Sem reabertura.** Um evento encerrado não volta a `ABERTO`. Reabrir exigiria um novo prazo e deixaria a regra do prazo mais complexa.

O enum `EventStatus` guarda essas regras. Os services consultam o status e não repetem `if` próprios.

## Consulta SQL nativa da cobertura por categoria

O enunciado exige pelo menos uma consulta SQL nativa. Ela está em `EventItemRepository.coveragePerCategory`. Para cada categoria do evento, o banco devolve uma linha com a meta, o total de itens, e a quantidade necessária e a contribuída, separando itens obrigatórios e opcionais. A cobertura (passo seguinte) é calculada sobre essas linhas.

Somar e agrupar várias tabelas é trabalho do banco: trazer todas as linhas para o Java e somar lá seria mais lento e mais código. Três pontos da consulta merecem atenção:

- **Subconsulta das contribuições.** As contribuições entram já somadas por item. Se entrassem linha a linha, um item com 4 contribuições viraria 4 linhas, e a quantidade necessária dele seria somada 4 vezes. Um teste com 100 necessários e 4 contribuições de 10 prova que o resultado é 100 necessários e 40 contribuídos.
- **`LEFT JOIN`.** Uma categoria sem itens continua na lista, com zeros. Com um `JOIN` comum, ela sumiria.
- **`SUM(...) FILTER (WHERE ...)`.** Recurso do PostgreSQL que soma só as linhas que passam na condição. Ele separa obrigatórios de opcionais em uma única passada.

## Cálculo da cobertura

`GET /api/events/{id}/coverage` mostra quanto das necessidades do evento foi atendido, em três níveis: por categoria, geral e prontidão.

**Por que a conta é por categoria.** Cada categoria tem uma unidade (mililitros, gramas, unidades). Somar categorias diferentes misturaria unidades: 5000 mililitros pesariam mais que 40 salgados. Dentro de uma categoria a unidade é a mesma, então somar e comparar é válido. O geral é a média simples das categorias.

**Cobertura de uma categoria** (obrigatórios valem 2, opcionais valem 1, e o que a meta pede além dos itens entra no total):

```
(2 × contribuído_obrigatórios + 1 × contribuído_opcionais)
÷ (2 × necessário_obrigatórios + 1 × necessário_opcionais + 1 × déficit_da_meta)

déficit_da_meta = max(0, meta − soma dos itens da categoria)
alvo efetivo    = max(meta, soma dos itens da categoria)
```

- **Peso 2 nos obrigatórios:** sem eles, o evento não acontece.
- **Déficit da meta no total:** sem ele, uma categoria com todos os itens prontos mostraria 100%, mesmo com a meta longe de ser atingida.
- **Exemplo:** obrigatórios 6 de 6, opcionais 0 de 4, sem déficit: `(2×6 + 0) ÷ (2×6 + 4) = 75%`.
- **Meta sem itens:** uma categoria com meta 20 e nenhum item vale 0%. Uma categoria sem itens e sem meta fica fora da média.

**Geral e por tipo.** `overallPercentage` é a média da cobertura das categorias. `mandatoryPercentage` e `optionalPercentage` são médias da cobertura de cada tipo, entre as categorias que têm itens daquele tipo. Quando não existe nenhum item do tipo, não falta nada, e o valor é 100. Todos os percentuais arredondam para baixo: só mostram 100 quando está completo.

**Prontidão** (a primeira regra que vale):

| Nível | Quando |
| :--- | :--- |
| `NO_ITEMS` | o evento não tem nenhum item |
| `CRITICAL` | a cobertura dos obrigatórios está abaixo de 50% |
| `COMPLETE` | todos os itens estão completos e a meta de cada categoria foi atingida |
| `MANDATORY_SECURED` | todos os itens obrigatórios estão completos |
| `IN_PROGRESS` | os demais casos |

Uma contribuição nunca passa do necessário do item, então 100% nos obrigatórios de uma categoria significa que todo item obrigatório dela está completo.

## Funcionalidade livre: sugestões do que levar

**O problema.** A pessoa entra no evento e não sabe o que levar. Escolher no escuro gera três falhas: tudo chega repetido, o que é essencial falta, e quem tem restrição alimentar recebe sugestões que não pode comer.

**Como funciona.** `GET /api/events/{id}/suggestions?participantId=X` olha os itens que ainda têm saldo e os ordena pela urgência para aquela pessoa. Cada regra soma pontos:

| Regra | Pontos |
| :--- | :---: |
| o item é obrigatório | +50 |
| ninguém contribuiu ainda | +25 |
| a cobertura do item está abaixo da média do evento | +20 |
| a meta da categoria do item ainda não foi atingida | +20 |
| o item é seguro para as restrições da pessoa (só quando ela tem restrições) | +25 |
| o item **não** é seguro para as restrições da pessoa | −10 |

Cada sugestão traz a quantidade pendente, a **quantidade sugerida**, a **prioridade** e os **motivos** em texto, um por regra que pontuou.

**Decisões.**

- **Prioridade de 0 a 100, e não a soma bruta.** A soma pode chegar a 140, e um número acima de 100 parece um erro. A prioridade é a soma dividida pelo máximo que aquela pessoa poderia ter (115 sem restrições, 140 com restrições), limitada a 0. Prioridade 100 quer dizer que o item pontua em todas as regras que se aplicam a ela. A ordem da lista usa os pontos brutos.
- **Quantidade sugerida:** o saldo inteiro, se for até 3. Acima disso, metade do saldo, arredondada para cima. Sugerir só metade deixa espaço para outras pessoas dividirem itens grandes.
- **Empate:** vence o item menos coberto, e depois o mais antigo.
- **Média e meta sem misturar unidades.** A média de cobertura é a média da cobertura de cada item, sem somar quantidades. Assim 1000 mililitros não pesam mais que 2 unidades. A meta da categoria conta como atingida quando a soma contribuída chega a ela, ou quando a meta é zero.
- **Item inseguro aparece, mas por último e com aviso.** O motivo diz o que conflita, por exemplo "Não é seguro para as suas restrições: Glúten." A pessoa decide: ela pode levar um item para os colegas, e a lista não fica vazia só porque o cardápio tem o alérgeno dela.
- **O que fica fora da lista:** itens completos, itens com que a pessoa já contribui (um segundo `POST` daria 409, e a pessoa deve editar a contribuição), e qualquer item quando o evento não aceita mais contribuições.
- **A conta é pura.** `SuggestionRanker` não conhece o banco, então os testes cobrem cada regra sem subir nada. `SuggestionService` só junta os dados e chama a conta.

### Resumo para compartilhar

O organizador cobra a equipe no grupo da empresa. `GET /api/events/{id}/summary` monta o texto dessa cobrança, com o que está nos dados do evento:

```
*Almoço de fim de ano* (Almoço)
Data: 18/12/2026 às 12:00 (America/Sao_Paulo)
Prazo para contribuir: 16/12/2026 às 18:00
Situação: Aberto

*Cobertura*
Geral: 47% | Obrigatórios: 100% | Opcionais: 25%
Prontidão: Obrigatórios garantidos

*O que ainda falta*
Obrigatórios:
- Lasagna (Pratos): faltam 4 (unidade)
Opcionais:
- Suco (Bebidas): faltam 1000 (mililitro)

*Quem já vai levar*
- Ana: Lasagna 6 (unidade)
- Beto: Suco 1000 (mililitro)
```

- **Formato.** A resposta é um JSON com o campo `text`, para a tela ter um botão "Copiar". Os títulos usam um asterisco, que o WhatsApp e o Slack mostram em negrito. O Teams mostra os asteriscos, o que não atrapalha a leitura.
- **Ordem do que falta.** Obrigatórios primeiro e, dentro de cada grupo, os itens menos cobertos primeiro.
- **O que fica de fora.** CPF e restrições alimentares nunca entram no texto. O texto vai para um grupo, e restrição alimentar é dado de saúde. O aviso de quem pode ficar sem comer fica na análise alimentar, que só o organizador vê.
- **Casos vazios.** Sem itens, o texto diz "Ainda não há itens no cardápio." Sem nada faltando, diz "Nada: o cardápio está completo." Sem contribuições, diz "Ninguém contribuiu ainda."
