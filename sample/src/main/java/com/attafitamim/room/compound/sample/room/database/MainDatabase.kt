package com.attafitamim.room.compound.sample.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.attafitamim.room.compound.sample.room.entities.ForthEntity
import com.attafitamim.room.compound.sample.room.entities.IMainCompoundDao
import com.attafitamim.room.compound.sample.room.entities.MainEntity
import com.attafitamim.room.compound.sample.room.entities.ISecondCompoundDao
import com.attafitamim.room.compound.sample.room.entities.SecondEntity
import com.attafitamim.room.compound.sample.room.entities.IThirdCompoundDao
import com.attafitamim.room.compound.sample.room.entities.MainSecondJunction
import com.attafitamim.room.compound.sample.room.entities.SecondThirdJunction
import com.attafitamim.room.compound.sample.room.entities.ThirdEntity

@Database(
    entities = [
        MainEntity::class,
        MainSecondJunction::class,
        SecondEntity::class,
        SecondThirdJunction::class,
        ThirdEntity::class,
        ForthEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MainDatabase : RoomDatabase() {
    abstract val mainCompoundDao: IMainCompoundDao
    abstract val secondCompoundDao: ISecondCompoundDao
    abstract val thirdCompoundDao: IThirdCompoundDao
}