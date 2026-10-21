# ESCALA_CHURCH_IMPORT_V2 — contrato canônico

Fase 11.15, Etapa B/C. Representação JSON compartilhada (ou destinada a ser) entre Android e Web
para a Importação Inteligente de Escalas — substitui a ideia de "cinco strings por escala" por
atribuições individuais, uma por pessoa por função. **Implementado só no Android nesta fase**
(`app/src/main/java/com/escalachurch/app/domain/importer/ImportModelsV2.kt`); o site ainda não tem
tela de importação nenhuma (confirmado por auditoria — ver Fase 11.13/11.15 §A) e não interpreta
este JSON ainda.

`ESCALA_CHURCH_IMPORT_V1` (texto, `chave=valor` por linha) continua existindo e aceito — ver
`EscalaChurchImportParser.kt`. V1 nunca é substituído; `toCanonicalV2()` apenas o converte para
este modelo, sem perder nenhum participante.

## Exemplo completo

Entrada informal (não é o que o parser lê — é o que um humano, ou futuramente um LLM, descreveria):

> Domingo, 19h. Sonoplastia: João. Louvor: Ana, Pedro e Maria. Recepção: Carlos e Beatriz.

JSON canônico resultante (`CanonicalImportResult.toJson()`):

```json
{
  "contractVersion": "ESCALA_CHURCH_IMPORT_V2",
  "schedules": [
    {
      "localId": "a3f1...-uuid",
      "title": "Culto de domingo",
      "rawDate": "2026-11-15",
      "date": "2026-11-15",
      "dateAssumedYear": false,
      "startTime": "19:00",
      "endTime": null,
      "type": "Culto de domingo",
      "isSpecialEvent": false,
      "notes": "",
      "source": "manual_text_v1",
      "roles": [
        {
          "roleId": "r-sound-uuid",
          "rawRoleName": "Sonoplastia",
          "normalizedRoleName": "Sonoplastia",
          "position": 0,
          "participants": [
            { "personId": "p-joao-uuid", "rawName": "João", "resolvedName": "João Batista Santos", "status": "confirmed", "candidateNames": [] }
          ]
        },
        {
          "roleId": "r-louvor-uuid",
          "rawRoleName": "Louvor",
          "normalizedRoleName": "Regência",
          "position": 1,
          "participants": [
            { "personId": null, "rawName": "Ana", "resolvedName": null, "status": "unresolved", "candidateNames": [] },
            { "personId": null, "rawName": "Pedro", "resolvedName": "Pedro Souza", "status": "suggested", "candidateNames": ["Pedro Souza"] },
            { "personId": null, "rawName": "Maria", "resolvedName": null, "status": "conflict", "candidateNames": ["Maria Silva", "Maria Santos"] }
          ]
        },
        {
          "roleId": "r-recepcao-uuid",
          "rawRoleName": "Recepção",
          "normalizedRoleName": "Recepção",
          "position": 2,
          "participants": [
            { "personId": "p-carlos-uuid", "rawName": "Carlos", "resolvedName": "Carlos Mendes", "status": "confirmed", "candidateNames": [] },
            { "personId": null, "rawName": "Beatriz", "resolvedName": null, "status": "unresolved", "candidateNames": [] }
          ]
        }
      ],
      "warnings": [],
      "errors": [],
      "pendingFields": []
    }
  ],
  "globalWarnings": [],
  "globalErrors": []
}
```

Seis atribuições individuais (1 sonoplastia + 3 louvor + 2 recepção) — nenhuma colapsada em string.

## Campos

### `CanonicalImportResult`
| Campo | Obrigatório | Descrição |
|---|---|---|
| `contractVersion` | sim | sempre `"ESCALA_CHURCH_IMPORT_V2"`; outro valor é rejeitado explicitamente (`isSupportedVersion = false`) |
| `schedules` | não (pode ser vazio) | lista de `CanonicalSchedule` |
| `globalWarnings` / `globalErrors` | não | hoje atribuídos ao documento todo (V1 não tem granularidade por bloco — ver limitação abaixo) |

