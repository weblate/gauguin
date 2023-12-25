package org.piepmeyer.gauguin.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.piepmeyer.gauguin.R
import org.piepmeyer.gauguin.databinding.FragmentMainGameDifficultyLevelBinding
import org.piepmeyer.gauguin.difficulty.GameDifficultyRater
import org.piepmeyer.gauguin.game.Game

class MainGameDifficultyLevelFragment : Fragment(R.layout.fragment_main_game_difficulty_level),
    KoinComponent {
    private val game: Game by inject()

    private lateinit var binding: FragmentMainGameDifficultyLevelBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainGameDifficultyLevelBinding.inflate(inflater, parent, false)

        val rater = GameDifficultyRater()

        val difficulty = rater.difficulty(game.grid)
        val rating = rater.byVariant(game.grid.variant)

        rating?.let {
            binding.veryEasyMaximumValue.text = "0 ... ${it.thresholdEasy.toInt()}"
            binding.easyMinimumValue.text = "${it.thresholdEasy.toInt()}"
            binding.easyMaximumValue.text = "${it.thresholdMedium.toInt()}"
            binding.mediumMaximumValue.text = "${it.thresholdMedium.toInt()} ..." + it.thresholdHard.toInt()
            binding.hardMaximumValue.text = "${it.thresholdHard.toInt()} ..." + it.thresholdExtreme.toInt()
            binding.extremeMaximumValue.text = "${it.thresholdExtreme.toInt()} ... ?"
        }


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    }
}