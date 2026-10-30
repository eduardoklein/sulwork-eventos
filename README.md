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
| 3 | Como uma pessoa participa de vários eventos sem cadastro duplicado? | Cadastro único da pessoa, com CPF único, guarda nome e restrições alimentares. Uma inscrição liga a pessoa ao evento. Um índice único impede a mesma pessoa duàs vezes no mesmo evento. |
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
| 19 | Como a API informa os erros? | Todo erro sai como `ProblemDetail` (RFC 7807), com a mensagem em `detail`. `400` é entrada malformada, `422` é regra de negócio quebrada, `409` é conflito com dado existente e `404` é recurso inexistente. No `400` de validação, o campo `errors` lista o campo e a mensagem de cada erro. Um erro inesperado sai como `500` com mensagem genérica, e o detalhe vai só para o log. Um valor fora de uma lista fechada (por exemplo, `"type": "JANTAR"`) também sai como `400` com `errors`, dizendo o campo (`type`, ou `restrictions[1]` dentro de uma lista) e os valores aceitos. |
| 20 | Em que fuso o prazo e a data do evento são lidos? | Cada evento guarda o seu fuso (`America/Sao_Paulo` por padrão). A data e o prazo são horários de parede nesse fuso. O relógio do servidor fica em UTC e só informa o instante real, então o prazo vence na hora certa em qualquer servidor. As datas de criação e atualização (`createdAt`, `updatedAt`) usam o fuso do servidor e são só informativas. |
| 21 | O que se pode editar em cada status? | `RASCUNHO` e `ABERTO` editam tudo. Em `ABERTO`, o prazo novo precisa ainda não ter passado, senão o evento se encerraria sozinho. `ENCERRADO` edita só nome, descrição, tipo e data e hora. O prazo e o fuso ficam travados, porque não há reabertura. `REALIZADO` e `CANCELADO` não editam nada. |
| 22 | Quais eventos podem ser excluídos? | Só `RASCUNHO`. Excluir um evento aberto apagaria, em cascata, inscrições e contribuições. Quem quer desfazer um evento aberto o cancela, e o cancelamento preserva o histórico. |
| 23 | Como o status muda, e quando um evento vira `REALIZADO`? | Pela rota `PATCH /api/events/{id}/status`, que respeita a máquina de estados. Um evento só vira `REALIZADO` na data e hora marcadas, lidas no fuso do evento, ou depois. O cancelamento é livre a qualquer momento. Ao abrir um rascunho, a chamada pode trazer um prazo novo. Nas outras mudanças, o prazo novo é recusado. Uma mudança inválida devolve 422 com as transições permitidas. |
| 24 | Como o prazo e o status ficam coerentes? | Dupla garantia, com uma regra só, escrita em Java (`Event.isDeadlineExpired`). **Regra síncrona:** toda operação confere o prazo na hora, então nada passa depois do prazo, mesmo que o status ainda diga `ABERTO`. **Sincronização:** ao ler, listar ou alterar um evento aberto com prazo vencido, ele vira `ENCERRADO` antes de responder. Um agendador a cada 60 segundos faz o mesmo sem ninguém acessar. Uma só garantia falharia: só o agendador deixa uma janela de até 60 segundos, e só a leitura deixa o banco desatualizado para relatórios. Escolhemos Java a SQL em lote, para a regra não existir em dois lugares e um nome de fuso estranho afetar só o evento dele. Fechar o evento grava só o status e a hora da atualização (`@DynamicUpdate`): uma edição que outra pessoa salvou no mesmo instante nunca é desfeita pela sincronização. |
| 25 | Como funcionam as categorias de um evento? | Cada categoria tem nome, unidade e meta. O nome é único no evento, sem diferenciar maiúsculas nem espaços nas pontas: o banco garante isso com um índice em `lower(name)`. A unidade fica travada enquanto a categoria tem itens, para um item não mudar de significado. Uma categoria só se exclui quando está sem itens. O cardápio só muda em `RASCUNHO` e `ABERTO`. Cada categoria mostra o total de itens, a quantidade planejada (soma dos itens) e a quantidade contribuída (soma das contribuições). Excluir a categoria, mudar a unidade e criar um item nela passam por um `SELECT ... FOR UPDATE` na linha da categoria, então duas pessoas nunca veem a operação da outra pela metade. |
| 26 | Como funcionam os itens do cardápio? | O nome do item é único dentro da categoria, sem diferenciar maiúsculas nem espaços: o banco garante com um índice em `(category_id, lower(name))`. O mesmo nome pode existir em categorias diferentes. O item não muda de categoria depois de criado, porque a unidade vem da categoria. Para mudar, exclua e recrie, o que só é possível sem contribuições. Cada item mostra a quantidade contribuída, a pendente (nunca negativa) e o percentual coberto, arredondado para baixo: só mostra 100% quando está completo. Alérgenos e características são listas opcionais. |
| 27 | Como uma pessoa participa de vários eventos, e quem altera o cadastro dela? | O cadastro da pessoa (nome, CPF e restrições) é único e vale para todos os eventos. A inscrição só liga a pessoa ao evento. Inscrever uma pessoa já cadastrada não altera nada: nome e restrições do corpo são ignorados, e a resposta mostra o que está guardado. Um CPF novo exige o nome. O cadastro muda em um só lugar, `PUT /api/collaborators/{id}`, que exige nome e restrições, para que um erro de inscrição ou uma omissão não apague dados. Remover a inscrição apaga as contribuições da pessoa naquele evento e mantém o cadastro. Se duas inscrições criam o mesmo CPF novo ao mesmo tempo, o banco espera a primeira (`INSERT ... ON CONFLICT DO NOTHING`) e a segunda reaproveita a pessoa, sem erro. |
| 28 | Quanto do CPF a API mostra? | O CPF sai mascarado (`***.533.447-**`): só os seis dígitos do meio, o bastante para separar homônimos. O CPF completo só entra na inscrição e na busca por CPF exato. Não existe listagem geral de colaboradores. Restrição alimentar é dado de saúde, e sem login (decisão 8) quem conhece um CPF consegue ver o nome e as restrições da pessoa. É um risco aceito. |
| 29 | Como funcionam as contribuições, e o que acontece com pedidos simultâneos? | `POST` cria a contribuição de uma pessoa para um item, e `PUT` altera a quantidade e a observação. Um segundo `POST` da mesma pessoa para o mesmo item devolve 409, e nada soma por acidente: um clique duplo não dobra o valor. A soma de todas as contribuições do item nunca passa da quantidade necessária (422, com o que ainda cabe). Só um evento aberto e dentro do prazo aceita incluir, alterar ou remover. **Concorrência:** antes de mudar uma contribuição, o sistema bloqueia a linha do item no banco (`SELECT ... FOR UPDATE`). Dois pedidos simultâneos não leem o mesmo saldo livre: o segundo espera o primeiro terminar e lê a soma correta. Editar a quantidade do item e excluí-lo passam pelo mesmo bloqueio. Teste: 20 pessoas disputam 1 unidade, e só uma contribuição entra. Sem o bloqueio, o mesmo teste aceita 7 contribuições. Testes de concorrência também cobrem a edição e a exclusão do item enquanto uma contribuição está em andamento. |
| 30 | Como o sistema mostra quem pode ficar sem comer? | `GET /api/events/{id}/dietary-analysis` cruza as restrições de cada participante com os alérgenos e as características de cada item, pela matriz da seção "Alérgenos, características e restrições". Mostra, por pessoa, quantos itens são seguros e quais não são, com o motivo. Mostra, por restrição, quantas pessoas a têm e quantos itens são compatíveis. Gera alertas: `CRITICAL` quando uma pessoa com restrição não tem nenhum item seguro, `WARNING` quando menos de 35% do cardápio é seguro para ela, e `INFO` ("Cardápio inclusivo") quando não há outro alerta, há pessoas com restrição e o cardápio tem itens. Com o cardápio vazio, não há alertas. O sistema só avisa e não bloqueia. Um item sem nenhuma marca conta como seguro, então o aviso vale tanto quanto o cadastro dos itens. |
| 31 | Qual é a funcionalidade livre, e como ela decide o que sugerir? | Sugestões do que levar: `GET /api/events/{id}/suggestions?participantId=X`. Veja a seção "Funcionalidade livre: sugestões do que levar". A prioridade vai de 0 a 100, em relação ao máximo que aquela pessoa poderia pontuar. Itens que a pessoa já contribui, itens completos e eventos que não aceitam contribuições ficam fora da lista. |
| 32 | O que entra no resumo para compartilhar? | `GET /api/events/{id}/summary` devolve um texto pronto para colar em um grupo (WhatsApp, Slack ou Teams): o nome, a data, o prazo, a situação, a cobertura, o que ainda falta (obrigatórios primeiro) e quem já vai levar o quê. O texto não leva CPF nem restrições alimentares: ele vai para um grupo, e restrição alimentar é dado de saúde. |
| 33 | Como o frontend recebe os rótulos, e como a API é documentada e liberada para o navegador? | `GET /api/domain/options` devolve todas as listas fechadas (tipos, status, unidades, alérgenos, características, restrições e níveis de prontidão) com o código e o texto de cada opção. Os rótulos ficam só no backend. O Swagger descreve cada rota. O CORS libera só a origem configurada em `APP_CORS_ALLOWED_ORIGINS`, nunca `*`. Veja a seção "Documentação da API e CORS". |
| 34 | O sistema já nasce com dados de exemplo? | Sim. Cinco eventos, um em cada status, com pessoas e CPFs fictícios, entram na primeira subida. As datas são relativas ao momento da migration, para os eventos abertos continuarem abertos por semanas. Os dados ficam em uma pasta separada e se desligam com `APP_DEMO_DATA=false`. Veja a seção "Dados de demonstração". |
| 35 | Como o frontend fala com a API em desenvolvimento? | Pelo proxy do Vite. O frontend chama `/api/...` no próprio endereço (`http://localhost:5173`), e o Vite repassa ao backend em `http://localhost:8080`, assim como `/swagger-ui` e `/v3/api-docs`. No Docker, o nginx faz o mesmo papel, então o código não muda entre os ambientes. O CORS (decisão 33) fica como alternativa. |
| 36 | Os scripts de instalação dos pacotes do npm ficam liberados? | Não. O npm 11 bloqueia esses scripts por padrão, e o projeto mantém o bloqueio: menos código de terceiros roda na máquina de quem instala. Os dois pacotes que pedem script não precisam dele. O `esbuild` traz o binário pronto em um pacote opcional por sistema (`@esbuild/darwin-arm64`, `@esbuild/linux-x64`), e o script só conferiria esse binário. O `fsevents` já traz o `fsevents.node` compilado. Uma instalação do zero (`npm ci`) passa no build, no lint e nos testes sem eles. |
| 37 | Como o frontend fala com a API e trata os erros? | Um só arquivo, `api/client.ts`, tem uma função por rota, e as telas não montam URLs. Todo erro vira um `ApiError` com a mensagem do backend (`detail`), o status e os erros por campo (`fieldErrors`), para o formulário marcar o campo certo. Uma falha de rede vira "Não foi possível falar com o servidor. Tente de novo." Os tipos de `types/api.ts` são escritos à mão, espelhando os DTOs: curtos e fáceis de ler. Uma conferência contra a API real comparou os campos de 17 tipos com as respostas e não achou diferença. |
| 38 | O frontend gera CPF para testes? | Só nos testes. O plano previa um botão "Gerar CPF" na tela, mas ele convidaria a cadastrar dados falsos. No produto ficam a máscara enquanto a pessoa digita e a conferência dos dígitos, com as mesmas regras do backend. |
| 39 | Qual é a identidade visual da interface? | "Lista da copa": a folha de inscrição colada na copa da empresa. Fundo branco frio, tinta verde-escura e azul de caneta para pessoas e ações. O elemento marcante é a fila de vagas: cada porção de um item é uma vaga que alguém preenche em azul, e as vazias mostram o que falta (acima de 20 unidades, cada vaga vale 10%). Verde, âmbar e vermelho têm um significado só: atendido, falta, e conflito ou perigo. Por isso um alérgeno aparece neutro, e só fica vermelho quando conflita com a restrição de alguém. Títulos em Bricolage Grotesque e texto em Atkinson Hyperlegible Next, uma fonte feita para leitura fácil, as duas por pacote. Foco do teclado visível e respeito a `prefers-reduced-motion`. |
| 40 | Como o frontend navega e avisa a pessoa? | Por rotas com `#`, sem biblioteca e sem configurar o servidor: `#/events` (lista), `#/events/new`, `#/events/{id}/edit` e `#/events/{id}/{aba}`, com as abas `summary`, `menu`, `participants` e `contributions` (padrão `summary`). Cada tela tem endereço próprio e o botão voltar funciona. Um endereço que nenhuma tela tem vai para a lista, e o endereço é substituído, para o voltar não regressar ao endereço quebrado. Como a troca de `#` não recarrega a página, a tela nova recebe o foco e o título da aba muda, e um leitor de tela percebe a navegação. Há um atalho "Ir para o conteúdo". Os avisos têm duas regras: um sucesso some em 4 segundos, e um erro fica até a pessoa fechar, porque ela precisa ler o motivo. O sucesso é anunciado com `role="status"`, e o erro com `role="alert"`. |
| 41 | Como a lista de eventos mostra a cobertura de cada um? | A lista carrega primeiro e mostra os eventos logo. Depois, uma chamada de cobertura por evento roda em paralelo, só para os eventos Aberto, Encerrado e Realizado. Rascunho e Cancelado não têm cobertura que importe. Cada linha trata a própria falha: se uma cobertura falha, aquela linha mostra "Cobertura indisponível" e o resto da lista segue normal. Os grupos seguem a pergunta "onde ainda dá para contribuir?": Abertos (prazo mais próximo primeiro), Rascunhos, Encerrados e realizados e Cancelados. A ordem compara o instante real de cada prazo, pois os eventos podem estar em fusos diferentes. **Custo assumido:** são N+1 chamadas (1 da lista e N de cobertura). Isso serve para dezenas de eventos. Com centenas, a correção é uma rota agregada no backend que devolva a cobertura junto da lista. |
| 42 | Como o formulário cria e edita eventos? | Uma tela só serve para criar e editar. A situação inicial só aparece ao criar, com duas escolhas: Rascunho (padrão) e Aberto, as únicas que o backend aceita para um evento novo. Depois, a mudança de situação passa pelos botões do evento, que seguem a máquina de estados. A data e o prazo usam o campo nativo de data e hora e valem no fuso do evento, sem conversão. O fuso vem preenchido e fica atrás do link "Alterar fuso", com 4 nomes claros: Fernando de Noronha (UTC-2), Brasília (UTC-3), Manaus (UTC-4) e Rio Branco (UTC-5). Se o fuso do navegador não está na lista (Recife, Cuiabá), o formulário usa o fuso da lista com o mesmo deslocamento, ou Brasília se nenhum serve. Um evento que já tem outro fuso (Lisboa) o mantém ao ser editado. A tela segue as regras do backend: em evento encerrado, prazo e fuso ficam desativados com o motivo, e evento realizado ou cancelado mostra um aviso no lugar do formulário. O formulário só confere os campos obrigatórios. As outras regras (prazo depois da data, prazo no passado) vêm da API, que é a única fonte delas. Cada erro da API aparece no campo certo e em um aviso que recebe o foco. |
| 43 | Como a tela do evento carrega os dados e deixa a pessoa mudar a situação? | A tela carrega o evento, os itens, os participantes e as contribuições, em paralelo. Essas quatro listas dão os contadores das abas e servem a várias abas. Cobertura, análise e resumo carregam só quando a aba Resumo abre, pois a maioria das visitas nunca os vê. Se uma das quatro chamadas falha, a tela inteira mostra o erro com "Tentar de novo". Um evento que não existe mostra "Evento não encontrado", sem botão de tentar de novo. As abas são links, pois cada uma tem endereço próprio, e a atual leva `aria-current`. A tela só mostra um botão para cada transição que a API permite em `allowedTransitions`, então a regra de situação continua só no backend e a interface não oferece um botão que vai falhar. Os nomes dos botões dizem a ação: "Abrir para contribuições", "Encerrar contribuições", "Marcar como realizado" e "Cancelar evento". Pedem confirmação só as ações sem volta: marcar como realizado, cancelar e excluir. Abrir um rascunho cujo prazo já passou abre uma janela que pede o novo prazo e o envia como `newChangeDeadline`. Um erro da API dentro de uma janela fica nela, e fora dela vira um aviso. "Excluir evento" só aparece em rascunho, a única situação que o backend deixa excluir, e "Editar evento" some em evento realizado ou cancelado. O nome do evento é o título principal da tela. |
| 44 | Como a aba Cardápio edita categorias e itens? | A aba tem duas seções: "Categorias e metas" e "Itens do cardápio", este agrupado por categoria. Cada item mostra se é obrigatório, a quantidade contribuída, os alérgenos, as características e a barra de cobertura. As categorias entram na carga da tela do evento, junto dos itens, e as mudanças recarregam tudo de uma vez com `reload()`. Assim, itens, contadores e categorias nunca ficam fora de sincronia. (Isso muda a decisão 43: as categorias deixam de ser da aba.) Criar e editar usam janelas, que abrem limpas a cada vez. Em erro, a janela fica aberta com o que foi digitado, e o erro da API aparece no campo certo e em um aviso. Escape fecha sem salvar. O item não muda de categoria, então a categoria só é uma escolha ao criar, e o botão "Adicionar item" de cada categoria já a escolhe. A tela repete a regra de que só Rascunho e Aberto aceitam mudanças no cardápio: nos outros casos mostra o cardápio sem botões e uma frase com o motivo. A tela também desativa, com a razão à vista, o que a API recusaria de qualquer jeito: excluir categoria com itens, excluir item com contribuições e trocar a unidade de uma categoria com itens. Os botões de editar e excluir levam o nome do item em `aria-label`, para um leitor de tela distinguir um do outro. As janelas desligam a validação nativa do navegador (`noValidate`), para a mensagem ser sempre a nossa. |
| 45 | Como a aba Participantes inscreve pessoas e mostra a segurança alimentar? | A inscrição tem duas etapas em uma janela. O CPF vem primeiro e sozinho, com máscara enquanto a pessoa digita. Só um CPF válido dispara a busca (`GET /api/collaborators?cpf=`), e o botão "Inscrever" fica desligado até a busca terminar. Se a API responde 404, a pessoa é nova e aparecem nome e restrições. Se responde 200, a janela mostra os dados guardados, sem campos para editar, com o aviso "Pessoa já cadastrada. Os dados do cadastro serão mantidos", e a inscrição envia só o CPF. Se a pessoa já está no evento, a janela avisa e não deixa inscrever. O resultado da busca guarda o CPF a que pertence, então um CPF que mudou nunca mostra a resposta do anterior. A pessoa só se edita em "Editar cadastro", uma janela própria que chama `PUT /api/collaborators/{id}`, avisa que a mudança vale em todos os eventos e nunca deixa mudar o CPF. Erros não editam a pessoa, como você pediu. Só um evento aberto inscreve e remove participantes: nos outros casos a aba mostra a lista e uma frase com o motivo, mas ainda deixa ver o cardápio e editar o cadastro. Remover pede confirmação e diz quantas contribuições saem junto, pois o banco as apaga em cascata. A análise de restrições (`GET /api/events/{id}/dietary-analysis`) entra na carga da tela, que passa a ter 6 chamadas, porque duas abas usam a mesma análise e `reload()` a mantém em dia. A aba mostra, por participante, "X de Y itens são seguros" e, para o evento, os alertas e o resumo por restrição. Os níveis de alerta (Crítico, Atenção, Aviso) não vêm das opções do domínio, então esses três textos moram no frontend. Na aba Cardápio, "Ver o cardápio como" marca cada item como "Seguro para Fulano" (verde) ou "Não é seguro: contém glúten" (vermelho), com o motivo em texto. A pessoa escolhida fica no estado da tela do evento: vale ao trocar de aba, e "Ver cardápio" de um participante já a escolhe. Ela some ao recarregar a página, e uma pessoa que saiu do evento conta como ninguém. |
| 46 | Quais partes repetidas viram componentes, e quais ficam de fora? | Só viram componente as partes que já se repetiam em 3 ou mais lugares (2, para um aviso). Contei antes de mexer: o bloco de salvar com `try/catch` aparecia em 8 lugares, o campo de nome em 5, o painel de erro com "Tentar de novo" em 3, os botões de editar e excluir em 3 e o aviso fixo em 2. Viraram `useAction` (chama a API, avisa o sucesso e guarda o erro e as marcas dos campos), `TextField`, `ErrorPanel`, `RowActions` (editar e excluir com nome acessível e, quando preciso, o motivo de o botão estar desligado) e `Notice`. Três peças já existiam, pois nasceram quando a repetição apareceu: `Field`, `CheckboxGroup` e `ConfirmDialog`, que serve de janela de formulário e de confirmação e faz o papel do `FormModal` do plano. Cada janela já tinha o próprio arquivo. Ficam de fora o que tem um uso só (telas, abas, painéis), a marcação das listas, que muda de tela para tela, e o `StatusActions`, que trata o erro de dois jeitos (na janela ou em aviso) e não cabe em `useAction` sem um caso especial. A regra de o teste ser a rede de segurança valeu: nenhum teste mudou, e os 266 passam antes e depois. O único efeito visível: o motivo de um item não poder ser excluído passa para a linha de baixo, como já era nas categorias. |
| 47 | Como a aba Contribuições registra, edita e remove, e como trata o bloqueio? | A aba mostra uma tabela com participante, item (com a observação embaixo), quantidade com a unidade, data e ações. A tabela rola dentro da própria caixa em tela estreita, e o resto da página não rola de lado. Registrar abre uma janela com participante, item, quantidade e observação. Só aparecem os itens que ainda têm espaço, com quanto falta, e a dica da quantidade diz quanto cabe ("Cabem mais 6 (porção)"). Na edição, a pessoa e o item ficam travados, como texto, e a dica diz o máximo, que é o que falta mais a quantidade que a pessoa já levava. A tela não confere o máximo: quem garante a regra é a API, que também responde um item completo e uma pessoa que já contribui com o item, e essas mensagens aparecem na janela, que continua aberta com o que foi digitado. O bloqueio usa o campo `acceptsContributions` da API, que já junta a situação e o prazo, então a tela não repete essa regra. Ela só explica o motivo: "Só um evento aberto aceita contribuições. Este evento está encerrado." (a situação vem das opções do domínio) ou "O prazo para alterações já passou.". Bloqueado, a tabela continua à vista, sem a coluna e os botões de ação. Quando o evento aceita contribuições mas não há como registrar uma nova, a aba diz o que falta: não há participantes, ou todos os itens já estão completos. Remover pede confirmação e diz quem deixa de levar quanto de quê. As sugestões do que levar ficam na aba Resumo, no passo 35. |
| 48 | O que a aba Resumo mostra e como ela carrega? | A aba tem três seções, e cada uma carrega e falha por conta própria, com "Tentar de novo": a cobertura, as sugestões e o texto para compartilhar. A cobertura (`GET /coverage`) e o texto (`GET /summary`) carregam quando a aba abre, e não na carga da tela, pois a maioria das visitas nunca as vê (decisão 43). **Situação do evento:** o percentual geral, o nível de prontidão (o texto vem das opções do domínio), os percentuais de obrigatórios e opcionais, e cada categoria com a barra, as quantidades e a meta. Há também "Como calculamos a cobertura" em um bloco que abre e fecha. O texto "faltam N para a meta" conta a partir do que foi contribuído, pois o `goalDeficit` da API é a meta menos a soma dos itens, e não o que falta contribuir. Sem itens, a API devolve 100% para obrigatórios (nada é necessário, nada falta), o que enganaria. A tela mostra "Ainda não há itens" e o link "Adicionar itens", e nenhum percentual. **Barra por item:** é uma barra para cada categoria, com um trecho por item e a largura igual à quantidade necessária. É por categoria porque as unidades de duas categorias não se somam (decisão do cálculo da cobertura). Verde é item completo, escuro é obrigatório que falta, e claro é opcional que falta. A legenda e o texto de cada trecho dizem o mesmo, para a cor não ser a única informação. **O que ainda falta levar:** sem participante escolhido, a seção lista o que falta, os obrigatórios primeiro, com os botões "Levar" desligados. Com um participante, ela pede as sugestões dele (`GET /suggestions`) e mostra o botão "Levar N (unidade)", a prioridade de 0 a 100, os motivos e um aviso quando o item não é seguro para a pessoa. O botão registra a quantidade sugerida e, em seguida, recarrega a cobertura, as sugestões e o texto, e avisa o resto da tela. Num evento que não aceita contribuições, a seção só explica o motivo, como na aba Contribuições. **Compartilhar:** o texto aparece em uma caixa que rola, e "Copiar texto" usa a área de transferência do navegador. Se o navegador recusar, um aviso pede para selecionar e copiar à mão. A API já deixa CPF e restrições fora do texto. As três cargas usam `useRemote`, que mostra "carregando" quando muda o que se pede e mantém os dados na tela quando só recarrega. |
| 49 | Como os textos da interface foram revisados? | Li todos os textos que a pessoa vê, procurando termos técnicos e botões que não dizem o que fazem. A interface já falava em português simples: "cobertura", "meta" e "prontidão" vêm do enunciado e têm explicação na tela, e os erros dizem o que aconteceu e o que fazer. Achei dois ajustes. O link "Documentação da API" virou "Documentação técnica", pois a sigla é jargão para quem só organiza o evento. E os botões das janelas, que diziam só "Salvar", "Excluir", "Remover" e "Inscrever", passaram a dizer o quê: "Salvar categoria", "Salvar item", "Salvar contribuição", "Salvar cadastro", "Salvar evento", "Excluir categoria", "Excluir item", "Remover participante", "Remover contribuição" e "Inscrever participante". Isso ajuda quem usa leitor de tela e quem abre a janela e perde o título de vista. Os botões "Voltar" e "Tentar de novo" já eram claros e ficaram como estavam. |
| 50 | Como o Docker sobe o sistema? | `docker compose up --build` sobe três serviços: o banco (PostgreSQL 16), o backend e o frontend. A interface abre em `http://localhost:3000`, e a documentação, em `http://localhost:3000/swagger-ui.html`. Só o frontend publica uma porta. O banco e o backend ficam na rede interna do Compose, o que evita conflito com um PostgreSQL ou um serviço que a pessoa já tenha na máquina. Para usar outra porta: `FRONTEND_PORT=3001 docker compose up --build`. O backend espera o banco ficar saudável (`pg_isready`), e o frontend espera o backend ficar saudável, então a interface nunca abre antes da API. Os dados de exemplo entram por padrão (`APP_DEMO_DATA=true`). O nginx serve os arquivos da interface e repassa `/api/`, `/swagger-ui` e `/v3/api-docs` ao backend, então o navegador fala com um endereço só e o código é o mesmo em todos os ambientes. Os dois `Dockerfile` têm dois estágios: o primeiro compila, e o segundo carrega só o resultado (o `.jar` com o JRE, e os arquivos prontos com o nginx). O backend roda sem ser administrador. O build da imagem não roda os testes, pois eles usam o Testcontainers, que precisa do Docker, e rodam fora da imagem. A senha do banco tem o valor de desenvolvimento `sulwork`. Em outro ambiente, troque-a pelas variáveis `DB_USERNAME` e `DB_PASSWORD`, no arquivo `.env`, que o Git ignora. O plano previa a variável `DB_DRIVER`, mas o sistema só usa PostgreSQL e não tem essa variável. Verificado: o log do backend mostra PostgreSQL 16 e as 2 migrations, e a interface abre com os dados de exemplo. |
| 51 | Como o sistema foi testado no ambiente real, e o que esse teste achou? | O script `scripts/smoke-docker.sh` testa o sistema no Docker Compose, pelo mesmo endereço do navegador (nginx na frente), com o cabeçalho `Origin` que o navegador manda. Ele confere duas coisas que os testes de integração não alcançam: uma escrita (POST, PATCH) que passa pelo proxy e pelo CORS, e 10 pedidos ao mesmo tempo para um item de 1 unidade, que devem dar 1 resposta 201 e 9 respostas 422. Rodei o script 6 vezes, e o resultado foi o mesmo em todas. Também percorri a interface em um banco vazio: criei um evento, categoria, item e participantes, abri o evento, registrei contribuições, tentei passar do limite, encerrei e cancelei. Esse teste achou um defeito que nenhum outro teste alcançava: toda escrita feita pela interface respondia 403 ("Invalid CORS request"). O nginx repassava `Host: $host`, que perde a porta. O navegador manda `Origin: http://localhost:3000` nas escritas, e o Spring comparou com `Host: localhost`, viu outra origem e recusou. As leituras (GET) não mandam `Origin`, e por isso a interface parecia boa até alguém salvar algo. A correção está na raiz: o nginx repassa `Host: $http_host`, que mantém a porta, e a interface passa a ser a mesma origem para o backend. O CORS não foi afrouxado: uma origem de fora (`Origin: http://evil.example`) continua recebendo 403. Antes da correção, o script falhava com 403 no primeiro passo, e depois passou. O script cria um evento de teste e o cancela no fim, e esse evento cancelado continua na lista. Por isso rode-o em um banco de teste e não no banco que você usa. |

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

