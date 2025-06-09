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
import androidx.navigation.ui.navigateUp
import com.example.budgetbuddy.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

// This marks MainActivity to allow Hilt (our dependency manager) to provide things it needs.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // View Binding helps us easily access layout elements (buttons, text views, etc.)
    private lateinit var binding: ActivityMainBinding
    // NavController handles moving between different screens (fragments).
    private lateinit var navController: NavController
    
    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Debug Firebase Auth state
        android.util.Log.d("MainActivity", "=== Firebase Auth Debug ===")
        android.util.Log.d("MainActivity", "Current user: ${firebaseAuth.currentUser}")
        android.util.Log.d("MainActivity", "Current user UID: ${firebaseAuth.currentUser?.uid}")
        android.util.Log.d("MainActivity", "Is user logged in: ${firebaseAuth.currentUser != null}")
        
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
                R.id.rewardsFragment,
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
                // Special case for Reports fragment - show white action bar with centered text
                R.id.reportsFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                    supportActionBar?.show() // Show action bar
                    supportActionBar?.setDisplayHomeAsUpEnabled(false) // Hide back arrow
                    // Set white background and black text for action bar
                    binding.toolbar.setBackgroundColor(android.graphics.Color.WHITE)
                    binding.toolbar.setTitleTextColor(android.graphics.Color.BLACK)
                    // Center the title by using a custom title
                    binding.toolbar.title = ""
                    binding.toolbar.setNavigationIcon(null)
                    
                    // Add a centered TextView for the title
                    val titleView = android.widget.TextView(this@MainActivity)
                    titleView.text = "Reports & Insights"
                    titleView.setTextColor(android.graphics.Color.BLACK)
                    titleView.textSize = 18f
                    titleView.gravity = android.view.Gravity.CENTER
                    titleView.setTypeface(null, android.graphics.Typeface.BOLD)
                    
                    // Clear any existing views and add the centered title
                    binding.toolbar.removeAllViews()
                    val layoutParams = androidx.appcompat.widget.Toolbar.LayoutParams(
                        androidx.appcompat.widget.Toolbar.LayoutParams.MATCH_PARENT,
                        androidx.appcompat.widget.Toolbar.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.gravity = android.view.Gravity.CENTER
                    binding.toolbar.addView(titleView, layoutParams)
                }
                // If we are on other main screens, show the bottom bar and action bar.
                R.id.homeFragment,
                R.id.rewardsFragment,
                R.id.profileFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                    supportActionBar?.show() // Make sure action bar is visible
                    supportActionBar?.setDisplayHomeAsUpEnabled(false) // Hide back arrow on main screens
                    // Reset toolbar to normal configuration
                    binding.toolbar.removeAllViews()
                    binding.toolbar.setBackgroundColor(resources.getColor(com.google.android.material.R.color.design_default_color_primary, theme))
                    binding.toolbar.setTitleTextColor(android.graphics.Color.WHITE)
                }
                // Otherwise (on secondary screens like Add Expense), hide bottom nav but show action bar.
                else -> {
                    bottomNavigationView.visibility = View.GONE
                    supportActionBar?.show() // Make sure action bar is visible
                    supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back arrow on other screens
                    // Reset toolbar to normal configuration
                    binding.toolbar.removeAllViews()
                    binding.toolbar.setBackgroundColor(resources.getColor(com.google.android.material.R.color.design_default_color_primary, theme))
                    binding.toolbar.setTitleTextColor(android.graphics.Color.WHITE)
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