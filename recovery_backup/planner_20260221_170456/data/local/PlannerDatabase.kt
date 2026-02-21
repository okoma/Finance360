package com.i2medier.financialpro.planner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
@Database(
    entities = [TransactionEntity::class, GoalEntity::class, StreakEntity::class, BillEntity::class, AccountEntity::class],
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
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN category TEXT NOT NULL DEFAULT 'all'"
                )
                    "CREATE INDEX IF NOT EXISTS index_transactions_category ON transactions(category)"
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
                db.execSQL("ALTER TABLE goals ADD COLUMN description TEXT")
                db.execSQL("ALTER TABLE goals ADD COLUMN target_date INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_goals_target_date ON goals(target_date)")
        private val MIGRATION_3_4 = object : Migration(3, 4) {
                    "CREATE TABLE IF NOT EXISTS bills (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "amount REAL NOT NULL, " +
                        "due_date INTEGER NOT NULL, " +
                        "is_paid INTEGER NOT NULL, " +
                        "category TEXT NOT NULL DEFAULT 'all', " +
                        "created_at INTEGER NOT NULL" +
                        ")"
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_due_date ON bills(due_date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_is_paid ON bills(is_paid)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_category ON bills(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_due_date_is_paid ON bills(due_date, is_paid)")
        private val MIGRATION_4_5 = object : Migration(4, 5) {
                db.execSQL("ALTER TABLE bills ADD COLUMN repeat TEXT")
        private val MIGRATION_5_6 = object : Migration(5, 6) {
                    "CREATE TABLE IF NOT EXISTS transactions_new (" +
                        "type TEXT NOT NULL, " +
                        "date INTEGER NOT NULL, " +
                        "goal_id INTEGER, " +
                        "bill_id INTEGER, " +
                        "note TEXT, " +
                        "category TEXT NOT NULL, " +
                        "FOREIGN KEY(goal_id) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE SET NULL, " +
                        "FOREIGN KEY(bill_id) REFERENCES bills(id) ON UPDATE NO ACTION ON DELETE SET NULL" +
                    "INSERT INTO transactions_new (id, amount, type, date, goal_id, bill_id, note, category) " +
                        "SELECT id, amount, type, date, goal_id, NULL, note, category FROM transactions"
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_goal_id ON transactions(goal_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_bill_id ON transactions(bill_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_category ON transactions(category)")
        private val MIGRATION_6_7 = object : Migration(6, 7) {
                    "CREATE TABLE IF NOT EXISTS accounts (" +
                        "name TEXT NOT NULL, " +
                        "opening_balance REAL NOT NULL, " +
                        "is_default INTEGER NOT NULL, " +
                        "currency TEXT NOT NULL" +
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_type ON accounts(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_is_default ON accounts(is_default)")
                    "INSERT OR IGNORE INTO accounts (id, name, type, opening_balance, is_default, currency) VALUES " +
                        "(1, 'Cash', 'ASSET', 0.0, 1, ''), " +
                        "(2, 'Savings', 'ASSET', 0.0, 1, ''), " +
                        "(3, 'Investments', 'ASSET', 0.0, 1, ''), " +
                        "(4, 'Loans', 'LIABILITY', 0.0, 1, ''), " +
                        "(5, 'Credit Cards', 'LIABILITY', 0.0, 1, '')"
                        "account_id INTEGER NOT NULL, " +
                        "FOREIGN KEY(bill_id) REFERENCES bills(id) ON UPDATE NO ACTION ON DELETE SET NULL, " +
                        "FOREIGN KEY(account_id) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE NO ACTION" +
                    "INSERT INTO transactions_new (id, amount, type, date, goal_id, bill_id, account_id, note, category) " +
                        "SELECT id, amount, type, date, goal_id, bill_id, " +
                        "CASE WHEN type = 'SAVING' THEN 2 ELSE 1 END, " +
                        "note, category FROM transactions"
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_account_id ON transactions(account_id)")
        private val MIGRATION_7_8 = object : Migration(7, 8) {
                        "note, COALESCE(NULLIF(category, ''), 'all') " +
                        "FROM transactions"
        private val MIGRATION_8_9 = object : Migration(8, 9) {
                db.execSQL("ALTER TABLE goals ADD COLUMN category TEXT NOT NULL DEFAULT 'other'")
        @Volatile
        private var INSTANCE: PlannerDatabase? = null
        fun getInstance(context: Context): PlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlannerDatabase::class.java,
                    DB_NAME
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
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
                    })
                    .build()
                INSTANCE = instance
                instance
    }
}
