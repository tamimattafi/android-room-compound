package com.attafitamim.room.compound.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.room.Room
import com.attafitamim.room.compound.sample.room.database.MainDatabase
import com.attafitamim.room.compound.sample.room.entities.ForthEntity
import com.attafitamim.room.compound.sample.room.entities.ThirdCompound
import com.attafitamim.room.compound.sample.room.entities.ThirdEntity

class MainActivity : AppCompatActivity() {
    private val database = Room.databaseBuilder(
        this,
        MainDatabase::class.java,
        "main_database"
    ).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val thirdEntity = ThirdEntity("thirdEntity", "1/2/3")
        val forthEntity = ForthEntity("forthEntity", "1/2/3/4")

        database.thirdCompoundDao.insertOrUpdate(ThirdCompound(thirdEntity, forthEntity))
    }
}