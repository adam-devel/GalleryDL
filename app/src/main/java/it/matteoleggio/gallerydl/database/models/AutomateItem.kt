package it.matteoleggio.gallerydl.database.models

import androidx.room.PrimaryKey
import it.matteoleggio.gallerydl.database.viewmodel.DownloadViewModel.Type

data class AutomateItem(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    var title: String,
    var url: String,
    var intervalNr: Long,
    var intervalCategory: String,
    var format: Format,
    var type: Type,
    var downloadPath: String,
    var runOnLowBattery: Boolean,
    var runOnMobileData: Boolean,
    var replaceFiles: Boolean,
)