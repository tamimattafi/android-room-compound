package com.attafitamim.room.compound.sample.room.database

import androidx.room.Dao
import androidx.room.Query

@Dao
interface MainCompound {

    @Query("UPDATE MainEntity SET name = :newName WHERE name = :oldName")
    suspend fun updateId(oldName: String, newName: String)
}
