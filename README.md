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
| 4 | Como calcular a cobertura? | Três visões, sem um número único: por item (com teto de 100%), por categoria (contra a meta) e geral (obrigatórios completos, e opcionais em separado). |
| 5 | Como relacionar restrições alimentares e alérgenos? | Matriz fixa de conflitos, em listas fechadas. Veja a seção "Alérgenos, características e restrições". A visão "ver o cardápio como" um participante marca os itens em conflito. O sistema avisa e não bloqueia. |
| 6 | Qual será a funcionalidade livre? | Sugestões do que levar (itens com maior falta e seguros para a restrição da pessoa) e resumo em texto para compartilhar. |
| 7 | Como tratar o CPF? | A API aceita 11 dígitos ou o formato completo com máscara (`390.533.447-05`). Ela recusa letras e máscara pela metade. Ela guarda só os 11 dígitos, confere os dígitos verificadores (módulo 11) e recusa sequências repetidas. O formulário do frontend mostra a máscara. O sistema não consulta a Receita Federal: um CPF que passa na conta pode não pertencer a ninguém. Para identificar a pessoa e evitar duplicidade, isso basta. |
| 8 | O sistema tem login? | Não. É uma premissa: qualquer pessoa pode alterar a contribuição de outra. |
| 9 | O que cada status permite? | Só `ABERTO` aceita inscrições e contribuições. Em `RASCUNHO`, o organizador monta o cardápio. `ENCERRADO`, `REALIZADO` e `CANCELADO` ficam só para leitura. |
| 10 | Como definir o tipo do evento? | Lista fechada: `CAFE_DA_MANHA`, `ALMOCO`, `LANCHE`, `CONFRATERNIZACAO` e `OUTRO`. |
| 11 | O item precisa ter categoria? | Sim. A categoria é obrigatória. O sistema bloqueia apagar uma categoria que ainda tem itens. Assim toda a cobertura por categoria é completa. |
| 12 | Em que unidade fica a meta da categoria? | A unidade pertence à categoria, e os itens dela usam essa unidade. Assim a soma dos itens e a meta são comparáveis. Quem precisa de outra unidade cria outra categoria. |
| 13 | A quantidade é inteira ou decimal? | Inteira. As unidades são uma lista fechada: `UNIDADE`, `PORCAO`, `GRAMA` e `MILILITRO`. Quem quer 1,5 kg escreve 1500 gramas. A soma e o limite ficam exatos, sem arredondamento. |
| 14 | Quem define a lista de restrições alimentares? | O enunciado não dá a lista. Nós definimos 11 restrições, escolhidas por serem comuns em eventos de empresa. |
| 15 | Todo item da lista de "alérgenos" é um alérgeno? | Não. Peixe é alérgeno, mas "carne de origem animal", "açúcar adicionado" e "rico em sal" não são. Separamos em dois campos: alérgenos e características alimentares. O enunciado só fala de alérgenos, e a interface mantém essa palavra. |
| 16 | O vegetariano aceita ovo e laticínios? | Sim. Quem evita ovo ou leite marca a restrição própria ou escolhe vegano. |
| 17 | O cardápio pode mudar com o evento `ABERTO`? | Sim, com duas proteções: não reduzir a quantidade de um item abaixo do total já contribuído, e não apagar um item que já tem contribuições. Assim o organizador corrige erros sem apagar o compromisso de alguém. |
| 18 | O prazo vale até quando? | Até o instante marcado, inclusive. No instante exato do prazo ainda é possível alterar. Um segundo depois, o prazo está vencido. |
| 19 | Como a API informa os erros? | Todo erro sai como `ProblemDetail` (RFC 7807), com a mensagem em `detail`. `400` é entrada malformada, `422` é regra de negócio quebrada, `409` é conflito com dado existente e `404` é recurso inexistente. No `400` de validação, o campo `erros` lista o campo e a mensagem de cada erro. Um erro inesperado sai como `500` com mensagem genérica, e o detalhe vai só para o log. |
| 20 | Em que fuso o prazo e a data do evento são lidos? | Cada evento guarda o seu fuso (`America/Sao_Paulo` por padrão). A data e o prazo são horários de parede nesse fuso. O relógio do servidor fica em UTC e só informa o instante real, então o prazo vence na hora certa em qualquer servidor. As datas de criação e atualização (`criadoEm`, `atualizadoEm`) usam o fuso do servidor e são só informativas. |
| 21 | O que se pode editar em cada status? | `RASCUNHO` e `ABERTO` editam tudo. Em `ABERTO`, o prazo novo precisa ainda não ter passado, senão o evento se encerraria sozinho. `ENCERRADO` edita só nome, descrição, tipo e data e hora. O prazo e o fuso ficam travados, porque não há reabertura. `REALIZADO` e `CANCELADO` não editam nada. |
| 22 | Quais eventos podem ser excluídos? | Só `RASCUNHO`. Excluir um evento aberto apagaria, em cascata, inscrições e contribuições. Quem quer desfazer um evento aberto o cancela, e o cancelamento preserva o histórico. |
| 23 | Como o status muda, e quando um evento vira `REALIZADO`? | Pela rota `PATCH /api/eventos/{id}/status`, que respeita a máquina de estados. Um evento só vira `REALIZADO` na data e hora marcadas, lidas no fuso do evento, ou depois. O cancelamento é livre a qualquer momento. Ao abrir um rascunho, a chamada pode trazer um prazo novo. Nas outras mudanças, o prazo novo é recusado. Uma mudança inválida devolve 422 com as transições permitidas. |

