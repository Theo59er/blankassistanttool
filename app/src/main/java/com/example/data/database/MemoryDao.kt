package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AssistantMemory
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM assistant_memories WHERE isArchived = 0 ORDER BY timestamp DESC")
    fun getAllActiveMemoriesFlow(): Flow<List<AssistantMemory>>

    @Query("SELECT * FROM assistant_memories WHERE isArchived = 0 ORDER BY timestamp DESC")
    suspend fun getAllActiveMemories(): List<AssistantMemory>

    @Query("SELECT * FROM assistant_memories ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentMemories(): List<AssistantMemory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AssistantMemory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<AssistantMemory>)

    @Update
    suspend fun updateMemory(memory: AssistantMemory)

    @Delete
    suspend fun deleteMemory(memory: AssistantMemory)

    @Query("DELETE FROM assistant_memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM assistant_memories WHERE isArchived = 0")
    suspend fun getCount(): Int

    @Query("DELETE FROM assistant_memories")
    suspend fun clearAll()
}
