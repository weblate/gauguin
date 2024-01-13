package org.piepmeyer.gauguin.creation

import io.kotest.core.spec.style.FunSpec
import org.piepmeyer.gauguin.creation.dlx.DLX
import org.piepmeyer.gauguin.creation.dlx.MathDokuDLX
import org.piepmeyer.gauguin.creation.dlx.MathDokuDLX2
import org.piepmeyer.gauguin.grid.Grid
import org.piepmeyer.gauguin.grid.GridSize
import org.piepmeyer.gauguin.options.GameOptionsVariant
import org.piepmeyer.gauguin.options.GameVariant

class TestGridCreatorPerformance : FunSpec({
    for (seed in 0..90) {
        xtest("seed performance-$seed") {
            val randomizer = SeedRandomizerMock(seed)

            val variant =
                GameVariant(
                    GridSize(10, 10),
                    GameOptionsVariant.createClassic(),
                )

            calculateGrid(randomizer, variant)
        }
    }

    for (seed in 0..90) {
        test("seed performance-DLX-$seed") {
            val randomizer = SeedRandomizerMock(seed)

            val variant =
                GameVariant(
                    GridSize(10, 10),
                    GameOptionsVariant.createClassic(),
                )

            val grid =
                GridCreator(variant, randomizer, RandomPossibleDigitsShuffler(randomizer.random))
                    .createRandomizedGridWithCages()

            MathDokuDLX(grid).solve(DLX.SolveType.ONE)
        }
    }

    for (seed in 0..90) {
        test("seed performance-DLX2-$seed") {
            val randomizer = SeedRandomizerMock(seed)

            val variant =
                GameVariant(
                    GridSize(10, 10),
                    GameOptionsVariant.createClassic(),
                )

            val grid =
                GridCreator(variant, randomizer, RandomPossibleDigitsShuffler(randomizer.random))
                    .createRandomizedGridWithCages()

            MathDokuDLX2(grid).solve(DLX.SolveType.ONE)
        }
    }
})

private suspend fun calculateGrid(
    randomizer: SeedRandomizerMock,
    variant: GameVariant,
): Grid {
    val creator =
        GridCalculator(
            variant,
            randomizer,
            RandomPossibleDigitsShuffler(randomizer.random),
        )

    return creator.calculate()
}