## Documentação da API e CORS

**Swagger.** A documentação interativa fica em `/swagger-ui.html`, e o contrato em `/v3/api-docs`. Cada grupo de rotas tem um título e uma descrição, e cada rota tem um resumo, às vezes com uma descrição das regras. A descrição geral explica o formato dos erros, que não há login e que as datas são horários de parede no fuso do evento. Um teste percorre o contrato e falha se alguma rota ficar sem resumo ou sem grupo.

**Opções do sistema.** `GET /api/domain/options` devolve, para cada lista fechada, o código que a API usa e o texto que o usuário lê, por exemplo `{"code": "CELIAC_GLUTEN_FREE", "description": "Celíaco (sem glúten)"}`. O frontend não repete a lista e nunca fica desatualizado. Os cinco status do evento mantêm os nomes que o enunciado manda.

**CORS.** O navegador bloqueia chamadas de outra origem. Em desenvolvimento, o frontend roda em outra porta (`http://localhost:5173`). A API libera `/api/**` só para as origens da variável `APP_CORS_ALLOWED_ORIGINS` (uma lista separada por vírgulas, com `http://localhost:5173` como padrão). Ela nunca responde `*`, não aceita credenciais e só aceita os métodos e o cabeçalho que a API usa. No Docker, o nginx repassa `/api` na mesma origem do frontend, então o CORS nem entra em ação.

