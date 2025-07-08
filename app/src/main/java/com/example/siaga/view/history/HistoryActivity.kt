package com.example.siaga.view.history

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.siaga.databinding.ActivityHistoryBinding
import com.example.siaga.view.model.ModelDatabase
import com.example.siaga.view.viewmodel.HistoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistoryActivity : AppCompatActivity(), HistoryAdapterCallback {

    private lateinit var binding: ActivityHistoryBinding
    private val historyViewModel: HistoryViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(this, this)
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        historyViewModel.dataLaporan.observe(this) { dataList ->
            if (dataList.isEmpty()) {
                showEmptyState()
            } else {
                hideEmptyState()
                historyAdapter.setData(dataList)
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

    override fun onDelete(modelDatabase: ModelDatabase) {
        AlertDialog.Builder(this)
            .setMessage("Hapus riwayat ini?")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                historyViewModel.deleteDataById(modelDatabase.uid)
                Toast.makeText(this, "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .create()
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}