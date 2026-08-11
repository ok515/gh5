package com.pecmi.studio.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val width: Int,
    val height: Int,
    val layersJson: String,
    val settingsJson: String,
    val thumbnailPath: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
