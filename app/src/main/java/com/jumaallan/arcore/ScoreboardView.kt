package com.jumaallan.arcore

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.jumaallan.arcore.databinding.ScoreboardViewBinding

class ScoreboardView(context: Context, attrs: AttributeSet? = null, defStyle: Int = -1) :
    FrameLayout(context, attrs, defStyle) {

    private var binding: ScoreboardViewBinding

    init {
        inflate(context, R.layout.scoreboard_view, this)

        binding = ScoreboardViewBinding.inflate(LayoutInflater.from(context))
        addView(binding.root)

        binding.startButton.setOnClickListener {
            it.isEnabled = false
            onStartTapped?.invoke()
        }
    }

    var onStartTapped: (() -> Unit)? = null
    var score: Int = 0
        set(value) {
            field = value
            binding.textViewScoreCounter.text = value.toString()
        }

    var life: Int = 0
        set(value) {
            if (field == 0 && value > field) {
                // Game has been restarted, hide game over message
                binding.textViewGameOver.visibility = GONE
            }
            field = value
            binding.textViewLifeCounter.text = value.toString()

            // If player has 0 lives, show a game over message,
            // re enable start btn and change it's message
            if (value <= 0) {
                binding.textViewGameOver.visibility = View.VISIBLE
                binding.startButton.isEnabled = true
                binding.startButton.setText(R.string.restart)
            }
        }
}
