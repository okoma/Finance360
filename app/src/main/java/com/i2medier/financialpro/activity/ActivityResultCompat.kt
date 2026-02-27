package com.i2medier.financialpro.activity

import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

private const val RESULT_RELAY_TAG = "result_relay_fragment"

fun FragmentActivity.launchForResult(intent: Intent, @Suppress("UNUSED_PARAMETER") requestCode: Int) {
    // None of our call sites currently consume activity result payloads.
    // Use direct launch to avoid fragment transaction state issues from dialog callbacks.
    startActivity(intent)
}

private class ActivityResultRelayFragment : Fragment() {
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Existing call sites do not consume the activity result payload.
    }

    fun launch(intent: Intent) {
        launcher.launch(intent)
    }
}
