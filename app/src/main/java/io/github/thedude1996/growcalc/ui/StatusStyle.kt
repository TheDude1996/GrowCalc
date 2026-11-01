package io.github.thedude1996.growcalc.ui

import android.content.res.ColorStateList
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import io.github.thedude1996.growcalc.R

/** Semantic tone for status pills in result cards. */
enum class StatusTone(@ColorRes val background: Int, @ColorRes val foreground: Int) {
    GOOD(R.color.status_good_bg, R.color.status_good),
    INFO(R.color.status_info_bg, R.color.status_info),
    WARN(R.color.status_warn_bg, R.color.status_warn),
    BAD(R.color.status_bad_bg, R.color.status_bad)
}

/** Shows [text] in this pill-styled TextView, colored according to [tone]. */
fun TextView.showStatus(text: CharSequence, tone: StatusTone) {
    this.text = text
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, tone.background))
    setTextColor(ContextCompat.getColor(context, tone.foreground))
}
