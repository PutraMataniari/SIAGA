package com.example.siaga.view.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.siaga.databinding.ActivitySplashBinding
import com.example.siaga.view.login.LoginActivity
import com.example.siaga.view.profil.ProfilActivity

class SplashActivity : AppCompatActivity () {//Class utama untuk splash

    private lateinit var binding: ActivitySplashBinding //Binding untuk layout

    private val SPLASH_DURASI = 2000L // Durasi tampilan splash 2 detik (dalam milidetik)

    //Handler untuk mengatur delay
    private val handler = Handler(Looper.getMainLooper())
    //Runnable yang akan dijalankan setelah delay
    private val splashRunnable = Runnable {
        //Pindah ke LoginActivity setelah splash selesai
        val intent = Intent(this@SplashActivity, LoginActivity::class.java)
        startActivity(intent)
        finish() //Tutup splash activity agar tidak bisa kembali
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SplashActivity", "onCreate called")
        //Inisialisasi view binding
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root) //Set layout dari binding


        handler.postDelayed(splashRunnable, SPLASH_DURASI)
    }


    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(splashRunnable)
    }
}