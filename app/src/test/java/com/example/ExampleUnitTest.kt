package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleLogicTest {
    @Test
    fun emptyCellsDoNotBlockAnArrow() {
        val arrow = CellState(id = 0, x = 0, y = 0, direction = Direction.RIGHT, isDot = false)
        val emptyCell = CellState(id = 1, x = 1, y = 0)

        assertFalse(isBlocked(arrow, listOf(arrow, emptyCell)))
    }

    @Test
    fun anotherArrowBlocksThePath() {
        val arrow = CellState(id = 0, x = 0, y = 0, direction = Direction.RIGHT, isDot = false)
        val blocker = CellState(id = 1, x = 1, y = 0, direction = Direction.UP, isDot = false)

        assertTrue(isBlocked(arrow, listOf(arrow, blocker)))
    }
}
