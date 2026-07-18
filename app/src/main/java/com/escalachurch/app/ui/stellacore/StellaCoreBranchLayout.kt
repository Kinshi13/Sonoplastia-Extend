package com.escalachurch.app.ui.stellacore

/**
 * Fase 11.9B Entrega 3 Bloco 8 - splits the resolved action list into the "two ascending branches"
 * layout: left/right count follows leftCount = n/2, rightCount = n - leftCount, which matches every
 * case the spec gives explicitly (2->1+1, 3->1+2, 4->2+2, 5->2+3, 6->3+3) without special-casing
 * each one. At most 6 actions are ever placed on the branches - anything beyond that becomes
 * [BranchAssignment.overflow], shown behind a single "Mais" node instead of extending the branches
 * indefinitely (Bloco 8: "acima de 6, usar Mais").
 *
 * Pure - no Compose/Android dependency - see StellaCoreBranchLayoutTest.
 */
data class BranchAssignment<T>(val left: List<T>, val right: List<T>, val overflow: List<T>)

fun <T> assignToBranches(actions: List<T>): BranchAssignment<T> {
    val visible = actions.take(6)
    val overflow = actions.drop(6)
    val leftCount = visible.size / 2
    return BranchAssignment(
        left = visible.take(leftCount),
        right = visible.drop(leftCount),
        overflow = overflow
    )
}
