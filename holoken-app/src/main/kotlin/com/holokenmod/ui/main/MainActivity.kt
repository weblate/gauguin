/***************************************************************************
 * Copyright 2016 Adam Queler
 * HolokenMod - KenKen(tm) game developed for Android
 * (A modified version of Holoken 1.1.1 and 1.2 by Amanda Chow which was)
 * (a modified version of MathDoku 1.9 by Ben Buxton and Stephen Lee)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */
package com.holokenmod.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.Fade
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import androidx.preference.PreferenceManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.holokenmod.R
import com.holokenmod.StatisticsManager
import com.holokenmod.Utils.convertTimetoStr
import com.holokenmod.calculation.GridCalculationListener
import com.holokenmod.calculation.GridCalculationService
import com.holokenmod.databinding.ActivityMainBinding
import com.holokenmod.game.Game
import com.holokenmod.game.GridCreationListener
import com.holokenmod.game.SaveGame.Companion.createWithDirectory
import com.holokenmod.game.SaveGame.Companion.createWithFile
import com.holokenmod.grid.Grid
import com.holokenmod.grid.GridSize
import com.holokenmod.options.*
import com.holokenmod.options.CurrentGameOptionsVariant.instance
import com.holokenmod.ui.ActivityUtils
import com.holokenmod.ui.MainDialogs
import com.holokenmod.ui.grid.GridCellSizeService
import com.holokenmod.undo.UndoListener
import com.holokenmod.undo.UndoManager
import nl.dionsegijn.konfetti.core.PartyFactory
import nl.dionsegijn.konfetti.core.emitter.Emitter
import org.koin.android.ext.android.inject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.max

class MainActivity : AppCompatActivity(), GridCreationListener {
    private val game: Game by inject()
    private val calculationService: GridCalculationService by inject()
    private val applicationPreferences: ApplicationPreferences by inject()
    private val activityUtils: ActivityUtils by inject()
    private val cellSizeService: GridCellSizeService by inject()

    private val mTimerHandler = Handler(Looper.getMainLooper())
    private var starttime: Long = 0

    //runs without timer be reposting self
    private val playTimer: Runnable = object : Runnable {
        override fun run() {
            val millis = System.currentTimeMillis() - starttime
            topFragment.setGameTime(convertTimetoStr(millis))
            mTimerHandler.postDelayed(this, UPDATE_RATE.toLong())
        }
    }

