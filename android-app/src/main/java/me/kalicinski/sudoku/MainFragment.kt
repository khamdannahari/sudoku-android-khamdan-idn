/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package me.kalicinski.sudoku

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import dagger.android.support.AndroidSupportInjection
import me.kalicinski.sudoku.R.string
import me.kalicinski.sudoku.databinding.FragmentMainBinding
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

class MainFragment : Fragment() {

    private lateinit var boardViewModel: BoardViewModel

    @Inject
    lateinit var viewModelFactory: SudokuViewModelFactory
    val mainFragmentArgs: MainFragmentArgs by navArgs()

    private lateinit var timer: Timer

    private val tvTimeRemaining by lazy {
        activity?.findViewById<TextView>(R.id.tvTimeRemaining)
    }

    private val tvSolveMe by lazy {
        activity?.findViewById<TextView>(R.id.tvSolveMe)
    }

    private val ivPlayPauseLabel by lazy {
        activity?.findViewById<ImageView>(R.id.ivPlayPauseLabel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this);
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        boardViewModel = ViewModelProvider(this, viewModelFactory).get(BoardViewModel::class.java)

        mainFragmentArgs.seed.takeUnless { it == 0L }.let { seed ->
            if (seed == null) {
                boardViewModel.initIfEmpty()
            } else {
                boardViewModel.generateNewBoard(true, seed)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DataBindingUtil.inflate<FragmentMainBinding>(
            inflater,
            R.layout.fragment_main,
            container,
            false
        ).run {
            setLifecycleOwner(this@MainFragment)
            boardvm = boardViewModel
            root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startNewGame()
        observeEventVm()
    }

    private fun showGuideToFillBox() {
        Toast.makeText(
            activity,
            getString(string.guide_to_fill_box),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun startNewGame() {
        showGuideToFillBox()
        boardViewModel.generateNewBoard(true)
    }

    private fun observeEventVm() {
        boardViewModel.onNewGame.observe(viewLifecycleOwner, {
            try {
                timer.cancel()
                timer.purge()
                showGuideToFillBox()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            startTimer()
            ivPlayPauseLabel?.setImageResource(R.drawable.ic_baseline_pause_24)
        })

        boardViewModel.onSolveGame.observe(viewLifecycleOwner, {
            timer.cancel()
            timer.purge()
            tvTimeRemaining?.setText(R.string._00_00_00)
            ivPlayPauseLabel?.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        })
    }

    private fun startTimer() {
        var seconds = 60
        var minutes = 59

        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {

            @SuppressLint("SetTextI18n")
            override fun run() {
                activity?.runOnUiThread {
                    val minutesTwoDigit = minutes.toTwoDigitFormat()
                    val secondsTwoDigit = seconds.toTwoDigitFormat()

                    tvTimeRemaining?.setText(getString(R.string.format_timer, minutesTwoDigit, secondsTwoDigit))
                    seconds -= 1

                    if (seconds == 0) {
                        if (minutes == 0) {
                            onTimeUp()
                        }

                        tvTimeRemaining?.setText(getString(R.string.format_timer, minutesTwoDigit, secondsTwoDigit))
                        seconds = 60
                        minutes -= 1
                    }
                }
            }
        }, 0, 1000)
    }

    private fun Int.toTwoDigitFormat(): String {
        return if (this < 10) "0$this" else this.toString()
    }

    private fun onTimeUp() {
        timer.cancel()
        timer.purge()
        tvSolveMe?.performClick()
        tvTimeRemaining?.setText(R.string._00_00_00)
        Toast.makeText(activity, getString(string.time_up), Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        boardViewModel.saveNow()
    }
}
