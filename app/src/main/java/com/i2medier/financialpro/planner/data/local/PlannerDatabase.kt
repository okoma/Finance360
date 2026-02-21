package com.i2medier.financialpro.planner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TransactionEntity::class,
        GoalEntity::class,
        StreakEntity::class,
        BillEntity::class,
        AccountEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(PlannerTypeConverters::class)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao
    abstract fun streakDao(): StreakDao
    abstract fun billDao(): BillDao
    abstract fun accountDao(): AccountDao

    companion object {
        private const val DB_NAME = "planner_database"

        @Volatile
        private var INSTANCE: PlannerDatabase? = null

        fun getInstance(context: Context): PlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlannerDatabase::class.java,
                    DB_NAME
                ).addCallback(object : Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL(
                            "INSERT OR IGNORE INTO accounts (id, name, type, opening_balance, is_default, currency) VALUES " +
                                "(1, 'Cash', 'ASSET', 0.0, 1, ''), " +
                                "(2, 'Savings', 'ASSET', 0.0, 1, ''), " +
                                "(3, 'Investments', 'ASSET', 0.0, 1, ''), " +
                                "(4, 'Loans', 'LIABILITY', 0.0, 1, ''), " +
                                "(5, 'Credit Cards', 'LIABILITY', 0.0, 1, '')"
                        )
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
