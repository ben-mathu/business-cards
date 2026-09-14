package com.benatt.businesscards.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.benatt.businesscards.data.local.entity.VCardEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for vCard storage and retrieval.
 */
@Dao
interface VCardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: VCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<VCardEntity>): List<Long>

    @Update
    suspend fun updateCard(card: VCardEntity): Int

    @Delete
    suspend fun deleteCard(card: VCardEntity): Int

    @Query("DELETE FROM vcards WHERE id = :id")
    suspend fun deleteCardById(id: Long): Int

    @Query("DELETE FROM vcards")
    suspend fun deleteAllCards(): Int

    @Query("SELECT * FROM vcards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): VCardEntity?

    @Query("SELECT * FROM vcards WHERE id = :id LIMIT 1")
    fun getCardByIdFlow(id: Long): Flow<VCardEntity?>

    @Query("SELECT * FROM vcards ORDER BY formattedName COLLATE NOCASE ASC")
    fun getAllCards(): Flow<List<VCardEntity>>

    @Query("SELECT * FROM vcards ORDER BY createdAt DESC")
    fun getAllCardsSortedByRecent(): Flow<List<VCardEntity>>

    @Query("""
        SELECT * FROM vcards 
        WHERE formattedName LIKE '%' || :query || '%'
           OR organization LIKE '%' || :query || '%'
           OR primaryPhone LIKE '%' || :query || '%'
           OR primaryEmail LIKE '%' || :query || '%'
           OR title LIKE '%' || :query || '%'
        ORDER BY formattedName COLLATE NOCASE ASC
    """)
    fun searchCards(query: String): Flow<List<VCardEntity>>

    @Query("SELECT COUNT(*) FROM vcards")
    suspend fun getCount(): Int
}
