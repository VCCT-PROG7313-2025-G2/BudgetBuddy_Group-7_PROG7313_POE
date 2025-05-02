// Minor change to trigger Actions
package com.example.budgetbuddy

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.budgetbuddy.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

// This marks MainActivity to allow Hilt (our dependency manager) to provide things it needs.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // View Binding helps us easily access layout elements (buttons, text views, etc.)
    private lateinit var binding: ActivityMainBinding
    // NavController handles moving between different screens (fragments).
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prepare the layout defined in activity_main.xml for use.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Find the main navigation container in our layout.
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        // Get the NavController associated with that container.
        navController = navHostFragment.navController

        // Find the Bottom Navigation view in our layout.
        val bottomNavigationView = binding.bottomNavigation
        // Connect the Bottom Navigation clicks to the NavController so it changes screens.
        bottomNavigationView.setupWithNavController(navController)

        // Define which screens are considered "top-level" (don't show a back arrow in the toolbar).
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.reportsFragment,
                // R.id.rewardsFragment, // Add other top-level destinations here if needed
                R.id.profileFragment
            )
        )
        // Set our custom toolbar as the main action bar.
        setSupportActionBar(binding.toolbar)

        // Connect the action bar (toolbar) to the NavController.
        // This automatically updates the title and handles the back button.
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Listen for screen changes to show/hide the bottom navigation bar.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // If we are on one of the main screens, show the bottom bar.
                R.id.homeFragment,
                R.id.reportsFragment,
                R.id.rewardsFragment,
                R.id.profileFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                    supportActionBar?.setDisplayHomeAsUpEnabled(false) // Hide back arrow on main screens
                }
                // Otherwise (on secondary screens like Add Expense), hide it.
                else -> {
                    bottomNavigationView.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back arrow on other screens
                }
            }
        }

        // The code below was for making the app draw behind the system bars (status bar, nav bar)
        // It's commented out for now.
        // enableEdgeToEdge()
        // ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
        //     val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        //     v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        //     insets
        // }
    }

    // This function ensures the up arrow (back button) in the toolbar works with the NavController.
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}