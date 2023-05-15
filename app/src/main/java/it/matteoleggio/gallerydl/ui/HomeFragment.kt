package it.matteoleggio.gallerydl.ui

import android.app.Activity
import android.content.*
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.View.*
import android.widget.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.chip.ChipGroup
import it.matteoleggio.gallerydl.MainActivity
import it.matteoleggio.gallerydl.R
import it.matteoleggio.gallerydl.adapter.HomeAdapter
import it.matteoleggio.gallerydl.database.models.ResultItem
import it.matteoleggio.gallerydl.database.viewmodel.DownloadViewModel
import it.matteoleggio.gallerydl.database.viewmodel.ResultViewModel
import it.matteoleggio.gallerydl.databinding.FragmentHomeBinding
import it.matteoleggio.gallerydl.util.FileUtil
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.concurrent.thread


class HomeFragment : Fragment(), HomeAdapter.OnItemClickListener, OnClickListener {
    private var inputQueries: MutableList<String>? = null
    private var homeAdapter: HomeAdapter? = null

    private var searchSuggestions: ScrollView? = null
    private var searchSuggestionsLinearLayout: LinearLayout? = null
    private var searchHistory: ScrollView? = null
    private var searchHistoryLinearLayout: LinearLayout? = null

    private var downloadQueue: ArrayList<ResultItem>? = null

    private lateinit var resultViewModel : ResultViewModel
    private lateinit var downloadViewModel : DownloadViewModel

    private var fragmentView: View? = null
    private var activity: Activity? = null
    private var mainActivity: MainActivity? = null
    private var fragmentContext: Context? = null
    private var layoutinflater: LayoutInflater? = null
    private var searchBar: AppCompatEditText? = null
    private var okButton: AppCompatButton? = null
    private var linkYouCopied: ConstraintLayout? = null
    private var queriesChipGroup: ChipGroup? = null
    private var recyclerView: RecyclerView? = null
    private var uiHandler: Handler? = null
    private var resultsList: List<ResultItem?>? = null
    private var selectedObjects: ArrayList<ResultItem>? = null
    private var fileUtil: FileUtil? = null
    private var quickLaunchSheet = false
    private var sharedPreferences: SharedPreferences? = null
    private var _binding : FragmentHomeBinding? = null
    private var actionMode: ActionMode? = null
    private var appBarLayout: AppBarLayout? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        fragmentView = inflater.inflate(R.layout.fragment_home, container, false)
        activity = getActivity()
        mainActivity = activity as MainActivity?
        quickLaunchSheet = false
        return fragmentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentContext = context
        layoutinflater = LayoutInflater.from(context)
        fileUtil = FileUtil()
        uiHandler = Handler(Looper.getMainLooper())
        selectedObjects = ArrayList()

        downloadViewModel = ViewModelProvider(this)[DownloadViewModel::class.java]

        downloadQueue = ArrayList()
        resultsList = mutableListOf()
        selectedObjects = ArrayList()

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        //initViews
        searchBar = view.findViewById(R.id.search_bar)
        okButton = view.findViewById(R.id.ok_button)
        appBarLayout = view.findViewById(R.id.topbar)
        queriesChipGroup = view.findViewById(R.id.queries)
        searchSuggestions = view.findViewById(R.id.search_suggestions_scroll_view)
        searchSuggestionsLinearLayout = view.findViewById(R.id.search_suggestions_linear_layout)
        searchHistory = view.findViewById(R.id.search_history_scroll_view)
        searchHistoryLinearLayout = view.findViewById(R.id.search_history_linear_layout)

        homeAdapter =
            HomeAdapter(
                this,
                requireActivity()
            )
        recyclerView = view.findViewById(R.id.recyclerViewHome)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && ! resources.getBoolean(R.bool.isTablet)){
            recyclerView?.layoutManager = GridLayoutManager(context, 2)
        }

        okButton!!.setOnClickListener {
            thread(start = true) {
                try {
                    Handler(Looper.getMainLooper()).post {
                        view.findViewById<TextView>(R.id.progress_text).text =
                            "Looking up images!"
                    }
                    val py = Python.getInstance()
                    val downloader = py.getModule("gallery_dl_util")
                    val URLs = downloader.callAttr("metadata", searchBar!!.text.toString()).repr().toInt()
                    val progress = view.findViewById<ProgressBar>(R.id.progress_bar)
                    Handler(Looper.getMainLooper()).post {
                        progress.visibility = VISIBLE
                        progress.max = URLs
                        view.findViewById<TextView>(R.id.progress_text).text =
                            "Downloaded 0 / $URLs images!"
                        view.findViewById<TextView>(R.id.progress_text).visibility = VISIBLE
                    }
                    actuallyDownload(downloader, searchBar!!.text.toString(), progress, URLs)
                    Handler(Looper.getMainLooper()).post {
                        progress.visibility = INVISIBLE
                        progress.progress = 0
                        view.findViewById<TextView>(R.id.progress_text).visibility = INVISIBLE
                    }
                } catch (ignored: Exception) {}
            }
        }

    }

    private fun actuallyDownload(downloader: PyObject, url: String, progress: ProgressBar, n: Int) {
        val process = Runtime.getRuntime().exec("logcat")
        val bufferedReader = BufferedReader(
            InputStreamReader(process.inputStream)
        )
        val uid = UUID.randomUUID().toString()
        thread(start = true) {
            downloader.callAttr("download", url, uid)
        }
        var line: String? = ""
        while (bufferedReader.readLine().also { line = it } != null) {
            if (!(line!!.contains("python.stdout"))) continue
            if (line!!.contains("--FINISHED DOWNLOADING $uid--")) {
                break
            }
            progress.progress = progress.progress + 1
            requireView().findViewById<TextView>(R.id.progress_text)!!.text =
                "Downloaded ${progress.progress} / $n images!"
        }
    }

    fun handleFileIntent(lines: List<String>) {
        inputQueries = lines.toMutableList()
    }

    fun scrollToTop() {
        recyclerView!!.scrollToPosition(0)
        (searchBar!!.parent as AppBarLayout).setExpanded(true, true)
    }

    override fun onButtonClick(videoURL: String, type: DownloadViewModel.Type?) {
        TODO("Not yet implemented")
    }


    override fun onLongButtonClick(videoURL: String, type: DownloadViewModel.Type?) {
        Log.e(TAG, type.toString() + " " + videoURL)
        val item = resultsList!!.find { it?.url == videoURL }
    }

    override fun onCardClick(videoURL: String, add: Boolean) {
        TODO("Not yet implemented")
    }

    private fun clearCheckedItems(){
        homeAdapter?.clearCheckedItems()
        selectedObjects?.forEach {
            homeAdapter?.notifyItemChanged(resultsList!!.indexOf(it))
        }
        selectedObjects?.clear()
    }


    override fun onStop() {
        actionMode?.finish()
        super.onStop()
    }


    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onClick(p0: View?) {
        TODO("Not yet implemented")
    }
}