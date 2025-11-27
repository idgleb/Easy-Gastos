package com.example.gestorgastos.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.gestorgastos.data.local.dao.CategoryDao;
import com.example.gestorgastos.data.local.dao.ExpenseDao;
import com.example.gestorgastos.data.local.dao.PlanDao;
import com.example.gestorgastos.data.local.dao.UserDao;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.data.local.entity.ExpenseEntity;
import com.example.gestorgastos.data.local.entity.PlanEntity;
import com.example.gestorgastos.data.local.entity.UserEntity;

@Database(
    entities = {
        UserEntity.class,
        PlanEntity.class,
        CategoryEntity.class,
        ExpenseEntity.class
    },
    version = 3,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    // DAOs
    public abstract UserDao userDao();
    public abstract PlanDao planDao();
    public abstract CategoryDao categoryDao();
    public abstract ExpenseDao expenseDao();
    
    // Singleton
    private static volatile AppDatabase INSTANCE;
    
    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "gestor_gastos_database"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    // Migración de versión 1 a 2: agregar campo planExpiresAt
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE users ADD COLUMN planExpiresAt INTEGER");
        }
    };
    
    // Migración de versión 2 a 3: agregar campos syncState y deletedAt
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE users ADD COLUMN syncState TEXT");
            database.execSQL("ALTER TABLE users ADD COLUMN deletedAt INTEGER");
        }
    };
}
