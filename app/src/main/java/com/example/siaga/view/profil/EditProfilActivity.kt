package com.example.siaga.view.profil

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.siaga.R
import com.example.siaga.api.ApiClient
import com.example.siaga.databinding.ActivityEditProfilBinding
import com.example.siaga.datastore.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class EditProfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfilBinding
    private lateinit var dataStore: DataStoreManager
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null
    private var tanggalLahirLama: String? = null
    private val calendar = Calendar.getInstance()

    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataStore = DataStoreManager(this)

        // Ambil data profil saat activity dibuka
        loadProfil()
        setupBackButton()



        // Setup camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val photo: Bitmap? = result.data?.extras?.get("data") as Bitmap?
                binding.profileImage2.setImageBitmap(photo)
            }
        }

        // Setup gallery launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                binding.profileImage2.setImageURI(selectedImageUri)
            }
        }

        // Klik foto profil → pilih kamera atau galeri
        binding.profileImage2.setOnClickListener { showImagePickerDialog() }

        // Klik tanggal lahir → buka date picker
        binding.inputTanggalLhr1.setOnClickListener { showDatePicker() }

        // Klik tombol update profil
        binding.btnUpdate.setOnClickListener { showSaveConfirmationDialog() }
    }

    /**
     * Tombol kembali
     */
    private fun setupBackButton() {
        binding.icback.setOnClickListener { finish() }
    }

    @SuppressLint("MissingInflatedId")
    private fun showLoadingDialog(message: String) {
        if (loadingDialog == null) {
            val builder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.dialog_loading, null)

            val textMessage = view.findViewById<TextView>(R.id.textMessage)
            textMessage.text = message

            builder.setView(view)
            builder.setCancelable(false)
            loadingDialog = builder.create()
        } else {
            val textMessage = loadingDialog?.findViewById<TextView>(R.id.textMessage)
            textMessage?.text = message
        }
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
    }

    /** ==================== LOAD PROFIL ==================== */
    private fun loadProfil() {
        showLoadingDialog("Memuat data profil...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = dataStore.getUserData()?.token ?: ""
                val profil = ApiClient.instance.getProfil("Bearer $token")

                withContext(Dispatchers.Main) {
                    hideLoadingDialog()
                    profil.data?.pegawai?.let { pegawai ->
                        binding.inputNama1.setText(pegawai.nama)
                        binding.inputNip1.setText(pegawai.nip)
                        binding.inputJabatan1.setText(pegawai.jabatan)
                        binding.inputBagian1.setText(pegawai.bagian)
                        binding.inputSubBagian1.setText(pegawai.sub_bagian ?: "")
                        binding.inputNoTelp1.setText(pegawai.no_telp)
                        binding.inputEmail1.setText(pegawai.email)

                        // Simpan tanggal lahir asli (format dari server yyyy-MM-dd)
                        pegawai.tanggal_lahir?.let { tgl ->
                            tanggalLahirLama = tgl
                            try {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                                val date = inputFormat.parse(tgl)
                                binding.inputTanggalLhr1.setText(outputFormat.format(date!!))
                            } catch (e: Exception) {
                                binding.inputTanggalLhr1.setText("")
                            }
                        }

                        if (!pegawai.foto_profil.isNullOrEmpty()) {
                            Glide.with(this@EditProfilActivity)
                                .load(pegawai.foto_profil)
                                .into(binding.profileImage2)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoadingDialog()
                    Toast.makeText(
                        this@EditProfilActivity,
                        "Gagal memuat profil: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /** ==================== IMAGE PICKER ==================== */
    private fun showImagePickerDialog() {
        val options = arrayOf("Kamera", "Galeri")
        AlertDialog.Builder(this)
            .setTitle("Ganti Foto Profil")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    /** ==================== DATE PICKER ==================== */
    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)

            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            binding.inputTanggalLhr1.setText(sdf.format(calendar.time))
        }

        // Gunakan tanggal lama dari server jika ada
        val dateParts = tanggalLahirLama?.split("-")
        if (dateParts?.size == 3) {
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt() - 1
            val day = dateParts[2].toInt()
            DatePickerDialog(this, dateSetListener, year, month, day).show()
        } else {
            DatePickerDialog(
                this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    /** ==================== CONFIRMATION DIALOG ==================== */
    private fun showSaveConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Simpan Perubahan")
            .setMessage("Apakah Anda ingin menyimpan perubahan profil?")
            .setPositiveButton("Ya") { _, _ -> updateProfil() }
            .setNegativeButton("Tidak") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /** ==================== UPDATE PROFIL ==================== */
    private fun updateProfil() {
        showLoadingDialog("Sedang mengupdate data...")

        binding.progressBar.visibility = View.VISIBLE

        val nama = binding.inputNama1.text.toString()
        val nip = binding.inputNip1.text.toString()
        val jabatan = binding.inputJabatan1.text.toString()
        val bagian = binding.inputBagian1.text.toString()
        val subBagian = binding.inputSubBagian1.text.toString()
        val noTelp = binding.inputNoTelp1.text.toString()
        val email = binding.inputEmail1.text.toString()
        val tanggalLahirInput = binding.inputTanggalLhr1.text.toString()

        // Pastikan format untuk API tetap yyyy-MM-dd
        val tanggalFormatted: String? = if (tanggalLahirInput.isNotEmpty()) {
            try {
                val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                // Cek apakah user mengubah tanggal
                val tglLamaFormatted = try {
                    val dfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dfOut = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    dfOut.format(dfIn.parse(tanggalLahirLama!!)!!)
                } catch (e: Exception) {
                    ""
                }

                if (tanggalLahirInput == tglLamaFormatted) {
                    tanggalLahirLama // tidak berubah → kirim data lama
                } else {
                    val date = inputFormat.parse(tanggalLahirInput)
                    outputFormat.format(date!!)
                }
            } catch (e: Exception) {
                tanggalLahirLama
            }
        } else {
            tanggalLahirLama
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = dataStore.getUserData()?.token ?: ""

                val namaRB = createPartFromString(nama)
                val nipRB = createPartFromString(nip)
                val jabatanRB = createPartFromString(jabatan)
                val bagianRB = createPartFromString(bagian)
                val subbagianRB = if (subBagian.isNotEmpty()) createPartFromString(subBagian) else null
                val noTelpRB = createPartFromString(noTelp)
                val emailRB = createPartFromString(email)
                val tanggalRB = tanggalFormatted?.let { createPartFromString(it) }

                var fotoPart: MultipartBody.Part? = null
                selectedImageUri?.let { uri ->
                    val file = uriToFile(uri)
                    val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
                    fotoPart = MultipartBody.Part.createFormData("foto_profil", file.name, requestFile)
                }

                val response = ApiClient.instance.updateProfil(
                    "Bearer $token",
                    namaRB, nipRB, jabatanRB, bagianRB,
                    subbagianRB, noTelpRB, emailRB, tanggalRB,
                    fotoPart
                )

                withContext(Dispatchers.Main) {
                    hideLoadingDialog()
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@EditProfilActivity, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()

                    val resultIntent = Intent()
                    resultIntent.putExtra("refreshProfile", true)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoadingDialog()
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@EditProfilActivity, "Gagal update: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun createPartFromString(value: String): RequestBody {
        return RequestBody.create("text/plain".toMediaTypeOrNull(), value)
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload", ".jpg", cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return tempFile
    }
}