### `CanonicalSchedule`
| Campo | Obrigatório p/ análise | Obrigatório p/ publicação | Descrição |
|---|---|---|---|
| `localId` | gerado automaticamente | — | id só-cliente, nunca um `scales.id` real |
| `title`, `type`, `notes`, `isSpecialEvent` | não | não | — |
| `rawDate` | não | — | texto exato da data, mesmo quando não interpretável |
| `date` | não | **sim** (via `pendingFields`) | ISO `yyyy-MM-dd` ou `null` — nunca um valor adivinhado |
| `dateAssumedYear` | — | — | `true` quando o ano foi assumido (`DD/MM` sem ano) — exige confirmação antes de publicar |
| `startTime` | não | **sim** (via `pendingFields`) | `"HH:MM"` ou `null` — nunca defaultado |
| `source` | gerado automaticamente | — | `manual_text_v1` \| `manual_json_v2` \| `llm_assisted` (reservado, não implementado) \| `reuse` |
| `roles` | não | — | lista de `CanonicalRole` |
| `warnings` / `errors` | não | `errors` bloqueia | informativo vs. bloqueante — nunca confundidos |
| `pendingFields` | calculado | **sim** | nomes de campo (`"date"`, `"startTime"`) ou `"role:<nome>"` ainda pendentes |

`isReadyToPublish` = `errors.isEmpty() && pendingFields.isEmpty()`.

### `CanonicalRole`
| Campo | Descrição |
|---|---|
| `roleId` | `organization_roles.id` real, só depois de `RoleReconciler` — `null` = pendente, nunca inventado |
| `rawRoleName` | texto exato da fonte ("Som") |
| `normalizedRoleName` | nome canônico (`RoleAliasResolver`) ou o próprio texto bruto quando não há alias conhecido |
| `position` | ordem de exibição/gravação, preservada da fonte |
| `participants` | lista de `CanonicalParticipant` — **nunca** uma string única |

### `CanonicalParticipant`
| Campo | Descrição |
|---|---|
| `personId` | `organization_people.id` real — só quando `status = confirmed`, nunca inventado |
| `rawName` | texto exato digitado, sempre preservado mesmo após resolvido |
| `resolvedName` | nome da pessoa conhecida, quando `confirmed`/`suggested` |
| `status` | `confirmed` \| `suggested` \| `conflict` \| `unresolved` \| `temporary_name` |
| `candidateNames` | populado em `suggested` (1 nome) e `conflict` (2+ nomes) |

`status` nunca é escolhido automaticamente para `confirmed` a não ser por correspondência exata
(case/acento-insensível) — qualquer outra correspondência é `suggested`/`conflict`, decidida pelo
administrador.

## Regras de validação (Etapa B)

- Nenhum campo é obrigatório para a escala **aparecer** na prévia/ser analisada — só para ser
  **publicada** (`isReadyToPublish`).
- Ausência de informação (`date = null`) nunca é confundida com valor confirmado.
- Nenhum horário/data desconhecido é preenchido com um valor arbitrário — vira `pendingFields`.
- Chaves JSON desconhecidas são ignoradas (`ignoreUnknownKeys = true`), não tratadas como erro —
  compatibilidade futura sem quebrar um payload mais antigo.
- A ordem de `roles`/`participants` é sempre a da fonte — nunca reordenada alfabeticamente.

## Limitação conhecida

`globalWarnings`/`globalErrors` ficam no nível do documento, não por escala — porque
`EscalaChurchImportParser` (V1) já reporta assim, sem marcar a que bloco `[ESCALA]` um erro
pertence. Atribuir isso a uma escala específica exigiria reescrever o parser V1 para rastrear
limites de bloco nos avisos/erros, o que não foi feito nesta fase (fora do escopo de "não
reescrever o parser V1", Etapa C).

## O que NÃO está implementado ainda (Etapa 11.15-B, não autorizada)

- Gravação de `scale_assignments` a partir do modelo V2 — Android ainda só grava os 5 campos
  legados (`ScaleRepository.save()`/`ScaleImportViewModel`, inalterados nesta fase).
- RPC transacional escala+atribuições.
- Interface de prévia V2 (cards editáveis por participante) — a tela atual ainda é a V1.
- Qualquer integração com a API da OpenAI.
- Qualquer leitura/gravação por parte do site.
