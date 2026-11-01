package io.github.thedude1996.growcalc.settings

import android.content.Context
import androidx.core.content.edit
import io.github.thedude1996.growcalc.calculation.DripperPreset
import io.github.thedude1996.growcalc.calculation.DurationUnit
import io.github.thedude1996.growcalc.calculation.IrrigationInput
import io.github.thedude1996.growcalc.calculation.WateringInput
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** A saved setup, e.g. "Zelt 1 – 4 Töpfe, 2× 2,2 L/h". */
data class IrrigationPreset(val name: String, val input: IrrigationInput)

/** Persists the last irrigation input and the saved presets. */
interface IrrigationStore {
    fun loadLast(): IrrigationInput?
    fun saveLast(input: IrrigationInput)
    fun loadPresets(): List<IrrigationPreset>
    fun savePresets(presets: List<IrrigationPreset>)
}

class SharedPrefsIrrigationStore(context: Context) : IrrigationStore {

    private val prefs = context.applicationContext.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadLast(): IrrigationInput? = prefs.getString(KEY_LAST, null)?.let { text ->
        try {
            JSONObject(text).toInput()
        } catch (_: JSONException) {
            null
        }
    }

    override fun saveLast(input: IrrigationInput) {
        prefs.edit { putString(KEY_LAST, input.toJson().toString()) }
    }

    override fun loadPresets(): List<IrrigationPreset> {
        val text = prefs.getString(KEY_PRESETS, null) ?: return emptyList()
        return try {
            val array = JSONArray(text)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                IrrigationPreset(obj.getString("name"), obj.getJSONObject("input").toInput())
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    override fun savePresets(presets: List<IrrigationPreset>) {
        val array = JSONArray()
        presets.forEach { array.put(JSONObject().put("name", it.name).put("input", it.input.toJson())) }
        prefs.edit { putString(KEY_PRESETS, array.toString()) }
    }

    private fun IrrigationInput.toJson(): JSONObject {
        val waterings = JSONArray()
        this.waterings.forEach { w ->
            waterings.put(
                JSONObject()
                    .put("duration", w.durationText)
                    .put("unit", w.unit.name)
                    .put("time", w.timeOfDay ?: JSONObject.NULL)
            )
        }
        return JSONObject()
            .put("preset", preset?.name ?: JSONObject.NULL)
            .put("customFlow", customFlowText)
            .put("drippers", drippers)
            .put("waterings", waterings)
            .put("pots", potsText)
            .put("tank", tankText)
            .put("target", targetText)
    }

    private fun JSONObject.toInput(): IrrigationInput {
        val array = getJSONArray("waterings")
        val waterings = (0 until array.length()).map { i ->
            val w = array.getJSONObject(i)
            WateringInput(
                durationText = w.optString("duration"),
                unit = enumOrDefault(w.optString("unit"), DurationUnit.MINUTES),
                timeOfDay = if (w.isNull("time")) null else w.getInt("time")
            )
        }.ifEmpty { listOf(WateringInput()) }
        return IrrigationInput(
            preset = if (isNull("preset")) null else enumOrDefault(getString("preset"), DripperPreset.BLUE),
            customFlowText = optString("customFlow"),
            drippers = optInt("drippers", 1),
            waterings = waterings,
            potsText = optString("pots"),
            tankText = optString("tank"),
            targetText = optString("target")
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        const val KEY_LAST = "irrigation_last_input"
        const val KEY_PRESETS = "irrigation_presets"
    }
}
