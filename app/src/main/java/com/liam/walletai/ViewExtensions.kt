package com.liam.walletai

import android.view.View

fun View.playClickScaleAnimation(onEnd: () -> Unit) {
    this.animate()
        .scaleX(0.94f)
        .scaleY(0.94f)
        .setDuration(40)
        .withEndAction {
            this.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(60)
                .withEndAction {
                    onEnd()
                }
                .start()
        }
        .start()
}
