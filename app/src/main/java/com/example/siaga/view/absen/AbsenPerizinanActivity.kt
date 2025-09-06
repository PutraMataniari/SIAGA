package com.example.siaga.view.absen

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.location.Geocoder
import androidx.exifinterface.media.ExifInterface
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
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
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

class AbsenPerizinanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAbsenPerizinanBinding

    private var strCurrentLatitude = 0.0
    private var strCurrentLongitude = 0.0
    private var strFilePath: String = ""
    private var strLampiranPath: String = ""
    private var strBase64Photo: String = ""
    private var strCurrentLocation: String = ""
    private var strTitle: String = ""
    private var strTimeStamp: String = ""
    private var strImageName: String = ""

    private lateinit var progressDialog: ProgressDialog
//    private var cameraUri: Uri? = null
    private lateinit var cameraUri: Uri
    private lateinit var dataStoreManager: DataStoreManager

    private var userToken: String? = null
    private var userNama: String? = null

    // === Helpers permission
    private fun hasCameraPermission(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    // Minta izin kamera + lokasi sekali jalan
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val allGranted = result.entries.all { it.value }
            if (!allGranted) {
                Toast.makeText(this, "Izin Kamera & Lokasi wajib diberikan!", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            // ✅ Panggil lokasi HANYA jika izin benar-benar ada (menghilangkan MissingPermission)
            if (hasLocationPermission()) setCurrentLocation()
        }

    // Ambil foto (menyimpan ke URI dari FileProvider)
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                //tampilkan langsung di ImageView dari URI
                Glide.with(this)
                    .load(cameraUri)
                    .placeholder(R.drawable.ic_photo_camera)
                    .into(binding.imageSelfie)

                //proses konversi ke Base64
                convertImage(cameraUri?.path)
            } else {
                Toast.makeText(this, "Pengambilan foto dibatalkan/gagal", Toast.LENGTH_SHORT).show()
            }
        }

    // Pilih lampiran via SAF (tidak butuh READ_EXTERNAL_STORAGE)
    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val file = uriToFile(it)
                if (file != null) {
                    strLampiranPath = file.absolutePath
                    val fileName = getFileNameFromUri(it) ?: file.name
                    binding.btnUploadLampiran.text = "File terpilih"
                    binding.textLampiran.text = fileName
                }else {
                    Toast.makeText(this, "Gagal membaca file lampiran", Toast.LENGTH_SHORT).show()
                }

            }
        }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name = "lampiran_unknown"
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx= it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = it.getString(idx)
                }
            }
        return name
    }

    private fun uriToFile(uri: Uri): File? {
        return try{
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = getFileNameFromUri(uri)
            val tempFile = File(cacheDir, fileName)
            inputStream?.use { input -> tempFile.outputStream().use { output ->
                input.copyTo(output)
            }}
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
        fetchToken()

        setupKeteranganDropdown()
        setInitLayout()

        // ✅ Cek & minta izin di awal
        requestPermissions.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setUploadData()

        //Dropdown Keterangan
        val keteranganList = listOf("Cuti", "Dinas", "Sakit")
        val keteranganAdapter = ArrayAdapter(this, R.layout.list_item_dropdown, keteranganList)
        binding.inputketerangan.setAdapter(keteranganAdapter)
    }

    private fun fetchToken() {
        lifecycleScope.launch {
            val user = DataStoreManager(this@AbsenPerizinanActivity).getUserData()
            user?.let {
                userToken =  it.token
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

    // === Lokasi ===
    @SuppressLint("MissingPermission") // aman: selalu dipanggil setelah hasLocationPermission() true
    private fun setCurrentLocation() {
        if (!hasLocationPermission()) return

        progressDialog.show()
        val fused = LocationServices.getFusedLocationProviderClient(this)

        // 1) Coba lastLocation dulu (cepat)
        fused.lastLocation
            .addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    handleLocation(loc)
                    progressDialog.dismiss()
                } else {
                    // 2) Fallback: getCurrentLocation (lebih akurat/terbaru)
                    val cts = CancellationTokenSource()
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { freshLoc ->
                            progressDialog.dismiss()
                            if (freshLoc != null) {
                                handleLocation(freshLoc)
                            } else {
                                Toast.makeText(this, "Lokasi tidak tersedia. Periksa GPS!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            progressDialog.dismiss()
                            Toast.makeText(this, "Gagal ambil lokasi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error lokasi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleLocation(location: Location) {
        strCurrentLatitude = location.latitude
        strCurrentLongitude = location.longitude

        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val list = geocoder.getFromLocation(strCurrentLatitude, strCurrentLongitude, 1)
            if (!list.isNullOrEmpty()) {
                strCurrentLocation = list[0].getAddressLine(0) ?: ""
                binding.inputLokasi.setText(strCurrentLocation)
                binding.inputLokasi.isEnabled = false
            } else {
                binding.inputLokasi.setText("${strCurrentLatitude}, ${strCurrentLongitude}")
                binding.inputLokasi.isEnabled = false
            }
        } catch (e: IOException) {
            binding.inputLokasi.setText("${strCurrentLatitude}, ${strCurrentLongitude}")
            binding.inputLokasi.isEnabled = false
        }
    }

    private fun setInitLayout() {
        progressDialog = ProgressDialog(this).apply {
            setTitle("Mohon tunggu")
            setMessage("Mengambil lokasi...")
            setCancelable(false)
        }

        strTitle = intent.getStringExtra(DATA_TITLE) ?: ""
        binding.tvTitle.text = strTitle

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Tanggal & waktu otomatis
        setCurrentDateTime()

        // Klik area selfie → pastikan izin kamera OK, lalu buka kamera
        binding.layoutImage.setOnClickListener {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestPermissions.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        // Pilih lampiran
        binding.btnUploadLampiran.setOnClickListener {
            pickFileLauncher.launch("*/*")
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

        // Agar user tidak bisa ketik manual
        binding.inputketerangan.keyListener = null
    }

    private fun setUploadData() {
        binding.btnAbsen.setOnClickListener {
            val strNama = binding.inputNama.text.toString().trim()
            val strTanggal = binding.inputTanggal.text.toString().trim()
            val strlokasi = binding.inputLokasi.text.toString().trim()
            val strKeterangan = binding.inputketerangan.text.toString().trim()

            if (strNama.isEmpty() || strTanggal.isEmpty() || strlokasi.isEmpty() || strKeterangan.isEmpty()) {
                Toast.makeText(this, "Semua data harus diisi!", Toast.LENGTH_SHORT).show()
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

    private fun sendAbsenToApi(token: String) {

        val nama = binding.inputNama.text.toString().trim()
        val lokasi = binding.inputLokasi.text.toString().trim()
        val jenisIzin = binding.inputketerangan.text.toString().trim()

        if (nama.isEmpty() || lokasi.isEmpty() || jenisIzin.isEmpty()) {
            Toast.makeText(this, "Semua data wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        if (strFilePath.isEmpty()) {
            Toast.makeText(this, "Foto wajib diambil!", Toast.LENGTH_SHORT).show()
            return
        }

        // Cek lampiran wajib
        if (strLampiranPath.isEmpty()) {
            Toast.makeText(this, "Lampiran wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        val namaPart = nama.toRequestBody("text/plain".toMediaType())
        val lokasiPart = lokasi.toRequestBody("text/plain".toMediaType())
        val jenisIzinPart = jenisIzin.lowercase().toRequestBody("text/plain".toMediaType())

        // Selfie wajib (ambil dari filePath terakhir)
        val fileGambar = File(strFilePath)
        val RequestGambar = fileGambar.asRequestBody("image/*".toMediaType())
        val gambarPart = MultipartBody.Part.createFormData("gambar", fileGambar.name, RequestGambar)

        // Lampiran wajib
            val fileBukti = File(strLampiranPath)
            val mimeTypeBukti = when {
                strLampiranPath.endsWith("pdf") -> "application/pdf"
                strLampiranPath.endsWith("doc") -> "application/msword"
                strLampiranPath.endsWith("docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                else -> "image/*"
            }
            if (!fileBukti.exists()) {
                Toast.makeText(this, "Lampiran wajib diisi!", Toast.LENGTH_SHORT).show()
                return
            }
            val requestBukti = fileBukti.asRequestBody(mimeTypeBukti.toMediaType())
            val buktiPart = MultipartBody.Part.createFormData("bukti", fileBukti.name, requestBukti)
            val buktiAsliPart = fileBukti.name.toRequestBody("text/plain".toMediaType())

        ApiClient.instance.Perizinan(
            token,
            namaPart,
            lokasiPart,
            jenisIzinPart,
            gambarPart,
            buktiPart,
            buktiAsliPart
            ).enqueue(object : Callback<IzinResponse> { override fun onResponse(call: Call<IzinResponse>, response: Response<IzinResponse>){
                    if (response.isSuccessful) {
                        Toast.makeText(this@AbsenPerizinanActivity, "Izin berhasil dikirim", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AbsenPerizinanActivity, "Gagal: ${response.message()}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<IzinResponse>, t: Throwable) {
                    Toast.makeText(this@AbsenPerizinanActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Nama file unik berdasarkan timestamp
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Buat file sementara di folder Pictures app
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg",               /* suffix */
            storageDir            /* directory */
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
            bitmapImage = Bitmap.createBitmap(
                bitmapImage, 0, 0,
                bitmapImage.width, bitmapImage.height, matrix, true
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val resizeHeight = (bitmapImage.height * (512.0 / bitmapImage.width)).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(bitmapImage, 512, resizeHeight, true)

        Glide.with(this)
            .load(scaledBitmap)
            .placeholder(R.drawable.ic_photo_camera)
            .into(binding.imageSelfie)

        strBase64Photo = bitmapToBase64(scaledBitmap)
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


