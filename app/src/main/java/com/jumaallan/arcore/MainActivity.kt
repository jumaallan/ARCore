package com.jumaallan.arcore

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.jumaallan.arcore.Configuration.Companion.COL_NUM
import com.jumaallan.arcore.Configuration.Companion.MAX_MOVE_DELAY_MS
import com.jumaallan.arcore.Configuration.Companion.MAX_PULL_DOWN_DELAY_MS
import com.jumaallan.arcore.Configuration.Companion.MIN_MOVE_DELAY_MS
import com.jumaallan.arcore.Configuration.Companion.MIN_PULL_DOWN_DELAY_MS
import com.jumaallan.arcore.Configuration.Companion.MOVES_PER_TIME
import com.jumaallan.arcore.Configuration.Companion.ROW_NUM
import com.jumaallan.arcore.Configuration.Companion.START_LIVES

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment

    private lateinit var scoreboard: ScoreboardView

    private var gameHandler = Handler()

    private var droidRenderable: ModelRenderable? = null

    private var scoreboardRenderable: ViewRenderable? = null

    private var failLight: Light? = null

    private var grid = Array(ROW_NUM) { arrayOfNulls<TranslatableNode>(COL_NUM) }
    private var initialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment

        initResources()

        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            if (initialized) {
                failHit()
                return@setOnTapArPlaneListener
            }

            if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                "Find a HORIZONTAL and UPWARD FACING plane!".toast(this)
                return@setOnTapArPlaneListener
            }

            if (droidRenderable == null || scoreboardRenderable == null ||
                failLight == null
            ) {
                return@setOnTapArPlaneListener
            }

            val spacing = 0.3F
            val anchorNode = AnchorNode(hitResult.createAnchor())
            anchorNode.setParent(arFragment.arSceneView.scene)

            grid.matrixIndices { col, row ->
                val renderableModel = droidRenderable?.makeCopy() ?: return@matrixIndices
                TranslatableNode().apply {
                    setParent(anchorNode)
                    renderable = renderableModel
                    addOffset(x = row * spacing, z = col * spacing)
                    grid[col][row] = this
                    this.setOnTapListener { _, _ ->
                        if (this.position != DroidPosition.DOWN) {
                            // Droid hit!
                            scoreboard.score += 100
                            this.pullDown()
                        } else {
                            failHit()
                        }
                    }
                }
            }

            val renderableView = scoreboardRenderable ?: return@setOnTapArPlaneListener
            TranslatableNode()
                .also {
                    it.setParent(anchorNode)
                    it.renderable = renderableView
                    it.addOffset(x = spacing, y = 0.6F)
                }

            Node().apply {
                setParent(anchorNode)
                light = failLight
                localPosition = Vector3(.3F, .3F, .3F)
            }

            initialized = true
        }
    }

    private fun initResources() {
        ModelRenderable.builder()
            .setSource(this, R.raw.andy)
            .build()
            .thenAccept { droidRenderable = it }
            .exceptionally { it.toast(this) }

        scoreboard = ScoreboardView(this)

        scoreboard.onStartTapped = {
            scoreboard.life = START_LIVES
            scoreboard.score = 0
            gameHandler.post {
                repeat(MOVES_PER_TIME) {
                    gameHandler.post(pullUpRunnable)
                }
            }
        }

        ViewRenderable.builder()
            .setView(this, scoreboard)
            .build()
            .thenAccept {
                it.isShadowReceiver = true
                scoreboardRenderable = it
            }
            .exceptionally { it.toast(this) }

        failLight = Light.builder(Light.Type.POINT)
            .setColor(com.google.ar.sceneform.rendering.Color(Color.RED))
            .setShadowCastingEnabled(true)
            .setIntensity(0F)
            .build()
    }

    private val pullUpRunnable: Runnable by lazy {
        Runnable {
            if (scoreboard.life > 0) {
                grid.flatMap { it.toList() }
                    .filter { it?.position == DroidPosition.DOWN }
                    .run { takeIf { size > 0 }?.getOrNull((0..size).random()) }
                    ?.apply {
                        pullUp()
                        val pullDownDelay =
                            (MIN_PULL_DOWN_DELAY_MS..MAX_PULL_DOWN_DELAY_MS).random()
                        gameHandler.postDelayed({ pullDown() }, pullDownDelay)
                    }

                val nextMoveDelay = (MIN_MOVE_DELAY_MS..MAX_MOVE_DELAY_MS).random()
                gameHandler.postDelayed(pullUpRunnable, nextMoveDelay)
            }
        }
    }

    private fun failHit() {
        scoreboard.score -= 50
        scoreboard.life -= 1
        failLight?.blink()
        if (scoreboard.life <= 0) {
            // Game over
            gameHandler.removeCallbacksAndMessages(null)
            grid.flatMap { it.toList() }
                .filterNotNull()
                .filter { it.position != DroidPosition.DOWN && it.position != DroidPosition.MOVING_DOWN }
                .forEach { it.pullDown() }
        }
    }
}