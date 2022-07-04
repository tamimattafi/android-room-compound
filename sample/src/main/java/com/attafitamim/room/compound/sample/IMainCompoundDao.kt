package com.attafitamim.room.compound.sample

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction

@Dao
interface IMainCompoundDao {

    @Transaction
    fun insertOrUpdate(compound: MainCompound) {

    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(mainEntities: List<MainEntity>)
}