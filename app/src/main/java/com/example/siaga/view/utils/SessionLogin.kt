//package com.example.siaga.view.utils
//
//import android.content.Context
//import android.content.Intent
//import android.content.SharedPreferences
//import com.example.siaga.view.login.LoginActivity
//
//class SessionLogin(private val context: Context) {
//
//    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//    private val editor: SharedPreferences.Editor = sharedPreferences.edit()
//
//    /**
//     * Membuat sesi login dengan menyimpan status dan data pengguna
//     */
//    fun createLoginSession(nama: String, nip: String, password: String) {
//        editor.putBoolean(IS_LOGIN, true)
//        editor.putString(KEY_NAMA, nama)
////        editor.putString(KEY_NIP, nip)
//        editor.putString(KEY_PASSWORD, password)
//        editor.apply() // Gunakan apply() untuk async, lebih aman
//    }
//
//    /**
//     * Cek apakah user sudah login. Jika belum, arahkan ke LoginActivity
//     */
//    fun checkLogin() {
//        if (!isLoggedIn()) {
//            val intent = Intent(context, LoginActivity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            }
//            context.startActivity(intent)
//        }
//    }
//
//    /**
//     * Logout: Hapus sesi dan arahkan ke LoginActivity
//     */
//    fun logout() {
//        editor.clear()
//        editor.apply() // Gunakan apply() untuk async
//
//        val intent = Intent(context, LoginActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//        context.startActivity(intent)
//    }
//
//    /**
//     * Cek apakah user sedang login
//     */
//    fun isLoggedIn(): Boolean {
//        return sharedPreferences.getBoolean(IS_LOGIN, false)
//    }
//
//    /**
//     * Ambil data pengguna (opsional, bisa digunakan di MainActivity)
//     */
//    fun getUserNama(): String? = sharedPreferences.getString(KEY_NAMA, null)
////    fun getUserNip(): String? = sharedPreferences.getString(KEY_NIP, null)
//    fun getUserPassword(): String? = sharedPreferences.getString(KEY_PASSWORD, null)
//
//    companion object {
//        private const val PREF_NAME = "AbsensiPref"
//        private const val IS_LOGIN = "IsLoggedIn"
//        const val KEY_NAMA = "NAMA"
////        const val KEY_NIP = "NIP"
//        const val KEY_PASSWORD = "PASSWORD"
//    }
//}