package it.matteoleggio.gallerydl.database

import androidx.room.TypeConverter
import it.matteoleggio.gallerydl.database.models.AudioPreferences
import it.matteoleggio.gallerydl.database.models.Format
import it.matteoleggio.gallerydl.database.models.VideoPreferences
import it.matteoleggio.gallerydl.database.viewmodel.DownloadViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class Converters {
    @TypeConverter
    fun stringToListOfFormats(value: String?): ArrayList<Format> {
        val listType: Type = object : TypeToken<ArrayList<Format?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun listOfFormatsToString(list: ArrayList<Format?>?): String {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun formatToString(format: Format): String = Gson().toJson(format)

    @TypeConverter
    fun stringToFormat(string: String): Format = Gson().fromJson(string, Format::class.java)


    @TypeConverter
    fun typeToString(type: DownloadViewModel.Type) : String = type.toString()
    @TypeConverter
    fun stringToType(string: String) : DownloadViewModel.Type {
        return when(string){
            "image" -> DownloadViewModel.Type.image
            else -> DownloadViewModel.Type.command
        }
    }

    @TypeConverter
    fun audioPreferencesToString(audioPreferences: AudioPreferences): String = Gson().toJson(audioPreferences)
    @TypeConverter
    fun stringToAudioPreferences(string: String): AudioPreferences = Gson().fromJson(string, AudioPreferences::class.java)

    @TypeConverter
    fun videoPreferencesToString(videoPreferences: VideoPreferences): String = Gson().toJson(videoPreferences)
    @TypeConverter
    fun stringToVideoPreferences(string: String): VideoPreferences = Gson().fromJson(string, VideoPreferences::class.java)
}