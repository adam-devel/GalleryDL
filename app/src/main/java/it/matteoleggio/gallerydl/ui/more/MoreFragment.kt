package it.matteoleggio.gallerydl.ui.more

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import it.matteoleggio.gallerydl.MainActivity
import it.matteoleggio.gallerydl.R
import org.w3c.dom.Text

class MoreFragment : Fragment() {
    private lateinit var mainSharedPreferences: SharedPreferences
    private lateinit var mainSharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var cookies: TextView
    private lateinit var appearance: TextView
    private lateinit var mainActivity: MainActivity
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mainActivity = activity as MainActivity
        return inflater.inflate(R.layout.fragment_more, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainSharedPreferences =  PreferenceManager.getDefaultSharedPreferences(requireContext())
        mainSharedPreferencesEditor = mainSharedPreferences.edit()
    }

    companion object {
        const val TAG = "MoreFragment"
    }

}