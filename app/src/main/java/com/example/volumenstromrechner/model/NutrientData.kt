package com.example.volumenstromrechner.model

data class Manufacturer(
    val name: String,
    val lines: List<NutrientLine>
)

data class NutrientLine(
    val name: String,
    val programs: List<FeedProgram>
)

data class FeedProgram(
    val name: String,
    val weeks: List<FeedWeek>,
    val notes: String = ""
)

data class FeedWeek(
    val label: String,
    val targetEc: Double,
    val components: List<NutrientComponent>
)

data class NutrientComponent(
    val name: String,
    val dosage: Double, // amount per liter
    val unit: String, // "g" or "ml"
    val profile: NutrientProfile = NutrientProfile(),
    // g/ml, converts ml dosages to grams for the weight-based percentages. 1.0 for powders (unit "g").
    val density: Double = 1.0
)

data class NutrientProfile(
    val n: Double = 0.0,
    val p2o5: Double = 0.0,
    val k2o: Double = 0.0,
    val ca: Double = 0.0,
    val mg: Double = 0.0
)

object NutrientDataProvider {
    // Nutrient Profiles (Guaranteed Analysis)
    private val profiles = mapOf(
        "Grow" to NutrientProfile(n = 24.0, p2o5 = 6.0, k2o = 12.0, mg = 1.2),
        "Short Flowering" to NutrientProfile(n = 16.0, p2o5 = 6.0, k2o = 26.0, mg = 2.0),
        "Hybrids" to NutrientProfile(n = 15.0, p2o5 = 7.0, k2o = 22.0, mg = 3.6),
        "Long Flowering" to NutrientProfile(n = 18.0, p2o5 = 12.0, k2o = 18.0, mg = 1.2),
        "Booster PK+" to NutrientProfile(p2o5 = 30.0, k2o = 27.0, mg = 5.0),
        "Calcium" to NutrientProfile(ca = 9.0), // GHF Calcium provides 9% chelated Calcium

        "Core" to NutrientProfile(n = 14.0, ca = 17.0),
        "Athena_Grow" to NutrientProfile(n = 2.0, p2o5 = 8.0, k2o = 20.0, mg = 3.0),
        "Athena_Bloom" to NutrientProfile(p2o5 = 12.0, k2o = 24.0, mg = 4.0),

        "AN_Grow" to NutrientProfile(n = 10.0, p2o5 = 5.0, k2o = 14.0, ca = 2.0, mg = 0.5),
        "AN_Base" to NutrientProfile(n = 4.0, ca = 4.0),
        "AN_Bloom" to NutrientProfile(p2o5 = 10.0, k2o = 15.0, mg = 2.0),

        "AB_Grow_A" to NutrientProfile(n = 4.72, k2o = 1.18, ca = 3.78, mg = 0.20),
        "AB_Grow_B" to NutrientProfile(n = 1.15, p2o5 = 3.45, k2o = 5.75, mg = 1.04),
        "AB_Bloom_A" to NutrientProfile(n = 4.72, k2o = 5.9, ca = 3.78, mg = 0.20),
        "AB_Bloom_B" to NutrientProfile(p2o5 = 6.9, k2o = 5.75, mg = 0.92),
        "AB_CaMg" to NutrientProfile(n = 2.24, ca = 2.35, mg = 1.23),
        "AB_PK" to NutrientProfile(p2o5 = 4.64, k2o = 5.8, mg = 1.11)
    )

    // Densities (g/ml) of liquid products. Missing entries fall back to 1.0 –
    // TODO: fill in the values from the manufacturers' safety data sheets.
    private val densities = mapOf<String, Double>()

