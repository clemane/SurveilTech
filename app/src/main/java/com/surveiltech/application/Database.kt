package com.surveiltech.application

import android.app.Application
import androidx.room.*
import com.surveiltech.application.scanner.MacAddress
import com.surveiltech.application.util.inet4AddressFromInt
import java.net.Inet4Address
import java.util.*

@Database(
    entities = [Network::class, Device::class, Port::class, MacVendor::class, Scan::class],
    views = [DeviceWithName::class],
    version = 22
)
@TypeConverters(Converter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun networkDao(): NetworkDao
    abstract fun deviceDao(): DeviceDao
    abstract fun portDao(): PortDao
    abstract fun scanDao(): ScanDao

    companion object {
        fun createInstance(application: Application): AppDatabase {
            return Room
                .databaseBuilder(
                    application.applicationContext,
                    AppDatabase::class.java, "ning-db"
                )
                .createFromAsset("mac_devices.db")
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

class Converter {

    @TypeConverter
    fun toInet4Address(value: Int?): Inet4Address? {
        return if (value != null) inet4AddressFromInt(
            "",
            value
        ) else null
    }

    @TypeConverter
    fun fromInet4Address(value: Inet4Address?): Int? {
        return value?.hashCode()
    }

    @TypeConverter
    fun toProtocol(value: String?): Protocol? {
        if (value == null) return null
        return Protocol.valueOf(value)
    }

    @TypeConverter
    fun fromProtocol(value: Protocol?): String? {
        if (value == null) return null
        return value.name
    }

    @TypeConverter
    fun toMacAddress(value: String?): MacAddress? {
        return if (value != null) MacAddress(value.uppercase(Locale.getDefault())) else null
    }

    @TypeConverter
    fun fromMacAddress(value: MacAddress?): String? {
        return value?.address?.uppercase(Locale.getDefault())
    }
}