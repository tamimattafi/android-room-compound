package com.attafitamim.room.compound.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.room.Room
import com.attafitamim.room.compound.sample.room.database.MainDatabase
import com.attafitamim.room.compound.sample.room.entities.ForthEntity
import com.attafitamim.room.compound.sample.room.entities.MainCompound
import com.attafitamim.room.compound.sample.room.entities.MainEntity
import com.attafitamim.room.compound.sample.room.entities.SecondCompound
import com.attafitamim.room.compound.sample.room.entities.SecondEntity
import com.attafitamim.room.compound.sample.room.entities.ThirdCompound
import com.attafitamim.room.compound.sample.room.entities.ThirdEntity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val database by lazy {
        Room.databaseBuilder(
            this,
            MainDatabase::class.java,
            "main_database"
        ).build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val forthEntity = ForthEntity("forthEntity", "1/2/3/4")
        val thirdEntity = ThirdEntity("thirdEntity", "1/2/3")
        val thirdCompound = ThirdCompound(thirdEntity, forthEntity)

        val secondEntity = SecondEntity("secondEntity", "1/2/3")
        val secondCompound = SecondCompound(secondEntity, thirdCompound)

        val mainEntity = MainEntity("mainEntity", "1/2/3")
        GlobalScope.launch {
            database.mainCompoundDao.insertOrUpdate(MainCompound(mainEntity, secondCompound))
        }
    }
}