## Tecnologias escolhidas

- **Backend:** Java 21, Spring Boot 4.0.9, Maven, JPA/Hibernate, PostgreSQL 16, Flyway e springdoc-openapi 3.0.3 (Swagger). O Spring Boot 4 é a versão estável com suporte. A linha 3.4 já saiu do gerador oficial e não recebe mais correções de segurança.
- **Testes do backend:** JUnit 5 e Testcontainers. Os testes de integração usam um PostgreSQL real, o mesmo banco da produção.
- **Frontend:** React, TypeScript, Vite e Tailwind. Vitest e React Testing Library nos testes.
- **Execução:** Docker Compose, com frontend, backend e banco.
- **Publicação:** Render, definida no fim do projeto.

## Idioma

README, commits e interface em português. Termos do domínio ficam em português (`Evento`, `StatusEvento`). Termos técnicos ficam em inglês (`Controller`, `Service`).

## Alérgenos, características e restrições

Cada item guarda dois conjuntos. Os **alérgenos** são `GLUTEN`, `LACTOSE`, `AMENDOIM`, `CASTANHAS`, `FRUTOS_DO_MAR`, `PEIXE`, `OVO` e `SOJA`. As **características alimentares** são `CARNE_ORIGEM_ANIMAL`, `ACUCAR_ADICIONADO` e `ALTO_SODIO`. Quem cadastra o item decide se ele é rico em sal. Esse dado é um aviso e não um aconselhamento médico.

Cada restrição de um participante conflita com alguns valores:

| Restrição | Conflita com (alérgenos) | Conflita com (características) |
| :--- | :--- | :--- |
| `INTOLERANCIA_LACTOSE` | `LACTOSE` | |
| `CELIACO_SEM_GLUTEN` | `GLUTEN` | |
| `ALERGIA_AMENDOIM_CASTANHAS` | `AMENDOIM`, `CASTANHAS` | |
| `ALERGIA_FRUTOS_DO_MAR` | `FRUTOS_DO_MAR` | |
| `ALERGIA_PEIXE` | `PEIXE` | |
| `ALERGIA_OVO` | `OVO` | |
| `ALERGIA_SOJA` | `SOJA` | |
| `DIABETICO_SEM_ACUCAR` | | `ACUCAR_ADICIONADO` |
| `HIPERTENSO_POUCO_SODIO` | | `ALTO_SODIO` |
| `VEGETARIANO` | `FRUTOS_DO_MAR`, `PEIXE` | `CARNE_ORIGEM_ANIMAL` |
| `VEGANO` | `FRUTOS_DO_MAR`, `PEIXE`, `LACTOSE`, `OVO` | `CARNE_ORIGEM_ANIMAL` |

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

O enum `StatusEvento` guarda essas regras. Os services consultam o status e não repetem `if` próprios.
