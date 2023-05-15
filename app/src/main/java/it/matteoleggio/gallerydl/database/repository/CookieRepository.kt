package it.matteoleggio.gallerydl.database.repository

import androidx.lifecycle.LiveData
import it.matteoleggio.gallerydl.database.dao.CookieDao
import it.matteoleggio.gallerydl.database.models.CookieItem

class CookieRepository(private val cookieDao: CookieDao) {
    val items : LiveData<List<CookieItem>> = cookieDao.getAllCookiesLiveData()

    fun getAll() : List<CookieItem> {
        return cookieDao.getAllCookies()
    }


    suspend fun insert(item: CookieItem) : Long{
        if (! cookieDao.checkIfExistsWithSameURL(item.url)){
            return cookieDao.insert(item)
        }
        return -1
    }

    suspend fun delete(item: CookieItem){
        cookieDao.delete(item.id)
    }


    suspend fun deleteAll(){
        cookieDao.deleteAll()
    }

    suspend fun update(item: CookieItem){
        cookieDao.update(item)
    }

}