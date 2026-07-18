package com.escalachurch.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

/** Fase 11.9B Entrega 3 Bloco 6 - the exact bug this targets: save/delete failures used to show
 *  `it.message` (raw exception text) directly to the user. friendlyErrorMessage() must never let
 *  that leak through, regardless of what the real exception says. */
class ErrorMessagesTest {

    @Test
    fun networkFailure_neverLeaksRawExceptionText() {
        val raw = UnknownHostException("pyiepvzmrhqgepvhxnwz.supabase.co")
        val result = friendlyErrorMessage(raw, "fallback")
        assertEquals("Sem conexão com a internet. Verifique sua rede e tente novamente.", result)
    }

    @Test
    fun genericIOException_alsoMapsToOfflineMessage() {
        val result = friendlyErrorMessage(IOException("Connection reset"), "fallback")
        assertEquals("Sem conexão com a internet. Verifique sua rede e tente novamente.", result)
    }

    @Test
    fun timeout_mapsToTimeoutMessage() {
        val result = friendlyErrorMessage(RuntimeException("Request timeout after 30s"), "fallback")
        assertEquals("A operação demorou demais para responder. Tente novamente.", result)
    }

    @Test
    fun unknownError_usesFallback_neverRawMessage() {
        val raw = RuntimeException("PGRST205: relation \"public.scales\" access denied")
        val result = friendlyErrorMessage(raw, "Falha ao salvar a escala.")
        assertEquals("Falha ao salvar a escala.", result)
    }
}
