package si.uni_lj.fri.pbd.routinetracker

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import si.uni_lj.fri.pbd.routinetracker.data.RoutinesDatabase

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences hierarchy and UI components from the XML resource file
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        // Find the specific preference by its key to attach a custom click listener

        val resetPref: Preference? = findPreference("reset_app")
        resetPref?.setOnPreferenceClickListener {
            showResetDialog()
            true
        }
    }

    private fun showResetDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Reset App")
            .setMessage("Are you sure you want to delete all data and reset settings?")
            .setPositiveButton("Yes") { _, _ ->

// Use Kotlin Coroutines on the IO dispatcher to safely clear the Room database without blocking the Main (UI) thread
                lifecycleScope.launch(Dispatchers.IO) {
                    RoutinesDatabase.getDatabase(requireContext().applicationContext).clearAllTables()
                }

// Clear all saved user preferences and restore the default required state
                preferenceManager.sharedPreferences?.edit()?.apply {
                    clear()
                    putString("USER_CITY", "Ljubljana, Slovenia")
                    apply()
                }


                Toast.makeText(requireContext(), "App reset successfully!", Toast.LENGTH_SHORT).show()


                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .setNegativeButton("No", null)
            .show()
    }
}