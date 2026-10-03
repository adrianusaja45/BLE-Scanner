# BLE Scanner & Radar

Aplikasi Android untuk memindai perangkat Bluetooth Low Energy (BLE) di sekitar, melacak
kedekatan perangkat secara visual melalui radar, dan menyimpan riwayat perangkat yang
ditemukan.

Dibangun sebagai study case posisi **Mobile Engineer**, dengan fokus pada tiga hal yang
sering jadi sumber bug di aplikasi BLE: **error handling**, **manajemen siklus hidup**, dan
**structured concurrency**.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Hilt](https://img.shields.io/badge/DI-Dagger%20Hilt-FF6D00?style=for-the-badge)](https://dagger.dev/hilt)
[![AGP](https://img.shields.io/badge/AGP-9.4.1-02303A?style=for-the-badge&logo=androidstudio&logoColor=white)](https://developer.android.com/build)

---

## Daftar Isi

- [Download APK](#download-apk)
- [Fitur](#fitur)
- [Cara Menjalankan](#cara-menjalankan)
- [Arsitektur](#arsitektur)
- [Library & Alasan Pemilihan Teknologi](#library--alasan-pemilihan-teknologi)
- [Handling Kasus Sulit](#handling-kasus-sulit)
- [Pernyataan Penggunaan AI](#pernyataan penggunaan-ai)
- [Known Issues](#known-issues)
- [Asumsi Teknis & Kendala](#asumsi-teknis--kendala)

---

## Download APK

APK build **release** yang sudah ditandatangani tersedia di halaman
[**Releases**](https://github.com/adrianusaja45/BLE-Scanner/releases).

| | |
|---|---|
| Format | `.apk` (signed, siap install) |
| Ukuran | ± 2,6 MB |
| Min. Android | 7.0 (API 24) |
| Target | Android 16 (API 37) |

**Cara memasang:** unduh APK-nya, transfer ke HP, lalu buka dari File Manager dan izinkan
*"Install unknown apps"* bila diminta.

> **Harus memakai HP asli.** Emulator tidak punya hardware Bluetooth LE, sehingga
> pemindaian tidak akan menemukan perangkat apa pun.

---

## Fitur

| Halaman | Fitur |
|---|---|
| **Scanner** | Daftar perangkat real-time, pencarian nama/MAC, filter ambang RSSI, estimasi jarak, status Bluetooth |
| **Radar** | Visualisasi posisi perangkat pada radar, warna sesuai kekuatan sinyal, status "Sinyal Hilang" |
| **History** | Riwayat perangkat yang pernah ditemukan, tersimpan di Room, bisa dihapus |

---

## Cara Menjalankan

### Prasyarat

| Komponen | Versi |
|---|---|
| Android Studio | versi yang mendukung **AGP 9.4.1** |
| JDK | **17** (dibutuhkan AGP 9.x) |
| Gradle | via wrapper, tidak perlu install manual |
| Device | **HP Android asli** — emulator **tidak mendukung** pemindaian BLE |

### Langkah

```bash
git clone https://github.com/adrianusaja45/BLE-Scanner.git
cd BLE-Scanner
```

1. Buka folder tersebut di Android Studio, tunggu sinkronisasi Gradle selesai.
2. Hubungkan HP Android, lalu aktifkan **USB Debugging** atau **Wireless Debugging**.
3. Pastikan Bluetooth dan **Location Service** menyala di HP.
   - Di **Android 11 ke bawah**, Location Service wajib menyala agar hasil pemindaian tidak kosong.
4. Tekan **Run** di Android Studio.
5. Berikan izin **Nearby Devices / Location** saat diminta.

### Build dari Terminal

```bash
./gradlew assembleDebug     # build APK debug
./gradlew installDebug      # build + pasang ke perangkat
./gradlew check             # jalankan static analysis (detekt)
```

### Build Release Bertanda Tangan

```bash
./gradlew clean assembleRelease
# hasil: app/build/outputs/apk/release/app-release.apk
```

Release build memakai R8 untuk mengecilkan ukuran APK dan menambahkan tanda tangan.
Kredensialnya **tidak** disimpan di dalam repository — dibaca dari
`keystore.properties` yang sudah masuk `.gitignore`.

Tanpa file tersebut, build **tetap berhasil** dan hanya menghasilkan APK unsigned.
Ini disengaja: orang lain yang meng-clone repository ini tetap bisa membangunnya tanpa
perlu memiliki keystore.

---

## Arsitektur

Menggunakan **MVVM** + **Repository Pattern**, dengan pemisahan tanggung jawab yang jelas:

```
┌─────────────────────────────────────────────────────────┐
│  View (Fragment)                                        │
│  Menampilkan state, menerima input. Tidak Holds logika.  │
└────────────────────┬────────────────────────────────────┘
                     │ StateFlow
┌────────────────────▼────────────────────────────────────┐
│  ViewModel                                             │
│  Menyaring, mengurutkan, menggabungkan state.           │
│  Menahan status (isScanning, bluetoothStatus, target).  │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│  Repository (BleScannerRepo)                            │
│  Sumber data: Bluetooth LE Scanner + Room Database.     │
│  Satu-satunya tempat yang tahu soal API Android BLE.    │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
   BLE Scanner              Room (SQLite)
```

### Alur data satu siklus pemindaian

```
onScanResult (ratusan/detik)
      │
      ├─► scannedDevicesMap[mac] = ScannedDevice   ← update cepat
      ├─► _scannedDevicesFlow.value = ...          ─┐
      └─► applicationScope.launch { dao.insert }   │  asynchronous
                                                    │
      ┌─────────────────────────────────────────────┘
      ▼
ScannerViewModel: combine(scannedDevices, searchQuery, minRssi)
      │  filter + sortByDescending { rssi }
      ▼
ScannerFragment: combine(isScanning, bluetoothStatus, scannedDevice)
      ▼
ListAdapter + DiffUtil → RecyclerView
```

### Keputusan struktural

- **`@Singleton` pada Repository.** Halaman Scanner dan Radar butuh sesi pemindaian yang
  sama. Tanpa scope, Hilt membuat instance berbeda per Fragment sehingga halaman Radar
  membaca data kosong.
- **`activityViewModels()` di kedua Fragment.** Scanner dan Radar harus berbagi state
  (MAC target, filter pencarian, status scan). `viewModels()` akan memberi instance
  berbeda per Fragment.
- **Satu `CoroutineScope` level aplikasi** (`SupervisorJob() + Dispatchers.IO`) yang
  di-*inject* lewat Hilt, bukan dibuat inline di dalam callback.
- **Semua penulisan `tvStatus` berasal dari satu `combine()`.** Awalnya tiga collector
  terpisah menulis ke TextView yang sama sehingga teks yang tampil tidak terduga.

---

## Library & Alasan Pemilihan Teknologi

| Library | Versi | Alasan |
|---|---|---|
| **Dagger Hilt** | 2.60.1 | Dependency injection yang dikompilasi oleh Dagger (bukan reflection), waktu build cepat dan runtime cepat. Menyediakan scope yang jelas untuk `Repository` dan `Database`. |
| **Room** | 2.8.5 | Abstraksi atas SQLite dengan integrasi langsung ke Flow, sehingga perubahan di database otomatis terpantau UI tanpa polling manual. |
| **Kotlin Coroutines** | - | StateFlow membuat aliran data satu arah dan lifecycle-aware lewat 
epeatOnLifecycle, sehingga UI berhenti menerima data saat tidak terlihat. |
| **ViewBinding** | — | Menghilangkan `findViewById` dan memberi type safety. Dipilih manual daripada Data Binding karena proyek ini tidak butuh two-way binding. |
| **ListAdapter + DiffUtil** | — | Hanya memperbarui baris yang berubah, bukan me-refresh seluruh RecyclerView. Penting karena daftar diperbarui sangat sering saat pemindaian aktif. |
| **Material Components 3** | 1.10.0 | Bottom Navigation dan tema Material 3 dengan dukungan dark mode otomatis (`DayNight`). |
| **detekt** | 2.0.0-alpha.6 | Static analysis. `./gradlew check` wajib hijau. |
| **KSP** | 2.3.12 | Code generation untuk Hilt dan Room, lebih cepat dari KAPT. |

---

## Handling Kasus Sulit

Bagian ini menjelaskan beberapa masalah nyata yang ditemukan selama pengerjaan, dan cara
mengatasinya.

### 1. Aplikasi crash saat Bluetooth dimatikan

**Gejala:** mematikan Bluetooth lalu menekan tombol scan langsung menutup aplikasi.

**Penyebab:** `BluetoothLeScanner.startScan()` melempar `IllegalStateException` saat adapter
nonaktif, dan `SecurityException` saat izin belum diberikan. Keduanya tidak tertangani.

**Perbaikan:** prasyarat diperiksa eksplisit sebelum memanggil scanner, dan hasilnya
dikembalikan sebagai sealed interface `ScanLaunchResult` agar UI bisa menampilkan
penjelasan, bukan crash.

### 2. Urutan pemeriksaan yang salah

Ada bug halus: `BluetoothAdapter.getBluetoothLeScanner()` mengembalikan `null` ketika LE
sedang nonaktif, **bukan hanya** ketika LE tidak didukung hardware. Karena pemeriksaan
`null` dilakukan lebih dulu, mematikan Bluetooth dilaporkan sebagai "perangkat ini tidak
mendukung Bluetooth LE" — padahal perangkatnya mendukung, hanya sedang mati.

Akibatnya pemeriksaan `isEnabled()` menjadi dead code yang tidak pernah dieksekusi. Bug ini
tidak terlihat dari membaca kode, dan baru ketahuan setelah diuji di perangkat nyata.
Urutan pemeriksaan yang benar: **adapter → izin → `isEnabled()` → scanner → lokasi**.

### 3. Pemindaian tetap jalan saat aplikasi di background

Adapter BLE yang aktif saat aplikasi tidak terlihat adalah sumber borosnya baterai, dan
sebelumnya tidak ada titik yang memanggil `stopScan()` selain tombol di layar.

**Perbaikan:** pemindaian dihentikan di `Activity.onStop()` dan dilanjutkan otomatis di
`onStart()`. Memakai level **Activity**, bukan Fragment, dengan alasan: halaman Radar
membutuhkan data pemindaian yang aktif. Kalau pemindaian berhenti saat Fragment tak
terlihat, berpindah ke Radar akan mematikan justru yang paling dibutuhkan.

Dua detail yang penting:

- **`isChangingConfigurations` dilewati**, karena rotasi dan ganti tema juga memicu
  `onStop()`. Tanpa itu, setiap kali perangkat diputar, pemindaian ikut mati.
- **Status Bluetooth di-*refresh* saat kembali ke foreground.** Selama aplikasi di
  background receiver dilepas, jadi perubahan Bluetooth yang terjadi di luar aplikasi tidak
  pernah sampai ke ViewModel. Tanpa refresh, UI akan menampilkan "Scan Dihentikan"
  padahal penyebab sebenarnya Bluetooth mati.

### 4. User terkunci setelah menolak izin dua kali

Di Android 11+, menolak izin yang sama dua kali membuat sistem **berhenti menampilkan
dialog** permintaan izin — tanpa error, callback-nya langsung mengembalikan `false`.

Aplikasi yang tetap memanggil `launch()` akan membuat user melihat tombol scan terkunci
tanpa penjelasan dan tanpa jalan keluar.

**Perbaikan:** `shouldShowRequestPermissionRationale()` dipakai untuk mendeteksi status
"ditolak permanen", lalu aplikasi menampilkan dialog yang mengarahkan user ke halaman
Pengaturan. Deteksi ini hanya dijalankan setelah izin benar-benar pernah diminta, karena
nilai `false` dari fungsi tersebut ambigu antara "belum pernah diminta" dan "ditolak
permanen".

### 5. Ratusan CoroutineScope per detik

`onScanResult` dipanggil ratusan kali per detik saat mode `SCAN_MODE_LOW_LATENCY` aktif.
Setiap pemanggilan membuat `CoroutineScope(Dispatchers.IO)` sendiri, menghasilkan ratusan
scope yang tidak pernah di-*cancel* dan tidak punya `Job` parent.

**Perbaikan:** satu scope berumur aplikasi disediakan lewat Hilt dengan qualifier
`@ApplicationScope`. Dipakai `SupervisorJob()` (bukan `Job()` biasa) agar satu kegagalan
penulisan tidak membatalkan operasi lain di scope yang sama.

---

## Pernyataan Penggunaan AI

Project ini dikembangkan dengan bantuan **AI generatif**.

**Alat yang digunakan:** Gemini, OpenCode.

**AI digunakan untuk:**
- Menjelaskan perilaku Android API yang belum dipahami, terutama perilaku
  `BluetoothLeScanner` dan siklus hidup Activity/Fragment
- Membuat boilerplate dan struktur awal (MVVM setup, Hilt module, Room DAO)
- Membantu mendiagnosis bug dari keluaran logcat
- Meninjau kode dan memberi saran perbaikan

**Yang dikerjakan manual:**
- Seluruh kode disalin dan diketik manual ke Android Studio
- Setiap perubahan dijalankan dan diuji langsung di perangkat fisik
- Keputusan arsitektur dan prioritas fitur diambil sendiri

**Verifikasi:** Setiap perubahan yang masuk ke branch `main` telah dijalankan dan diuji di
perangkat nyata. Penjelasan setiap keputusan teknis di atas ditulis berdasarkan pemahaman
tentang apa yang dilakukan dan mengapa. Saya sedang dalam proses memperdalam pemahaman
mengenai concurrency dan siklus hidup Android, dan memperlakukan proyek ini sebagai bahan
belajar.

---

## Known Issues

Hal-hal yang **diketahui masih bermasalah** dan sengaja dicatat terbuka:

1. **Emulator tidak mendukung BLE.** Pemindaian hanya bisa diuji di perangkat fisik.
2. **Estimasi jarak bukan jarak absolut.** Angka meter adalah konversi kasar dari RSSI.
   Nilai RSSI sangat dipengaruhi dinding, interferensi, dan orientasi antena, sehingga
   hasilnya bisa meleset jauh.
3. **Sebagian perangkat menyembunyikan namanya.** Aplikasi menampilkan "Unknown Device".
4. **Perangkat yang menjauh tidak pernah dihapus dari daftar.** Peta perangkat di memori
   tidak memiliki mekanisme eviction, sehingga perangkat yang sudah pergi masih tampil
   dengan RSSI basi. Dampaknya ringan, tapi membuat daftar dan radar terasa "basi".
5. **Riwayat bisa terhapustotal saat skema database berubah.** `AppDatabase` memakai
   `fallbackToDestructiveMigration()`, jadi migrasi skema akan menghapus seluruh data
   yang tersimpan. Perlu diganti dengan migrasi eksplisit sebelum schema diubah lagi.
6. **Belum ada unit test.** Hanya ada test template bawaan Android Studio. Logika yang
   layak diuji — terutama konversi RSSI dan filtering — belum punya test otomatis.
7. **MAC Address bisa di-*randomize*.** Mulai Android 6, banyak perangkat BLE
   menggunakan MAC acak yang berbeda setiap kali advertise, sehingga perangkat yang
   sama bisa muncul sebagai beberapa entri berbeda.

---

## Asumsi Teknis & Kendala

1. **Minimum Android 7.0 (API 24).** `targetSdk` 37, `compileSdk` 37.
2. **Handling izin berbeda per level SDK.** Android 12 (API 31) mengganti pasangan izin
   lama (`BLUETOOTH`/`BLUETOOTH_ADMIN`) dengan `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`.
   Di bawah API 31, pemindaian justru diblokir oleh izin lokasi — itu sebabnya daftar
   izin harus dibangun per versi SDK, bukan satu daftar statis.
3. **`ACCESS_COARSE_LOCATION` sengaja tidak diminta.** Di bawah API 31 hanya
   `ACCESS_FINE_LOCATION` yang memblokir pemindaian. Meminta keduanya membuat pengecekan
   selalu bernilai "belum diberi" di perangkat API 24–28.
4. **Android 11 ke bawah wajib menyalakan Location Service**, bukan hanya memberi izin.
   Kalau service-nya mati, pemindaian berjalan normal tapi mengembalikan nol hasil tanpa
   pesan apa pun.
5. **Pemindaian hanya berhenti saat aplikasi masuk background**, bukan saat berpindah tab.
   Ini keputusan sadar supaya halaman Radar dan perpindahan tab tetap mendapat data
   real-time.
6. **Estimasi jarak memakai asumsi RSSI -100 dBm sebagai tepi luar dan -30 dBm sebagai
   titik tengah.** Nilai ini adalah pendekatan umum; untuk mengukur distance yang akurat
   diperlukan path-loss exponent yang bergantung pada lingkungan.

---

## Lisensi

Hak cipta dilindungi.
