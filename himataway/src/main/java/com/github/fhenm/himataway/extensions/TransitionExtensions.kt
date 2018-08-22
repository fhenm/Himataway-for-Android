/**
 * Transition に関する拡張関数群
 */
package com.github.fhenm.himataway.extensions

import android.os.Build
import android.transition.Transition
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * Transition が終了するまで待つ
 */
suspend fun Transition.waitForFinish() : Boolean {
    return suspendCoroutine { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            this.addListener(object : Transition.TransitionListener {
                override fun onTransitionEnd(transition: Transition) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        transition.removeListener(this)
                    }
                    cont.resume(true)
                }

                override fun onTransitionCancel(transition: Transition) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        transition.removeListener(this)
                    }
                    cont.resume(false)
                }

                override fun onTransitionStart(transition: Transition) {}
                override fun onTransitionPause(transition: Transition) {}
                override fun onTransitionResume(transition: Transition) {}
            })
        } else {
            cont.resume(false)
        }
    }
}