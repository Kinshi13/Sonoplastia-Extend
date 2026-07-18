# Changelog — Fase 11.9B, Entrega 3 Final

Registro por versão do que cada Bloco da Entrega 3 Final entregou. Cada linha do app
carrega `BuildConfig.GIT_COMMIT` (SHA curto) para conferência exata de qual commit
gerou um APK instalado — ver `ChurchBootstrap`/logs de debug.

## 1.1.21-11.9b-release-candidate (versionCode 24) — Bloco 24

- Empacotamento final desta Entrega: `testDebugUnitTest`, `compileDebugKotlin`,
  `lintDebug`, `assembleDebug` e `assembleRelease` executados em sequência.
- Este arquivo (`CHANGELOG.md`) criado, documentando os Blocos 5–24.
- Não publicado. Não gerado para a Play Store.

## 1.1.20-11.9b-spotlight-validated (versionCode 23) — Bloco 23

Validação do Announcement Spotlight. Corrigido: anúncio de vídeo não mostrava
nenhuma pista visual no espaço de mídia (placeholder com ícone de play adicionado).
Confirmado sem alteração: elegibilidade Novo/Alterado/Já visto/Agora não, isolamento
por igreja, persistência do "visto" via DataStore por igreja.

## 1.1.19-11.9b-stellacore-validated (versionCode 22) — Bloco 22

Validação do Stella Core. Adicionados 5 testes unitários para
`StellaCorePermissionResolver` (Admin/Público), até então sem cobertura. Confirmado
por diff que a implementação do Core não muda desde o Bloco 9.

## 1.1.18-11.9b-regression (versionCode 21) — Bloco 21

Regressão completa: build limpo do zero, 51 testes / 0 falhas, lint sem erros.
Auditoria de diff confirma que ActiveChurchManager, login/logout, PRO/FREE e
offline/cache/sincronização não foram tocados em nenhum bloco desta Entrega.

## 1.1.17-11.9b-perf (versionCode 20) — Bloco 19

Auditoria de desempenho. Corrigido: vídeo de Anúncios não pausava ao ir para
background; lista de Planos recalculada a cada recomposição sem `remember`/`key`.
Confirmado: nenhum outro `LazyColumn` sem key, nenhum `.blur()`, nenhum loop
descontrolado. Benchmarks JVM das funções puras mais "quentes" (todas < 1µs/chamada).

## 1.1.16-11.9b-a11y (versionCode 19) — Bloco 18

Auditoria de acessibilidade. Corrigido: `DatePickerField`/`TimePickerField` com
semântica ambígua para TalkBack; área de toque do dia no calendário abaixo de 48dp;
`CelestialSyncIndicator` ignorando "Animações" desligado. Confirmado: todo ícone
acionável já tem rótulo; fonte dinâmica já respeita a escala do sistema.

## 1.1.15-11.9b-theme-contrast (versionCode 18) — Bloco 17

Auditoria clara/escura. Corrigido: contraste real abaixo do mínimo WCAG nos glifos de
constelação por dia (Farol/Aurora/dourado da Coroa) no tema claro; `errorContainer`/
`tertiary` do tema usando cores genéricas do Material3 em vez da paleta Constellation.

## 1.1.14-11.9b-admin-menus (versionCode 17) — Bloco 16

`CelestialOverflowMenu`, `CelestialContextMenu` e `CelestialDropdown` (novos
componentes reutilizáveis). Escala Geral ganhou "Exportar" no menu administrativo
(reaproveitando `FeatureKey.EXPORT`/`ScaleExporter` já existentes).

## 1.1.13-11.9b-admin-celestial (versionCode 16) — Bloco 15

`CelestialAdminCard` (novo): card administrativo com Editar sempre visível e
Duplicar/Exportar/Excluir atrás de menu overflow — Excluir nunca mais aparece como
ícone solto. Aplicado à Escala Geral; `SettingsSection` migrada para `CelestialFrame`.

## 1.1.12-11.9b-visual-settings (versionCode 15) — Bloco 14

`VisualQualityResolver`/`resolveEffectiveVisualSettings`: camada única de resolução
combinando qualidade visual, parallax, animações, "remover animações" do sistema,
tier de RAM e economia de bateria. Interruptor dedicado de Parallax em Configurações.

## 1.1.11-11.9b-ambient-parallax (versionCode 14) — Bloco 12

Parallax ambiental (drift lento e contínuo) nas camadas decorativas de
`CelestialBackground`/`ParallaxStarfield` — nunca no conteúdo funcional. Controles de
"Efeitos visuais" e "Qualidade visual" em Configurações.

## 1.1.10-11.9b-bottombar-dim (versionCode 13) — Bloco 11

Ícones da barra inferior esmaecem enquanto o Stella Core está aberto, reforçando o
foco visual no menu — continuam clicáveis (trocar de aba ainda fecha o Core).

## 1.1.9-11.9b-stellacore-cards (versionCode 12) — Bloco 9

Subtítulo opcional nos mini cards do Stella Core (oculto em telas estreitas) e
feedback de toque real (escala ao pressionar) nos nodes do menu.

## 1.1.8-11.9b-stellacore-branches (versionCode 11) — Bloco 8

Layout de duas ramificações ascendentes para o menu do Stella Core
(`assignToBranches`, testado para 0–9 ações) com nó "Mais" para além de 6 ações.

## 1.1.7-11.9b-stellacore (versionCode 10) — Bloco 7

Botão Voltar fecha o Stella Core em vez de sair da tela (`BackHandler` nas duas
variantes: barra inferior e flutuante).

## 1.1.6-11.9b-error-states (versionCode 9) — Bloco 6

`CelestialErrorState`/`CelestialOfflineState`/`CelestialPermissionState`. Corrigido:
erros técnicos crus (ex. mensagens do Supabase) vazando direto para o usuário —
`friendlyErrorMessage()` sanitiza antes de exibir. Retry real via `flatMapLatest`.

## 1.1.5-11.9b-loading-states (versionCode 8) — Bloco 5

`CelestialLoadingState`/`CelestialSkeletonCard`/`CelestialSyncIndicator`. Corrigido:
tela de "nenhum item" piscando antes dos dados reais chegarem (isLoading existia mas
nenhuma tela lia).

---

Blocos sem entrada própria (10, 13, 20) foram auditorias que confirmaram requisitos já
satisfeitos por um bloco anterior, sem gerar novo código — documentados nas respectivas
mensagens de commit, sem bump de versão.
