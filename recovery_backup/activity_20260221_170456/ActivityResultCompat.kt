package com.i2medier.financialpro.activity

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

private const val RESULT_RELAY_TAG = "result_relay_fragment"

fun FragmentActivity.launchForResult(intent: Intent, @Suppress("UNUSED_PARAMETER") requestCode: Int) {
    val fragmentManager = supportFragmentManager
    val relay = (fragmentManager.findFragmentByTag(RESULT_RELAY_TAG) as? ActivityResultRelayFragment)
        ?: ActivityResultRelayFragment().also {
            fragmentManager.beginTransaction()
                .add(it, RESULT_RELAY_TAG)
                .commitNow()
        }
    relay.launch(intent)
}

private class ActivityResultRelayFragment : Fragment() {
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Existing call sites do not consume the activity result payload.
    }

    fun launch(intent: Intent) {
        launcher.launch(intent)
    }
}
