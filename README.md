# WalletAI – Aplikasi Manajemen Keuangan Pribadi
Nama : Rafli Dhiya Fadhaly

NIM : 312410251

WalletAI adalah aplikasi manajemen keuangan berbasis Android yang membantu pengguna mencatat pemasukan dan pengeluaran, memantau kondisi keuangan secara real time, dan mendapatkan insight otomatis berbasis AI dari pola transaksi mereka.

---

## 1. Informasi Project

- Nama aplikasi : **WalletAI**
- Platform      : Android (Native, Android Studio)
- Bahasa        : Kotlin (sesuaikan kalau lo pakai Java)
- Pengembang    : Rafli Dhiya Fadhaly (ganti dengan nama lengkap lo)
- Mata kuliah   : (isi nama matkul / tugas project sesuai dosen)

---

## 2. Fitur Utama

- **Pencatatan Transaksi**  
  Tambah, edit, dan hapus transaksi pemasukan maupun pengeluaran dengan detail seperti kategori, tanggal, dan nominal.

- **Ringkasan Keuangan di Home**  
  Menampilkan total saldo, total income, dan total expense yang selalu ter‑update setiap ada perubahan transaksi.

- **Laporan & Grafik Keuangan**  
  Halaman Reports yang menampilkan statistik keuangan per periode, grafik perbandingan income vs expense, dan grafik pengeluaran per kategori.

- **AI Insights**  
  Modul AI yang menganalisis data transaksi untuk memberikan insight, seperti kategori yang paling boros atau tren pengeluaran tertentu.

- **Keamanan (PIN / Biometrik)**  
  Fitur lock screen dengan PIN dan opsi biometric (jika didukung perangkat) untuk melindungi data keuangan pengguna.

- **Scan Receipt**  
  Fitur pemindaian struk belanja untuk mempercepat proses input transaksi dari foto struk.

---

## 3. Desain UI & Penjelasan Screen

Bagian ini menjawab permintaan dosen untuk keterangan UI di README.

### 3.1. Splash & Lock Screen

- **Splash Screen**  
  Menampilkan logo dan nama WalletAI saat aplikasi pertama kali dibuka, sebagai entry point sebelum masuk ke aplikasi utama.

- **Lock Screen (PIN/Biometric)**  
  Pengguna diminta memasukkan PIN atau menggunakan biometric sebelum bisa mengakses data keuangan.  
  Fungsinya untuk menjaga privasi dan keamanan data transaksi.

### 3.2. Home Screen

- Menampilkan sapaan dinamis (misalnya “Good Morning”) dan tagline singkat aplikasi.  
- Menunjukkan ringkasan **Total Balance**, **Total Income**, dan **Total Expense** untuk periode berjalan.  
- Menampilkan daftar **Recent Transactions** (transaksi terbaru).  
- Terdapat tombol tambah (+) untuk menambahkan transaksi baru.

### 3.3. Add / Edit Transaction

- Form untuk memasukkan detail transaksi: jenis (income/expense), kategori, tanggal, nominal, dan catatan.  
- Pengguna bisa menyimpan transaksi baru atau mengubah transaksi yang sudah ada.  
- Setelah disimpan, data langsung mengupdate ringkasan dan laporan.

### 3.4. Reports Screen (Laporan & Grafik)

- Pilihan periode laporan (misalnya per bulan).  
- Menampilkan angka ringkasan income dan expense pada periode yang dipilih.  
- Grafik **Income vs Expense** untuk membandingkan pemasukan dan pengeluaran.  
- Grafik **Expense by Category** untuk melihat kategori mana yang paling banyak menyerap pengeluaran.

### 3.5. AI Insights Screen

- Menampilkan insight otomatis berdasarkan data transaksi pengguna.  
- Contoh insight: kategori dengan pengeluaran tertinggi, tren pengeluaran yang naik turun, atau saran penghematan sederhana.  
- Insight diperbarui berdasarkan data transaksi terbaru.

### 3.6. Settings Screen

- Pengaturan **Kategori** transaksi (tambah/edit/hapus kategori).  
- Pengaturan **Budget** (anggaran) per kategori / periode.  
- Pengaturan **Recording Reminder** (pengingat mencatat transaksi).  
- Pengaturan **PIN & Biometrik** untuk keamanan.  

### 3.7. Scan Receipt

- Pengguna memotret struk belanja melalui kamera.  
- Aplikasi membaca informasi penting dari struk (misalnya tanggal dan total belanja) dan mengubahnya menjadi draft transaksi.  
- Pengguna bisa mengoreksi data terlebih dahulu sebelum menyimpan.

*(Kalau punya screenshot, lo bisa tambahin di bawah tiap subbagian pakai format `![nama](path/gambar.png)`.)*

---

## 4. Arsitektur & Teknologi

- Pattern arsitektur : (misalnya) MVVM + Repository
- Database lokal     : SQLite (melalui Room Database)
- Library utama      :
  - Library chart untuk menampilkan grafik laporan.
  - Library kamera/OCR untuk fitur scan receipt.
  - Library lain yang digunakan (tulis sesuai project lo).

---

## 5. Cara Build & Menjalankan Project

1. Clone repository ini:
   ```bash
   git clone https://github.com/USERNAME/walletai-android.git
   ```
2. Buka folder project di **Android Studio**.  
3. Biarkan Gradle melakukan proses sync.  
4. Pilih device/emulator Android.  
5. Klik **Run** untuk menjalankan aplikasi.

Jika ada konfigurasi khusus (misalnya API key untuk layanan tertentu), jelaskan di bagian ini.

---

## 6. Link Pendukung

- Link ClickUp (SCRUM / Timeline Pengembangan):  
  `[tambahkan link ClickUp di sini]`

- Dokumentasi tambahan (jika ada):  
  `[link ke file PDF / drive / lainnya]`
