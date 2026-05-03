package com.liam.walletai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class ScanReceiptActivity : AppCompatActivity() {

    private lateinit var btnPickImage: Button
    private lateinit var btnScanCamera: Button
    private lateinit var btnCreateTransaction: Button
    private lateinit var ivReceiptPreview: ImageView
    private lateinit var tvOcrResult: TextView

    private var selectedImageUri: Uri? = null
    private var storedReceiptUri: Uri? = null
    private var detectedTotal: Double? = null
    private var detectedRawText: String = ""

    private var cameraImageUri: Uri? = null

    private val REQUEST_CAMERA_PERMISSION = 101

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                handleImageForScan(uri)
            } else {
                Toast.makeText(this, "Tidak ada gambar dipilih", Toast.LENGTH_SHORT).show()
            }
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                cameraImageUri?.let { uri ->
                    handleImageForScan(uri)
                } ?: Toast.makeText(
                    this,
                    "Gagal mengambil foto struk",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "Gagal mengambil foto struk", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_receipt)

        btnPickImage = findViewById(R.id.btnPickImage)
        btnScanCamera = findViewById(R.id.btnScanCamera)
        btnCreateTransaction = findViewById(R.id.btnCreateTransaction)
        ivReceiptPreview = findViewById(R.id.ivReceiptPreview)
        tvOcrResult = findViewById(R.id.tvOcrResult)

        btnCreateTransaction.isEnabled = false

        // Begitu masuk screen, langsung buka kamera
        startCameraFlow()

        btnScanCamera.setOnClickListener {
            startCameraFlow()
        }

        btnPickImage.setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        btnCreateTransaction.setOnClickListener {
            val uri = storedReceiptUri
            if (uri == null) {
                Toast.makeText(this, "Ambil / pilih foto struk dulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, AddTransactionActivity::class.java)

            detectedTotal?.let { total ->
                intent.putExtra("prefill_amount", total)
            }
            if (detectedRawText.isNotBlank()) {
                intent.putExtra("prefill_notes", detectedRawText.take(500))

                val guessedCategory = guessCategoryFromText(detectedRawText)
                if (guessedCategory != null) {
                    intent.putExtra("prefill_category", guessedCategory)
                }
            }

            intent.putExtra("receipt_image_uri", uri.toString())

            startActivity(intent)
        }
    }

    private fun startCameraFlow() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            return
        }

        openCamera()
    }

    private fun openCamera() {
        val photoFile = try {
            File.createTempFile("scan_receipt_", ".jpg", cacheDir)
        } catch (e: IOException) {
            Toast.makeText(this, "Gagal membuat file sementara", Toast.LENGTH_SHORT).show()
            return
        }

        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )

        takePicture.launch(cameraImageUri!!)
    }

    private fun handleImageForScan(uri: Uri) {
        selectedImageUri = uri

        storedReceiptUri = copyReceiptToInternalStorage(uri)

        val previewUri = storedReceiptUri ?: uri
        ivReceiptPreview.setImageURI(previewUri)

        btnCreateTransaction.isEnabled = true
        tvOcrResult.text = "Membaca teks dari struk..."
        runTextRecognition(previewUri)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else if (requestCode == REQUEST_CAMERA_PERMISSION) {
            Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runTextRecognition(uri: Uri) {
        val bitmap = uriToBitmap(uri) ?: run {
            Toast.makeText(this, "Gagal membaca gambar", Toast.LENGTH_SHORT).show()
            tvOcrResult.text = "Gagal membaca gambar."
            return
        }

        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                detectedRawText = visionText.text
                detectedTotal = extractTotalAmount(visionText.text)

                if (detectedTotal != null) {
                    tvOcrResult.text =
                        "Deteksi total: Rp ${detectedTotal?.toLong()}\n" +
                                "Tap 'Buka transaksi dari struk' untuk prefill."
                    Toast.makeText(
                        this,
                        "Deteksi total: $detectedTotal",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    tvOcrResult.text =
                        "Tidak bisa menemukan total otomatis.\n" +
                                "Kamu masih bisa isi manual di form transaksi."
                    Toast.makeText(
                        this,
                        "Tidak bisa menemukan total di struk, isi manual nanti",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                tvOcrResult.text = "Gagal mengenali teks: ${e.message}"
                Toast.makeText(this, "Gagal mengenali teks: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun extractTotalAmount(text: String): Double? {
        val lines = text.lines()
        val normalizedLines = lines.map { it.uppercase().replace(",", ".") }

        val totalKeywords = listOf(
            "TOTAL TAGIHAN",
            "TOTAL BAYAR",
            "TOTAL DIBAYAR",
            "TOTAL",
            "SUBTOTAL",
            "JUMLAH",
            "GRAND TOTAL"
        )

        val moneyRegex = Regex("""\d[\d\.]*\d""")

        var candidateFromTotal: Double? = null

        for (line in normalizedLines) {
            if (totalKeywords.any { kw -> line.contains(kw) }) {
                val matches = moneyRegex.findAll(line)
                for (m in matches) {
                    val clean = m.value.replace(".", "")
                    val v = clean.toDoubleOrNull() ?: continue
                    if (v >= 1000) {
                        if (candidateFromTotal == null || v > candidateFromTotal!!) {
                            candidateFromTotal = v
                        }
                    }
                }
            }
        }

        if (candidateFromTotal != null) return candidateFromTotal

        val allMatches = moneyRegex.findAll(normalizedLines.joinToString(" "))
        var maxVal: Double? = null
        for (m in allMatches) {
            val clean = m.value.replace(".", "")
            val v = clean.toDoubleOrNull() ?: continue
            if (v >= 1000) {
                if (maxVal == null || v > maxVal!!) {
                    maxVal = v
                }
            }
        }
        return maxVal
    }

    private fun guessCategoryFromText(text: String): String? {
        val upper = text.uppercase()

        return when {
            listOf("WARTEG", "RESTO", "CAFE", "KOPI", "COFFEE", "MCD", "KFC", "RESTORAN")
                .any { upper.contains(it) } -> "Food"
            listOf("GRAB", "GOJEK", "MAXIM", "BLUEBIRD", "KRL", "MRT", "TRANSJAKARTA")
                .any { upper.contains(it) } -> "Transport"
            listOf("TOKOPEDIA", "SHOPEE", "LAZADA", "BUKALAPAK")
                .any { upper.contains(it) } -> "Shopping"
            listOf("PLN", "PULSA", "TELKOMSEL", "INDOSAT", "XL", "BY.U")
                .any { upper.contains(it) } -> "Bills"
            else -> null
        }
    }

    // Copy foto struk ke internal storage app (filesDir/receipts)
    private fun copyReceiptToInternalStorage(sourceUri: Uri): Uri? {
        return try {
            val receiptsDir = File(filesDir, "receipts")
            if (!receiptsDir.exists()) {
                receiptsDir.mkdirs()
            }

            val fileName = "receipt_${System.currentTimeMillis()}.jpg"
            val destFile = File(receiptsDir, fileName)

            contentResolver.openInputStream(sourceUri).use { input ->
                if (input == null) return null
                FileOutputStream(destFile).use { output ->
                    copyStream(input, output)
                }
            }

            Uri.fromFile(destFile)
        } catch (e: Exception) {
            null
        }
    }

    private fun copyStream(input: InputStream, output: FileOutputStream) {
        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        while (true) {
            bytesRead = input.read(buffer)
            if (bytesRead == -1) break
            output.write(buffer, 0, bytesRead)
        }
    }
}
