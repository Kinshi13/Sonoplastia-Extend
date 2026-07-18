package com.escalachurch.app.domain.model

/**
 * Fase 11.10 - placeholder payload for the future "Gerar QR Code" feature.
 * TODO: Implementar QR Code na versão futura.
 *
 * [payload] is whatever the QR code should encode - today that's just the public church link
 * (same one ChurchShareLinkBuilder produces), never a church id or token.
 */
data class ChurchQrData(
    val churchSlug: String,
    val payload: String
)
