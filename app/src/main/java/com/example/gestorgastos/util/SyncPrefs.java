package com.example.gestorgastos.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SyncPrefs {

    private static final String PREFS_NAME = "gestor_gastos_prefs";
    private static final String KEY_LAST_SYNC_MILLIS = "last_sync_millis";
    private static final String KEY_LAST_SYNC_CATEGORIES = "last_sync_categories_";
    private static final String KEY_LAST_SYNC_EXPENSES = "last_sync_expenses_";

    public static void setLastSyncMillis(Context context, long millis) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_SYNC_MILLIS, millis).apply();
    }

    public static long getLastSyncMillis(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_SYNC_MILLIS, 0L);
    }

    /**
     * Guarda el timestamp de la última sincronización de categorías para un usuario específico
     */
    public static void setLastSyncCategoriesMillis(Context context, String userUid, long millis) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_SYNC_CATEGORIES + userUid, millis).apply();
    }

    /**
     * Obtiene el timestamp de la última sincronización de categorías para un usuario específico
     */
    public static long getLastSyncCategoriesMillis(Context context, String userUid) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_SYNC_CATEGORIES + userUid, 0L);
    }

    /**
     * Guarda el timestamp de la última sincronización de gastos para un usuario específico
     */
    public static void setLastSyncExpensesMillis(Context context, String userUid, long millis) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_SYNC_EXPENSES + userUid, millis).apply();
    }

    /**
     * Obtiene el timestamp de la última sincronización de gastos para un usuario específico
     */
    public static long getLastSyncExpensesMillis(Context context, String userUid) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_SYNC_EXPENSES + userUid, 0L);
    }

}


