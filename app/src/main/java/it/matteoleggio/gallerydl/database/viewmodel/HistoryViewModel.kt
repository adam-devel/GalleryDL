package it.matteoleggio.gallerydl.database.viewmodel

import android.app.Application
import androidx.lifecycle.*
import it.matteoleggio.gallerydl.database.DBManager
import it.matteoleggio.gallerydl.database.models.HistoryItem
import it.matteoleggio.gallerydl.database.repository.HistoryRepository
import it.matteoleggio.gallerydl.database.repository.HistoryRepository.HistorySort
import it.matteoleggio.gallerydl.database.repository.HistoryRepository.HistorySortType
import it.matteoleggio.gallerydl.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository : HistoryRepository
    val sortOrder = MutableLiveData(HistorySort.DESC)
    val sortType = MutableLiveData(HistorySortType.DATE)
    val websiteFilter = MutableLiveData("")
    private val queryFilter = MutableLiveData("")
    private val formatFilter = MutableLiveData("")
    val allItems : LiveData<List<HistoryItem>>
    private var _items = MediatorLiveData<List<HistoryItem>>()

    init {
        val dao = DBManager.getInstance(application).historyDao
        repository = HistoryRepository(dao)
        allItems = repository.items

        _items.addSource(allItems){
            filter(queryFilter.value!!, formatFilter.value!!, websiteFilter.value!!, sortType.value!!, sortOrder.value!!)
        }
        _items.addSource(formatFilter){
            filter(queryFilter.value!!, formatFilter.value!!, websiteFilter.value!!, sortType.value!!, sortOrder.value!!)
        }
        _items.addSource(sortType){
            filter(queryFilter.value!!, formatFilter.value!!, websiteFilter.value!!, sortType.value!!, sortOrder.value!!)
        }
        _items.addSource(websiteFilter){
            filter(queryFilter.value!!, formatFilter.value!!, websiteFilter.value!!, sortType.value!!, sortOrder.value!!)
        }
        _items.addSource(queryFilter){
            filter(queryFilter.value!!, formatFilter.value!!, websiteFilter.value!!, sortType.value!!, sortOrder.value!!)
        }

    }

    fun getFilteredList() : LiveData<List<HistoryItem>>{
        return _items
    }

    fun setSorting(sort: HistorySortType){
        if (sortType.value != sort){
            sortOrder.value = HistorySort.DESC
        }else{
            sortOrder.value = if (sortOrder.value == HistorySort.DESC) {
                HistorySort.ASC
            } else HistorySort.DESC
        }
        sortType.value = sort
    }

    fun setWebsiteFilter(filter : String){
        websiteFilter.value = filter
    }

    fun setQueryFilter(filter: String){
        queryFilter.value = filter
    }

    fun setFormatFilter(filter: String){
        formatFilter.value = filter
    }

    private fun filter(query : String, format : String, site : String, sortType: HistorySortType, sort: HistorySort) = viewModelScope.launch(Dispatchers.IO){
        _items.postValue(repository.getFiltered(query, format, site, sortType, sort))
    }

    fun getAll() : List<HistoryItem> {
        return repository.getAll()
    }

    fun insert(item: HistoryItem) = viewModelScope.launch(Dispatchers.IO){
        repository.insert(item)
    }

    fun delete(item: HistoryItem, deleteFile: Boolean) = viewModelScope.launch(Dispatchers.IO){
        repository.delete(item)
        if (deleteFile){
            val fileUtil = FileUtil()
            fileUtil.deleteFile(item.downloadPath)
        }
    }

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAll()
    }

    fun deleteDuplicates() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteDuplicates()
    }

    fun update(item: HistoryItem) = viewModelScope.launch(Dispatchers.IO){
        repository.update(item)
    }

    fun clearDeleted() = viewModelScope.launch(Dispatchers.IO) {
        repository.clearDeletedHistory()
    }

}