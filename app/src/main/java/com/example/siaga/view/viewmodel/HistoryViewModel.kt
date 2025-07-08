package com.example.siaga.view.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.siaga.view.database.Dao.DatabaseDao
import com.example.siaga.view.database.DatabaseClient
import com.example.siaga.view.model.ModelDatabase
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    var dataLaporan: LiveData<List<ModelDatabase>>
    private var databaseDao: DatabaseDao? = DatabaseClient.getInstance(application)?.appDatabase?.databaseDao()

    fun deleteDataById(uid: Int) {
        Completable.fromAction {
            databaseDao?.deleteHistoryById(uid)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe()
    }

    init {
        dataLaporan = databaseDao!!.getAllHistory()
    }
}