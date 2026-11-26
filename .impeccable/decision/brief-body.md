## Scope

App Android inteiro (Compose, Material 3). Mode: Operate. Primeira superfície: aba Hoje.
Audiência: turma PM46S, olhadas rápidas entre aulas; professor avalia requisitos.
Tarefa: iniciar a atividade, ver se o dia já contou, ver a posição no grupo.

## Direction contract

THESIS: Canon de app fitness social (padrão Strava/GymRats, escolha do usuário pela saída padrão): o dia vira conquista visível e o grupo vira placar com rosto; recusa tema de futebol e cantos duros.

OWN-WORLD: Fundo claro neutro e frio, cards brancos de 24dp, roxo elétrico #5B3DF5 como ação e destaque, laranja-chama #FF6B2C só para sequência de dias, verde #12B76A para dia conquistado. Figtree em pesos fortes para números, regular no texto. Avatares circulares com iniciais coloridas. Botões em pílula.

STORY: Abro, vejo quanto falta para conquistar o dia, toco em Iniciar atividade, vejo o número subir e meu avatar subir no grupo.

FIRST VIEWPORT: Cabeçalho com saudação, sequência 🔥 e avatar. Card principal: número grande de movimento, barra arredondada até a meta, passos, e botão pílula Iniciar atividade. Abaixo, a faixa de dias do desafio (bolinhas conquistadas). Depois, a prévia do ranking do grupo com o top 3 e você.

FORM: canon (escolha do usuário); seed 16ac70e2.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

## Signature interaction

Durante a atividade o card principal fica vivo: o número sobe com animação, a barra preenche com mola e as barras de "agora" respiram com o sensor. Ao bater a meta, a bolinha de hoje acende em verde e sua linha sobe no ranking com animação de mola.

## Open

Valor da meta diária e duração padrão do desafio (ver PRODUCT.md).
