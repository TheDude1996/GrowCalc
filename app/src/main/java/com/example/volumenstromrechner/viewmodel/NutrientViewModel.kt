package com.example.volumenstromrechner.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.volumenstromrechner.calculation.*
import com.example.volumenstromrechner.model.*

class NutrientViewModel : ViewModel() {

    // Input States
    val waterCa = MutableLiveData(0.0)
    val waterMg = MutableLiveData(0.0)
    val waterEc = MutableLiveData(0.0)

    val selectedManufacturer = MutableLiveData<Manufacturer?>()
    val selectedLine = MutableLiveData<NutrientLine?>()
    val selectedProgram = MutableLiveData<FeedProgram?>()
    val selectedWeekIndex = MutableLiveData(0)

    val mixingVolume = MutableLiveData(10.0)
    val caMgMode = MutableLiveData(CaMgMode.RATIO_3_0)

    // Result State
    private val _calculationResult = MutableLiveData<CalculationResult?>()
    val calculationResult: LiveData<CalculationResult?> = _calculationResult

    fun selectManufacturer(manufacturer: Manufacturer) {
        selectedManufacturer.value = manufacturer
        selectedLine.value = null
        selectedProgram.value = null
        selectedWeekIndex.value = 0
        _calculationResult.value = null
    }

    fun selectLine(line: NutrientLine) {
        selectedLine.value = line
        selectedProgram.value = null
        selectedWeekIndex.value = 0
        _calculationResult.value = null
    }

    fun selectProgram(program: FeedProgram) {
        selectedProgram.value = program
        selectedWeekIndex.value = 0
        calculate()
    }

    fun supportsRatioMode(program: FeedProgram?): Boolean = NutrientCalculator.supportsRatioMode(program)

    fun calculate() {
        val program = selectedProgram.value ?: return
        val weekIndex = selectedWeekIndex.value ?: 0
        val vol = mixingVolume.value ?: 0.0
        if (weekIndex !in program.weeks.indices || vol <= 0.0) {
            _calculationResult.value = null
            return
        }

        _calculationResult.value = NutrientCalculator.calculate(
            program = program,
            week = program.weeks[weekIndex],
            water = WaterInput(waterCa.value ?: 0.0, waterMg.value ?: 0.0, waterEc.value ?: 0.0),
            volumeLiters = vol,
            requestedMode = caMgMode.value ?: CaMgMode.RATIO_3_0
        )
    }
}
