# Como deixar o Rodada rodando de verdade

Sem configuração o app abre em **modo demonstração** (dados fictícios, nada vai para o servidor).
Seguindo estes passos ele passa a usar login real e o Firestore. Faça na ordem: o passo 3 depende do 2.

Dados que você vai precisar copiar:

| O quê | Valor |
|---|---|
| Pacote do app | `br.edu.utfpr.rodada` |
| Classe da Activity | `br.edu.utfpr.rodada.MainActivity` |
| SHA-1 do debug | `EE:D7:10:6D:D1:AB:D3:5F:50:15:60:B4:43:C4:DE:B1:09:22:56:D3` |
| Key hash do debug (Facebook) | `7tcQbdGr019QFWC0Q8TesQkiVtM=` |

O SHA-1 e o key hash valem só para este computador. Em outra máquina, rode `cd android && ./gradlew signingReport` e use o SHA-1 que aparecer.

---

## 1. Criar o projeto no Firebase

- [ ] Abra <https://console.firebase.google.com> e clique em **Criar projeto**. Nome: `Rodada` (qualquer um serve). O Google Analytics pode ficar desligado.
- [ ] No projeto, clique no ícone do **Android** para adicionar um app.
  - Nome do pacote: `br.edu.utfpr.rodada`
  - Apelido: `Rodada`
  - Certificado SHA-1: o da tabela acima
- [ ] **Ainda não baixe** o `google-services.json`. Clique em avançar até o fim (ele baixa de novo no passo 3).

## 2. Ligar o login com Google

- [ ] Menu **Criação › Authentication › Vamos começar**.
- [ ] Aba **Método de login › Google › Ativar**. Escolha o seu e-mail como e-mail de suporte e salve.

## 3. Baixar o google-services.json

Precisa ser baixado **depois** de ativar o Google, senão vem sem o client ID do login.

- [ ] Engrenagem ao lado de "Visão geral do projeto" › **Configurações do projeto** › seção "Seus apps" › **google-services.json**.
- [ ] Salve o arquivo em `android/app/google-services.json`.

## 4. Criar o Firestore (o "servidor remoto")

- [ ] Menu **Criação › Firestore Database › Criar banco de dados**.
- [ ] Local: `southamerica-east1 (São Paulo)`. Escolha **modo de produção**.
- [ ] Aba **Regras**: apague o conteúdo, cole o arquivo [firebase/firestore.rules](firebase/firestore.rules) e clique em **Publicar**.

Não precisa criar índice: as consultas do app usam um campo só.

## 5. Primeiro teste (só Google)

- [ ] Compile e instale:
  ```bash
  cd android
  ./gradlew installDebug
  ```
  Ou abra a pasta `android/` no Android Studio e clique em Run.
- [ ] No emulador, a imagem precisa ter Google Play e uma conta Google adicionada (Configurações › Contas). Num celular, basta a conta do aparelho.
- [ ] Abra o app. O aviso "Modo demonstração" deve ter sumido.
- [ ] Toque em **Continuar com Google**, depois **Iniciar atividade**, mexa o aparelho por 1 ou 2 minutos e pare.
- [ ] No console, em **Firestore › Dados**, devem aparecer `users`, `readings`, `daily` e `workouts`. Cada leitura tem nome, data/hora, intensidade e passos.
- [ ] Crie um grupo no app, entre com outra conta (outro aparelho ou outra conta Google) usando o código e confira o ranking.

Se o login falhar com "Developer console is not set up correctly" ou erro 10: o SHA-1 não está cadastrado ou o json foi baixado antes do passo 2. Refaça os passos 1 (SHA-1) e 3.

## 6. Login com Microsoft

Precisa de uma conta Microsoft (Outlook/Hotmail serve). A conta da UTFPR pode estar bloqueada para registrar apps; se der erro de permissão, use uma conta pessoal.

