package io.github.thedude1996.growcalc.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.thedude1996.growcalc.MainActivity
import io.github.thedude1996.growcalc.R
import io.github.thedude1996.growcalc.calculation.LightSource
import io.github.thedude1996.growcalc.calculation.LuxPpfdConverter
import io.github.thedude1996.growcalc.sensor.AndroidLightSensorSource
import io.github.thedude1996.growcalc.sensor.LightSensorSource
import io.github.thedude1996.growcalc.settings.AppSettings
import io.github.thedude1996.growcalc.ui.StatusTone
import io.github.thedude1996.growcalc.ui.showStatus
import io.github.thedude1996.growcalc.viewmodel.PpfdUiState
import io.github.thedude1996.growcalc.viewmodel.PpfdViewModel
import kotlin.math.abs

/** Full-screen light meter: live PPFD from the phone's light sensor. */
class PpfdFragment : Fragment() {

    private val viewModel: PpfdViewModel by viewModels {
        viewModelFactory {
            initializer {
                val appContext = requireContext().applicationContext
                val settings = AppSettings(appContext)
                PpfdViewModel(sensorSourceFactory(appContext), { settings.lightCalibrationFactor })
            }
        }
    }

    private lateinit var statusDot: View
    private lateinit var textStatus: TextView
    private lateinit var textLightSource: TextView
    private lateinit var textPpfd: TextView
    private lateinit var textLux: TextView
    private lateinit var textSaturated: TextView
    private lateinit var textHint: TextView
    private lateinit var btnMain: ImageButton
    private lateinit var dots: List<View>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ppfd, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusDot = view.findViewById(R.id.statusDot)
        textStatus = view.findViewById(R.id.textStatus)
        textLightSource = view.findViewById(R.id.textLightSource)
        textPpfd = view.findViewById(R.id.textPpfd)
        textLux = view.findViewById(R.id.textLux)
        textSaturated = view.findViewById(R.id.textSaturated)
        textHint = view.findViewById(R.id.textHint)
        btnMain = view.findViewById(R.id.btnMain)
        dots = buildDots(view.findViewById(R.id.dotsContainer))

        textSaturated.showStatus(getString(R.string.warn_sensor_saturated), StatusTone.WARN)

        view.findViewById<View>(R.id.btnInfo).setOnClickListener { showExplanation() }
        view.findViewById<View>(R.id.btnSettings).setOnClickListener {
            (requireActivity() as MainActivity).navigateToSettings()
        }
        view.findViewById<View>(R.id.btnLightSource).setOnClickListener { viewModel.nextLightSource() }
        btnMain.setOnClickListener { viewModel.toggleMeasurement() }
        setupSwipe(view.findViewById(R.id.meterPanel))

        viewModel.state.observe(viewLifecycleOwner, ::render)

        // Register the sensor only while this screen is visible (saves battery)
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = viewModel.setScreenVisible(true)
            override fun onPause(owner: LifecycleOwner) = viewModel.setScreenVisible(false)
        })
    }

    private fun render(state: PpfdUiState) {
        textLightSource.setText(state.lightSource.labelRes())
        dots.forEachIndexed { index, dot ->
            val selected = index == state.lightSource.ordinal
            dot.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), if (selected) R.color.on_meter else R.color.meter_dot_inactive)
            )
            dot.isSelected = selected
        }

        val ppfd = state.ppfd
        textPpfd.text = ppfd?.let { LuxPpfdConverter.formatPpfd(it) } ?: getString(R.string.result_placeholder)
        val lux = state.lux
        textLux.text = lux?.let { getString(R.string.lux_approx_format, LuxPpfdConverter.formatRounded(it)) }
        textLux.visibility = if (lux != null) View.VISIBLE else View.INVISIBLE

        val statusVisible = state.sensorAvailable
        statusDot.visibility = if (statusVisible && state.isMeasuring) View.VISIBLE else View.GONE
        textStatus.visibility = if (statusVisible) View.VISIBLE else View.GONE
        textStatus.setText(if (state.isMeasuring) R.string.status_live else R.string.status_held)

        textSaturated.visibility = if (state.isMeasuring && state.isSaturated) View.VISIBLE else View.GONE
        textHint.setText(if (state.sensorAvailable) R.string.measure_hint else R.string.measure_no_sensor)

        btnMain.isEnabled = state.sensorAvailable
        btnMain.alpha = if (state.sensorAvailable) 1f else 0.4f
        if (state.isMeasuring) {
            btnMain.setImageResource(R.drawable.ic_pause)
            btnMain.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.on_meter)
            btnMain.contentDescription = getString(R.string.btn_hold)
        } else {
            btnMain.setImageResource(R.drawable.ic_tool_dli)
            btnMain.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.meter_accent)
            btnMain.contentDescription = getString(R.string.btn_measure)
        }
    }

    /** One dot per light source; tapping a dot selects it. */
    private fun buildDots(container: LinearLayout): List<View> {
        val density = resources.displayMetrics.density
        val dotSize = (10 * density).toInt()
        val touchPadding = (8 * density).toInt()
        return LightSource.entries.map { source ->
            val dot = View(requireContext()).apply {
                setBackgroundResource(R.drawable.bg_meter_dot)
            }
            val touchTarget = android.widget.FrameLayout(requireContext()).apply {
                setPadding(touchPadding, touchPadding, touchPadding, touchPadding)
                contentDescription = getString(R.string.light_source_dot, getString(source.labelRes()))
                setOnClickListener { viewModel.selectLightSource(source) }
                addView(dot, android.widget.FrameLayout.LayoutParams(dotSize, dotSize))
            }
            container.addView(touchTarget)
            dot
        }
    }

    /** Horizontal swipes on the meter switch the light source. */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipe(panel: View) {
        val detector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val start = e1 ?: return false
                val dx = e2.x - start.x
                val dy = e2.y - start.y
                if (abs(dx) < SWIPE_MIN_DISTANCE_PX || abs(dx) < abs(dy)) return false
                if (dx < 0) viewModel.nextLightSource() else viewModel.previousLightSource()
                return true
            }
        })
        panel.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
    }

    private fun showExplanation() {
        val message = listOf(
            R.string.measure_guide,
            R.string.ppfd_explanation_1,
            R.string.ppfd_explanation_2,
            R.string.ppfd_explanation_3,
            R.string.ppfd_formula
        ).joinToString("\n\n") { getString(it) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.label_explanation)
            .setMessage(message)
            .setPositiveButton(R.string.btn_ok, null)
            .show()
    }

    companion object {
        private const val SWIPE_MIN_DISTANCE_PX = 120

        /** Creates the light sensor; replaced by a fake in instrumented tests. */
        @VisibleForTesting
        var sensorSourceFactory: (Context) -> LightSensorSource = { AndroidLightSensorSource(it) }
    }
}

@StringRes
private fun LightSource.labelRes(): Int = when (this) {
    LightSource.SUNLIGHT -> R.string.light_source_sun
    LightSource.LED_FULL_SPECTRUM -> R.string.light_source_led
    LightSource.HPS -> R.string.light_source_hps
    LightSource.FLUORESCENT -> R.string.light_source_fluorescent
}
