package org.ergoplatform.android.mosaik

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.ergoplatform.mosaik.MosaikAppEntry
import org.ergoplatform.mosaik.MosaikAppHost

@Entity(tableName = "mosaik_app", indices = [Index("favorite", "name")])
data class MosaikAppDbEntity(
    @PrimaryKey val url: String,
    val name: String,
    val description: String?,
    val iconFile: String?,
    @ColumnInfo(name = "last_visited") val lastVisited: Long,
    val favorite: Boolean,
) {
    fun toModel(): MosaikAppEntry {
        return MosaikAppEntry(
            url,
            name,
            description = description,
            iconFile = iconFile,
            lastVisited = lastVisited,
            favorite,
        )
    }
}

fun MosaikAppEntry.toDbEntity() = MosaikAppDbEntity(
    url,
    name,
    description = description,
    iconFile = iconFile,
    lastVisited = lastVisited,
    favorite,
)

@Entity(tableName = "mosaik_host")
data class MosaikHostDbEntity(
    @PrimaryKey val hostName: String,
    val guid: String,
) {
    fun toModel(): MosaikAppHost =
        MosaikAppHost(
            hostName,
            guid,
        )
}

fun MosaikAppHost.toDbEntity() = MosaikHostDbEntity(
    hostName,
    guid,
)