- [ ] No Firebase: **Authentication › Método de login › Adicionar novo provedor › Microsoft**. Deixe a janela aberta e copie o **URI de redirecionamento** (algo como `https://rodada-xxxx.firebaseapp.com/__/auth/handler`).
- [ ] Em outra aba, abra <https://portal.azure.com> › **Microsoft Entra ID › Registros de aplicativo › Novo registro**.
  - Nome: `Rodada`
  - Tipos de conta: **Contas em qualquer diretório organizacional e contas Microsoft pessoais**
  - URI de redirecionamento: tipo **Web**, cole o URI do Firebase
- [ ] Na página do app criado, copie o **ID do aplicativo (cliente)**.
- [ ] **Certificados e segredos › Novo segredo do cliente**. Copie a coluna **Valor**, porque ela só aparece uma vez.
- [ ] Volte ao Firebase, cole o ID e o segredo, ative e salve.
- [ ] No app, teste **Continuar com Microsoft**. Não precisa recompilar.

## 7. Login com Facebook

- [ ] Abra <https://developers.facebook.com/apps> › **Criar app**. Caso de uso: **Autenticar e solicitar dados dos usuários com o Login do Facebook**.
- [ ] Em **Configurações do app › Básico**, copie o **ID do app** e a **Chave secreta do app**.
- [ ] Em **Configurações do app › Avançado**, copie o **Token do cliente**.
- [ ] No Firebase: **Authentication › Método de login › Adicionar novo provedor › Facebook**. Cole o ID e a chave secreta, copie o **URI de redirecionamento** e salve.
- [ ] No Facebook: **Casos de uso › Login do Facebook › Configurações**. Em **URIs de redirecionamento do OAuth válidos**, cole o URI do Firebase e salve.
- [ ] Em **Configurações do app › Básico › Adicionar plataforma › Android**:
  - Nome do pacote: `br.edu.utfpr.rodada`
  - Nome da classe: `br.edu.utfpr.rodada.MainActivity`
  - Key hash: `7tcQbdGr019QFWC0Q8TesQkiVtM=`
- [ ] Em `android/local.properties`, acrescente:
  ```properties
  FACEBOOK_APP_ID=seu_id_do_app
  FACEBOOK_CLIENT_TOKEN=seu_token_do_cliente
  ```
- [ ] Recompile (`./gradlew installDebug`). O botão do Facebook só aparece depois disso.

O app do Facebook nasce em **modo de desenvolvimento**: só entram você e quem for adicionado em **Funções do app › Testadores**.
Para a turma e o professor entrarem, adicione-os como testadores ou publique o app (pede uma URL de política de privacidade).

## 8. Mapa com ruas (opcional)

Sem isto o percurso aparece sobre um fundo roxo, o que já funciona.

- [ ] Abra <https://console.cloud.google.com> › **APIs e serviços › Credenciais**, no projeto onde está a chave `MAPS_API_KEY` do `android/local.properties`.
- [ ] Edite a chave. Em **Restrições do aplicativo › Apps Android › Adicionar**, informe o pacote `br.edu.utfpr.rodada` e o SHA-1 da tabela.
- [ ] Confirme que a **Maps SDK for Android** está ativada no mesmo projeto.

## 9. Antes da apresentação

- [ ] Teste num celular de verdade: o emulador não tem contador de passos, e o GPS dele é simulado.
- [ ] Ande uns 10 minutos com o app ligado e veja se a meta diária (1.800 pontos ou 6.000 passos) faz sentido. Se ficar fácil ou difícil demais, ajuste em `Rules.kt`.
- [ ] Desative a otimização de bateria do app no celular (Configurações › Apps › Rodada › Bateria › Sem restrições) para o monitoramento não parar com a tela desligada.
- [ ] Deixe um grupo criado com algumas contas e alguns dias de dados, para o ranking não aparecer vazio.

Não coloque no Git o `google-services.json` nem o `local.properties`. Os dois já estão no `.gitignore`.
