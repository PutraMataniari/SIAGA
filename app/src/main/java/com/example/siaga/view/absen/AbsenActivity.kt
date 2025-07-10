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
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.siaga.R
import com.example.siaga.databinding.ActivityAbsenBinding
import com.example.siaga.view.utils.BitmapManager.bitmapToBase64
import com.example.siaga.view.viewmodel.AbsenViewModel
import com.google.android.gms.location.LocationServices
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.all
//import com.example.siaga.BuildConfig

class AbsenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAbsenBinding
    private var strCurrentLatitude = 0.0
    private var strCurrentLongitude = 0.0
    private var strFilePath: String = ""
    private lateinit var imageFilename: File
    private lateinit var strBase64Photo: String
    private lateinit var strCurrentLocation: String
    private lateinit var strTitle: String
    private lateinit var strTimeStamp: String
    private lateinit var strImageName: String
    private lateinit var absenViewModel: AbsenViewModel
    private lateinit var progressDialog: ProgressDialog
    private var cameraUri: Uri? = null

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                openCamera()
            } else {
                Toast.makeText(
                    this@AbsenActivity,
                    "Izin diperlukan untuk menggunakan fitur ini",
                    Toast.LENGTH_SHORT
                ).show()
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

        setInitLayout()
        if (checkLocationPermission()) {
            setCurrentLocation()
        }
        setUploadData()
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
                        val geocoder = Geocoder(this@AbsenActivity, Locale.getDefault())
                        try {
                            val addressList =
                                geocoder.getFromLocation(strCurrentLatitude, strCurrentLongitude, 1)
                            if (addressList != null && addressList.isNotEmpty()) {
                                strCurrentLocation = addressList[0].getAddressLine(0)
                                binding.inputLokasi.setText(strCurrentLocation)
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@AbsenActivity,
                            "Ups, gagal mendapatkan lokasi. Silahkan periksa GPS atau koneksi internet Anda!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@AbsenActivity,
                        "Error: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
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

        absenViewModel = ViewModelProvider(this)[AbsenViewModel::class.java]

        binding.inputTanggal.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this@AbsenActivity,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    val strFormatDefault = "dd MMMM yyyy HH:mm"
                    val simpleDateFormat = SimpleDateFormat(strFormatDefault, Locale.getDefault())
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
            cameraUri = FileProvider.getUriForFile(
                this,
                "${this.packageName}.provider",
                photoFile
            )

            // Perbaikan utama: cek apakah cameraUri tidak null sebelum di-launch
            cameraUri?.let { uri ->
                takePicture.launch(uri)
            } ?: run {
                Toast.makeText(
                    this@AbsenActivity,
                    "Gagal membuat URI untuk kamera",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (ex: IOException) {
            Toast.makeText(
                this@AbsenActivity,
                "Ups, gagal membuka kamera",
                Toast.LENGTH_SHORT
            ).show()
            Log.e("CameraError", ex.message, ex)
        } catch (ex: Exception) {
            Toast.makeText(
                this@AbsenActivity,
                "Error: ${ex.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
            Log.e("CameraError", ex.message, ex)
        }
    }

    private fun setUploadData() {
        binding.btnAbsen.setOnClickListener {
            val strNama = binding.inputNama.text.toString()
            val strNip = binding.inputNip.text.toString()
            val strTanggal = binding.inputTanggal.text.toString()
            val strKeterangan = binding.inputKeterangan.text.toString()

            if (strFilePath.isEmpty() || strNama.isEmpty() || strNip.isEmpty()
                || strCurrentLocation.isEmpty() || strTanggal.isEmpty() || strKeterangan.isEmpty()
            ) {
                Toast.makeText(
                    this@AbsenActivity,
                    "Data tidak boleh ada yang kosong!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                absenViewModel.addDataAbsen(
                    strBase64Photo,
                    strNama,
                    strNip,
                    strTanggal,
                    strCurrentLocation,
                    strKeterangan
                )
                Toast.makeText(
                    this@AbsenActivity,
                    "Laporan Anda terkirim, tunggu info selanjutnya ya!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
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
                val exifInterface = ExifInterface(imageFile.absolutePath)
                val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }

                bitmapImage = Bitmap.createBitmap(
                    bitmapImage,
                    0,
                    0,
                    bitmapImage.width,
                    bitmapImage.height,
                    matrix,
                    true
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (bitmapImage != null) {
                val resizeImage = (bitmapImage.height * (512.0 / bitmapImage.width)).toInt()
                val scaledBitmap = Bitmap.createScaledBitmap(bitmapImage, 512, resizeImage, true)
                Glide.with(this)
                    .load(scaledBitmap)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_photo_camera)
                    .into(binding.imageSelfie)
                strBase64Photo = bitmapToBase64(scaledBitmap)
            } else {
                Toast.makeText(
                    this@AbsenActivity,
                    "Ups, foto kamu belum ada!",
                    Toast.LENGTH_LONG
                ).show()
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