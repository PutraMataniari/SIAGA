package com.example.siaga.view.main

class MsyncFactory private constructor() {

    companion object {
        private var instance: MsyncFactory? = null

        @JvmStatic
        fun getInstance(): MsyncFactory {
            if (instance == null) {
                instance = MsyncFactory()
            }
            return instance!!
        }
    }

    // Tambahkan logika lain yang diperlukan
}
