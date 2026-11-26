# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Kotlin + Jetpack Compose (Material 3), MVVM, Firebase Auth (Google) + Cloud Firestore. Decided by the user; see CLAUDE.md.

## Users

Colegas da turma de PM46S (UTFPR), jovens, com celulares Android variados, competindo entre si em grupos.
O professor usa o app para avaliar se os requisitos do enunciado foram cumpridos.

## Product Purpose

Medir o nível de atividade física do usuário pelos sensores do celular e transformar isso em competição de grupo.
Sucesso: a pessoa liga o monitoramento, se mexe, vê o dia contar e vê a própria posição no ranking do grupo.

## Positioning

Desafio em grupo no estilo "cada dia ativo vale um ponto" (mecânica popularizada por apps de check-in de treino),
mas **sem foto nem declaração manual**: o dia conta porque o sensor mediu movimento, não porque a pessoa disse que treinou.

## Operating Context

- O monitoramento roda durante o dia todo em foreground service, com notificação persistente; o app é aberto poucas vezes por dia, para conferir o progresso e o ranking.
- Grupos: qualquer usuário cria e outros entram por código curto de convite. Um usuário pode participar de vários grupos.
- Cada grupo tem um desafio com período (início e fim). Cada dia em que o usuário atinge a meta de atividade conta como 1 dia.
- Ranking do grupo: ordenado por dias contados; desempate pela intensidade acumulada. Também existe ranking individual geral, entre todos os usuários do app.

## Capabilities and Constraints

- Login **exclusivamente** por rede social (Google; Facebook opcional). Sem e-mail e senha.
- Métricas: intensidade de movimento pelo acelerômetro (principal) e passos pelo `TYPE_STEP_COUNTER` (secundária, quando o aparelho tiver o sensor).
- Cada leitura vai ao servidor com nome do usuário, data e hora e o nível medido.
- Permissões em runtime: notificações (Android 13+), reconhecimento de atividade (Android 10+), foreground service do tipo `health` (Android 14+).
- Indefinido: o valor da meta diária, ou seja, quanta intensidade ou quantos passos fazem o dia contar. Pode ser fixa ou configurável pelo grupo.
- Indefinido: duração padrão do desafio.

## Brand Commitments

- Linguagem visual de app fitness social moderno, na linha de Strava e GymRats: arredondada, fluida, sem tema de futebol. Decisão do usuário em 2026-11-18, depois de rejeitar a direção "tabela do Brasileirão". Inspiração de gênero e de qualidade, sem copiar marca, cores nem layout de nenhum dos dois.
- Nome: Rodada, no sentido de "cada dia é uma rodada do desafio".

## Evidence on Hand

Nenhum dado real de usuários. Não inventar números, depoimentos nem usuários de exemplo apresentados como reais. Dados de exemplo em previews devem ser claramente fictícios.

## Product Principles

1. O sensor é a prova. O dia só conta com movimento medido, e a interface deve mostrar de onde veio o número.
2. Primeiro o grupo. A tela que importa é "como estou no meu grupo hoje", não estatística solitária.
3. Monitorar não pode virar trabalho: um toque para ligar e estado sempre visível, inclusive fora do app.
4. Transparência sobre bateria e permissões: explicar por que cada permissão existe antes de pedir.

## Accessibility & Inclusion

Atender Material 3 / Android: alvos de toque ≥ 48dp, contraste AA, suporte a fonte grande do sistema, TalkBack com descrição do estado do monitoramento.