## Dados de demonstração

Quem avalia abre a aplicação e já vê o sistema funcionando, sem cadastrar nada. Na primeira subida, a migration `V1000__demo_data.sql` cria:

| Evento | Status | O que mostra |
| :--- | :--- | :--- |
| Almoço de fim de ano | `ABERTO` | O evento completo: quatro categorias (uma em mililitros), 14 itens obrigatórios e opcionais com alérgenos, 8 participantes com restrições variadas, contribuições parciais, uma meta com déficit e um alerta de segurança alimentar (Hugo, que é vegano, diabético e hipertenso, só tem 4 de 14 itens seguros). |
| Café da manhã de integração | `ABERTO` | Os obrigatórios garantidos e os opcionais ainda incompletos. |
| Churrasco de confraternização | `RASCUNHO` | Só o cardápio montado, sem participantes. |
| Lanche da tarde do time | `REALIZADO` | Um evento que já aconteceu, com tudo completo. |
| Happy hour de aniversário | `CANCELADO` | O quinto status. |

- **Datas relativas.** A migration calcula as datas a partir de "agora", no fuso do evento: os eventos abertos têm o prazo daqui a 15 e 45 dias. Com datas fixas, o exemplo envelheceria e todos os eventos apareceriam encerrados. Depois que o prazo passa, o encerramento automático acontece, o que também demonstra a regra.
- **Pessoas fictícias.** Os nomes são inventados, e os CPFs passam na conta dos dígitos verificadores mas não pertencem a ninguém.
- **Pasta separada, e uma chave para desligar.** Os dados ficam em `db/demo`, e não junto das migrations normais. A variável `APP_DEMO_DATA` (padrão `true`) liga e desliga a pasta. Os testes desligam, porque assumem um banco vazio. Para publicar sem dados de exemplo, defina `APP_DEMO_DATA=false` antes da primeira subida: desligar depois de aplicada faz o Flyway reclamar de uma migration que sumiu, e o jeito é recriar o banco.
- **Versão alta.** A migration tem a versão 1000, para nunca colidir com as migrations futuras do schema.
