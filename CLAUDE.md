# Projeto 2 — App de nível de atividade física (PM46S / UTFPR)

App Android que mede o nível de atividade física do usuário durante o dia do desafio,
envia as leituras a um servidor remoto e mostra ranking individual e por grupo.

## Estrutura da pasta

- `android/`: o projeto Gradle (abrir esta pasta no Android Studio). Tudo que compila fica aqui.
- `firebase/firestore.rules`: regras de segurança do Firestore (colar no console).
- [COMO-RODAR.md](COMO-RODAR.md): passo a passo para ligar Firebase, Microsoft, Facebook e Maps.
- `referencias/`: repositórios do professor (fora do Git).
- `PRODUCT.md`, `.impeccable/`: design (contrato e capturas de revisão).
- `CLAUDE.md`, `.claude/`, `.agents/`, `skills-lock.json`: harness de IA. Ficam na raiz porque o Claude Code os procura ali.

## Requisitos (enunciado)

1. Acesso **somente** por login com rede social (Google, Facebook e Microsoft).
2. Após login: opção de **iniciar monitoramento** — leitura de sensor(es) que identifica o
   nível de movimento do aparelho e, por consequência, da pessoa.
3. Cada leitura vai para **servidor remoto** com nome do usuário e data/hora.
4. **Ranking**: listagem individual do nível de atividade de quem usa o app.
5. **Grupos** para classificação (ranking por grupo).

## Stack

- Kotlin + **Jetpack Compose** (Material 3). Nada de XML/View, nem `AppCompatActivity` nas telas.
- MVVM: `ViewModel` + `StateFlow<UiState>` + Repository com `withContext(Dispatchers.IO)`.
- Firebase Auth (Google via Credential Manager) + Cloud Firestore como servidor remoto.
- Gradle Kotlin DSL com version catalog (`gradle/libs.versions.toml`), igual às referências.
- Pacote base: `br.edu.utfpr.<nomeapp>` (padrão dos repositórios do professor).

## Referências — usar como modelo

Clones em [referencias/](referencias/) (somente leitura, não compilar a partir dali).
Código do professor é didático: copiar a **API usada**, não a estrutura (tudo em `MainActivity`).

| Funcionalidade | Modelo | O que aproveitar |
|---|---|---|
| Login Google | `referencias/Login-PM46S-2025-1` | `CredentialManager` + `GetGoogleIdOption(setServerClientId(default_web_client_id))` + `GoogleAuthProvider.getCredential` + `signInWithCredential`. Ref é View-based: portar para Compose (`LocalContext` + `rememberCoroutineScope`). |
| Firestore | `referencias/UsandoFirebaseCF-PM46S-2026-2` | `Firebase.firestore`, `collection().document().set/get/delete`, plugin `google-services`, versões do catálogo (Compose BOM, AGP, Firestore). |
| Sensores | `referencias/UsandoSensores-Pm46S-2025-1` | `SensorManager`, `TYPE_ACCELEROMETER`, `SensorEventListener`. Em Compose: registrar em `DisposableEffect` ou num Service, nunca direto no corpo do composable. |
| GPS | `referencias/UsandoGPS-pm46s-2025-1` | `LocationManager.requestLocationUpdates`. Permissão em runtime como no turismo-offline. |
| ViewModel em Compose | `referencias/Calcula-IMC-Compose-PM46S` | `viewModel()` + `mutableStateOf` com `private set`. |
| Câmera / SQLite | `referencias/UsandoCamera-pm46s-2026-2` | `TakePicturePreview`, `SQLiteOpenHelper` (cache local de leituras, se preciso). |
| Layout Compose básico | `referencias/MyCard-Compose-PM46S-2026-1` | Scaffold + tema. |
| Arquitetura completa | `D:\UTFPR\mnoile\turismo-offline` | MVVM com `UiState` data class, `ViewModelFactory`, Repository, `SQLiteOpenHelper`, permissões com `rememberLauncherForActivityResult`, `local.properties` → `BuildConfig`, tema/fontes em `ui/theme`. |

## App: Rodada

Cada grupo é um desafio com período. Cada dia que bate a meta é um dia conquistado e vale 1 ponto.
O ranking do grupo é ordenado por dias conquistados, com desempate pela intensidade acumulada.

### Convenções

- **Código em inglês** (classes, funções, variáveis, comentários e campos do Firestore). Os textos da interface ficam em pt-BR.
- **Sem testes unitários**: é trabalho de faculdade. A verificação é feita rodando no emulador.

### Decisões tomadas

- **Métricas, as duas:** intensidade do acelerômetro como métrica principal e passos (`TYPE_STEP_COUNTER`) como secundária.
  O dia conta se bater **qualquer uma** das metas. As regras estão em [Rules.kt](android/app/src/main/java/br/edu/utfpr/rodada/data/Rules.kt).
  A conversão do sensor em nível está em [ActivityMeter.kt](android/app/src/main/java/br/edu/utfpr/rodada/sensor/ActivityMeter.kt):
  a cada segundo o nível vai de 0 a 10 e vale nível/6 pontos. A meta padrão é 1.800 pontos (≈ 30 min de caminhada) ou 6.000 passos.
- **Foreground service** do tipo `health` ([MonitorService.kt](android/app/src/main/java/br/edu/utfpr/rodada/sensor/MonitorService.kt)),
  com wake lock parcial. Envia uma leitura por minuto.