    val manufacturers = listOf(
        Manufacturer(
            name = "Green House Feeding",
            lines = listOf(
                NutrientLine("Short Flowering", listOf(
                    createGHFProgram("Short Flowering (Soil)", "Short Flowering", isSoil = true, bloomWeeks = 8),
                    createGHFProgram("Short Flowering (Coco/Hydro)", "Short Flowering", isSoil = false, bloomWeeks = 8)
                )),
                NutrientLine("Hybrids", listOf(
                    createGHFProgram("Hybrids (Soil)", "Hybrids", isSoil = true, bloomWeeks = 10),
                    createGHFProgram("Hybrids (Coco/Hydro)", "Hybrids", isSoil = false, bloomWeeks = 10)
                )),
                NutrientLine("Long Flowering", listOf(
                    createGHFProgram("Long Flowering (Soil)", "Long Flowering", isSoil = true, bloomWeeks = 12),
                    createGHFProgram("Long Flowering (Coco/Hydro)", "Long Flowering", isSoil = false, bloomWeeks = 12)
                ))
            )
        ),
        Manufacturer(
            name = "Athena",
            lines = listOf(
                NutrientLine("Pro Line", listOf(
                    FeedProgram(
                        name = "Metric Chart",
                        notes = "Werte sind Gramm pro 10 Liter laut Hersteller, hier auf 1 Liter umgerechnet.",
                        weeks = listOf(
                            FeedWeek("Veg Early", 2.0, listOf(comp("Core", 1.2, "g"), comp("Grow", 1.9, "g", "Athena_Grow"))),
                            FeedWeek("Veg Late", 2.5, listOf(comp("Core", 1.6, "g"), comp("Grow", 2.6, "g", "Athena_Grow"))),
                            FeedWeek("Bloom 1-3", 3.0, listOf(comp("Core", 1.8, "g"), comp("Bloom", 3.0, "g", "Athena_Bloom"))),
                            FeedWeek("Bloom 4-6", 3.0, listOf(comp("Core", 1.8, "g"), comp("Bloom", 3.0, "g", "Athena_Bloom"))),
                            FeedWeek("Bloom 7-9", 3.0, listOf(comp("Core", 1.8, "g"), comp("Bloom", 3.0, "g", "Athena_Bloom"))),
                            FeedWeek("Flush", 0.2, listOf())
                        )
                    )
                )),
                NutrientLine("Blended Line", listOf(
                    FeedProgram(
                        name = "Standard Program (Soil/Coco/Hydro)",
                        notes = "Dosierung in ml pro Liter. Nutze CaMg bei weichem Wasser/LED.",
                        weeks = listOf(
                            FeedWeek("Veg Early", 1.2, listOf(
                                comp("Grow A", 0.8, "ml", "AB_Grow_A"),
                                comp("Grow B", 0.8, "ml", "AB_Grow_B"),
                                comp("CaMg", 0.5, "ml", "AB_CaMg")
                            )),
                            FeedWeek("Veg Late", 1.8, listOf(
                                comp("Grow A", 1.3, "ml", "AB_Grow_A"),
                                comp("Grow B", 1.3, "ml", "AB_Grow_B"),
                                comp("CaMg", 0.5, "ml", "AB_CaMg")
                            )),
                            FeedWeek("Bloom 1-2", 2.2, listOf(
                                comp("Bloom A", 1.6, "ml", "AB_Bloom_A"),
                                comp("Bloom B", 1.6, "ml", "AB_Bloom_B"),
                                comp("CaMg", 0.5, "ml", "AB_CaMg")
                            )),
                            FeedWeek("Bloom 3-4 (Peak)", 2.6, listOf(
                                comp("Bloom A", 1.8, "ml", "AB_Bloom_A"),
                                comp("Bloom B", 1.8, "ml", "AB_Bloom_B"),
                                comp("PK", 0.8, "ml", "AB_PK"),
                                comp("CaMg", 0.3, "ml", "AB_CaMg")
                            )),
                            FeedWeek("Bloom 5-7", 2.4, listOf(
                                comp("Bloom A", 1.8, "ml", "AB_Bloom_A"),
                                comp("Bloom B", 1.8, "ml", "AB_Bloom_B"),
                                comp("PK", 1.2, "ml", "AB_PK")
                            )),
                            FeedWeek("Bloom 8-9 (Ripen)", 2.0, listOf(
                                comp("Bloom A", 1.3, "ml", "AB_Bloom_A"),
                                comp("Bloom B", 1.3, "ml", "AB_Bloom_B"),
                                comp("PK", 0.5, "ml", "AB_PK")
                            )),
                            FeedWeek("Flush", 0.0, listOf())
                        )
                    )
                ))
            )
        ),
        Manufacturer(
            name = "Advanced Nutrients",
            lines = listOf(
                NutrientLine("Cultivator Series", listOf(
                    FeedProgram(
                        name = "Professional Chart",
                        weeks = listOf(
                            FeedWeek("Veg Week 1", 1.2, listOf(comp("Grow", 1.0, "g", "AN_Grow"), comp("Base", 1.0, "g", "AN_Base"))),
                            FeedWeek("Veg Week 2", 1.5, listOf(comp("Grow", 1.3, "g", "AN_Grow"), comp("Base", 1.3, "g", "AN_Base"))),
                            FeedWeek("Veg Week 3-4", 1.7, listOf(comp("Grow", 1.5, "g", "AN_Grow"), comp("Base", 1.5, "g", "AN_Base"))),
                            FeedWeek("Bloom Week 1-2", 2.0, listOf(comp("Base", 1.8, "g", "AN_Base"), comp("Bloom", 1.8, "g", "AN_Bloom"))),
                            FeedWeek("Bloom Week 3-4", 2.3, listOf(comp("Base", 2.0, "g", "AN_Base"), comp("Bloom", 2.0, "g", "AN_Bloom"))),
                            FeedWeek("Bloom Week 5-6", 2.3, listOf(comp("Base", 2.0, "g", "AN_Base"), comp("Bloom", 2.0, "g", "AN_Bloom"))),
                            FeedWeek("Bloom Week 7 (Flush)", 0.0, listOf())
                        )
                    )
                ))
            )
        )
    )

