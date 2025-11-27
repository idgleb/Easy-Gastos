package com.example.gestorgastos.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.gestorgastos.data.repository.CategoryRepositoryImpl;
import com.example.gestorgastos.data.repository.ExpenseRepositoryImpl;
import com.example.gestorgastos.data.repository.AdminRepositoryImpl;
import com.example.gestorgastos.util.SyncPrefs;
import com.example.gestorgastos.data.repository.AuthRepositoryImpl;

/**
 * Worker encargado de sincronizar con Firestore todas las entidades pendientes.
 * Sincroniza:
 * - Categorías con syncState = "PENDING" (creaciones, actualizaciones, eliminaciones)
 * - Gastos con syncState = "PENDING" (creaciones, actualizaciones, eliminaciones)
 * - Usuarios con syncState = "PENDING" (actualizaciones)
 * - Eliminaciones de usuarios pendientes (soft deletes)
 */
public class SyncWorker extends Worker {

    public static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Iniciando sincronización en segundo plano con Firestore");

            Context appContext = getApplicationContext();

            // Repositorios (cada uno maneja sus propios hilos internos)
            CategoryRepositoryImpl categoryRepository = new CategoryRepositoryImpl(appContext);
            ExpenseRepositoryImpl expenseRepository = new ExpenseRepositoryImpl(appContext);
            AdminRepositoryImpl adminRepository = new AdminRepositoryImpl(appContext);

            // Lanzar sincronización de pendientes (fire-and-forget)
            categoryRepository.syncPendingCategoriesWithFirestore();
            expenseRepository.syncPendingExpensesWithFirestore();
            adminRepository.syncPendingUsersWithFirestore(); // Actualizaciones pendientes
            adminRepository.syncPendingDeletionsWithServer(); // Eliminaciones pendientes

            // Además, sincronizar datos desde Firestore hacia Room para el usuario actual
            AuthRepositoryImpl authRepository = new AuthRepositoryImpl(appContext);
            authRepository.syncUserDataIfNeeded();

            // Guardar la hora del último intento de sincronización
            SyncPrefs.setLastSyncMillis(appContext, System.currentTimeMillis());

            Log.d(TAG, "SyncWorker ejecutado correctamente (sincronización lanzada y timestamp guardado)");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error en SyncWorker", e);
            // Permitir reintento en caso de fallo
            return Result.retry();
        }
    }
}


