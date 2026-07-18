package com.escalachurch.app.domain.util

import java.io.IOException
import java.net.UnknownHostException

/**
 * Fase 11.9B Entrega 3 Bloco 6 - every save/delete failure in this app used to show
 * `it.message ?: "fallback"` directly in an ErrorBanner - `it.message` is the raw exception text,
 * which for a network/Postgrest failure is things like "Unable to resolve host" or a raw PostgREST
 * JSON error body. Bloco 6 explicitly forbids that ("mensagens técnicas do Supabase não devem ser
 * mostradas ao usuário"). This maps the *kind* of failure to a short, honest, non-technical
 * message instead - never the original exception text.
 */
fun friendlyErrorMessage(error: Throwable, fallback: String): String = when {
    error is UnknownHostException || error is IOException ->
        "Sem conexão com a internet. Verifique sua rede e tente novamente."
    error.message?.contains("timeout", ignoreCase = true) == true ->
        "A operação demorou demais para responder. Tente novamente."
    else -> fallback
}
