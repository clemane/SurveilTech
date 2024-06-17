package com.surveiltech.application.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.surveiltech.application.R

class AppPreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_preference_fragment, rootKey)
    }

}