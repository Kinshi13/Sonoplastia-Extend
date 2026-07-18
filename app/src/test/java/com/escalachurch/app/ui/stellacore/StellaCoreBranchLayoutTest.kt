package com.escalachurch.app.ui.stellacore

import org.junit.Assert.assertEquals
import org.junit.Test

class StellaCoreBranchLayoutTest {

    private fun actions(n: Int) = (1..n).map { "action-$it" }

    @Test
    fun oneAction_singleBranchOnTheRight() {
        val result = assignToBranches(actions(1))
        assertEquals(0, result.left.size)
        assertEquals(1, result.right.size)
        assertEquals(emptyList<String>(), result.overflow)
    }

    @Test
    fun twoActions_onePerBranch() {
        val result = assignToBranches(actions(2))
        assertEquals(1, result.left.size)
        assertEquals(1, result.right.size)
    }

    @Test
    fun threeActions_oneLeftTwoRight() {
        val result = assignToBranches(actions(3))
        assertEquals(1, result.left.size)
        assertEquals(2, result.right.size)
    }

    @Test
    fun fourActions_twoPerBranch() {
        val result = assignToBranches(actions(4))
        assertEquals(2, result.left.size)
        assertEquals(2, result.right.size)
    }

    @Test
    fun fiveActions_twoLeftThreeRight() {
        val result = assignToBranches(actions(5))
        assertEquals(2, result.left.size)
        assertEquals(3, result.right.size)
    }

    @Test
    fun sixActions_threePerBranch() {
        val result = assignToBranches(actions(6))
        assertEquals(3, result.left.size)
        assertEquals(3, result.right.size)
    }

    @Test
    fun sevenActions_cappedAtSixOnBranches_restOverflow() {
        val result = assignToBranches(actions(7))
        assertEquals(3, result.left.size)
        assertEquals(3, result.right.size)
        assertEquals(listOf("action-7"), result.overflow)
    }

    @Test
    fun noActions_bothBranchesEmpty() {
        val result = assignToBranches(emptyList<String>())
        assertEquals(0, result.left.size)
        assertEquals(0, result.right.size)
        assertEquals(0, result.overflow.size)
    }

    @Test
    fun everyActionIsPlacedExactlyOnce() {
        val input = actions(9)
        val result = assignToBranches(input)
        val placed = result.left + result.right + result.overflow
        assertEquals(input, placed)
    }
}
