//package com.example.siaga.view.viewmodel
//
//import android.app.Application
//import androidx.lifecycle.AndroidViewModel
//import com.example.siaga.view.database.Dao.DatabaseDao
//import com.example.siaga.view.database.DatabaseClient.Companion.getInstance
//import com.example.siaga.view.model.ModelDatabase
//import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
//import io.reactivex.rxjava3.schedulers.Schedulers
//import io.reactivex.rxjava3.core.Completable
//
//
//class AbsenViewModel(application: Application) : AndroidViewModel(application) {
//    var databaseDao: DatabaseDao? = getInstance(application)?.appDatabase?.databaseDao()
//
//    fun addDataAbsen(
//        foto: String, nama: String, nip: String,
//        tanggal: String, lokasi: String, keterangan: String) {
//        Completable.fromAction {
//            val modelDatabase = ModelDatabase()
//            modelDatabase.fotoSelfie = foto
//            modelDatabase.nama = nama
//            modelDatabase.nip = nip
//            modelDatabase.tanggal = tanggal
//            modelDatabase.lokasi = lokasi
//            modelDatabase.keterangan = keterangan
//            databaseDao?.insertData(modelDatabase)
//        }
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe()
//    }
//
//}
//
//class Completable {
//    companion object
//
//}
