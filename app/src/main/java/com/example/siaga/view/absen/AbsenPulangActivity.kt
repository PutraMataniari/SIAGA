package com.example.siaga.view.absen

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.siaga.R
import com.example.siaga.api.ApiClient
import com.example.siaga.datastore.DataStoreManager
import com.example.siaga.databinding.ActivityAbsenPulangBinding
import com.example.siaga.model.AbsensiResponse
import com.example.siaga.view.utils.BitmapManager.bitmapToBase64
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import androidx.exifinterface.media.ExifInterface
import com.example.siaga.view.utils.BitmapManager
import com.example.siaga.view.utils.BitmapManager.PhotoResult
import java.io.FileOutputStream



class AbsenPulangActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAbsenPulangBinding
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var progressDialog: ProgressDialog

    private var strCurrentLatitude = 0.0
    private var strCurrentLongitude = 0.0
    private var strFilePath: String = ""
    private var strBase64Photo: String = ""
    private var strCurrentLocation: String = ""
    private var strTitle: String = ""
    private var strImageName: String = ""

    private lateinit var cameraUri: Uri
    private var userToken: String? = null
    private var userNama: String? = null

    // === Permission Helpers ===
    private fun hasCameraPermission(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    // === Request Permissions ===
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val allGranted = result.entries.all { it.value }
            if (!allGranted) {
                Toast.makeText(this, "Izin Kamera & Lokasi wajib diberikan!", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            if (hasLocationPermission()) setCurrentLocation()
        }

    // === Ambil Foto ===
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                binding.imageSelfie.setImageURI(cameraUri)

                convertImage(cameraUri)
            } else {
                Toast.makeText(this, "Pengambilan foto dibatalkan/gagal", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAbsenPulangBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataStoreManager = DataStoreManager(this)

        initLoading()
        fetchToken()
        setInitLayout()

        // Minta izin kamera + lokasi di awal
        requestPermissions.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setUploadData()
    }

    private fun initLoading() {
        progressDialog = ProgressDialog(this).apply {
            setTitle("Mohon tunggu")
            setMessage("Sedang mengirim absen pulang...")
            setCancelable(false)
        }
    }

    private fun showLoading() {
        progressDialog.show()
    }

    private fun hideLoading() {
        if (progressDialog.isShowing) progressDialog.dismiss()
    }

    private fun fetchToken() {
        lifecycleScope.launch {
            val user = dataStoreManager.getUserData()
            user?.let {
                Log.d("USER_DATA", "Token: ${it.token}, Name: ${it.name}, Email: ${it.email}")
                userToken = it.token
                userNama = it.name

                binding.inputNama.setText(it.name)

                if (it.token.isNullOrEmpty()) {
                    Toast.makeText(this@AbsenPulangActivity, "Sesi login habis, silakan login ulang!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    // === Lokasi ===
    private fun isGpsEnabled(): Boolean {
        val lm = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
        return lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun setCurrentLocation() {
        if (!hasLocationPermission()) return
        if (!isGpsEnabled()) {
            Toast.makeText(this, "Harap aktifkan GPS!", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading()
        val fused = LocationServices.getFusedLocationProviderClient(this)

        fused.lastLocation
            .addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    handleLocation(loc)
                    hideLoading()
                } else {
                    val cts = CancellationTokenSource()
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { freshLoc ->
                            hideLoading()
                            if (freshLoc != null) handleLocation(freshLoc)
                            else Toast.makeText(this, "Lokasi tidak tersedia. Periksa GPS!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            hideLoading()
                            Toast.makeText(this, "Gagal ambil lokasi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                hideLoading()
                Toast.makeText(this, "Error lokasi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleLocation(location: Location) {
        strCurrentLatitude = location.latitude
        strCurrentLongitude = location.longitude

        lifecycleScope.launch {
            strCurrentLocation = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(this@AbsenPulangActivity, Locale.getDefault())
                    val list = geocoder.getFromLocation(strCurrentLatitude, strCurrentLongitude, 1)
                    if (!list.isNullOrEmpty()) list[0].getAddressLine(0) else "$strCurrentLatitude, $strCurrentLongitude"
                } catch (e: IOException) {
                    "$strCurrentLatitude, $strCurrentLongitude"
                }
            }
            binding.inputLokasi.setText(strCurrentLocation)
            binding.inputLokasi.isEnabled = false
        }
    }

    private fun setInitLayout() {
        strTitle = intent.getStringExtra(DATA_TITLE) ?: ""
        binding.tvTitle.text = strTitle

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setCurrentDateTime()

        binding.layoutImage.setOnClickListener {
            if (hasCameraPermission()) openCamera()
            else requestPermissions.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun setCurrentDateTime() {
        val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())
        binding.inputTanggal.setText(sdf.format(Date()))
        binding.inputTanggal.isEnabled = false
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            cameraUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                photoFile
            )
            takePicture.launch(cameraUri)
        } catch (ex: Exception) {
            Toast.makeText(this, "Gagal membuka kamera: ${ex.message}", Toast.LENGTH_SHORT).show()
            Log.e("CameraError", "${ex.message}", ex)
        }
    }

    private fun setUploadData() {
        binding.btnAbsen.setOnClickListener {
            val strNama = binding.inputNama.text.toString()
            val strTanggal = binding.inputTanggal.text.toString()
            val strLaporan = binding.inputLaporan.text.toString()

            // Debug log supaya tau isi variable
            Log.d("VALIDASI_ABSEN", "strFilePath=$strFilePath, strBase64Photo=$strBase64Photo, " +
                    "nama=$strNama, lokasi=$strCurrentLocation, tanggal=$strTanggal, laporan=$strLaporan")

            if (strFilePath.isEmpty() || strBase64Photo.isEmpty() || !File(strFilePath).exists()) {
                Toast.makeText(this, "Foto wajib diambil!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (strLaporan.isEmpty()) {
                Toast.makeText(this, "Laporan harian wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Jika mau pertahankan minimal 10 kata
            val wordCount = strLaporan.trim().split("\\s+".toRegex()).size
            if (wordCount < 10) {
                Toast.makeText(this, "Laporan harian minimal 10 kata!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            if (userToken.isNullOrEmpty()) {
                Toast.makeText(this, "Anda belum login!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val token = "Bearer $userToken"
            sendAbsenToApi(token)
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("photo_", ".jpg", cacheDir)
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }


    private fun sendAbsenToApi(token: String) {
        val nama = binding.inputNama.text.toString().trim()
        val lokasi = binding.inputLokasi.text.toString().trim()
        val laporan = binding.inputLaporan.text.toString().trim()

        if (strBase64Photo.isEmpty() || strFilePath.isEmpty()) {
            Toast.makeText(this, "Foto wajib diambil!", Toast.LENGTH_SHORT).show()
            return
        }

        val namaBody = nama.toRequestBody("text/plain".toMediaTypeOrNull())
        val lokasiBody = lokasi.toRequestBody("text/plain".toMediaTypeOrNull())
        val laporanBody = laporan.toRequestBody("text/plain".toMediaTypeOrNull())

        val file = File(strFilePath)
        if (!file.exists()) {
            Toast.makeText(this, "File foto tidak ditemukan!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                //Kompres gambar sebelum upload
                val compressedFile = reduceImageFile(file, maxWidth = 1080, quality = 80)

                Log.d(
                    "COMPRESS",
                    "Original: ${file.length() / 1024} KB | Compressed: ${compressedFile.length() / 1024} KB"
                )


                val reqFile = compressedFile.asRequestBody("image/*".toMediaTypeOrNull())
                val gambarPart =
                    MultipartBody.Part.createFormData("gambar", compressedFile.name, reqFile)

                withContext(Dispatchers.Main) {
                    showLoading()
                }

                ApiClient.instance.absenPulang(token, namaBody, gambarPart, lokasiBody, laporanBody)
                    .enqueue(object : Callback<AbsensiResponse> {
                        override fun onResponse(
                            call: Call<AbsensiResponse>,
                            response: Response<AbsensiResponse>
                        ) {
                            hideLoading()
                            if (response.isSuccessful) {
                                val body = response.body()
                                Toast.makeText(
                                    this@AbsenPulangActivity,
                                    "Absen sukses: ${body?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            } else {
                                hideLoading()
                                try {
                                    val errorBody = response.errorBody()?.string()
                                    val msg = if (!errorBody.isNullOrEmpty()) {
                                        val jsonObj = org.json.JSONObject(errorBody)
                                        jsonObj.optString(
                                            "message",
                                            "Terjadi kesalahan (${response.code()})"
                                        )
                                    } else {
                                        "Terjadi kesalahan (${response.code()})"
                                    }

                                    if (msg.contains("sudah absen", ignoreCase = true)) {
                                        AlertDialog.Builder(this@AbsenPulangActivity)
                                            .setTitle("Peringatan")
                                            .setMessage(msg)
                                            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                                            .show()
                                    } else {
                                        Toast.makeText(
                                            this@AbsenPulangActivity,
                                            msg,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    Log.e("ABSEN_API", "Code: ${response.code()} - Msg: $msg")
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        this@AbsenPulangActivity,
                                        "Error ${response.code()}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    Log.e("ABSEN_API", "Parsing error body gagal", e)
                                }
                            }
                        }

                        override fun onFailure(call: Call<AbsensiResponse>, t: Throwable) {
                            hideLoading()
                            Toast.makeText(
                                this@AbsenPulangActivity,
                                "Gagal: ${t.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("ABSEN_API", "Failure: ${t.message}", t)
                        }
                    })
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Toast.makeText(
                        this@AbsenPulangActivity,
                        "Kompresi gagal: ${e.message}",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    Log.e("COMPRESS_ERROR", "Gagal kompres gambar", e)
                }
            }
        }
    }


    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            strFilePath = absolutePath
            strImageName = name
        }
    }

    private fun convertImage(imageUri: Uri) {
        try {
            // Buka stream gambar dari URI
            val inputStream = contentResolver.openInputStream(imageUri)
            var bitmapImage = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmapImage == null) {
                Toast.makeText(this, "Gagal memuat foto!", Toast.LENGTH_SHORT).show()
                return
            }

            // Koreksi orientasi foto (Exif)
            try {
                val exif = contentResolver.openInputStream(imageUri)?.use { ExifInterface(it) }
                val orientation = exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) ?: 0
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
                bitmapImage = Bitmap.createBitmap(bitmapImage, 0, 0, bitmapImage.width, bitmapImage.height, matrix, true)
            } catch (e: Exception) {
                Log.e("EXIF", "Exif error: ${e.message}")
            }

            // Resize foto supaya lebih kecil
            val resizeHeight = (bitmapImage.height * (512.0 / bitmapImage.width)).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(bitmapImage, 512, resizeHeight, true)

            // Tampilkan di ImageView
            Glide.with(this)
                .load(scaledBitmap)
                .placeholder(R.drawable.ic_photo_camera)
                .into(binding.imageSelfie)

            // Simpan Base64 dan file path untuk dikirim ke API
            strBase64Photo = bitmapToBase64(scaledBitmap)

            // Simpan file lokal agar upload via API sukses
            val tempFile = uriToFile(imageUri)
            strFilePath = tempFile?.absolutePath ?: ""

            Log.d("CONVERT_IMAGE", "Base64 size=${strBase64Photo.length}, Path=$strFilePath")
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memproses foto!", Toast.LENGTH_SHORT).show()
            Log.e("CONVERT_IMAGE", "Error: ${e.message}", e)
        }
    }

    @Throws(IOException::class)
    private fun reduceImageFile(originalFile: File, maxWidth: Int = 1080, quality: Int = 80): File {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(originalFile.absolutePath, options)
        val origWidth = options.outWidth
        val origHeight = options.outHeight

        var inSampleSize = 1
        if (origWidth > maxWidth) {
            inSampleSize = Math.max(1, Math.round(origWidth.toFloat() / maxWidth).toInt())
        }

        val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        val sampledBitmap = BitmapFactory.decodeFile(originalFile.absolutePath, decodeOptions)
            ?: throw IOException("Gagal decode bitmap")

        val finalBitmap: Bitmap = if (sampledBitmap.width > maxWidth) {
            val ratio = maxWidth.toFloat() / sampledBitmap.width.toFloat()
            val newH = (sampledBitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(sampledBitmap, maxWidth, newH, true)
        } else {
            sampledBitmap
        }

        val compressedFile = File(cacheDir, "compressed_${originalFile.name}")
        FileOutputStream(compressedFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.flush()
        }

        if (finalBitmap !== sampledBitmap) sampledBitmap.recycle()
        return compressedFile
    }




    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val DATA_TITLE = "TITLE"
    }
}
