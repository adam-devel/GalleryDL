package it.matteoleggio.gallerydl

import android.Manifest
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationView
import it.matteoleggio.gallerydl.ui.BaseActivity
import it.matteoleggio.gallerydl.ui.HomeFragment
import it.matteoleggio.gallerydl.util.ThemeUtil
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess


class MainActivity : BaseActivity() {

    lateinit var context: Context
    private lateinit var preferences: SharedPreferences
    private lateinit var navigationView: View
    private lateinit var navHostFragment : NavHostFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.updateTheme(this)
        setContentView(R.layout.activity_main)
        context = baseContext
        preferences = PreferenceManager.getDefaultSharedPreferences(context)

        askPermissions()
        createDefaultFolders()

        navHostFragment = supportFragmentManager.findFragmentById(R.id.frame_layout) as NavHostFragment
        val navController = navHostFragment.findNavController()
        navigationView = try {
            findViewById(R.id.bottomNavigationView)
        }catch (e: Exception){
            findViewById<NavigationView>(R.id.navigationView)
        }

        if (navigationView is NavigationBarView){
            window.decorView.setOnApplyWindowInsetsListener { view: View, windowInsets: WindowInsets? ->
                val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(
                    windowInsets!!, view
                )
                val isImeVisible = windowInsetsCompat.isVisible(WindowInsetsCompat.Type.ime())
                navigationView.visibility =
                    if (isImeVisible) View.GONE else View.VISIBLE
                view.onApplyWindowInsets(windowInsets)
            }
        }

        val sharedPreferences =  PreferenceManager.getDefaultSharedPreferences(this)

        val startDestination = sharedPreferences.getString("start_destination", "")
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        when(startDestination) {
            "History" -> graph.setStartDestination(R.id.historyFragment)
            "More" -> if (navigationView is NavigationBarView) graph.setStartDestination(R.id.moreFragment) else graph.setStartDestination(R.id.homeFragment)
            else -> graph.setStartDestination(R.id.homeFragment)
        }
        navController.graph = graph

        if (navigationView is NavigationBarView){
            (navigationView as NavigationBarView).selectedItemId = graph.startDestinationId
            (navigationView as NavigationBarView).setupWithNavController(navController)
            window.navigationBarColor = SurfaceColors.SURFACE_2.getColor(this)
        }
        if (navigationView is NavigationView){
            (navigationView as NavigationView).setCheckedItem(graph.startDestinationId)
            (navigationView as NavigationView).setupWithNavController(navController)
            //terminate button
            (navigationView as NavigationView).menu.getItem(7).setOnMenuItemClickListener {
                if (sharedPreferences.getBoolean("ask_terminate_app", true)){
                    var doNotShowAgain = false
                    val terminateDialog = MaterialAlertDialogBuilder(this)
                    terminateDialog.setTitle(getString(R.string.confirm_delete_history))
                    val dialogView = layoutInflater.inflate(R.layout.dialog_terminate_app, null)
                    val checkbox = dialogView.findViewById<CheckBox>(R.id.doNotShowAgain)
                    terminateDialog.setView(dialogView)
                    checkbox.setOnCheckedChangeListener { compoundButton, _ ->
                        doNotShowAgain = compoundButton.isChecked
                    }

                    terminateDialog.setNegativeButton(getString(R.string.cancel)) { dialogInterface: DialogInterface, _: Int -> dialogInterface.cancel() }
                    terminateDialog.setPositiveButton(getString(R.string.ok)) { _: DialogInterface?, _: Int ->
                        if (doNotShowAgain){
                            sharedPreferences.edit().putBoolean("ask_terminate_app", false).apply()
                        }
                        finishAndRemoveTask()
                        exitProcess(0)
                    }
                    terminateDialog.show()
                }else{
                    finishAndRemoveTask()
                    exitProcess(0)
                }
                true
            }
        }
        val intent = intent
        handleIntents(intent)
    }

    fun disableBottomNavigation(){
        if (navigationView is NavigationBarView){
            (navigationView as NavigationBarView).menu.forEach { it.isEnabled = false }
        }else{
            (navigationView as NavigationView).menu.forEach { it.isEnabled = false }
        }
    }

    fun enableBottomNavigation(){
        if (navigationView is NavigationBarView){
            (navigationView as NavigationBarView).menu.forEach { it.isEnabled = true }
        }else{
            (navigationView as NavigationView).menu.forEach { it.isEnabled = true }
        }
    }

    override fun onResume() {
        super.onResume()
        val incognitoHeader = findViewById<TextView>(R.id.incognito_header)
        if (preferences.getBoolean("incognito", false)){
            incognitoHeader.visibility = View.VISIBLE
            window.statusBarColor = (incognitoHeader.background as ColorDrawable).color
        }else{
            window.statusBarColor = getColor(android.R.color.transparent)
            incognitoHeader.visibility = View.GONE
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntents(intent)
    }

    private fun handleIntents(intent: Intent) {
        val action = intent.action
        val type = intent.type
        if (Intent.ACTION_SEND == action && type != null) {
            Log.e(TAG, action)
            try {
                val uri = if (Build.VERSION.SDK_INT >= 33){
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                }else{
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                val `is` = contentResolver.openInputStream(uri!!)
                val textBuilder = StringBuilder()
                val reader: Reader = BufferedReader(
                    InputStreamReader(
                        `is`, Charset.forName(
                            StandardCharsets.UTF_8.name()
                        )
                    )
                )
                var c: Int
                while (reader.read().also { c = it } != -1) {
                    textBuilder.append(c.toChar())
                }
                val l = listOf(*textBuilder.toString().split("\n").toTypedArray())
                (navHostFragment.childFragmentManager.primaryNavigationFragment!! as HomeFragment).handleFileIntent(l)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun askPermissions() {
        val permissions = arrayListOf<String>()
        if (!checkFilePermission()) {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.setData(Uri.parse(String.format("package:%s", applicationContext.packageName)))
                    startActivityForResult(intent, 2296)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(intent, 2296)
                }
            } else {
                //below android 11
                ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 2296);
            }
        }
    }


    private fun createDefaultFolders(){
        val image = File(Environment.DIRECTORY_DOWNLOADS + "/GalleryDL/Image")
        val command = File(Environment.DIRECTORY_DOWNLOADS + "/GalleryDL/Command")

        image.mkdirs()
        command.mkdirs()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (i in permissions.indices) {
            if (permissions.contains(Manifest.permission.POST_NOTIFICATIONS)) continue
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                createPermissionRequestDialog()
            }else{
                createDefaultFolders()
            }
        }
    }

    private fun exit() {
        finishAffinity()
        exitProcess(0)
    }

    private fun checkFilePermission(): Boolean {
        return if(SDK_INT >= Build.VERSION_CODES.R){
            Environment.isExternalStorageManager()
        }else{
            (ActivityCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkNotificationPermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun createPermissionRequestDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
        dialog.setTitle(getString(R.string.warning))
        dialog.setMessage(getString(R.string.request_permission_desc))
        dialog.setOnCancelListener { exit() }
        dialog.setNegativeButton(getString(R.string.exit_app)) { _: DialogInterface?, _: Int -> exit() }
        dialog.setPositiveButton(getString(R.string.ok)) { _: DialogInterface?, _: Int ->
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
            startActivity(intent)
            exitProcess(0)
        }
        dialog.show()
    }

    public fun changeNavbarVisibility(vis: Int) {
        navigationView.findViewById<BottomNavigationView>(R.id.bottomNavigationView).visibility = vis
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        startActivity(Intent(this, MainActivity::class.java))
        super.onConfigurationChanged(newConfig)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}