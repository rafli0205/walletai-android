package com.liam.walletai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var prefsManager: PrefsManager
    private lateinit var viewPager: ViewPager2
    private lateinit var tvIndicator: TextView
    private lateinit var btnSkip: Button
    private lateinit var btnNext: Button

    private val pages = listOf(
        OnboardingPage(
            title = "Catat Pengeluaran Cepat",
            description = "Tambah transaksi manual dalam beberapa detik untuk cash, bank, atau e-wallet."
        ),
        OnboardingPage(
            title = "Scan dan Import Struk",
            description = "Pakai kamera atau ambil dari galeri untuk bantu isi merchant dan nominal."
        ),
        OnboardingPage(
            title = "Pantau Budget dan Insight",
            description = "Lihat total mingguan, bulanan, budget kategori, dan insight pengeluaran."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        prefsManager = PrefsManager(this)

        viewPager = findViewById(R.id.viewPager)
        tvIndicator = findViewById(R.id.tvIndicator)
        btnSkip = findViewById(R.id.btnSkip)
        btnNext = findViewById(R.id.btnNext)

        viewPager.adapter = OnboardingAdapter(pages)
        updateUi(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateUi(position)
            }
        })

        btnSkip.setOnClickListener {
            finishOnboarding()
        }

        btnNext.setOnClickListener {
            val nextIndex = viewPager.currentItem + 1
            if (nextIndex < pages.size) {
                viewPager.currentItem = nextIndex
            } else {
                finishOnboarding()
            }
        }
    }

    private fun updateUi(position: Int) {
        tvIndicator.text = "${position + 1}/${pages.size}"
        btnNext.text = if (position == pages.lastIndex) {
            getString(R.string.action_start)
        } else {
            getString(R.string.action_next)
        }
    }

    private fun finishOnboarding() {
        prefsManager.setOnboardingDone(true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
