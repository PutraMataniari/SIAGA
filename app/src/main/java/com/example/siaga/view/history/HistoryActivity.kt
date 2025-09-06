package com.example.siaga.view.history

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.IOException
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.siaga.api.ApiClient
import com.example.siaga.api.ApiService
import com.example.siaga.databinding.ActivityHistoryBinding
import com.example.siaga.view.adapter.HistoryAdapter
import com.example.siaga.view.model.HistoryResponse
import com.example.siaga.datastore.DataStoreManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import retrofit2.Response
import retrofit2.HttpException

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var apiService: ApiService
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var dataStoreManager: DataStoreManager
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Toolbar
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }


        //RecyclerView
        historyAdapter = HistoryAdapter(mutableListOf())
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = historyAdapter

        apiService = ApiClient.instance
        dataStoreManager = DataStoreManager(this) // Inisialisasi

        setupToolbar()
        setupRecyclerView()
        loadHistoryData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(mutableListOf())
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadHistoryData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvHistory.visibility = View.GONE
        binding.tvNotFound.visibility = View.GONE

        scope.launch {
            try {
                val token = dataStoreManager.tokenFlow.first()

                if (token.isNullOrEmpty()) {
                    showError("Token tidak ditemukan. Silakan login ulang.")
                    return@launch
                }

                val response = withContext(Dispatchers.IO) {
                    try {
                        apiService.getAllHistory("Bearer $token").execute()
                    } catch (e: IOException) {
                        throw e
                    } catch (e: Exception) {
                        throw RuntimeException("Unexpected error: ${e.message}")
                    }
                }

                binding.progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val dataList = response.body()!!.data  // ✅ ambil dari .data

                    if (dataList.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                        historyAdapter.setData(dataList)
                        binding.rvHistory.visibility = View.VISIBLE
                    }
                } else {
                    showError("Gagal memuat data: ${response.message()}")
                }
            } catch (e: HttpException) {
                binding.progressBar.visibility = View.GONE
                showError("Server error: ${e.message()}")
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showError("Kesalahan jaringan: ${e.message}")
            }
        }
    }


    private fun showEmptyState() {
        binding.tvNotFound.visibility = View.VISIBLE
        binding.rvHistory.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.tvNotFound.visibility = View.GONE
        binding.rvHistory.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        binding.tvNotFound.text = message
        showEmptyState()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}