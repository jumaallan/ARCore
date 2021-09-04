package com.jumaallan.arcore

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3

class TranslatableNode : Node() {

    var position: DroidPosition = DroidPosition.DOWN

    fun addOffset(x: Float = 0F, y: Float = 0F, z: Float = 0F) {
        val posX = localPosition.x + x
        val posY = localPosition.y + y
        val posZ = localPosition.z + z

        localPosition = Vector3(posX, posY, posZ)
    }

    fun pullUp() {
        if (position != DroidPosition.MOVING_UP && position != DroidPosition.UP) {
            animatePullUp()
        }
    }

    fun pullDown() {
        if (position != DroidPosition.MOVING_DOWN && position != DroidPosition.DOWN) {
            animatePullDown()
        }
    }

    private fun localPositionAnimator(vararg values: Any?): ObjectAnimator {
        return ObjectAnimator().apply {
            target = this@TranslatableNode
            duration = 250
            interpolator = LinearInterpolator()

            setAutoCancel(true)
            setObjectValues(*values)
            setEvaluator(VectorEvaluator())
        }
    }

    private fun animatePullUp() {
        val low = Vector3(localPosition)
        val high = Vector3(localPosition).apply { y = +.4F }

        val animation = localPositionAnimator(low, high)

        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                position = DroidPosition.UP
            }

            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {
                position = DroidPosition.MOVING_UP
            }
        })

        animation.start()
    }

    private fun animatePullDown() {
        val low = Vector3(localPosition).apply { y = 0F }
        val high = Vector3(localPosition)

        val animation = localPositionAnimator(high, low)

        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationEnd(animation: Animator?) {
                position = DroidPosition.DOWN
            }

            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationStart(animation: Animator?) {
                position = DroidPosition.MOVING_DOWN
            }
        })

        animation.start()
    }
}