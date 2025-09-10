    // file: com/example/siaga/datastore/DataStoreManager.kt

    package com.example.siaga.datastore

    import android.content.Context
    import android.util.Log
    import androidx.datastore.core.DataStore
    import androidx.datastore.preferences.core.*
    import androidx.datastore.preferences.preferencesDataStore
    import kotlinx.coroutines.flow.Flow
    import kotlinx.coroutines.flow.first
    import kotlinx.coroutines.flow.map

    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

    class DataStoreManager(private val context: Context) {

        companion object {
            private val AUTH_TOKEN = stringPreferencesKey("auth_token")
            private val USER_NAME = stringPreferencesKey("user_name")
            private val USER_NAMA = stringPreferencesKey("user_nama")
            private val USER_EMAIL = stringPreferencesKey("user_email")
            private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        }

        val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
            prefs[IS_LOGGED_IN] ?: false
        }

        val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
            prefs[AUTH_TOKEN]
        }

        suspend fun saveUserData(token: String, name: String, email: String, nama:String?) {
            context.dataStore.edit { prefs ->
                prefs[AUTH_TOKEN] = token
                prefs[USER_NAME] = name
                prefs[USER_NAMA] = nama ?:name
                prefs[USER_EMAIL] = email
                prefs[IS_LOGGED_IN] = true
            }
            Log.d("DATASTORE_SAVE", "token=$token, name=$name, nama=$nama, email=$email")
        }

        suspend fun getUserData(): UserData? {
            val prefs = context.dataStore.data.first()
            Log.d("DATASTORE_READ", prefs.toString())
            return if (prefs[IS_LOGGED_IN] == true) {
                UserData(
                    token = prefs[AUTH_TOKEN] ?: "",
                    name = prefs[USER_NAME] ?: "",
                    email = prefs[USER_EMAIL] ?: "",
                    nama = prefs[USER_NAMA] ?: ""
                )
            } else null
        }

        suspend fun getToken(): String? {
            val prefs = context.dataStore.data.first()
            return prefs[AUTH_TOKEN]
        }

        suspend fun clear() {
            context.dataStore.edit { prefs -> prefs.clear() }
        }

        suspend fun logout() = clear() // logout tinggal panggil clear
    }

    data class UserData(
        val token: String,
        val name: String,
        val email: String,
        val nama: String
    )