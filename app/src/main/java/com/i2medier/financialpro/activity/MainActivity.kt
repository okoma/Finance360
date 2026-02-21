package com.i2medier.financialpro.activity

import android.os.Bundle
import android.graphics.Color
import android.os.Build
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.i2medier.financialpro.R
import com.i2medier.financialpro.planner.integration.PlannerIntegrationContract
import com.i2medier.financialpro.ui.CalculatorsFragment
import com.i2medier.financialpro.ui.HomeFragment
import com.i2medier.financialpro.ui.PlannerFragment
import com.i2medier.financialpro.ui.SettingsFragment
import com.i2medier.financialpro.util.CountrySettingsManager

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private var pendingCalculatorCategory: String = com.i2medier.financialpro.ui.CalculatorRegistry.CATEGORY_ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        CountrySettingsManager.applySelectedLocale(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
        }
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottomNavigation)
        val root = findViewById<android.view.View>(R.id.rootMain)
        val container = findViewById<FrameLayout>(R.id.mainFragmentContainer)
        val containerTop = container.paddingTop
        val containerLeft = container.paddingLeft
        val containerRight = container.paddingRight
        val bottomNavBottom = bottomNavigationView.paddingBottom
        val bottomNavLeft = bottomNavigationView.paddingLeft
        val bottomNavRight = bottomNavigationView.paddingRight

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            container.updatePadding(
                left = containerLeft + bars.left,
                top = containerTop + bars.top,
                right = containerRight + bars.right
            )
            bottomNavigationView.updatePadding(
                left = bottomNavLeft + bars.left,
                right = bottomNavRight + bars.right,
                bottom = bottomNavBottom + bars.bottom
            )
            insets
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    openFragment(HomeFragment())
                    true
                }

                R.id.nav_calculators -> {
                    openFragment(CalculatorsFragment.newInstance(pendingCalculatorCategory))
                    pendingCalculatorCategory = com.i2medier.financialpro.ui.CalculatorRegistry.CATEGORY_ALL
                    true
                }

                R.id.nav_planner -> {
                    openFragment(PlannerFragment())
                    true
                }

                R.id.nav_settings -> {
                    openFragment(SettingsFragment())
                    true
                }

                else -> false
            }
        }

        if (savedInstanceState == null) {
            if (!handlePlannerLaunchIntent(intent)) {
                bottomNavigationView.selectedItemId = R.id.nav_home
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePlannerLaunchIntent(intent)
    }

    fun selectTab(tabId: Int) {
        bottomNavigationView.selectedItemId = tabId
    }

    fun selectCalculatorsCategory(categoryId: String) {
        pendingCalculatorCategory = categoryId
        bottomNavigationView.selectedItemId = R.id.nav_calculators
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mainFragmentContainer, fragment)
            .commit()
    }

    private fun handlePlannerLaunchIntent(intent: android.content.Intent?): Boolean {
        val shouldOpenPlanner = PlannerIntegrationContract.shouldOpenPlanner(intent)
        if (shouldOpenPlanner) {
            bottomNavigationView.selectedItemId = R.id.nav_planner
            return true
        }
        return false
    }
}
