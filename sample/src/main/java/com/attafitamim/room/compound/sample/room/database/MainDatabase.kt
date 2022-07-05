package com.attafitamim.room.compound.sample.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.attafitamim.room.compound.sample.room.entities.ForthEntity
import com.attafitamim.room.compound.sample.room.entities.MainCompoundDao
import com.attafitamim.room.compound.sample.room.entities.MainEntity
import com.attafitamim.room.compound.sample.room.entities.SecondCompoundDao
import com.attafitamim.room.compound.sample.room.entities.SecondEntity
import com.attafitamim.room.compound.sample.room.entities.ThirdCompoundDao
import com.attafitamim.room.compound.sample.room.entities.ThirdEntity

@Database(
    entities = [
        MainEntity::class,
        SecondEntity::class,
        ThirdEntity::class,
        ForthEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MainDatabase : RoomDatabase() {
    abstract val mainCompoundDao: MainCompoundDao
    abstract val secondCompoundDao: SecondCompoundDao
    abstract val thirdCompoundDao: ThirdCompoundDao
}