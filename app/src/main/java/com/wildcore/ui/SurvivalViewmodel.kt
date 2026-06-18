package com.wildcore.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildcore.data.SurvivalDao
import com.wildcore.data.SurvivalSpot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SurvivalViewModel(private val survivalDao: SurvivalDao) : ViewModel() {

    // Pobieranie strumienia danych z bazy i zamiana na StateFlow dla Compose
    val survivalSpots = survivalDao.getAllSpots()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addSpot(title: String, description: String, lat: Double, lon: Double, category: String) {
        viewModelScope.launch {
            val newSpot = SurvivalSpot(
                title = title,
                description = description,
                latitude = lat,
                longitude = lon,
                category = category
            )
            survivalDao.insertSpot(newSpot)
        }
    }

    fun removeSpot(spot: SurvivalSpot) {
        viewModelScope.launch {
            survivalDao.deleteSpot(spot)
        }
    }
}