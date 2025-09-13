package com.example.siaga.view.absen

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import com.example.siaga.databinding.ActivityAbsenMasukBinding
import com.example.siaga.model.AbsensiResponse
import com.example.siaga.view.login.LoginActivity
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

class AbsenMasukActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAbsenMasukBinding
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var progressDialog: ProgressDialog
    private lateinit var cameraUri: Uri

    private var strCurrentLatitude = 0.0
    private var strCurrentLongitude = 0.0
    private var strFilePath: String = ""
    private var strBase64Photo: String = ""
    private var strCurrentLocation: String = ""
    private var strTitle: String = ""
    private var strTimeStamp: String = ""
    private var strImageName: String = ""

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

                convertImage(strFilePath)
            } else {
                Toast.makeText(this, "Pengambilan foto dibatalkan/gagal", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAbsenMasukBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataStoreManager = DataStoreManager(this)
        fetchToken()

        initLoading()
        setInitLayout()

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
            setMessage("Sedang mengirim absen masuk...")
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
                Log.d("USER_DATA", "Token: ${it.token}, Name: ${it.name}, Nama: ${it.nama}, Email: ${it.email}")
                userToken = it.token

                // Gunakan nama dengan fallback
//                userNama = if (it.nama.isNotEmpty()) it.nama else it.name
                userNama = when {
                    !it.nama.isNullOrEmpty() -> it.nama
                    !it.name.isNullOrEmpty() -> it.name
                    else -> "User" // fallback
                }

                binding.inputNama.setText(userNama)
                binding.inputNama.isEnabled = false

                if (it.token.isEmpty()) {
                    Toast.makeText(this@AbsenMasukActivity, "Sesi login habis, silakan login ulang!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@AbsenMasukActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }


    // === Lokasi ===
    @SuppressLint("MissingPermission")
    private fun setCurrentLocation() {
        if (!hasLocationPermission()) return
        if (!isGpsEnabled()) {
            Toast.makeText(this, "Harap aktifkan GPS!", Toast.LENGTH_SHORT).show()
            return
        }
        showLoading()

        val fused = LocationServices.getFusedLocationProviderClient(this)
        fused.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                handleLocation(loc)
                hideLoading()
            } else {
                val cts = CancellationTokenSource()
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { freshLoc ->
                        hideLoading()
                        if (freshLoc != null) {
                            handleLocation(freshLoc)
                        } else {
                            Toast.makeText(this, "Lokasi tidak tersedia. Periksa GPS!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        hideLoading()
                        Toast.makeText(this, "Gagal ambil lokasi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener { e ->
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
                    val geocoder = Geocoder(this@AbsenMasukActivity, Locale.getDefault())
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

    private fun isGpsEnabled(): Boolean {
        val lm = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
        return lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
    }

    private fun setInitLayout() {
        strTitle = intent.getStringExtra("TITLE") ?: "Absen Masuk"
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

            if (strNama.isEmpty() || strTanggal.isEmpty()) {
                Toast.makeText(this, "Semua data harus diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener

            }
            if (strFilePath.isEmpty() || !File(strFilePath).exists()) {
                Toast.makeText(this, "Foto wajib diambil!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userToken.isNullOrEmpty()) {
                Toast.makeText(this, "Anda belum login!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener

            }

            sendAbsenToApi("Bearer $userToken", strNama)
        }
    }


    private fun sendAbsenToApi(token: String, nama: String) {
        val lokasi = binding.inputLokasi.text.toString().trim()

        val file = File(strFilePath)
        if (!file.exists()) {
            Toast.makeText(this, "File foto tidak ditemukan!", Toast.LENGTH_SHORT).show()
            return
        }

        val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val gambarPart = MultipartBody.Part.createFormData("gambar", file.name, reqFile)

        val namaBody = nama.toRequestBody("text/plain".toMediaTypeOrNull())
        val lokasiBody = lokasi.toRequestBody("text/plain".toMediaTypeOrNull())

        showLoading()

        ApiClient.instance.absenMasuk(token, namaBody, gambarPart, lokasiBody)
            .enqueue(object : Callback<AbsensiResponse> {
                override fun onResponse(
                    call: Call<AbsensiResponse>,
                    response: Response<AbsensiResponse>
                ) {
                    hideLoading()
                    if (response.isSuccessful) {
                        val body = response.body()
                        Toast.makeText(
                            this@AbsenMasukActivity,
                            "Absen sukses: ${body?.message ?: "Berhasil"}",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        hideLoading()
                        try {
                            val errorBody = response.errorBody()?.string()
                            val msg = if (!errorBody.isNullOrEmpty()) {
                                val jsonObj = org.json.JSONObject(errorBody)
                                jsonObj.optString("message", "Terjadi kesalahan (${response.code()})")
                            } else {
                                "Terjadi kesalahan (${response.code()})"
                            }

                            if (msg.contains("sudah absen", ignoreCase = true)) {
                                // 🚨 Tampilkan AlertDialog khusus
                                AlertDialog.Builder(this@AbsenMasukActivity)
                                    .setTitle("Peringatan")
                                    .setMessage(msg)
                                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                                    .show()
                            } else {
                                Toast.makeText(this@AbsenMasukActivity, msg, Toast.LENGTH_LONG).show()
                            }

                            Log.e("ABSEN_API", "Code: ${response.code()} - Msg: $msg")
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@AbsenMasukActivity,
                                "Error ${response.code()}",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("ABSEN_API", "Parsing error body gagal", e)
                        }
                    }
                }

                override fun onFailure(call: Call<AbsensiResponse>, t: Throwable) {
                    hideLoading()
                    Toast.makeText(this@AbsenMasukActivity, "Gagal: ${t.message}", Toast.LENGTH_LONG).show()
                    Log.e("ABSEN_API", "Failure: ${t.message}", t)
                }
            })
    }


    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            strFilePath = absolutePath
            strImageName = name
            strTimeStamp = timeStamp
        }
    }

    private fun convertImage(imageFilePath: String?) {
        if (imageFilePath == null) return
        val imageFile = File(imageFilePath)
        if (!imageFile.exists()) return

        val bitmap = BitmapFactory.decodeFile(imageFilePath)
        strBase64Photo = bitmapToBase64(bitmap)

        Glide.with(this)
            .load(bitmap)
            .placeholder(R.drawable.ic_photo_camera)
            .into(binding.imageSelfie)
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
