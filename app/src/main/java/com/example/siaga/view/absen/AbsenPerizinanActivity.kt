package com.example.siaga.view.absen

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.siaga.R
import com.example.siaga.api.ApiClient
import com.example.siaga.datastore.DataStoreManager
import com.example.siaga.databinding.ActivityAbsenPerizinanBinding
import com.example.siaga.model.IzinResponse
import com.example.siaga.view.utils.BitmapManager.bitmapToBase64
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
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

class AbsenPerizinanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAbsenPerizinanBinding
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var progressDialog: ProgressDialog

    private var strCurrentLatitude = 0.0
    private var strCurrentLongitude = 0.0
    private var strFilePath: String = ""
    private var strLampiranPath: String = ""
    private var strBase64Photo: String = ""
    private var strCurrentLocation: String = ""
    private var strTitle: String = ""
    private var strTimeStamp: String = ""
    private var strImageName: String = ""

    private lateinit var cameraUri: Uri
    private var userToken: String? = null
    private var userNama: String? = null

    // === Permission helpers ===
    private fun hasCameraPermission(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val allGranted = result.entries.all { it.value }
            if (!allGranted) {
                Toast.makeText(this, "Izin Kamera & Lokasi wajib diberikan!", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            if (hasLocationPermission()) setCurrentLocation()
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                Glide.with(this)
                    .load(cameraUri)
                    .placeholder(R.drawable.ic_photo_camera)
                    .into(binding.imageSelfie)
                convertImage(cameraUri.path)
            } else {
                Toast.makeText(this, "Pengambilan foto dibatalkan/gagal", Toast.LENGTH_SHORT).show()
            }
        }

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val file = uriToFile(it)
                if (file != null) {
                    strLampiranPath = file.absolutePath
                    val fileName = getFileNameFromUri(it) ?: file.name
                    binding.btnUploadLampiran.text = "File terpilih"
                    binding.textLampiran.text = fileName
                } else {
                    Toast.makeText(this, "Gagal membaca file lampiran", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name = "lampiran_unknown"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = it.getString(idx)
            }
        }
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = getFileNameFromUri(uri)
            val tempFile = File(cacheDir, fileName)
            inputStream?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAbsenPerizinanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataStoreManager = DataStoreManager(this)

        // Init progress dialog
        progressDialog = ProgressDialog(this).apply {
            setTitle("Mohon tunggu")
            setMessage("Sedang mengirim data...")
            setCancelable(false)
        }

        fetchToken()
        setupKeteranganDropdown()
        setInitLayout()

        requestPermissions.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )

        setUploadData()
    }

    private fun fetchToken() {
        lifecycleScope.launch {
            val user = dataStoreManager.getUserData()
            user?.let {
                userToken = it.token
                userNama = it.name
                binding.inputNama.setText(it.name)
                binding.inputNama.isEnabled = false

                if (it.token == null) {
                    Toast.makeText(this@AbsenPerizinanActivity, "Anda belum login!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setCurrentLocation() {
        if (!hasLocationPermission()) return

        val fused = LocationServices.getFusedLocationProviderClient(this)
        fused.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) handleLocation(loc)
                else {
                    val cts = CancellationTokenSource()
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { freshLoc -> freshLoc?.let { handleLocation(it) } ?: run {
                            Toast.makeText(this, "Lokasi tidak tersedia. Periksa GPS!", Toast.LENGTH_SHORT).show()
                        }}
                        .addOnFailureListener { e -> Toast.makeText(this, "Gagal ambil lokasi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show() }
                }
            }
            .addOnFailureListener { e -> Toast.makeText(this, "Error lokasi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show() }
    }

    private fun handleLocation(location: android.location.Location) {
        strCurrentLatitude = location.latitude
        strCurrentLongitude = location.longitude

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@AbsenPerizinanActivity, Locale.getDefault())
                val list = geocoder.getFromLocation(strCurrentLatitude, strCurrentLongitude, 1)
                val address = if (!list.isNullOrEmpty()) list[0].getAddressLine(0) else "$strCurrentLatitude, $strCurrentLongitude"
                withContext(Dispatchers.Main) {
                    strCurrentLocation = address
                    binding.inputLokasi.setText(strCurrentLocation)
                    binding.inputLokasi.isEnabled = false
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    strCurrentLocation = "$strCurrentLatitude, $strCurrentLongitude"
                    binding.inputLokasi.setText(strCurrentLocation)
                    binding.inputLokasi.isEnabled = false
                }
            }
        }
    }

    private fun setInitLayout() {
        strTitle = intent.getStringExtra(DATA_TITLE) ?: ""
        binding.tvTitle.text = strTitle

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setCurrentDateTime()

        binding.layoutImage.setOnClickListener {
            if (hasCameraPermission()) openCamera() else requestPermissions.launch(arrayOf(Manifest.permission.CAMERA))
        }

        binding.btnUploadLampiran.setOnClickListener { pickFileLauncher.launch("*/*") }
    }

    private fun setCurrentDateTime() {
        val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())
        binding.inputTanggal.setText(sdf.format(Date()))
        binding.inputTanggal.isEnabled = false
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            cameraUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)
            takePicture.launch(cameraUri)
        } catch (ex: Exception) {
            Toast.makeText(this, "Gagal membuka kamera: ${ex.message}", Toast.LENGTH_SHORT).show()
            Log.e("CameraError", "${ex.message}", ex)
        }
    }

    private fun setupKeteranganDropdown() {
        val keteranganList = listOf("Cuti", "Dinas", "Sakit")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, keteranganList)
        binding.inputketerangan.setAdapter(adapter)
        binding.inputketerangan.keyListener = null
    }

    private fun setUploadData() {
        binding.btnAbsen.setOnClickListener {
            val strNama = binding.inputNama.text.toString().trim()
            val strTanggal = binding.inputTanggal.text.toString().trim()
            val strLokasi = binding.inputLokasi.text.toString().trim()
            val strKeterangan = binding.inputketerangan.text.toString().trim()

            if (strNama.isEmpty() || strTanggal.isEmpty() || strLokasi.isEmpty() || strKeterangan.isEmpty()) {
                Toast.makeText(this, "Semua data harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userToken.isNullOrEmpty()) {
                Toast.makeText(this, "Anda belum login!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendAbsenToApi("Bearer $userToken")
        }
    }

    private fun sendAbsenToApi(token: String) {
        progressDialog.show()

        val namaPart = binding.inputNama.text.toString().trim().toRequestBody("text/plain".toMediaType())
        val lokasiPart = binding.inputLokasi.text.toString().trim().toRequestBody("text/plain".toMediaType())
        val jenisIzin = binding.inputketerangan.text.toString().trim().lowercase()

        if (jenisIzin.isEmpty()) {
            Toast.makeText(this, "Jenis izin wajib diisi!", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            return
        }

        val jenisIzinPart = jenisIzin.toRequestBody("text/plain".toMediaType())

        // Validasi foto selfie
        val fileGambar = File(strFilePath)
        if (!fileGambar.exists()) {
            Toast.makeText(this, "Foto selfie tidak ditemukan!", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            return
        }
        if (fileGambar.length() > 5 * 1024 * 1024) {
            Toast.makeText(this, "Ukuran foto tidak boleh lebih dari 5MB", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            return
        }
        val gambarPart = MultipartBody.Part.createFormData(
            "gambar",
            fileGambar.name,
            fileGambar.asRequestBody("image/*".toMediaType())
        )

        // Validasi file lampiran
        val fileBukti = File(strLampiranPath)
        if (!fileBukti.exists()) {
            Toast.makeText(this, "Lampiran tidak ditemukan!", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            return
        }
        if (fileBukti.length() > 5 * 1024 * 1024) {
            Toast.makeText(this, "Ukuran file lampiran tidak boleh lebih dari 5MB", Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
            return
        }
        val mimeTypeBukti = when {
            strLampiranPath.endsWith("pdf", true) -> "application/pdf"
            strLampiranPath.endsWith("doc", true) -> "application/msword"
            strLampiranPath.endsWith("docx", true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }
        val buktiPart = MultipartBody.Part.createFormData(
            "bukti",
            fileBukti.name,
            fileBukti.asRequestBody(mimeTypeBukti.toMediaType())
        )

        // Panggil API
        ApiClient.instance.submitIzin(token, namaPart, lokasiPart, jenisIzinPart, gambarPart, buktiPart)
            .enqueue(object : Callback<IzinResponse> {
                override fun onResponse(call: Call<IzinResponse>, response: Response<IzinResponse>) {
                    progressDialog.dismiss()
                    if (response.isSuccessful && response.body() != null) {
                        val izinResponse = response.body()!!
                        Toast.makeText(this@AbsenPerizinanActivity, izinResponse.message, Toast.LENGTH_LONG).show()
                        if (izinResponse.status) finish()
                    } else {
                        Toast.makeText(this@AbsenPerizinanActivity, "Gagal mengirim izin (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<IzinResponse>, t: Throwable) {
                    progressDialog.dismiss()
                    Toast.makeText(this@AbsenPerizinanActivity, "Error: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            })
    }


    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            strFilePath = absolutePath
            strImageName = name
            strTimeStamp = timeStamp
        }
    }

    private fun convertImage(imageFilePath: String?) {
        if (imageFilePath == null) return
        val imageFile = File(imageFilePath)
        if (!imageFile.exists()) return

        val options = BitmapFactory.Options()
        var bitmapImage = BitmapFactory.decodeFile(imageFilePath, options)

        try {
            val exif = androidx.exifinterface.media.ExifInterface(imageFile.absolutePath)
            val matrix = Matrix()
            when (exif.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, 0)) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }
            bitmapImage = Bitmap.createBitmap(bitmapImage, 0, 0, bitmapImage.width, bitmapImage.height, matrix, true)
        } catch (e: IOException) { e.printStackTrace() }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmapImage, 512, (bitmapImage.height * (512.0 / bitmapImage.width)).toInt(), true)
        Glide.with(this).load(scaledBitmap).placeholder(R.drawable.ic_photo_camera).into(binding.imageSelfie)
        strBase64Photo = bitmapToBase64(scaledBitmap)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    companion object { const val DATA_TITLE = "TITLE" }
}
