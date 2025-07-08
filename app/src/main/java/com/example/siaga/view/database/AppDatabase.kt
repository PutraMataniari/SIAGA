package com.example.siaga.view.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.siaga.view.database.Dao.DatabaseDao
import com.example.siaga.view.model.ModelDatabase

@Database(entities = [ModelDatabase::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun databaseDao(): DatabaseDao?
}