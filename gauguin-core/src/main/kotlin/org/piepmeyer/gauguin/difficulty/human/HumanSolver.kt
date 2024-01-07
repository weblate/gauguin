package org.piepmeyer.gauguin.difficulty.human

import org.piepmeyer.gauguin.grid.Grid

class HumanSolver(
    private val grid: Grid,
) {
    private val humanSolverStrategy =
        listOf(
            HumanSolverStrategySingleCage(),
            HumanSolverStrategySinglePossible(),
        )

    fun solve() {
        humanSolverStrategy.forEach { it.fillCells(grid) }
    }
}
