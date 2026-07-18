# Homologação — Fase 11.9B, Entrega 3 Final

Encerramento oficial da Fase 11.9B. Este documento é o pacote de entrega para quem
vai instalar e validar o app em dispositivo real — algo que não foi possível fazer
neste ambiente (sem emulador/adb).

## Identificação do build

| Campo | Valor |
|---|---|
| versionCode | 25 |
| versionName | `1.1.22-11.9b-final` |
| Commit | ver `BuildConfig.GIT_COMMIT` no APK instalado, ou o commit apontado pela branch `claude/escala-church-android-5uch4n` no momento da entrega |
| APK debug | `app/build/outputs/apk/debug/app-debug.apk` |
| APK release | `app/build/outputs/apk/release/app-release-unsigned.apk` (**unsigned** — o projeto não tem `signingConfig` de release; precisa ser assinado antes de instalar em um aparelho com `INSTALL_UNKNOWN_APPS` restrito, ou instalado via `adb install -r` que aceita unsigned em debug) |

`ChurchBootstrap`/logs de debug imprimem `BuildConfig.GIT_COMMIT`, `VERSION_CODE` e
`VERSION_NAME` na inicialização — use isso para confirmar que o APK instalado é
exatamente este build, não uma versão antiga reinstalada por engano.

## Testes automatizados

- `testDebugUnitTest`: suíte completa de testes de unidade das funções puras
  extraídas ao longo da Entrega 3 (resolvers de tema/qualidade visual/permissão,
  layout de galhos do Stella Core, fila do Spotlight, sanitizador de erro,
  contraste WCAG, benchmarks). Ver saída do build para a contagem exata no momento
  da entrega.
- `lintDebug`: sem erros; warnings restantes são pré-existentes e não relacionados
  a esta fase (dependências desatualizadas, `ObsoleteSdkInt`, ícone monocromático
  do launcher).
- `compileDebugKotlin` / `assembleDebug` / `assembleRelease`: todos verdes.

Não existe suíte de teste instrumentado (`androidTest`) rodando neste sandbox nem
teste de UI automatizado (Compose UI Test) executado - dependências já estão no
projeto (`ui-test-junit4`, `ui-test-manifest`) mas não foram exercitadas aqui.

## Limitações reais (sem emulador/adb neste ambiente)

Tudo abaixo foi verificado por leitura de código, cálculo (ex. contraste WCAG) ou
teste de unidade — nunca por execução visual real. Precisa de confirmação em
dispositivo antes de considerar a fase "visualmente" homologada:

1. **Aparência geral** — glow, sombras, blur (onde aplicável), tipografia
   renderizada, espaçamento — em nenhuma tela isso foi visto na tela real.
2. **Stella Core** — abrir/fechar, distribuição em duas ramificações, colisão de
   texto/cards saindo da viewport, feedback de toque, esmaecimento da barra
   inferior. A lógica está testada; a composição visual final, não.
3. **Parallax ambiental** — o drift lento em ChurchEntry/Home, os 3 perfis de
   qualidade (Automática/Reduzida/Completa) aplicados em runtime.
4. **Tema claro/escuro** — os ajustes de contraste (Bloco 17) corrigem valores
   calculados, mas "parecer bem" nos dois temas, lado a lado, não foi conferido.
5. **Acessibilidade** — TalkBack de verdade (ordem de foco, anúncios, o
   `clearAndSetSemantics` dos campos de data/hora), tamanho de fonte dinâmica do
   sistema, "remover animações" do Android.
6. **Menus administrativos** — `CelestialOverflowMenu`/overflow da Escala Geral,
   diálogo de exportação com `CelestialDropdown`, fluxo de preview premium.
7. **Announcement Spotlight** — placeholder de vídeo, diálogo fechando com Voltar.
8. **Performance real** — jank de UI, contagem de recomposição via Layout
   Inspector, tempo de frame do Canvas em dispositivo de gama baixa (o app tem
   perfil "Automática" que detecta RAM baixa via `ActivityManager.isLowRamDevice`,
   mas nunca foi testado em um aparelho assim).
9. **APK release não assinado** — precisa de uma `signingConfig` antes de qualquer
   distribuição real (mesmo interna).

## Checklist para homologação em dispositivo real

Marque cada item ao testar em aparelho físico (ou emulador) antes de aprovar a fase:

### Fluxo básico
- [ ] ChurchEntry: entrar com código de igreja válido
- [ ] Troca de igreja (se aplicável ao perfil de teste)
- [ ] Login administrador / Logout
- [ ] Plano PRO exibe recursos completos; plano FREE mostra os limites certos

### Telas principais
- [ ] Home: carrossel, parallax do scroll, Spotlight aparecendo quando há novidade
- [ ] Escala Geral: lista, criar/editar/duplicar/excluir (admin), exportar PDF/JPEG
- [ ] Doxologia: timeline, "ao vivo", criar/editar (admin)
- [ ] Anúncios: feed, vídeo inline (um por vez), placeholder de vídeo no Spotlight
- [ ] Calendário: dias marcados, toque no dia (verificar área de toque confortável)

### Stella Core
- [ ] Abre com toque simples, fecha com toque fora, fecha com Voltar
- [ ] Duplo toque leva para Início
- [ ] Testar em uma rota com 2, 3 e 4 ações reais (Home tem 4 hoje)
- [ ] Nenhum texto colide, nenhum card sai da tela em nenhuma orientação/tamanho
- [ ] Barra inferior esmaece com o menu aberto e volta ao normal ao fechar

### Configurações
- [ ] Alternar Tema (Claro/Escuro/Automático) sem reiniciar o app
- [ ] Alternar Animações, Parallax, Qualidade visual — aplica na hora
- [ ] Tamanho de fonte do app + fonte grande do sistema juntos (não deve quebrar layout)

### Acessibilidade
- [ ] TalkBack navega por todas as telas principais sem itens mudos
- [ ] Campos de data/hora anunciam o valor atual e "toque para selecionar"
- [ ] "Remover animações" do Android desliga parallax/drift

### Offline/Online
- [ ] Abrir o app sem internet: mostra dados em cache, nunca remove PRO por falha temporária
- [ ] Erros de rede mostram mensagem amigável, nunca texto técnico cru
- [ ] Reconectar: sincroniza e atualiza sem precisar reiniciar o app

### Build
- [ ] Instalar `app-debug.apk` limpo (desinstalar versão anterior antes)
- [ ] Conferir `VERSION_CODE`/`VERSION_NAME`/`GIT_COMMIT` nos logs de boot batem com este documento

---

**Fase 11.9B — Entrega 3 Final: encerrada.** Nenhuma Fase 12 foi iniciada.
