# Figure Mortis

Sebuah aplikasi Android inovatif yang menampilkan daftar tokoh sejarah yang meninggal pada hari ini atau tanggal tertentu, diperkaya dengan biografi mendetail yang dihasilkan oleh AI Generatif Google Gemini.

## Deskripsi Proyek

Figure Mortis adalah aplikasi mobile Android yang dirancang untuk memberikan pengalaman unik dalam menjelajahi sejarah. Aplikasi ini secara otomatis mendeteksi tanggal saat ini dan mengambil daftar tokoh sejarah yang meninggal pada tanggal tersebut dari Muffinlabs History API. Untuk setiap tokoh, aplikasi ini memanfaatkan Google Gemini dengan fitur "Grounding Search" untuk menghasilkan biografi yang terverifikasi dan mendetail. Pengguna juga memiliki kemampuan untuk menyimpan biografi tokoh sejarah favorit mereka untuk diakses kembali di kemudian hari.

## Tujuan Proyek

Proyek ini bertujuan untuk:

*   Mengembangkan aplikasi mobile Android yang fungsional menggunakan bahasa pemrograman Java.
*   Mengintegrasikan dan menampilkan data dari API eksternal (Muffinlabs History API).
*   Memperkaya konten dan memberikan informasi mendalam kepada pengguna melalui integrasi model AI generatif (Google Gemini).
*   Menerapkan antarmuka pengguna (UI) yang bersih, intuitif, dan menarik.
*   Mendemonstrasikan kemampuan dalam integrasi API, penanganan data JSON, dan pemanfaatan teknologi AI modern.

## Fitur Utama

*   **Daftar Tokoh Harian:** Aplikasi secara otomatis menampilkan daftar tokoh yang meninggal pada tanggal saat ini, lengkap dengan nama dan tahun kematian, yang diambil dari `history.muffinlabs.com`.
*   **Tampilan Detail Biografi:** Ketika pengguna memilih tokoh dari daftar, aplikasi akan menampilkan halaman detail yang berisi biografi mendetail yang dihasilkan oleh Google Gemini. Biografi ini mencakup nama lengkap, tanggal lahir (jika tersedia), biografi tiga paragraf, dan daftar sumber yang dapat diverifikasi.
*   **Menyimpan Detail Biografi:** Pengguna dapat menyimpan biografi tokoh sejarah ke database lokal untuk akses offline.
*   **Indikator Pemuatan:** Menampilkan animasi atau indikator pemuatan yang jelas saat aplikasi mengambil data dari API atau menunggu respons dari Google Gemini, memastikan pengguna mengetahui status proses.

## Teknologi yang Digunakan

*   **Bahasa Pemrograman:** Java
*   **Framework:** Android SDK
*   **Database:** SQLite
*   **API Eksternal:**
    *   Muffinlabs History API: Untuk data awal tokoh sejarah.
    *   Google AI (Gemini API): Untuk menghasilkan biografi mendetail dengan sumber.
*   **Model Bahasa Besar (LLM):** Gemini 2.0 Flash (dengan kemampuan *grounding search*).
*   **Pustaka (Libraries):**
    *   Retrofit/Volley: Untuk penanganan permintaan jaringan (API calls).
    *   Gson: Untuk *parsing* data JSON.
    *   Google AI Java Client Library: Untuk interaksi dengan Gemini API.

## Desain Aplikasi

### Desain Antarmuka Pengguna (UI)

*   **Halaman Utama:** Menggunakan `RecyclerView` dengan `CardView` untuk tampilan daftar tokoh yang modern dan rapi. Judul halaman menampilkan tanggal saat ini.
*   **Halaman Detail:** Menggunakan `ScrollView` yang berisi beberapa `TextView` untuk menampilkan nama, tanggal lahir, biografi, dan daftar sumber, dengan fokus pada keterbacaan.
*   **Halaman Saved Figures:** Menggunakan `RecyclerView` untuk menampilkan daftar tokoh yang disimpan, dengan tampilan yang mirip halaman utama.

### Desain Database

Aplikasi menggunakan SQLite untuk menyimpan informasi tokoh yang disimpan. Berikut adalah skema database:

*   **Nama Database:** `mortis.db`
*   **Nama Tabel:** `saved_figures`
*   **Kolom Tabel:**
    *   `id`: INTEGER PRIMARY KEY AUTOINCREMENT
    *   `name`: TEXT NOT NULL
    *   `birth_date`: TEXT
    *   `death_year`: TEXT
    *   `details`: TEXT NOT NULL
    *   `sources`: TEXT (Menyimpan daftar sumber sebagai string JSON)

### Desain Visual

Aplikasi mengadopsi tema gelap/vampir/gothic yang selaras dengan konsep "Mortis" (kematian). Palet warna ditentukan menggunakan Material Theme Builder, terinspirasi dari artwork game Warhammer. Font yang digunakan adalah Cinzel dan Crimson Text.

### Logo, Maskot, dan Ikon

Logo utama adalah "Gravestone" yang merepresentasikan kematian atau tempat peristirahatan. Maskot aplikasi berupa kerangka hidup, digunakan untuk halaman pemuatan dan halaman kosong, dengan referensi dari artwork yang memiliki palet warna serupa.

## Implementasi (Rencana Langkah-langkah)

1.  **Setup Proyek:** Membuat proyek baru di Android Studio.
2.  **Konfigurasi Dependensi:** Menambahkan pustaka Retrofit/Volley, Gson, dan Google AI ke dalam file `build.gradle`.
3.  **Integrasi History API:** Membuat kelas model Java untuk merepresentasikan struktur data JSON dari Muffinlabs History API dan melakukan permintaan API ke `https://history.muffinlabs.com/date/{m}/{d}`.
4.  **UI Halaman Utama:** Mendesain layout XML dan membuat Adapter untuk `RecyclerView`.
5.  **Integrasi Gemini API:** Membuat prompt Generatif AI yang sesuai, mengimplementasikan *grounding search*, memproses respons JSON, dan merender biografi dengan anotasi sumber.
6.  **UI Halaman Detail:** Membuat `Activity` dan layout XML baru untuk menampilkan biografi.
7.  **Navigasi:** Mengimplementasikan `Intent` untuk berpindah dari halaman utama ke halaman detail, sambil mengirimkan data tokoh yang dipilih.

## Pengujian

Pengujian akan dilakukan untuk memastikan fungsionalitas, UI, dan penanganan jaringan aplikasi:

*   **Pengujian Fungsional:** Memastikan halaman detail terbuka dengan data yang benar saat item diklik, indikator pemuatan berfungsi, dan respons AI cepat (di bawah 60 detik).
*   **Pengujian UI:** Menguji aplikasi pada berbagai ukuran layar (emulator) untuk memastikan tidak ada elemen UI yang tumpang tindih atau terpotong.
*   **Pengujian Jaringan:** Menguji dalam mode pesawat untuk memastikan penanganan error koneksi berfungsi dan halaman "Saved Figures" dapat diakses.

## Kesimpulan (yang Diharapkan)

Proyek "Figure Mortis" diharapkan menjadi aplikasi yang fungsional dan inovatif, menunjukkan kemampuan dalam pengembangan aplikasi Android dengan Java, integrasi layanan eksternal, dan penerapan teknologi AI canggih untuk menciptakan pengalaman pengguna yang kaya informasi dan menarik.

## Saran

*   Menggunakan model AI dengan konteks yang lebih besar dan kemampuan *reasoning* untuk sintesis informasi yang lebih kaya.
*   Membuat layanan API model Generatif AI sendiri untuk menghemat biaya.