    private fun comp(name: String, dosage: Double, unit: String, profileKey: String? = null): NutrientComponent {
        val key = profileKey ?: name
        return NutrientComponent(name, dosage, unit, profiles[key] ?: NutrientProfile(), densities[key] ?: 1.0)
    }

    private fun createGHFProgram(name: String, flowerComp: String, isSoil: Boolean, bloomWeeks: Int): FeedProgram {
        val weeks = mutableListOf<FeedWeek>()
        
        // Seedling / Early Veg
        weeks.add(FeedWeek("Woche 1 (V)", if (isSoil) 0.5 else 0.8, listOf(comp("Grow", 0.25, "g"), comp("Calcium", if (isSoil) 0.1 else 0.2, "g"))))
        
        // Mid Veg
        weeks.add(FeedWeek("Woche 2-3 (V)", if (isSoil) 1.0 else 1.4, listOf(comp("Grow", 0.5, "g"), comp("Calcium", if (isSoil) 0.2 else 0.4, "g"))))
        
        // Late Veg
        weeks.add(FeedWeek("Woche 4+ (V)", if (isSoil) 1.2 else 1.6, listOf(comp("Grow", 0.7, "g"), comp("Calcium", if (isSoil) 0.3 else 0.5, "g"))))
        
        // Bloom Phases
        for (i in 1..bloomWeeks) {
            val label = "Woche $i (B)"
            val isFlush = i == bloomWeeks

            if (isFlush) {
                weeks.add(FeedWeek("Woche $i (Flush)", 0.0, listOf()))
                continue
            }

            // GHF Base Dosage usually 0.7g/L to 1.0g/L
            val baseDosage = if (i <= 2) 0.7 else 1.0
            val components = mutableListOf(
                comp(flowerComp, baseDosage, "g"),
                comp("Calcium", if (isSoil) 0.3 else 0.5, "g")
            )

            // PK+ Booster Logic: Starts Week 3, increases
            val boosterAmount = when {
                i <= 2 -> 0.0
                i <= 4 -> 0.2
                i <= 6 -> 0.3
                else -> 0.4 // Max dosage for PK+ usually around 0.4-0.5g/L
            }
            
            if (boosterAmount > 0) {
                components.add(comp("Booster PK+", boosterAmount, "g"))
            }

            val targetEc = if (isSoil) {
                if (i <= 2) 1.2 else if (i > bloomWeeks - 3) 1.6 else 1.4
            } else {
                if (i <= 2) 1.6 else if (i > bloomWeeks - 3) 2.2 else 2.0
            }

            weeks.add(FeedWeek(label, targetEc, components))
        }

        return FeedProgram(
            name = name,
            notes = "Düngeschema optimiert auf 3:1 Ca:Mg. Wochenangaben sind Richtwerte je nach Sorte.",
            weeks = weeks
        )
    }
}
