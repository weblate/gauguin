package org.piepmeyer.gauguin.difficulty

import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.piepmeyer.gauguin.RandomSingleton
import org.piepmeyer.gauguin.Randomizer
import org.piepmeyer.gauguin.creation.GridCreator
import org.piepmeyer.gauguin.creation.PossibleDigitsShuffler
import org.piepmeyer.gauguin.creation.RandomPossibleDigitsShuffler
import org.piepmeyer.gauguin.creation.dlx.DLX
import org.piepmeyer.gauguin.creation.dlx.MathDokuDLX
import org.piepmeyer.gauguin.game.save.SaveGame
import org.piepmeyer.gauguin.grid.GridSize
import org.piepmeyer.gauguin.options.DifficultySetting
import org.piepmeyer.gauguin.options.DigitSetting
import org.piepmeyer.gauguin.options.GameOptionsVariant
import org.piepmeyer.gauguin.options.GameVariant
import org.piepmeyer.gauguin.options.GridCageOperation
import org.piepmeyer.gauguin.options.NumeralSystem
import org.piepmeyer.gauguin.options.SingleCageUsage
import java.io.File
import kotlin.time.Duration.Companion.minutes

class TestGridMostDifficultOnes : FunSpec({
    test("calculateValues") {
        runBlocking(Dispatchers.Default) {

            withTimeout(30.minutes) {
                calculateDifficulties(3, "most-difficult")
            }
        }
    }
})

private suspend fun calculateDifficulties(size: Int, filename: String): List<Deferred<Pair<GameVariant, Double>>> = kotlinx.coroutines.coroutineScope {
    val deferreds = mutableListOf<Deferred<Pair<GameVariant, Double>>>()

    var foundDifficulty = 0.0

    val variant = GameVariant(
        GridSize(size, size),
        GameOptionsVariant(
            true,
            GridCageOperation.OPERATIONS_ALL,
            DigitSetting.FIRST_DIGIT_ONE,
            DifficultySetting.VERY_EASY,
            SingleCageUsage.DYNAMIC,
            NumeralSystem.Decimal
        )
    )


    for (i in 0..1_000_000) {
        async {
            val randomizer: Randomizer = RandomSingleton.instance
            val shuffler: PossibleDigitsShuffler = RandomPossibleDigitsShuffler()

            val grid = GridCreator(variant, randomizer, shuffler).createRandomizedGridWithCages()

            val difficulty = GridDifficultyCalculator(grid).calculate()

            if (difficulty > foundDifficulty) {
                println("Found grid with difficulty $difficulty, testing it.")

                if (MathDokuDLX(grid).solve(DLX.SolveType.MULTIPLE) == 1 && difficulty > foundDifficulty) {
                    val file = File("$filename-$size.yml")
                    SaveGame.createWithFile(file).save(grid)

                    println("Found grid with unique solution and difficulty $difficulty: $grid, saved to ${file.absoluteFile}")

                    foundDifficulty = difficulty
                }

                println("Hardest grid found: $foundDifficulty")
            }
        }
    }

    return@coroutineScope deferreds
}
