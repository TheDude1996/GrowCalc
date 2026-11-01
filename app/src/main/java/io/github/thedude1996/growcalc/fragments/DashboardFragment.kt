package io.github.thedude1996.growcalc.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.thedude1996.growcalc.MainActivity
import io.github.thedude1996.growcalc.R
import com.google.android.material.card.MaterialCardView

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialCardView>(R.id.cardVolumenstrom).setOnClickListener {
            (requireActivity() as MainActivity).navigateToVolumenstrom()
        }

        view.findViewById<MaterialCardView>(R.id.cardVpd).setOnClickListener {
            (requireActivity() as MainActivity).navigateToVpd()
        }

        view.findViewById<MaterialCardView>(R.id.cardDli).setOnClickListener {
            (requireActivity() as MainActivity).navigateToDli()
        }

        view.findViewById<MaterialCardView>(R.id.cardNutrient).setOnClickListener {
            (requireActivity() as MainActivity).navigateToNutrient()
        }
    }
}
