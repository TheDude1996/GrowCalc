package io.github.thedude1996.growcalc

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import io.github.thedude1996.growcalc.fragments.DashboardFragment
import io.github.thedude1996.growcalc.fragments.VolumenstromFragment
import io.github.thedude1996.growcalc.fragments.VpdFragment
import io.github.thedude1996.growcalc.fragments.DliFragment
import io.github.thedude1996.growcalc.fragments.NutrientFragment
import io.github.thedude1996.growcalc.fragments.PpfdFragment
import io.github.thedude1996.growcalc.fragments.SettingsFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupToolbar()
        setupNavigation()
        setupBackHandling()

        if (savedInstanceState == null) {
            replaceFragment(DashboardFragment(), getString(R.string.menu_home))
            navigationView.setCheckedItem(R.id.nav_home)
        } else {
            supportActionBar?.title = savedInstanceState.getString(KEY_TITLE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_TITLE, supportActionBar?.title?.toString())
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun setupNavigation() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.menu_drawer_open, R.string.menu_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(DashboardFragment(), getString(R.string.menu_home))
                R.id.nav_volumenstrom -> navigateToVolumenstrom()
                R.id.nav_vpd -> navigateToVpd()
                R.id.nav_dli -> navigateToDli()
                R.id.nav_nutrient -> navigateToNutrient()
                R.id.nav_ppfd -> navigateToPpfd()
                R.id.nav_info -> showToast(getString(R.string.menu_info))
                R.id.nav_settings -> navigateToSettings()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    fun navigateToVolumenstrom() {
        replaceFragment(VolumenstromFragment(), getString(R.string.menu_volumenstrom))
        navigationView.setCheckedItem(R.id.nav_volumenstrom)
    }

    fun navigateToVpd() {
        replaceFragment(VpdFragment(), getString(R.string.menu_vpd))
        navigationView.setCheckedItem(R.id.nav_vpd)
    }

    fun navigateToDli() {
        replaceFragment(DliFragment(), getString(R.string.menu_dli))
        navigationView.setCheckedItem(R.id.nav_dli)
    }

    fun navigateToNutrient() {
        replaceFragment(NutrientFragment(), getString(R.string.menu_nutrient))
        navigationView.setCheckedItem(R.id.nav_nutrient)
    }

    fun navigateToPpfd() {
        replaceFragment(PpfdFragment(), getString(R.string.menu_ppfd))
        navigationView.setCheckedItem(R.id.nav_ppfd)
    }

    fun navigateToSettings() {
        replaceFragment(SettingsFragment(), getString(R.string.menu_settings))
        // Settings is outside the checkable tools group, so clear the highlighted tool
        navigationView.checkedItem?.isChecked = false
    }

    private fun replaceFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
        supportActionBar?.title = title
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // onBackPressed() is no longer called with predictive back (targetSdk 36), so use the dispatcher
    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    return
                }
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (currentFragment !is DashboardFragment) {
                    replaceFragment(DashboardFragment(), getString(R.string.menu_home))
                    navigationView.setCheckedItem(R.id.nav_home)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    companion object {
        private const val KEY_TITLE = "toolbar_title"
    }
}
