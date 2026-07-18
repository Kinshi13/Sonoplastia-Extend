package com.escalachurch.app.share

import com.escalachurch.app.domain.model.ChurchQrData

/**
 * Fase 11.10 - architecture placeholder so ShareChurchAccessBottomSheet can wire a "Gerar QR Code"
 * affordance now and swap in a real implementation later without touching any call site.
 * TODO: Implementar QR Code na versão futura.
 */
interface GenerateChurchQrUseCase {
    fun generate(churchSlug: String, link: String): ChurchQrData
}
