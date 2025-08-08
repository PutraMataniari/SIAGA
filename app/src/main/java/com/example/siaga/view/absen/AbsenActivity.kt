package com.example.siaga.view.absen

import android.Manifest
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Geocoder
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.siaga.R
import com.example.siaga.api.ApiClient
import com.example.siaga.api.AbsenRequest
//import com.example.siaga.api.ModelDatabase
import com.example.siaga.datastore.DataStoreManager
import com.example.siaga.databinding.ActivityAbsenBinding
import com.example.siaga.view.model.HistoryResponse
import com.example.siaga.view.utils.BitmapManager.bitmapToBase64
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AbsenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAbsenBinding
    private var strCurrentLatitude = 0.0
    private var strCurrentLongitude = 0.0
    private var strFilePath: String = ""
    private lateinit var strBase64Photo: String
    private lateinit var strCurrentLocation: String
    private lateinit var strTitle: String
    private lateinit var strTimeStamp: String
    private lateinit var strImageName: String
    private lateinit var progressDialog: ProgressDialog
    private var cameraUri: Uri? = null
    private lateinit var dataStoreManager: DataStoreManager

    // 🔹 Variabel untuk menyimpan token
    private var userToken: String? = null

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Izin diperlukan untuk menggunakan fitur ini", Toast.LENGTH_SHORT).show()
            }
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraUri != null) {
                convertImage(cameraUri?.path)
            }
        }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAbsenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataStoreManager = DataStoreManager(this)

        // ✅ Ambil token menggunakan lifecycleScope
        fetchToken()

        setInitLayout()
        if (checkLocationPermission()) {
            setCurrentLocation()
        }
        setUploadData()
    }

    private fun fetchToken() {
        // ✅ Gunakan lifecycleScope untuk collect Flow
        lifecycleScope.launch {
            dataStoreManager.tokenFlow.collect { token ->
                userToken = token
                Log.d("AbsenActivity", "Token diterima: $token")
                if (token == null) {
                    Toast.makeText(this@AbsenActivity, "Anda belum login!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun setCurrentLocation() {
        progressDialog.show()
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    progressDialog.dismiss()
                    if (location != null) {
                        strCurrentLatitude = location.latitude
                        strCurrentLongitude = location.longitude
                        val geocoder = Geocoder(this, Locale.getDefault())
                        try {
                            val addressList = geocoder.getFromLocation(strCurrentLatitude, strCurrentLongitude, 1)
                            if (addressList != null && addressList.isNotEmpty()) {
                                strCurrentLocation = addressList[0].getAddressLine(0)
                                binding.inputLokasi.setText(strCurrentLocation)
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    } else {
                        Toast.makeText(this, "Gagal mendapatkan lokasi. Periksa GPS!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun setInitLayout() {
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Mohon tunggu")
        progressDialog.setMessage("Mengambil lokasi...")
        progressDialog.setCancelable(false)

        strTitle = intent.getStringExtra(DATA_TITLE) ?: ""
        binding.tvTitle.text = strTitle

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.inputTanggal.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    val simpleDateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault())
                    binding.inputTanggal.setText(simpleDateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.layoutImage.setOnClickListener {
            requestPermissions.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            cameraUri = FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)
            cameraUri?.let { takePicture.launch(it) }
        } catch (ex: Exception) {
            Toast.makeText(this, "Gagal membuka kamera: ${ex.message}", Toast.LENGTH_SHORT).show()
            Log.e("CameraError", ex.message, ex)
        }
    }

    private fun setUploadData() {
        binding.btnAbsen.setOnClickListener {
            val strNama = binding.inputNama.text.toString()
            val strTanggal = binding.inputTanggal.text.toString()
            val strLaporan = binding.inputlaporan.text.toString() // Pastikan ID-nya benar

            if (strFilePath.isEmpty() || strNama.isEmpty() ||
                strCurrentLocation.isEmpty() || strTanggal.isEmpty() || strLaporan.isEmpty()) {
                Toast.makeText(this, "Semua data harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userToken.isNullOrEmpty()) {
                Toast.makeText(this, "Anda belum login!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val token = "Bearer $userToken"

            val absenRequest = AbsenRequest(
                nama = strNama,
//                nip = "123456", // Tambahkan jika diperlukan
                tanggal = strTanggal,
                lokasi = strCurrentLocation,
                keterangan = strLaporan,
                gambar = strBase64Photo
            )

            sendAbsenToApi(token, absenRequest)
        }
    }

    private fun sendAbsenToApi(token: String, request: AbsenRequest) {
        val call = ApiClient.apiService.absenMasuk(token, request)
        call.enqueue(object : Callback<HistoryResponse> {
            override fun onResponse(
                call: Call<HistoryResponse>,
                response: Response<HistoryResponse>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AbsenActivity, "Absen berhasil dikirim!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AbsenActivity, "Gagal: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }


            override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                Toast.makeText(this@AbsenActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        strTimeStamp = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        strImageName = "IMG_$strTimeStamp"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(strImageName, ".jpg", storageDir).apply {
            strFilePath = absolutePath
        }
    }

    private fun convertImage(imageFilePath: String?) {
        if (imageFilePath == null) return
        val imageFile = File(imageFilePath)
        if (imageFile.exists()) {
            val options = BitmapFactory.Options()
            var bitmapImage = BitmapFactory.decodeFile(imageFilePath, options)
            try {
                val exif = ExifInterface(imageFile.absolutePath)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
                bitmapImage = Bitmap.createBitmap(bitmapImage, 0, 0, bitmapImage.width, bitmapImage.height, matrix, true)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if (bitmapImage != null) {
                val resizeHeight = (bitmapImage.height * (512.0 / bitmapImage.width)).toInt()
                val scaledBitmap = Bitmap.createScaledBitmap(bitmapImage, 512, resizeHeight, true)
                Glide.with(this)
                    .load(scaledBitmap)
                    .placeholder(R.drawable.ic_photo_camera)
                    .into(binding.imageSelfie)
                strBase64Photo = bitmapToBase64(scaledBitmap)
            }
        }
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