- **Firestore:**
  - `readings`: uma por minuto, com uid, name, timestamp, day, intensity e steps.
  - `daily/{uid_day}`: agregado do dia, somado com `increment`.
  - `users/{uid}`: totalIntensity e totalSteps.
  - `groups/{id}`: code, startDate, durationDays, intensityGoal, stepsGoal, `members` (lista de uid), `memberNames` (uid → nome) e ownerUid.

  Não há buffer em SQLite: o cache offline do Firestore guarda os envios até ter rede.
- **Atividades (treinos):** cada "Iniciar → Parar" vira um `Workout` (`workouts/{id}`: tempo, pontos, passos, distância e o percurso do GPS).
  Ao parar, abre o resumo com o percurso desenhado em animação ([RouteMap.kt](android/app/src/main/java/br/edu/utfpr/rodada/ui/components/RouteMap.kt)).
  Sem chave do Maps, ou se os blocos do mapa não carregam, o percurso é desenhado num canvas.
  A foto (câmera ou galeria) fica no aparelho, em SQLite como BLOB ([PhotoStore.kt](android/app/src/main/java/br/edu/utfpr/rodada/data/PhotoStore.kt)).
  O histórico fica na aba Atividades.
- **Grupos sem fim:** `durationDays = null` faz o desafio não acabar (opção "Sem fim" ao criar o grupo).
- **Modo demonstração:** sem `android/app/google-services.json` o plugin do Google não é aplicado e o app usa
  `DemoAuthRepository`/`DemoChallengeRepository`, com dados fictícios e um aviso de "Modo demonstração".
- **Design:** app fitness social no estilo Strava/GymRats (impeccable, caminho canon), com roxo #5B3DF5,
  laranja para a sequência de dias e fonte Figtree.
  Marca: seta circular afinada girando em volta de um relógio ([RodadaMark.kt](android/app/src/main/java/br/edu/utfpr/rodada/ui/components/RodadaMark.kt)),
  que também faz o "o" de Rodada no login e gira enquanto o login está aberto. Os ícones (launcher, monocromático, notificação)
  saem da mesma geometria: `python android/tools/generate_mark.py android/app/src/main/res`. O contrato está em `.impeccable/surfaces/` e o produto em [PRODUCT.md](PRODUCT.md).

### Ainda em aberto

- A meta diária é escolhida ao criar o grupo (Leve, Padrão ou Puxada). Falta calibrar em aparelho real.
- `whereIn` do Firestore limita a tabela a 30 membros por grupo.

## Ligar o Firebase

1. Criar o projeto no console do Firebase e registrar o app Android com `br.edu.utfpr.rodada`.
2. Cadastrar o SHA-1 do debug (`./gradlew signingReport`). Sem ele, o login Google falha.
3. Ativar o Authentication com o provedor Google e criar o Cloud Firestore.
4. Baixar o `google-services.json` para `android/app/` e recompilar. O modo demonstração desliga sozinho.
5. **Microsoft:** registrar um app no Azure (Microsoft Entra › Registros de aplicativo, contas pessoais e corporativas),
   com o URI de redirecionamento que o Firebase mostra ao ativar o provedor Microsoft. Copiar o ID e um segredo para o Firebase.
6. **Facebook:** criar um app em developers.facebook.com com o produto Login do Facebook. Ativar o provedor Facebook no Firebase
   com o ID e a chave secreta, e colar o URI de redirecionamento do Firebase no Facebook. Em `android/local.properties`:
   `FACEBOOK_APP_ID` e `FACEBOOK_CLIENT_TOKEN`. No painel do Facebook, cadastrar o pacote, `br.edu.utfpr.rodada.MainActivity`
   e o key hash do debug (`7tcQbdGr019QFWC0Q8TesQkiVtM=`). Sem essas chaves o botão do Facebook não aparece.

Login ([FirebaseAuthRepository.kt](android/app/src/main/java/br/edu/utfpr/rodada/data/FirebaseAuthRepository.kt)):
Google via Credential Manager, Facebook via SDK `facebook-login` + `FacebookAuthProvider`, Microsoft via
`OAuthProvider("microsoft.com")` + `startActivityForSignInWithProvider`. O SDK do Facebook roda sem auto-init e sem eventos.

## Comandos

```bash
cd android
./gradlew assembleDebug          # APK debug
```
No emulador:
- `adb emu sensor set acceleration x:y:z` simula movimento.
- Para simular GPS, `adb emu geo fix` não funciona neste AVD. Use o provedor de teste:
  `adb shell appops set com.android.shell android:mock_location allow`,
  depois `adb shell cmd location providers add-test-provider gps` e
  `adb shell cmd location providers set-test-provider-location gps --location LAT,LNG`.

Google Maps: `MAPS_API_KEY` fica em `android/local.properties`. A chave precisa autorizar o pacote `br.edu.utfpr.rodada`
com o SHA-1 de debug (`./gradlew signingReport`).

## Segredos

- `google-services.json` vem do console Firebase; SHA-1 do debug keystore precisa estar
  cadastrado lá para o login Google funcionar.
- Chaves de API em `local.properties` (fora do Git), lidas via `BuildConfig`, como no turismo-offline.

## Harness de IA

Skills de design em `.agents/skills/` (junctions em `.claude/skills/`), trazidos da oficina.
Para este app usar: `impeccable` (ver `reference/android.md`) e `imagegen-frontend-mobile`.
Os skills voltados para web (gpt-taste, image-to-code, imagegen-frontend-web, stitch) não se aplicam.
