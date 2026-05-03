package com.liam.walletai

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class ReceiptFullScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_fullscreen)

        val ivFull = findViewById<ImageView>(R.id.ivReceiptFull)
        val uriString = intent.getStringExtra("receipt_uri")

        if (uriString.isNullOrBlank()) {
            finish()
            return
        }

        try {
            Glide.with(this)
                .load(Uri.parse(uriString))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(ivFull)
        } catch (e: Exception) {
            finish()
            return
        }

        ivFull.setOnClickListener {
            finish()
        }
    }
}