    private lateinit var topFragment: GameTopFragment
    private lateinit var undoButton: View
    private lateinit var eraserButton: View

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("MissingInflatedId")
    public override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.MainScreenTheme)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)
        applicationPreferences.loadGameVariant()

        undoButton = findViewById(R.id.undo)
        eraserButton = findViewById(R.id.eraser)

        val undoListener = UndoListener { undoPossible -> undoButton.isEnabled = undoPossible }
        val undoList = UndoManager(undoListener)

        game.undoManager = undoList
        game.gridUI = binding.gridview
        binding.gridview.setOnLongClickListener {
            game.setValueOrPossiblesOnSelectedCell()
        }

        val ft = supportFragmentManager.beginTransaction()
        topFragment = GameTopFragment()
        ft.replace(R.id.keypadFrame, KeyPadFragment())
        ft.replace(R.id.gameTopFrame, topFragment)
        ft.commit()

        game.setRemovePencils(applicationPreferences.removePencils())

        cellSizeService.setCellSizeListener { cellSizePercent ->
            binding.gridview.setCellSizePercent(cellSizePercent)
            binding.gridview.forceLayout()
        }

        game.setSolvedHandler { gameSolved() }

        registerForContextMenu(binding.gridview)

        binding.hintOrNewGame.setOnClickListener { checkProgressOrStartNewGame() }
        undoButton.setOnClickListener { game.undoOneStep() }
        eraserButton.setOnClickListener { game.eraseSelectedCell() }

        MainNavigationViewInitializer(this, binding).initialize()

        binding.mainBottomAppBar.setOnMenuItemClickListener(
            BottomAppBarItemClickListener(
                binding.mainConstraintLayout,
            this)
        )
        binding.mainBottomAppBar.setNavigationOnClickListener { binding.container.open() }

        calculationService.addListener(createGridCalculationListener())
        loadApplicationPreferences()

        game.updateGrid(game.grid)

        updateAppBarState()

        if (applicationPreferences.newUserCheck()) {
            MainDialogs(this).openHelpDialog()
        }

        binding.gridview.addOnLayoutChangeListener { _, _, _, right, _, _, _, _, _ ->
            if (binding.mainBottomAppBar.marginStart != 0 && right > 0 && binding.mainBottomAppBar.marginStart != right) {
                val marginParams =
                    binding.mainBottomAppBar.layoutParams as ViewGroup.MarginLayoutParams
                marginParams.marginStart = right

                binding.mainBottomAppBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {  }
                binding.container.invalidate()
            }
        }
    }

    private fun createGridCalculationListener(): GridCalculationListener {
        return object : GridCalculationListener {
            override fun startingCurrentGridCalculation() {
                runOnUiThread {
                    binding.pendingCurrentGridCalculation.visibility = View.VISIBLE
                    binding.pendingNextGridCalculation.visibility = View.INVISIBLE
                    binding.gridview.visibility = View.INVISIBLE
                    binding.ferrisWheelView.visibility = View.VISIBLE
                    binding.ferrisWheelView.startAnimation()
                }
            }

            override fun currentGridCalculated(currentGrid: Grid) {
                runOnUiThread {
                    binding.pendingCurrentGridCalculation.visibility = View.INVISIBLE
                    binding.pendingNextGridCalculation.visibility = View.INVISIBLE
                }
                showAndStartGame(currentGrid)
            }

            override fun startingNextGridCalculation() {
                runOnUiThread {
                    binding.pendingCurrentGridCalculation.visibility = View.INVISIBLE
                    binding.pendingNextGridCalculation.visibility = View.VISIBLE
                }
            }

            override fun nextGridCalculated(currentGrid: Grid) {
                runOnUiThread {
                    binding.pendingCurrentGridCalculation.visibility = View.INVISIBLE
                    binding.pendingNextGridCalculation.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun gameSolved() {
        mTimerHandler.removeCallbacks(playTimer)
        grid.playTime = System.currentTimeMillis() - starttime
        showProgress(getString(R.string.puzzle_solved))

        updateAppBarState()

        binding.hintOrNewGame.hide()
        binding.hintOrNewGame.show()

        val statisticsManager = createStatisticsManager()
        val recordTime = statisticsManager.storeStatisticsAfterFinishedGame()
        recordTime?.let { showProgress("${getString(R.string.puzzle_record_time)} $it") }
        statisticsManager.storeStreak(true)
        val solvetime = grid.playTime
        val solveStr = convertTimetoStr(solvetime)
        topFragment.setGameTime(solveStr)

        val konfettiView = binding.konfettiView
        val emitterConfig = Emitter(15L, TimeUnit.SECONDS).perSecond(150)

        val colors = listOf(
            MaterialColors.getColor(konfettiView, com.google.android.material.R.attr.colorPrimary),
            MaterialColors.getColor(konfettiView, com.google.android.material.R.attr.colorOnPrimary),
            MaterialColors.getColor(konfettiView, com.google.android.material.R.attr.colorSecondary),
            MaterialColors.getColor(konfettiView, com.google.android.material.R.attr.colorOnSecondary),
            MaterialColors.getColor(konfettiView, com.google.android.material.R.attr.colorTertiary),
            MaterialColors.getColor(konfettiView, com.google.android.material.R.attr.colorOnTertiary))

        val party = PartyFactory(emitterConfig)
            .angle(270)
            .spread(90)
            .setSpeedBetween(1f, 5f)
            .timeToLive(3000L)
            .position(0.0, 0.0, 1.0, 0.0)
            .colors(colors)
            .build()
        konfettiView.start(party)
    }

    private fun createStatisticsManager(): StatisticsManager {
        return StatisticsManager(this, grid)
    }

    private fun showAndStartGame(currentGrid: Grid) {
        runOnUiThread {
            binding.konfettiView.reset()

            binding.gridview.grid = currentGrid
            updateGameObject(currentGrid)
            TransitionManager.beginDelayedTransition(binding.container, Fade(Fade.OUT))
            startFreshGrid(true)
            binding.gridview.visibility = View.VISIBLE
            binding.gridview.reCreate()
            binding.gridview.invalidate()
            binding.ferrisWheelView.visibility = View.INVISIBLE
            binding.ferrisWheelView.stopAnimation()
            TransitionManager.endTransitions(binding.container)
        }
    }

    fun cheatedOnGame() {
        makeToast(R.string.toast_cheated)
        createStatisticsManager().storeStreak(false)
    }

    private val grid: Grid
        get() = binding.gridview.grid

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) {
            return
        }
        if (resultCode == 99) {
            postNewGame()
            return
        }
        if (requestCode != 7 || resultCode != RESULT_OK) {
            return
        }
        val filename = data.extras!!.getString("filename")
        Log.d("HoloKen", "Loading game: $filename")

        val saver = createWithFile(File(filename))

        saver.restore()?.let {
            game.updateGrid(it)
            showGrid()
        }
    }

    public override fun onPause() {
        grid.playTime = System.currentTimeMillis() - starttime
        mTimerHandler.removeCallbacks(playTimer)
        // NB: saving solved games messes up the timer?
        val saver = createWithDirectory(this.filesDir)
        saver.save(grid)

        super.onPause()
    }

    public override fun onResume() {
        loadApplicationPreferences()
        if (grid.isActive) {
            binding.gridview.requestFocus()
            binding.gridview.invalidate()
            starttime = System.currentTimeMillis() - grid.playTime
            mTimerHandler.postDelayed(playTimer, 0)
        }
        super.onResume()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK && binding.gridview.isSelectorShown) {
            binding.gridview.requestFocus()
            binding.gridview.isSelectorShown = false
            binding.gridview.invalidate()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun loadApplicationPreferences() {
        activityUtils.configureNightMode()
        activityUtils.configureKeepScreenOn(this)
        activityUtils.configureFullscreen(this, binding.container)

//        topFragment.setTimerVisible(
//            applicationPreferences.preferences.getBoolean("showtimer", true)
//        )

        binding.gridview.updateTheme()
        insetsChanged()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (window != null) {
            insets = WindowInsetsCompat.toWindowInsetsCompat(
                window.decorView
                    .rootWindowInsets
            )
        }
        insetsChanged()
    }

    private fun insetsChanged() {
        insets?.let {
            runOnUiThread {
                if (binding.mainTopAreaStart == null)
                    return@runOnUiThread

                val constraintSet = ConstraintSet()
                constraintSet.clone(binding.mainConstraintLayout)
                constraintSet.setGuidelineBegin(binding.mainTopAreaStart!!.id, rightEdgeOfCutOutArea(it))
                constraintSet.setGuidelineEnd(
                    binding.mainTopAreaEnd!!.id,
                    it.getInsets(WindowInsetsCompat.Type.statusBars()).right
                )
                val topAreaBottom = max(
                    (0.25 * this@MainActivity.resources.displayMetrics.xdpi).toInt(),
                    it.getInsets(WindowInsetsCompat.Type.statusBars()).bottom
                )
                constraintSet.setGuidelineBegin(binding.mainTopAreaBottom!!.id, topAreaBottom)
                constraintSet.applyTo(binding.mainConstraintLayout)
                binding.mainConstraintLayout.requestLayout()
            }
        }
    }

    private fun rightEdgeOfCutOutArea(insets: WindowInsetsCompat): Int {
        val cutout = insets.displayCutout
        return if (cutout == null || cutout.boundingRects.isEmpty()) {
            0
        } else cutout.boundingRects[0].right
    }

    fun createNewGame() {
        MainDialogs(this).newGameGridDialog()
    }

    private fun postNewGame() {
        if (grid.isActive) {
            createStatisticsManager().storeStreak(false)
        }

        val gridSize = GridSize(
            applicationPreferences.gridWidth,
            applicationPreferences.gridHeigth
        )

        val variant = GameVariant(
            gridSize,
            instance().copy()
        )
        if (calculationService.hasCalculatedNextGrid(variant)) {
            val grid = calculationService.consumeNextGrid()
            grid.isActive = true
            showAndStartGame(grid)
            val t = Thread { calculationService.calculateNextGrid() }
            t.name = "PreviewCalculatorFromMainNext-" + variant.width + "x" + variant.height
            t.start()
        } else {
            val grid = Grid(variant)
            binding.gridview.grid = grid
            val t = Thread {
                calculationService.calculateCurrentAndNextGrids(variant)
            }
            t.name = "PreviewCalculatorFromMainNonNext-" + variant.width + "x" + variant.height
            t.start()
        }
    }

    private fun updateGameObject(newGrid: Grid) {
        game.updateGrid(newGrid)
    }

    fun startFreshGrid(newGame: Boolean) {
        game.clearUndoList()
        binding.gridview.updateTheme()

        updateAppBarState()

        if (newGame) {
            grid.isActive = true
            createStatisticsManager().storeStatisticsAfterNewGame()
            starttime = System.currentTimeMillis()
            mTimerHandler.postDelayed(playTimer, 0)
            if (applicationPreferences.preferences.getBoolean("pencilatstart", true)
            ) {
                grid.addPossiblesAtNewGame()
            }
        }
    }

    private fun updateAppBarState() {
        if (grid.isSolved) {
            binding.hintOrNewGame.isEnabled = true
            binding.hintOrNewGame.setImageResource(R.drawable.outline_add_24)

            undoButton.visibility = View.GONE
            eraserButton.visibility = View.GONE
        } else {
            binding.hintOrNewGame.isEnabled = true
            binding.hintOrNewGame.setImageResource(R.drawable.baseline_question_mark_24)

            undoButton.visibility = View.VISIBLE
            undoButton.isEnabled = false

            eraserButton.visibility = View.VISIBLE
        }
    }

    private fun showGrid() {
        val grid = game.grid

        startFreshGrid(false)

        game.updateGrid(grid)

        if (!this.grid.isSolved) {
            undoButton.isEnabled = false
            mTimerHandler.removeCallbacks(playTimer)
        }

        binding.gridview.invalidate()
        calculationService.setVariant(
            GameVariant(
                binding.gridview.grid.gridSize,
                instance().copy()
            )
        )
        //GridCalculationService.getInstance().calculateNextGrid();
    }

    fun checkProgressOrStartNewGame() {
        if (grid.isSolved) {
            postNewGame()
        } else {
            checkProgress()
        }
    }

    private fun checkProgress() {
        val mistakes = grid.numberOfMistakes(applicationPreferences.showDupedDigits())
        val filled = grid.numberOfFilledCells()
        val text = (resources.getQuantityString(
            R.plurals.toast_mistakes,
            mistakes, mistakes
        )
                + " / " +
                resources.getQuantityString(
                    R.plurals.toast_filled,
                    filled, filled
                ))
        val duration: Int = if (mistakes == 0) {
            1500
        } else {
            4000
        }
        Snackbar.make(binding.hintOrNewGame, text, duration)
            .setAnchorView(binding.hintOrNewGame)
            .setAction("Undo") {
                game.restoreUndo()
                binding.gridview.invalidate()
                checkProgressOrStartNewGame()
            }
            .show()
    }

    private fun showProgress(string: String) {
        Snackbar.make(binding.hintOrNewGame, string, Snackbar.LENGTH_LONG)
            .setAnchorView(binding.hintOrNewGame)
            .show()
    }


    fun gameSaved() {
        Snackbar.make(binding.hintOrNewGame, "Game saved sucessfully.", Snackbar.LENGTH_LONG)
            .setAnchorView(binding.hintOrNewGame)
            .show()
    }

    private fun makeToast(resId: Int) {
        Snackbar.make(binding.hintOrNewGame, resId, Snackbar.LENGTH_LONG)
            .setAnchorView(binding.hintOrNewGame)
            .show()
    }

    companion object {
        private const val UPDATE_RATE = 500
        private var insets: WindowInsetsCompat? = null
    }

    override fun freshGridWasCreated() {
        showGrid()
    }
}