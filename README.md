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
| 7 | Como tratar o CPF? | A API aceita com ou sem máscara, guarda só os 11 dígitos e confere os dígitos verificadores. Ela recusa sequências repetidas. O formulário do frontend mostra a máscara. |
| 8 | O sistema tem login? | Não. É uma premissa: qualquer pessoa pode alterar a contribuição de outra. |
| 9 | O que cada status permite? | Só `ABERTO` aceita inscrições e contribuições. Em `RASCUNHO`, o organizador monta o cardápio. `ENCERRADO`, `REALIZADO` e `CANCELADO` ficam só para leitura. |
| 10 | Como definir o tipo do evento? | Lista fechada: `CAFE_DA_MANHA`, `ALMOCO`, `LANCHE`, `CONFRATERNIZACAO` e `OUTRO`. |
| 11 | O item precisa ter categoria? | Sim. A categoria é obrigatória. O sistema bloqueia apagar uma categoria que ainda tem itens. Assim toda a cobertura por categoria é completa. |
| 12 | Em que unidade fica a meta da categoria? | A unidade pertence à categoria, e os itens dela usam essa unidade. Assim a soma dos itens e a meta são comparáveis. Quem precisa de outra unidade cria outra categoria. |
| 13 | A quantidade é inteira ou decimal? | Inteira. As unidades são uma lista fechada: `UNIDADE`, `PORCAO`, `GRAMA` e `MILILITRO`. Quem quer 1,5 kg escreve 1500 gramas. A soma e o limite ficam exatos, sem arredondamento. |
| 14 | Quem define a lista de restrições alimentares? | O enunciado não dá a lista. Nós definimos 11 restrições, escolhidas por serem comuns em eventos de empresa. |
| 15 | Todo item da lista de "alérgenos" é um alérgeno? | Não. Peixe é alérgeno, mas "carne de origem animal", "açúcar adicionado" e "rico em sal" não são. Separamos em dois campos: alérgenos e características alimentares. O enunciado só fala de alérgenos, e a interface mantém essa palavra. |
| 16 | O vegetariano aceita ovo e laticínios? | Sim. Quem evita ovo ou leite marca a restrição própria ou escolhe vegano. |

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
