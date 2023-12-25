package org.piepmeyer.gauguin.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.createBalloon
import com.skydoves.balloon.showAlignBottom
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.piepmeyer.gauguin.R
import org.piepmeyer.gauguin.Utils
import org.piepmeyer.gauguin.databinding.FragmentMainGameTopBinding
import org.piepmeyer.gauguin.difficulty.GameDifficulty
import org.piepmeyer.gauguin.difficulty.GameDifficultyRater
import org.piepmeyer.gauguin.difficulty.GridDifficultyCalculator
import org.piepmeyer.gauguin.game.Game
import org.piepmeyer.gauguin.game.GameLifecycle
import org.piepmeyer.gauguin.game.GridCreationListener
import org.piepmeyer.gauguin.game.PlayTimeListener
import org.piepmeyer.gauguin.preferences.ApplicationPreferencesImpl
import kotlin.time.Duration

class GameTopFragment : Fragment(R.layout.fragment_main_game_top), GridCreationListener,
    PlayTimeListener, KoinComponent {
    private val game: Game by inject()
    private val gameLifecycle: GameLifecycle by inject()
    private val applicationPreferences: ApplicationPreferencesImpl by inject()

    private lateinit var binding: FragmentMainGameTopBinding

    private var timeDescription: String? = null
    private var showtimer = false

    override fun onCreateView(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainGameTopBinding.inflate(inflater, parent, false)

        val onClickListener = View.OnClickListener {
            val difficultyFragment = MainGameDifficultyLevelFragment()

            val view = difficultyFragment.onCreateView(layoutInflater, null, null)

            val balloon = createBalloon(it.context) {
                setLayout(view)
                setWidth(BalloonSizeSpec.WRAP)
                setHeight(BalloonSizeSpec.WRAP)
                setBackgroundColorResource(R.color.md_theme_dark_surfaceVariant)
                setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                setArrowSize(10)
                setArrowPosition(0.5f)
                setPadding(12)
                setCornerRadius(8f)
                setBalloonAnimation(BalloonAnimation.ELASTIC)

                setLifecycleOwner(this@GameTopFragment)
                build()
            }

            binding.ratingStarThree.showAlignBottom(balloon)
        }

        binding.difficulty.setOnClickListener(onClickListener)
        binding.ratingStarOne.setOnClickListener(onClickListener)
        binding.ratingStarTwo.setOnClickListener(onClickListener)
        binding.ratingStarThree.setOnClickListener(onClickListener)
        binding.ratingStarFour.setOnClickListener(onClickListener)

        return binding.root
    }

    override fun onPause() {
        gameLifecycle.removePlayTimeListener(this)

        super.onPause()
    }

    override fun onResume() {
        this.showtimer = applicationPreferences.preferences.getBoolean("showtimer", true)
        updateTimerVisibility()

        gameLifecycle.addPlayTimeListener(this)

        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        game.addGridCreationListener(this)

        freshGridWasCreated()

        updateTimerVisibility()
    }

    private fun updateTimerVisibility() {
        binding.playtime.visibility = if (showtimer) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    override fun freshGridWasCreated() {
        if (!isAdded) {
            return
        }
        requireActivity().runOnUiThread {
            val rater = GameDifficultyRater()
            binding.difficulty.text = GridDifficultyCalculator(game.grid).info()

            val difficulty = rater.difficulty(game.grid)

            setStarsByDifficulty(difficulty)

            timeDescription?.let { binding.playtime.text = it }
        }
    }

    private fun setStarsByDifficulty(difficulty: GameDifficulty) {
        setStarByDifficulty(
            binding.ratingStarOne,
            difficulty,
            GameDifficulty.EASY
        )
        setStarByDifficulty(
            binding.ratingStarTwo,
            difficulty,
            GameDifficulty.MEDIUM
        )
        setStarByDifficulty(
            binding.ratingStarThree,
            difficulty,
            GameDifficulty.HARD
        )
        setStarByDifficulty(
            binding.ratingStarFour,
            difficulty,
            GameDifficulty.EXTREME
        )
    }

    private fun setStarByDifficulty(
        view: ImageView,
        difficulty: GameDifficulty,
        minimumDifficulty: GameDifficulty
    ) {
        if (difficulty >= minimumDifficulty) {
            view.setImageResource(R.drawable.filled_star_20)
        } else {
            view.setImageResource(R.drawable.outline_star_20)
        }
    }

    override fun playTimeUpdated() {
        activity?.runOnUiThread {
            setGameTime(game.grid.playTime)
        }
    }

    fun setGameTime(gameDuration: Duration) {
        val durationString = Utils.displayableGameDuration(gameDuration)

        if (this::binding.isInitialized) {
            binding.playtime.text = durationString
        } else {
            this.timeDescription = durationString
        }
    }
}