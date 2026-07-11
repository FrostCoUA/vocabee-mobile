package com.vocabee.android.feature.vocabulary.data.local

import androidx.room.TypeConverter
import com.vocabee.android.feature.vocabulary.domain.model.SyncStatus

class VocabeeTypeConverters {
    @TypeConverter
    fun syncStatusToStorage(syncStatus: SyncStatus): String {
        return syncStatus.name
    }

    @TypeConverter
    fun syncStatusFromStorage(value: String): SyncStatus {
        return SyncStatus.entries.firstOrNull { status -> status.name == value }
            ?: SyncStatus.PendingUpdate
    }
}
