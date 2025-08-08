package com.example.siaga.view.profil

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.siaga.R
import com.example.siaga.databinding.FragmentEditProfilPhotoBinding
import com.example.siaga.view.viewmodel.ProfilViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EditProfilPhotoBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentEditProfilPhotoBinding
    private val viewModel: ProfilViewModel by viewModels({ requireActivity() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditProfilPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCamera.setOnClickListener {
            pickImageFromCamera()
        }

        binding.btnGallery.setOnClickListener {
            pickImageFromGallery()
        }
    }

    private fun pickImageFromCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            resultLauncher.launch(intent)
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val data: Intent? = result.data
                val selectedImageUri: Uri? = data?.data

                // Update ViewModel
                viewModel.updateProfilImage(selectedImageUri)

                // Tutup bottom sheet
                dismiss()
            }
        }
}