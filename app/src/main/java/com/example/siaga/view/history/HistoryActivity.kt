package com.example.siaga.view.history

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.siaga.api.ApiClient
import com.example.siaga.api.ApiService
import com.example.siaga.databinding.ActivityHistoryBinding
import com.example.siaga.view.adapter.HistoryAdapter
import com.example.siaga.datastore.DataStoreManager
import com.example.siaga.view.model.HistoryResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.text.SimpleDateFormat
import androidx.appcompat.widget.SearchView
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var apiService: ApiService
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var dataStoreManager: DataStoreManager
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private var originalList: List<HistoryResponse> = listOf()
    private var selectedMonth: String? = null // format yyyy-MM kalau pilih bulan manual

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }

        apiService = ApiClient.instance
        dataStoreManager = DataStoreManager(this)

        setupRecyclerView()
        setupSearch()
        setupFilterDropdown()
        setupMonthPicker()
        loadHistoryData()
    }

    private fun setupFilterDropdown() {
        val options = listOf("Hari Ini", "Kemarin", "Bulan Ini")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        binding.spinnerFilter.adapter = adapter
        binding.spinnerFilter.setSelection(0)

        binding.spinnerFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                filterData()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterData()
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filterData()
                return true
            }
        })
    }

    private fun setupMonthPicker() {
        binding.btnPilihBulan.setOnClickListener {
            if (selectedMonth != null) {
                // Reset bulan kalau user klik lagi
                selectedMonth = null
                binding.btnPilihBulan.text = "Pilih Bulan"
                binding.btnPilihBulan.icon = null
                Toast.makeText(this, "Filter bulan direset", Toast.LENGTH_SHORT).show()
                filterData()
            } else {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)

                val dialog = DatePickerDialog(
                    this,
                    { _, selectedYear, selectedMonthIndex, _ ->
                        val monthNumber = String.format("%02d", selectedMonthIndex + 1)
                        selectedMonth = "$selectedYear-$monthNumber"

                        //Mengubah tombol menjadi nama bulan yang di pilih
                        val monthName = try {
                            SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                                .format(Calendar.getInstance().apply {
                                    set(Calendar.YEAR, selectedYear)
                                    set(Calendar.MONTH, selectedMonthIndex)
                                }.time)
                        } catch (e: Exception) {
                            "$selectedYear-$monthNumber"
                        }

                        binding.btnPilihBulan.text = monthName
                        binding.btnPilihBulan.setIconResource(com.example.siaga.R.drawable.ic_clear)

                        filterData()
                    },
                    year,
                    month,
                    1
                )

                // sembunyikan pilihan tanggal
                dialog.datePicker.findViewById<View>(
                    resources.getIdentifier("day", "id", "android")
                )?.visibility = View.GONE

                dialog.show()
            }
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
                    apiService.getAllHistory("Bearer $token").execute()
                }

                binding.progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    originalList = response.body()!!.data.sortedByDescending {
                        try {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(it.waktu_absen)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    filterData()
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

    private fun filterData() {
        if (originalList.isEmpty()) return

        val selectedFilter = binding.spinnerFilter.selectedItem?.toString() ?: "Hari Ini"
        val query = binding.searchView.query?.toString()?.lowercase(Locale.getDefault()) ?: ""

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = sdf.format(calendar.time)

        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonth = monthFormat.format(Date())

        //Filter data berdasarkan tanggal atau bulan manual
        val filteredByDate = when {
            selectedMonth != null -> originalList.filter { it.waktu_absen.startsWith(selectedMonth!!) }
            selectedFilter == "Hari Ini" -> originalList.filter { it.waktu_absen.startsWith(today) }
            selectedFilter == "Kemarin" -> originalList.filter { it.waktu_absen.startsWith(yesterday) }
            selectedFilter == "Bulan Ini" -> originalList.filter { it.waktu_absen.startsWith(currentMonth) }
            else -> originalList
        }

        //Filter tambahan berdasarkan query search
        val finalFiltered = if (query.isNotEmpty()) {
            filteredByDate.filter {
                (it.jenis ?: "").lowercase(Locale.getDefault()).contains(query)
            }
        } else {
            filteredByDate
        }.sortedByDescending {
            try {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(it.waktu_absen)
            } catch (e: Exception) {
                null
            }
        }

        if (finalFiltered.isEmpty()) {
            // 🔹 Pesan khusus sesuai konteks
            val message = when {
                selectedMonth != null -> {
                    try {
                        val bulan = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                            .format(SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(selectedMonth!!))
                        "Ups, Anda belum absen di bulan $bulan"
                    } catch (e: Exception) {
                        "Ups, Anda belum absen di bulan yang dipilih"
                    }
                }
                selectedFilter == "Hari Ini" -> "Ups, Anda belum absen hari ini"
                selectedFilter == "Kemarin" -> "Ups, Anda belum absen kemarin"
                selectedFilter == "Bulan Ini" -> "Ups, Anda belum absen di bulan ini"
                else -> "Data tidak ditemukan"
            }
            showEmptyState(message)
        } else {
            hideEmptyState()
            historyAdapter.setData(finalFiltered)
        }
    }

    private fun showEmptyState(message: String) {
        binding.tvNotFound.text = message
        binding.tvNotFound.visibility = View.VISIBLE
        binding.rvHistory.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.tvNotFound.visibility = View.GONE
        binding.rvHistory.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        showEmptyState(message)
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
