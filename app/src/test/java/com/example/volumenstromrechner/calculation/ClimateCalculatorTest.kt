package com.example.volumenstromrechner.calculation

import org.junit.Assert.assertEquals
import org.junit.Test

class ClimateCalculatorTest {

    @Test
    fun volumeFlow_125mmAt5ms() {
        // A = π · 0.0625² = 0.012272 m² → · 5 m/s · 3600 s/h
        assertEquals(220.89, ClimateCalculator.volumeFlow(125, 5.0), 0.01)
    }

    @Test
    fun saturationVaporPressure_25C() {
        assertEquals(3.168, ClimateCalculator.saturationVaporPressure(25.0), 0.005)
    }

    @Test
    fun vpd_withoutLeafOffset_equalsAirVpd() {
        val result = ClimateCalculator.vpd(25.0, 60.0, 0.0)
        assertEquals(25.0, result.leafTemp, 1e-9)
        assertEquals(1.267, result.vpdAir, 0.005)
        assertEquals(result.vpdAir, result.vpdLeaf, 1e-9)
        assertEquals(VpdStatus.HIGH, result.status)
    }

    @Test
    fun vpd_coolerLeaf_lowersLeafVpd() {
        val result = ClimateCalculator.vpd(25.0, 60.0, -2.0)
        assertEquals(23.0, result.leafTemp, 1e-9)
        assertEquals(0.908, result.vpdLeaf, 0.005)
        assertEquals(VpdStatus.IDEAL, result.status)
    }

    @Test
    fun vpd_statusBoundaries() {
        assertEquals(VpdStatus.LOW, ClimateCalculator.vpd(20.0, 100.0, 0.0).status)
        assertEquals(VpdStatus.DANGER, ClimateCalculator.vpd(35.0, 20.0, 0.0).status)
    }

    @Test
    fun dli_500ppfdFor18h() {
        assertEquals(32.4, ClimateCalculator.dli(500.0, 18.0), 1e-9)
    }

    @Test
    fun dliLevel_boundaries() {
        assertEquals(DliLevel.LOW, ClimateCalculator.dliLevel(9.99))
        assertEquals(DliLevel.MEDIUM, ClimateCalculator.dliLevel(10.0))
        assertEquals(DliLevel.MEDIUM, ClimateCalculator.dliLevel(20.0))
        assertEquals(DliLevel.HIGH, ClimateCalculator.dliLevel(40.0))
        assertEquals(DliLevel.VERY_HIGH, ClimateCalculator.dliLevel(40.01))
    }
}
