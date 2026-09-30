package si.uni_lj.fri.pbd.routinetracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import si.uni_lj.fri.pbd.routinetracker.databinding.ActivityMainBinding
import si.uni_lj.fri.pbd.routinetracker.util.RoutineEvaluationWorker
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private lateinit var appBarConfiguration: AppBarConfiguration

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission granted or denied
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize ViewBinding to interact with the UI components
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setSupportActionBar(binding.toolbar)
// Setup Jetpack Navigation Component with DrawerLayout and Toolbar integration
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController


        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.routineListFragment),
            binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        askNotificationPermission()
        handleNotificationIntent()
        scheduleBackgroundWorker()
    }
    // Track application lifecycle: record when the app enters the foreground
    override fun onStart() {
        super.onStart()
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit { putLong("lastForegroundTime", System.currentTimeMillis()) }
    }
    // Track application lifecycle: record when the app enters the background
    override fun onStop() {
        super.onStop()
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit { putLong("lastBackgroundTime", System.currentTimeMillis()) }
    }

    private fun scheduleBackgroundWorker() {
        // Schedule a periodic background task using WorkManager to evaluate routines every hour
        val workRequest = PeriodicWorkRequestBuilder<RoutineEvaluationWorker>(1, TimeUnit.HOURS).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RoutineEvaluation",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun handleNotificationIntent() {
        // Check if the activity was launched via a notification tap and navigate to the corresponding routine details
        val routineIdFromNotification = intent.getLongExtra("ROUTINE_ID", -1L)
        if (routineIdFromNotification != -1L) {
            val bundle = Bundle().apply {
                putLong("routineId", routineIdFromNotification)
            }
            navController.navigate(R.id.routineDetailsFragment, bundle)
        }
    }

    private fun askNotificationPermission() {
        // Request POST_NOTIFICATIONS runtime permission required for Android 13 (Tiramisu) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
// Handle the Up/Back button behavior in the app bar using the Navigation UI
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}