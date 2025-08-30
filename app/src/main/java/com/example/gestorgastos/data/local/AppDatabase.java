package com.example.gestorgastos.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
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
    version = 1,
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
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
