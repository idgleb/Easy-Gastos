package com.example.gestorgastos.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.gestorgastos.data.local.AppDatabase;
import com.example.gestorgastos.data.local.dao.UserDao;
import com.example.gestorgastos.data.local.entity.UserEntity;
import com.example.gestorgastos.data.remote.FirestoreDataSource;
import com.example.gestorgastos.util.DateTimeUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.content.SharedPreferences;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminRepositoryImpl implements AdminRepository {
    private static final String TAG = "AdminRepositoryImpl";
    
    private final UserDao userDao;
    private final FirestoreDataSource firestoreDataSource;
    private final ExecutorService executor;
    private final LiveData<List<UserEntity>> allUsersLiveData;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Context context;
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String CREATE_USER_ENDPOINT =
            "https://us-central1-gestor-gastos-app-6e1d9.cloudfunctions.net/createUserByAdmin";
    private static final String DELETE_USER_ENDPOINT =
            "https://us-central1-gestor-gastos-app-6e1d9.cloudfunctions.net/deleteUserByAdmin";
    private static final String PREF_PENDING_PASSWORDS = "pending_user_passwords";
    
    public AdminRepositoryImpl(Context context) {
        this.context = context;
        AppDatabase database = AppDatabase.getDatabase(context);
        this.userDao = database.userDao();
        this.firestoreDataSource = new FirestoreDataSource();
        this.executor = Executors.newSingleThreadExecutor();
        
        // Observar directamente desde Room (offline-first)
        this.allUsersLiveData = userDao.getAllUsers();
        
        // Cargar usuarios desde Firestore en background para sincronizar
        loadUsersFromFirestore();
    }
    
    /**
     * Guarda temporalmente una contraseña asociada a un UID temporal.
     * Se usa para poder reintentar la creación de usuarios cuando la app se vuelve a abrir.
     */
    private void savePendingPassword(String tempUid, String password) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_PENDING_PASSWORDS, Context.MODE_PRIVATE);
            // Encriptar básicamente la contraseña (usando Base64 como medida básica)
            // En producción, usar encriptación más robusta
            String encoded = Base64.encodeToString(password.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            prefs.edit().putString("pwd_" + tempUid, encoded).apply();
            Log.d(TAG, "Contraseña guardada temporalmente para UID: " + tempUid);
        } catch (Exception e) {
            Log.e(TAG, "Error al guardar contraseña temporal", e);
        }
    }
    
    /**
     * Recupera una contraseña temporal asociada a un UID.
     */
    private String getPendingPassword(String tempUid) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_PENDING_PASSWORDS, Context.MODE_PRIVATE);
            String encoded = prefs.getString("pwd_" + tempUid, null);
            if (encoded != null) {
                String password = new String(Base64.decode(encoded, Base64.NO_WRAP), StandardCharsets.UTF_8);
                return password;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al recuperar contraseña temporal", e);
        }
        return null;
    }
    
    /**
     * Elimina una contraseña temporal después de usarla.
     */
    private void removePendingPassword(String tempUid) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF_PENDING_PASSWORDS, Context.MODE_PRIVATE);
            prefs.edit().remove("pwd_" + tempUid).apply();
            Log.d(TAG, "Contraseña temporal eliminada para UID: " + tempUid);
        } catch (Exception e) {
            Log.e(TAG, "Error al eliminar contraseña temporal", e);
        }
    }
    
    @Override
    public LiveData<List<UserEntity>> getAllUsers() {
        return allUsersLiveData;
    }
    
    @Override
    public void createUser(UserEntity user, String password, RepositoryCallback<UserEntity> callback) {
        final String finalPassword = password;
        if (finalPassword == null || finalPassword.length() < 6) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Password inválida para creación de usuario"));
            }
            return;
        }
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (callback != null) {
                callback.onError(new IllegalStateException("Debe iniciar sesión como admin para crear usuarios."));
            }
            return;
        }

        executor.execute(() -> {
            try {
                // Generar un UID temporal para guardar en Room (offline-first)
                // El formato será "temp_" + timestamp para evitar colisiones
                String tempUid = "temp_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
                user.uid = tempUid;
                user.updatedAt = DateTimeUtil.getCurrentEpochMillis();
                user.syncState = "PENDING";
                user.deletedAt = null;
                
                // Establecer valores por defecto
                if (user.planId == null || user.planId.trim().isEmpty()) {
                    user.planId = "free";
                }
                if (user.role == null || user.role.trim().isEmpty()) {
                    user.role = "user";
                }
                if (user.zonaHoraria == null || user.zonaHoraria.trim().isEmpty()) {
                    user.zonaHoraria = DateTimeUtil.getCurrentZoneId();
                }
                
                // Guardar en Room inmediatamente (offline-first)
                user.idLocal = userDao.insertUser(user);
                Log.d(TAG, "✅ Usuario guardado en Room con UID temporal: " + tempUid + " (syncState: PENDING)");
                
                // Guardar contraseña temporalmente para poder reintentar más tarde
                savePendingPassword(tempUid, finalPassword);
                
                // El LiveData se actualizará automáticamente desde Room
                
                // Llamar al callback inmediatamente (no esperar a Cloud Function)
                if (callback != null) {
                    callback.onSuccess(user);
                }
                
                // Intentar crear en el servidor en background (no bloquea la UI)
                syncCreateUserWithServer(user, finalPassword);
                
            } catch (Exception e) {
                Log.e(TAG, "Error al crear usuario", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * Sincroniza la creación de un usuario con el servidor en background.
     * Se ejecuta de forma asíncrona y no bloquea la UI.
     */
    private void syncCreateUserWithServer(UserEntity user, String password) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "⚠️ No hay usuario autenticado, no se puede sincronizar creación");
            return;
        }

        currentUser.getIdToken(true)
                .addOnSuccessListener(result -> executor.execute(() ->
                        callCreateUserFunction(result.getToken(), user, password, null)))
                .addOnFailureListener(error -> {
                    Log.w(TAG, "⚠️ Error obteniendo ID token para sincronizar creación (se reintentará): " + user.uid, error);
                    // El usuario ya está guardado localmente, la sincronización puede reintentarse más tarde
                });
    }

    private void callCreateUserFunction(String idToken, UserEntity user, String password,
                                        RepositoryCallback<UserEntity> callback) {
        try {
            // Crear payload con los datos del usuario (sin el UID temporal)
            JSONObject payload = new JSONObject();
            payload.put("email", user.email);
            payload.put("password", password);
            payload.put("name", user.name);
            payload.put("role", user.role);
            payload.put("planId", user.planId);
            if (user.planExpiresAt != null) {
                payload.put("planExpiresAt", user.planExpiresAt);
            }
            payload.put("zonaHoraria", user.zonaHoraria);
            payload.put("isActive", user.isActive);

            RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(CREATE_USER_ENDPOINT)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + idToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new IOException("Cloud Function error: " + response.code() + " - " + responseBody);
                }
                JSONObject json = responseBody.isEmpty() ? new JSONObject() : new JSONObject(responseBody);
                String realUid = json.optString("uid", null);
                
                if (realUid == null || realUid.isEmpty()) {
                    throw new IOException("Cloud Function no devolvió UID");
                }
                
                Log.d(TAG, "✅ Usuario creado en el servidor: " + realUid);
                
                // Guardar el UID temporal antes de actualizarlo
                String tempUid = user.uid;
                
                // Actualizar el usuario en Room con el UID real y syncState = "SYNCED"
                executor.execute(() -> {
                    // Buscar el usuario por el UID temporal
                    UserEntity existingUser = userDao.getUserByUidSync(tempUid);
                    if (existingUser != null) {
                        // Actualizar con el UID real
                        existingUser.uid = realUid;
                        existingUser.syncState = "SYNCED";
                        existingUser.updatedAt = DateTimeUtil.getCurrentEpochMillis();
                        userDao.updateUser(existingUser);
                        Log.d(TAG, "✅ Usuario actualizado en Room con UID real: " + realUid);
                        
                        // Eliminar la contraseña temporal ya que la creación fue exitosa
                        removePendingPassword(tempUid);
                    } else {
                        // Si no se encuentra, crear uno nuevo con el UID real
                        user.uid = realUid;
                        user.syncState = "SYNCED";
                        user.updatedAt = DateTimeUtil.getCurrentEpochMillis();
                        user.idLocal = userDao.insertUser(user);
                        Log.d(TAG, "✅ Usuario creado en Room con UID real: " + realUid);
                        
                        // Eliminar la contraseña temporal
                        removePendingPassword(tempUid);
                    }
                });
                
                // Recargar lista desde Firestore para obtener datos actualizados del servidor
                loadUsersFromFirestore();
                
                if (callback != null) {
                    user.uid = realUid;
                    callback.onSuccess(user);
                }
            }
        } catch (IOException | JSONException e) {
            Log.w(TAG, "⚠️ Error al sincronizar creación con el servidor (se reintentará): " + user.uid, e);
            // Mantener syncState como "PENDING" para que se reintente más tarde
            // El usuario ya está guardado localmente con UID temporal
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    public void loadUsersFromFirestore() {
        executor.execute(() -> {
            try {
                firestoreDataSource.getAllUsers()
                    .addOnSuccessListener(querySnapshot -> {
                        executor.execute(() -> {
                            List<UserEntity> users = new ArrayList<>();
                            java.util.Set<String> firestoreUserIds = new java.util.HashSet<>();
                            
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    try {
                                        UserEntity user = mapDocumentToUser(doc);
                                        if (user != null) {
                                            firestoreUserIds.add(user.uid);
                                            
                                            // Guardar en Room también
                                            UserEntity existingUser = userDao.getUserByUidSync(user.uid);
                                            
                                            // Si no se encuentra por uid, buscar por email (para usuarios creados en offline con UID temporal)
                                            if (existingUser == null && user.email != null && !user.email.isEmpty()) {
                                                existingUser = userDao.findUserByEmail(user.email);
                                                if (existingUser != null) {
                                                    Log.d(TAG, "Usuario encontrado por email (creado en offline): " + existingUser.email);
                                                }
                                            }
                                            
                                            if (existingUser != null) {
                                                // Si el usuario está marcado como eliminado localmente, no sobrescribir
                                                if (existingUser.deletedAt != null && existingUser.deletedAt > 0) {
                                                    Log.d(TAG, "⚠️ Usuario " + user.uid + " está marcado como eliminado localmente, no se sobrescribe");
                                                    continue; // Saltar este usuario
                                                }
                                                
                                                // Actualizar si existe, pero preservar syncState si es "PENDING"
                                                // Si el usuario tiene cambios pendientes, no sobrescribir con "SYNCED"
                                                if (existingUser.syncState != null && existingUser.syncState.equals("PENDING")) {
                                                    user.syncState = "PENDING"; // Mantener pendiente si hay cambios locales
                                                } else {
                                                    user.syncState = "SYNCED"; // Sincronizado desde servidor
                                                }
                                                user.deletedAt = null; // Asegurar que no esté marcado como eliminado
                                                user.idLocal = existingUser.idLocal;
                                                userDao.updateUser(user);
                                            } else {
                                                // Insertar si no existe - viene del servidor, está sincronizado
                                                user.syncState = "SYNCED";
                                                user.deletedAt = null; // Asegurar que no esté marcado como eliminado
                                                user.idLocal = userDao.insertUser(user);
                                            }
                                            users.add(user);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error al mapear usuario: " + doc.getId(), e);
                                    }
                                }
                            }
                            
                            // Eliminar usuarios huérfanos (en Room pero no en Firestore)
                            try {
                                List<UserEntity> localUsers = userDao.getAllUsersSync();
                                int orphansRemoved = 0;
                                for (UserEntity localUser : localUsers) {
                                    // Solo eliminar si:
                                    // 1. No está en Firestore
                                    // 2. No tiene UID temporal (los temporales están pendientes de creación)
                                    // 3. No está marcado como eliminado
                                    // 4. No tiene syncState PENDING (tiene cambios pendientes)
                                    if (!firestoreUserIds.contains(localUser.uid) && 
                                        !localUser.uid.startsWith("temp_") &&
                                        (localUser.deletedAt == null || localUser.deletedAt == 0) &&
                                        (localUser.syncState == null || !localUser.syncState.equals("PENDING"))) {
                                        Log.w(TAG, "⚠️ Usuario huérfano detectado en refresh: " + localUser.uid + " (" + localUser.name + ") - Eliminando");
                                        userDao.deleteUserByUid(localUser.uid);
                                        orphansRemoved++;
                                    }
                                }
                                if (orphansRemoved > 0) {
                                    Log.d(TAG, "✅ " + orphansRemoved + " usuario(s) huérfano(s) eliminado(s)");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error al limpiar usuarios huérfanos", e);
                            }
                            
                            Log.d(TAG, "Usuarios sincronizados desde Firestore: " + users.size());
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al cargar usuarios desde Firestore", e);
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error al iniciar carga de usuarios", e);
            }
        });
    }
    
    private UserEntity mapDocumentToUser(DocumentSnapshot doc) {
        try {
            UserEntity user = new UserEntity();
            user.uid = doc.getId();
            user.name = doc.getString("name");
            user.email = doc.getString("email");
            user.role = doc.getString("role") != null ? doc.getString("role") : "user";
            user.planId = doc.getString("plan_id") != null ? doc.getString("plan_id") : "free";
            Long planExpiresAt = doc.getLong("plan_expires_at");
            user.planExpiresAt = planExpiresAt;
            user.zonaHoraria = doc.getString("zona_horaria") != null ? doc.getString("zona_horaria") : DateTimeUtil.getCurrentZoneId();
            Boolean isActive = doc.getBoolean("is_active");
            user.isActive = isActive != null ? isActive : true;
            
            // Obtener timestamp
            if (doc.getTimestamp("updated_at") != null) {
                user.updatedAt = doc.getTimestamp("updated_at").toDate().getTime();
            } else {
                user.updatedAt = DateTimeUtil.getCurrentEpochMillis();
            }
            
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error al mapear documento a UserEntity", e);
            return null;
        }
    }
    
    @Override
    public void updateUser(UserEntity user, RepositoryCallback<UserEntity> callback) {
        executor.execute(() -> {
            try {
                // Actualizar en Room inmediatamente (offline-first)
                user.updatedAt = DateTimeUtil.getCurrentEpochMillis();
                user.syncState = "PENDING"; // Marcar como pendiente de sincronización
                
                // Usar query explícita para asegurar que Room notifique cambios al LiveData
                userDao.updateUserFields(
                    user.uid,
                    user.name,
                    user.email,
                    user.role,
                    user.planId,
                    user.planExpiresAt,
                    user.zonaHoraria,
                    user.isActive,
                    user.syncState,
                    user.updatedAt
                );
                
                Log.d(TAG, "✅ Usuario actualizado en Room: " + user.uid + " (syncState: PENDING)");
                
                // El LiveData se actualizará automáticamente desde Room
                
                // Llamar al callback inmediatamente (no esperar a Firestore)
                if (callback != null) {
                    callback.onSuccess(user);
                }
                
                // Sincronizar con Firestore en background (no bloquea la UI)
                syncUserWithFirestore(user);
                
            } catch (Exception e) {
                Log.e(TAG, "Error al actualizar usuario", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * Sincroniza un usuario con Firestore en background.
     * Se ejecuta de forma asíncrona y no bloquea la UI.
     * Firestore manejará automáticamente el reintento cuando se restablezca la conexión.
     */
    private void syncUserWithFirestore(UserEntity user) {
        // Si el usuario tiene un UID temporal, no intentar actualizar en Firestore
        // En su lugar, debe pasar por syncCreateUserWithServer
        if (user.uid != null && user.uid.startsWith("temp_")) {
            Log.d(TAG, "⚠️ Usuario con UID temporal detectado en syncUserWithFirestore: " + user.uid + 
                  " - Debe crearse primero con syncCreateUserWithServer");
            return;
        }
        
        Log.d(TAG, "🔄 Sincronizando usuario con Firestore - UID: " + user.uid + ", Plan: " + user.planId);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", user.name);
        updates.put("email", user.email);
        updates.put("role", user.role);
        updates.put("plan_id", user.planId);
        if (user.planExpiresAt != null) {
            updates.put("plan_expires_at", user.planExpiresAt);
        } else {
            updates.put("plan_expires_at", null);
        }
        updates.put("zona_horaria", user.zonaHoraria);
        updates.put("is_active", user.isActive);
        updates.put("updated_at", com.google.firebase.firestore.FieldValue.serverTimestamp());
        
        Log.d(TAG, "📤 Datos a enviar a Firestore: plan_id=" + updates.get("plan_id"));
        
        firestoreDataSource.updateUser(user.uid, updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✅ Usuario sincronizado con Firestore: " + user.uid);
                // Actualizar estado de sincronización a SYNCED
                executor.execute(() -> {
                    UserEntity updatedUser = userDao.getUserByUidSync(user.uid);
                    if (updatedUser != null) {
                        updatedUser.syncState = "SYNCED";
                        // Usar query explícita para asegurar que Room notifique cambios
                        userDao.updateUserFields(
                            updatedUser.uid,
                            updatedUser.name,
                            updatedUser.email,
                            updatedUser.role,
                            updatedUser.planId,
                            updatedUser.planExpiresAt,
                            updatedUser.zonaHoraria,
                            updatedUser.isActive,
                            updatedUser.syncState,
                            updatedUser.updatedAt
                        );
                        Log.d(TAG, "✅ Estado de sincronización actualizado a SYNCED para: " + user.uid);
                    }
                });
                // Recargar lista desde Firestore para obtener datos actualizados del servidor
                loadUsersFromFirestore();
            })
            .addOnFailureListener(e -> {
                // Si el documento no existe (NOT_FOUND), el usuario probablemente fue creado en offline
                // y la Cloud Function falló. Eliminar el usuario huérfano de Room.
                if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                    com.google.firebase.firestore.FirebaseFirestoreException firestoreException = 
                        (com.google.firebase.firestore.FirebaseFirestoreException) e;
                    if (firestoreException.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND) {
                        Log.w(TAG, "⚠️ Usuario no existe en Firestore: " + user.uid + " - Eliminando usuario huérfano de Room");
                        executor.execute(() -> {
                            userDao.deleteUserByUid(user.uid);
                            Log.d(TAG, "✅ Usuario huérfano eliminado de Room: " + user.uid);
                        });
                        return;
                    }
                }
                
                Log.w(TAG, "⚠️ Error al sincronizar usuario con Firestore (se reintentará automáticamente): " + user.uid, e);
                // Mantener syncState como "PENDING" para que el icono siga visible
                // Firestore manejará automáticamente el reintento cuando se restablezca la conexión
                // No es necesario notificar al usuario aquí, ya que los cambios están guardados localmente
            });
    }
    
    @Override
    public void deleteUser(String uid, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                long deletedAt = DateTimeUtil.getCurrentEpochMillis();
                long updatedAt = DateTimeUtil.getCurrentEpochMillis();
                
                // Soft delete en Room (no eliminar físicamente)
                userDao.softDeleteUser(uid, deletedAt, updatedAt);
                Log.d(TAG, "✅ Usuario marcado como eliminado en Room: " + uid + " (syncState: PENDING)");
                
                // El LiveData se actualizará automáticamente desde Room
                
                // Llamar al callback inmediatamente (no esperar a Cloud Function)
                if (callback != null) {
                    callback.onSuccess(null);
                }
                
                // Intentar eliminar en el servidor en background (no bloquea la UI)
                syncDeleteUserWithServer(uid);
                
            } catch (Exception e) {
                Log.e(TAG, "Error al eliminar usuario", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * Sincroniza la eliminación de un usuario con el servidor en background.
     * Se ejecuta de forma asíncrona y no bloquea la UI.
     */
    private void syncDeleteUserWithServer(String uid) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "⚠️ No hay usuario autenticado, no se puede sincronizar eliminación");
            return;
        }

        currentUser.getIdToken(true)
                .addOnSuccessListener(result -> executor.execute(() ->
                        callDeleteUserFunction(result.getToken(), uid)))
                .addOnFailureListener(error -> {
                    Log.w(TAG, "⚠️ Error obteniendo ID token para sincronizar eliminación (se reintentará): " + uid, error);
                    // El usuario ya está eliminado localmente, la sincronización puede reintentarse más tarde
                });
    }

    private void callDeleteUserFunction(String idToken, String uid) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("uid", uid);

            RequestBody body = RequestBody.create(payload.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(DELETE_USER_ENDPOINT)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + idToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    throw new IOException("Cloud Function error: " + response.code() + " - " + responseBody);
                }

                Log.d(TAG, "✅ Usuario eliminado del servidor: " + uid);
                
                // Actualizar syncState a SYNCED y eliminar físicamente de Room
                executor.execute(() -> {
                    UserEntity user = userDao.getUserByUidSync(uid);
                    if (user != null && user.deletedAt != null) {
                        // Eliminar físicamente ahora que está sincronizado
                        userDao.deleteUserByUid(uid);
                        Log.d(TAG, "✅ Usuario eliminado físicamente de Room tras sincronización exitosa");
                    }
                });
                
                // Recargar lista desde Firestore para obtener datos actualizados del servidor
                loadUsersFromFirestore();
            }
        } catch (IOException | JSONException e) {
            Log.w(TAG, "⚠️ Error al sincronizar eliminación con el servidor (se reintentará): " + uid, e);
            // Mantener syncState como "PENDING" para que se reintente más tarde
        }
    }
    
    /**
     * Sincroniza todos los usuarios con actualizaciones pendientes con Firestore.
     * Se usa desde SyncWorker para reintentos en segundo plano cuando la app se vuelve a abrir.
     */
    public void syncPendingUsersWithFirestore() {
        executor.execute(() -> {
            try {
                List<UserEntity> pendingUsers = userDao.getPendingUsers();
                Log.d(TAG, "Sincronizando " + pendingUsers.size() + " usuarios PENDING con Firestore");
                
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Log.w(TAG, "⚠️ No hay usuario autenticado, no se pueden sincronizar usuarios pendientes");
                    return;
                }
                
                for (UserEntity user : pendingUsers) {
                    // Si el usuario tiene un UID temporal (empieza con "temp_"), es una creación pendiente
                    if (user.uid != null && user.uid.startsWith("temp_")) {
                        // Intentar crear en el servidor usando la contraseña guardada temporalmente
                        String savedPassword = getPendingPassword(user.uid);
                        if (savedPassword != null) {
                            Log.d(TAG, "Reintentando creación de usuario con UID temporal: " + user.uid);
                            syncCreateUserWithServer(user, savedPassword);
                        } else {
                            Log.w(TAG, "⚠️ Usuario con UID temporal encontrado pero sin contraseña guardada: " + user.uid + 
                                  " - No se puede reintentar la creación automáticamente");
                        }
                    } else {
                        // Es una actualización pendiente, sincronizar normalmente
                        syncUserWithFirestore(user);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al obtener usuarios pendientes para sincronizar", e);
            }
        });
    }
    
    /**
     * Sincroniza todas las eliminaciones pendientes con el servidor.
     * Se usa desde SyncWorker para reintentos en segundo plano.
     */
    public void syncPendingDeletionsWithServer() {
        executor.execute(() -> {
            try {
                List<UserEntity> pendingDeletions = userDao.getPendingDeletions();
                Log.d(TAG, "Sincronizando " + pendingDeletions.size() + " eliminaciones PENDING con el servidor");
                
                if (pendingDeletions.isEmpty()) {
                    return;
                }
                
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Log.w(TAG, "⚠️ No hay usuario autenticado, no se pueden sincronizar eliminaciones");
                    return;
                }
                
                currentUser.getIdToken(true)
                    .addOnSuccessListener(result -> {
                        String idToken = result.getToken();
                        for (UserEntity user : pendingDeletions) {
                            executor.execute(() -> callDeleteUserFunction(idToken, user.uid));
                        }
                    })
                    .addOnFailureListener(error -> {
                        Log.e(TAG, "Error obteniendo ID token para sincronizar eliminaciones", error);
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error al obtener eliminaciones pendientes", e);
            }
        });
    }
}

