# Escala Church

Aplicativo Android (Kotlin + Jetpack Compose) para gerenciamento de escalas, doxologia (ordem do culto), programações personalizadas e calendário da igreja — 100% local, sem login e sem servidor.

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Room (persistência local: escalas, doxologias, eventos personalizados)
- DataStore Preferences (configurações do app)
- Navigation Compose (bottom navigation)
- Sem framework de DI: `AppContainer` manual em `di/`

## Estrutura

```
app/src/main/java/com/escalachurch/app/
├── data/            # Room (entities, DAOs, database), DataStore, repositórios, seed inicial
├── domain/          # Modelos de domínio, feriados nacionais, regra de "próxima escala"
├── di/              # Container de dependências manual
└── ui/
    ├── components/  # ScaleCard, DoxologyCard, CustomEventCard, BottomNavBar, MonthCalendar, etc.
    ├── navigation/  # Destinos e NavGraph
    ├── screens/     # home, doxology, program, calendar, settings
    └── theme/       # Cores, tipografia, formas (reage às configurações em tempo real)
```

## Build

Requer Android SDK (compileSdk 34, minSdk 26) instalado localmente. Ao abrir no Android Studio, o Gradle Wrapper é resolvido automaticamente.

```
./gradlew assembleDebug
```

O APK gerado fica em `app/build/outputs/apk/debug/app-debug.apk`.

## Notas de arquitetura

- **Sem login/servidor**: todos os dados (escalas, doxologias, eventos, configurações) ficam no banco Room local e no DataStore do dispositivo.
- **Feriados nacionais**: calculados localmente (`domain/holidays/Holiday.kt`), incluindo o cálculo de Páscoa para Carnaval e Sexta-feira Santa. A estrutura já está pronta para substituir por uma API externa no futuro sem alterar os pontos de uso.
- **Fontes premium**: `AppFont` já distingue fontes gratuitas de "premium em breve"; nenhuma cobrança está implementada no MVP.
- **Próxima escala/doxologia**: `domain/util/NextItemResolver.kt` centraliza a lógica de encontrar o próximo item futuro por data/hora, reutilizada tanto em Início quanto em Doxologia.
