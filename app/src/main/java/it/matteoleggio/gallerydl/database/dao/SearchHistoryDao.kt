package it.matteoleggio.gallerydl.database.dao

import androidx.room.*
import it.matteoleggio.gallerydl.database.models.SearchHistoryItem

@Dao
interface SearchHistoryDao {
    @Query("SELECT * from searchHistory ORDER BY id DESC LIMIT 10")
    fun getAll() : List<SearchHistoryItem>

    @Query("SELECT * from searchHistory WHERE `query` COLLATE NOCASE ='%'||:keyword||'%'")
    fun getAllByKeyword(keyword: String) : List<SearchHistoryItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(new: SearchHistoryItem)

    @Query("DELETE FROM searchHistory")
    suspend fun deleteAll()

    @Query("DELETE FROM searchHistory WHERE `query`=:query")
    suspend fun delete(query: String)
}