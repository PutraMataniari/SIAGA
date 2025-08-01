package com.example.siaga.view.database

import android.content.Context
import androidx.room.Room
import com.example.siaga.view.database.AppDatabase

class DatabaseClient private constructor(context: Context) {

    var appDatabase: AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "absensi_db")
        .fallbackToDestructiveMigration()
        .build()

    companion object {
        private var mInstance: DatabaseClient? = null
        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): DatabaseClient? {
            if (mInstance == null) {
                mInstance = DatabaseClient(context)
            }
            return mInstance
        }
    }

}

