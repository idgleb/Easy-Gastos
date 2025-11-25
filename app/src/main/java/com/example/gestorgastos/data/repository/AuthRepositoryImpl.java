package com.example.gestorgastos.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.gestorgastos.data.local.AppDatabase;
import com.example.gestorgastos.data.local.dao.UserDao;
import com.example.gestorgastos.data.local.dao.CategoryDao;
import com.example.gestorgastos.data.local.entity.UserEntity;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import com.example.gestorgastos.data.remote.FirebaseAuthDataSource;
import com.example.gestorgastos.data.remote.FirestoreDataSource;
import com.example.gestorgastos.domain.repository.AuthRepository;
import com.example.gestorgastos.util.DateTimeUtil;
import com.example.gestorgastos.util.SyncPrefs;
import com.example.gestorgastos.data.repository.CategoryRepository;
import com.example.gestorgastos.data.repository.CategoryRepositoryImpl;
import com.example.gestorgastos.data.repository.ExpenseRepositoryImpl;
import com.example.gestorgastos.data.repository.ExpenseRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuthRepositoryImpl implements AuthRepository {
    private static final String TAG = "AuthRepositoryImpl";
    
    private final Context context;
    private final FirebaseAuthDataSource authDataSource;
    private final FirestoreDataSource firestoreDataSource;
    private final UserDao userDao;
    private final CategoryDao categoryDao;
    private final ExecutorService executor;
    private ListenerRegistration userListener; // Listener para cambios en tiempo real del usuario
    private MutableLiveData<UserEntity> userLiveData; // LiveData persistente para el usuario actual
    private final AtomicBoolean categoriesSyncInProgress = new AtomicBoolean(false);
    private final AtomicBoolean expensesSyncInProgress = new AtomicBoolean(false);
    
    public AuthRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
        this.authDataSource = new FirebaseAuthDataSource();
        this.firestoreDataSource = new FirestoreDataSource();
        this.userDao = AppDatabase.getDatabase(context).userDao();
        this.categoryDao = AppDatabase.getDatabase(context).categoryDao();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    @Override
    public LiveData<UserEntity> getCurrentUser() {
        // Usar instancia persistente de LiveData para que el listener siempre actualice el mismo objeto
        if (userLiveData == null) {
            userLiveData = new MutableLiveData<>();
        }
        
        FirebaseUser firebaseUser = authDataSource.getCurrentUser();
        
        if (firebaseUser != null) {
            // Verificar si el listener ya está configurado para este usuario
            String currentUid = firebaseUser.getUid();
            UserEntity currentUser = userLiveData.getValue();
            
            // Siempre configurar el listener si no existe o si cambió el usuario
            boolean shouldSetupListener = userListener == null || 
                                         currentUser == null || 
                                         !currentUid.equals(currentUser.uid);
            
            // Cargar datos de Room solo si el usuario no está en el LiveData
            boolean shouldLoadFromRoom = currentUser == null || !currentUid.equals(currentUser.uid);
            
            if (shouldLoadFromRoom) {
                Log.d(TAG, "📥 Cargando usuario desde Room para: " + currentUid);
            // Primero buscar en Room
            executor.execute(() -> {
                    // Verificar y expirar planes antes de obtener el usuario
                    long currentTime = DateTimeUtil.getCurrentEpochMillis();
                    userDao.expirePlans(currentTime, currentTime);
                    
                    UserEntity existingUser = userDao.getUserByUidSync(currentUid);
                
                if (existingUser != null) {
                        // Verificar si el plan expiró
                        if (existingUser.planExpiresAt != null && existingUser.planExpiresAt < currentTime && !"free".equals(existingUser.planId)) {
                            existingUser.planId = "free";
                            existingUser.planExpiresAt = null;
                            existingUser.updatedAt = currentTime;
                            userDao.updateUser(existingUser);
                            syncUserToFirestore(existingUser);
                            Log.d(TAG, "Plan expirado para usuario: " + existingUser.name);
                        }
                        
                    // Usuario existe en Room, devolverlo
                    Log.d(TAG, "Usuario encontrado en Room: " + existingUser.name);
                    userLiveData.postValue(existingUser);
                    
                    // Verificar y sincronizar datos si están desactualizados
                    checkAndSyncUserData(currentUid);
                } else {
                    // Usuario no existe en Room, crearlo y guardarlo
                    UserEntity userEntity = new UserEntity();
                        userEntity.uid = currentUid;
                    userEntity.email = firebaseUser.getEmail();
                    userEntity.name = firebaseUser.getDisplayName() != null ? 
                        firebaseUser.getDisplayName() : 
                        firebaseUser.getEmail().split("@")[0];
                    userEntity.role = "user";
                    userEntity.planId = "free";
                    userEntity.zonaHoraria = DateTimeUtil.getCurrentZoneId();
                    userEntity.isActive = true;
                    userEntity.updatedAt = DateTimeUtil.getCurrentEpochMillis();
                    
                    // Guardar en Room
                    long userId = userDao.insertUser(userEntity);
                    userEntity.idLocal = userId;
                    
                    Log.d(TAG, "Usuario creado y guardado en Room: " + userEntity.name + " (ID: " + userId + ")");

                    // Sincronizar datos básicos del usuario con Firestore
                    syncUserToFirestore(userEntity);

                    userLiveData.postValue(userEntity);
                    
                    // Sincronizar datos desde Firestore (dispositivo nuevo o primera vez)
                    checkAndSyncUserData(currentUid);
                }
            });
            }
            
            // Siempre configurar el listener si no está configurado o si cambió el usuario
            if (shouldSetupListener) {
                Log.d(TAG, "🔧 Configurando listener de Firestore para escuchar cambios en tiempo real");
                // Configurar listener de Firestore para escuchar cambios en tiempo real
                // Esto detectará cuando el webhook actualice el plan
                setupFirestoreUserListener(currentUid, userLiveData);
            } else {
                Log.d(TAG, "ℹ️ Listener ya está configurado para este usuario");
            }
        } else {
            Log.d(TAG, "No hay usuario autenticado");
            removeUserListener();
            // Limpiar LiveData
            userLiveData.postValue(null);
        }
        
        return userLiveData;
    }
    
    /**
     * Configura un listener de Firestore para escuchar cambios en tiempo real del usuario.
     * Cuando el webhook actualiza el plan, este listener detectará el cambio y actualizará Room.
     */
    private void setupFirestoreUserListener(String uid, MutableLiveData<UserEntity> userLiveData) {
        // Remover listener anterior si existe
        removeUserListener();
        
        // Configurar nuevo listener
        Log.d(TAG, "🔔 Configurando listener de Firestore para uid: " + uid);
        userListener = firestoreDataSource.getUserSnapshotListener(uid, (snapshot, error) -> {
            if (error != null) {
                if (error instanceof FirebaseFirestoreException) {
                    FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) error).getCode();
                    if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED && authDataSource.getCurrentUser() == null) {
                        Log.d(TAG, "Listener de Firestore cancelado tras cerrar sesión, ignorando PERMISSION_DENIED.");
                        removeUserListener();
                        return;
                    }
                }
                Log.e(TAG, "❌ Error en listener de Firestore", error);
                return;
            }
            
            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG, "📥 Snapshot recibido de Firestore para uid: " + uid);
                executor.execute(() -> {
                    try {
                        // Obtener datos de Firestore
                        String name = snapshot.getString("name");
                        String email = snapshot.getString("email");
                        String role = snapshot.getString("role");
                        String planId = snapshot.getString("plan_id");
                        Long planExpiresAt = snapshot.getLong("plan_expires_at");
                        String zonaHoraria = snapshot.getString("zona_horaria");
                        Boolean isActive = snapshot.getBoolean("is_active");
                        
                        Log.d(TAG, "📋 Datos de Firestore - planId: " + planId + ", planExpiresAt: " + planExpiresAt);
                        
                        // Obtener usuario actual de Room
                        UserEntity existingUser = userDao.getUserByUidSync(uid);
                        
                        if (existingUser != null) {
                            Log.d(TAG, "👤 Usuario encontrado en Room - planId actual: " + existingUser.planId);
                            
                            // Actualizar solo si hay cambios (especialmente en plan_id)
                            boolean needsUpdate = false;
                            
                            if (planId != null && !planId.equals(existingUser.planId)) {
                                Log.d(TAG, "🔄 Plan cambiado de '" + existingUser.planId + "' a '" + planId + "'");
                                existingUser.planId = planId;
                                needsUpdate = true;
                            }
                            
                            if (planExpiresAt != null && !planExpiresAt.equals(existingUser.planExpiresAt)) {
                                existingUser.planExpiresAt = planExpiresAt;
                                needsUpdate = true;
                            }
                            
                            if (name != null && !name.equals(existingUser.name)) {
                                existingUser.name = name;
                                needsUpdate = true;
                            }
                            
                            if (email != null && !email.equals(existingUser.email)) {
                                existingUser.email = email;
                                needsUpdate = true;
                            }
                            
                            if (role != null && !role.equals(existingUser.role)) {
                                existingUser.role = role;
                                needsUpdate = true;
                            }
                            
                            if (zonaHoraria != null && !zonaHoraria.equals(existingUser.zonaHoraria)) {
                                existingUser.zonaHoraria = zonaHoraria;
                                needsUpdate = true;
                            }
                            
                            if (isActive != null && isActive != existingUser.isActive) {
                                existingUser.isActive = isActive;
                                needsUpdate = true;
                            }
                            
                            if (needsUpdate) {
                                existingUser.updatedAt = DateTimeUtil.getCurrentEpochMillis();
                                userDao.updateUser(existingUser);
                                Log.d(TAG, "✅ Usuario actualizado desde Firestore: " + existingUser.name + " (planId: " + existingUser.planId + ")");
                                Log.d(TAG, "📤 Posteando usuario actualizado al LiveData");
                                userLiveData.postValue(existingUser);
                                Log.d(TAG, "✅ Usuario posteado al LiveData exitosamente");
                            } else {
                                Log.d(TAG, "ℹ️ No hay cambios en el usuario, no se actualiza");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al procesar snapshot de Firestore", e);
                    }
                });
            }
        });
    }
    
    @Override
    public String getCurrentUserUid() {
        FirebaseUser firebaseUser = authDataSource.getCurrentUser();
        return firebaseUser != null ? firebaseUser.getUid() : null;
    }
    
    @Override
    public void signIn(String email, String password, AuthCallback callback) {
        authDataSource.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        checkAndCreateUser(firebaseUser, callback);
                    } else {
                        callback.onError(new Exception("Error al obtener usuario"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error en sign in", e);
                    callback.onError(e);
                });
    }
    
    @Override
    public void signUp(String email, String password, String name, AuthCallback callback) {
        authDataSource.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        createNewUser(firebaseUser, name, email, callback);
                    } else {
                        callback.onError(new Exception("Error al crear usuario"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error en sign up", e);
                    callback.onError(e);
                });
    }
    
    @Override
    public void signInWithGoogle(String idToken, AuthCallback callback) {
        Log.d(TAG, "Iniciando autenticación con Google");
        
        if (idToken == null || idToken.isEmpty()) {
            callback.onError(new Exception("Token de Google inválido"));
            return;
        }
        
        // Crear credencial de Google
        com.google.firebase.auth.AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        
        // Autenticar con Firebase
        authDataSource.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        Log.d(TAG, "Autenticación con Google exitosa: " + firebaseUser.getEmail());
                        // Usar el nombre de Google si está disponible, sino usar el email
                        String name = firebaseUser.getDisplayName();
                        if (name == null || name.trim().isEmpty()) {
                            name = firebaseUser.getEmail() != null ? 
                                   firebaseUser.getEmail().split("@")[0] : "Usuario";
                        }
                        // Verificar si el usuario ya existe, si no, crearlo
                        checkAndCreateUser(firebaseUser, callback);
                    } else {
                        callback.onError(new Exception("Error al obtener usuario de Google"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error en autenticación con Google", e);
                    callback.onError(e);
                });
    }
    
    @Override
    public void signOut() {
        removeUserListener();
        authDataSource.signOut();
        if (userLiveData != null) {
            userLiveData.postValue(null);
        }
    }
    
    @Override
    public void resetPassword(String email, AuthCallback callback) {
        Log.d(TAG, "Iniciando reset de contraseña para: " + email);
        
        if (email == null || email.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("El email es requerido"));
            return;
        }
        
        authDataSource.resetPassword(email.trim())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Reset de contraseña exitoso para: " + email);
                    // Crear un UserEntity temporal para el callback
                    UserEntity tempUser = new UserEntity();
                    tempUser.email = email;
                    callback.onSuccess(tempUser);
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Error en reset de contraseña: " + error.getMessage());
                    callback.onError(error);
                });
    }
    
    @Override
    public void deleteAccount(AuthCallback callback) {
        FirebaseUser currentUser = authDataSource.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("No hay usuario autenticado"));
            return;
        }
        
        // Marcar usuario como inactivo en Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("is_active", false);
        updates.put("updated_at", FieldValue.serverTimestamp());
        
        firestoreDataSource.updateUser(currentUser.getUid(), updates)
                .addOnSuccessListener(aVoid -> {
                    // Eliminar cuenta de Firebase Auth
                    Task<Void> deleteTask = authDataSource.deleteUser();
                    if (deleteTask != null) {
                        deleteTask.addOnSuccessListener(aVoid2 -> {
                            // Limpiar datos locales
                            executor.execute(() -> {
                                userDao.deleteUserByUid(currentUser.getUid());
                                callback.onSuccess(null);
                            });
                        }).addOnFailureListener(callback::onError);
                    } else {
                        // Si no hay usuario para eliminar, solo limpiar datos locales
                        executor.execute(() -> {
                            userDao.deleteUserByUid(currentUser.getUid());
                            callback.onSuccess(null);
                        });
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    @Override
    public void syncUserDataIfNeeded() {
        String userUid = getCurrentUserUid();
        if (userUid == null || userUid.trim().isEmpty()) {
            Log.d(TAG, "syncUserDataIfNeeded: no hay usuario autenticado, se omite sincronización.");
            return;
        }
        Log.d(TAG, "syncUserDataIfNeeded: verificando sincronización para usuario " + userUid);
        checkAndSyncUserData(userUid);
    }
    
    private void checkAndCreateUser(FirebaseUser firebaseUser, AuthCallback callback) {
        String uid = firebaseUser.getUid();
        
        // Verificar si el usuario ya existe en Room
        executor.execute(() -> {
            UserEntity existingUser = userDao.getUserByUidSync(uid);
            
            if (existingUser != null) {
                // Usuario ya existe, solo devolverlo y asegurarse de sincronizarlo en Firestore
                Log.d(TAG, "Usuario existente encontrado: " + existingUser.name);
                syncUserToFirestore(existingUser);
                callback.onSuccess(existingUser);
            } else {
                // Usuario nuevo, crearlo y agregar categorías por defecto
                UserEntity userEntity = new UserEntity();
                userEntity.uid = uid;
                userEntity.email = firebaseUser.getEmail();
                userEntity.name = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : firebaseUser.getEmail().split("@")[0];
                userEntity.role = "user";
                userEntity.planId = "free";
                userEntity.zonaHoraria = DateTimeUtil.getCurrentZoneId();
                userEntity.isActive = true;
                userEntity.updatedAt = DateTimeUtil.getCurrentEpochMillis();
                
                // Guardar usuario en Room
                long userId = userDao.insertUser(userEntity);
                userEntity.idLocal = userId;
                
                Log.d(TAG, "Nuevo usuario creado: " + userEntity.name + " (ID: " + userId + ")");
                
                // Crear categorías por defecto para el nuevo usuario
                createDefaultCategories(uid);
                
                callback.onSuccess(userEntity);
            }
        });
    }
    
    private void removeUserListener() {
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }
    
    private void createNewUser(FirebaseUser firebaseUser, String name, String email, AuthCallback callback) {
        String uid = firebaseUser.getUid();
        // Generar nombre desde el email si está vacío o es null
        String userName;
        if (name != null && !name.trim().isEmpty()) {
            userName = name.trim();
        } else {
            // Extraer la parte del email antes del @
            userName = email.split("@")[0];
            Log.d(TAG, "Nombre generado automáticamente desde email: " + userName);
        }
        String zoneId = DateTimeUtil.getCurrentZoneId();
        
        // Crear UserEntity directamente
        UserEntity userEntity = new UserEntity();
        userEntity.uid = uid;
        userEntity.name = userName;
        userEntity.email = email;
        userEntity.role = "user";
        userEntity.planId = "free";
        userEntity.zonaHoraria = zoneId;
        userEntity.isActive = true;
        userEntity.updatedAt = DateTimeUtil.getCurrentEpochMillis();
        
        // Guardar usuario en Room
        executor.execute(() -> {
            long userId = userDao.insertUser(userEntity);
            userEntity.idLocal = userId;
            
            Log.d(TAG, "Nuevo usuario registrado: " + userEntity.name + " (ID: " + userId + ")");
            
            // Crear categorías por defecto para el nuevo usuario
            createDefaultCategories(uid);
            
            callback.onSuccess(userEntity);
        });
    }
    
    private void syncUserFromFirestore(com.google.firebase.firestore.DocumentSnapshot documentSnapshot, AuthCallback callback) {
        String uid = documentSnapshot.getId();
        String name = documentSnapshot.getString("name");
        String email = documentSnapshot.getString("email");
        String role = documentSnapshot.getString("role");
        String planId = documentSnapshot.getString("plan_id");
        String zonaHoraria = documentSnapshot.getString("zona_horaria");
        Boolean isActive = documentSnapshot.getBoolean("is_active");
        Long planExpiresAt = documentSnapshot.getLong("plan_expires_at");
        
        UserEntity userEntity = new UserEntity();
        userEntity.uid = uid;
        userEntity.name = name != null ? name : "";
        userEntity.email = email != null ? email : "";
        userEntity.role = role != null ? role : "user";
        userEntity.planId = planId != null ? planId : "free";
        userEntity.planExpiresAt = planExpiresAt;
        userEntity.zonaHoraria = zonaHoraria != null ? zonaHoraria : DateTimeUtil.getCurrentZoneId();
        userEntity.isActive = isActive != null ? isActive : true;
        userEntity.updatedAt = DateTimeUtil.getCurrentEpochMillis();
        
        executor.execute(() -> {
            // Guardar usuario en Room
            long userId = userDao.insertUser(userEntity);
            
            // Llamar al callback directamente (Firestore se implementará más tarde)
            callback.onSuccess(userEntity);
        });
    }
    
    private void createDefaultCategories(String userUid) {
        Log.d(TAG, "Creando categorías por defecto para usuario: " + userUid);
        
        // Crear categorías por defecto
        String[] defaultCategories = {
            "🛒 Supermercado", "☕ Café", "🚇 SUBE", "🚕 Taxi", "⛽ Combustible",
            "💊 Farmacia", "🌐 Internet", "📺 TV", "💡 Luz", "🔥 Gas", "💧 Agua"
        };
        
        executor.execute(() -> {
            try {
                for (String categoryData : defaultCategories) {
                    String[] parts = categoryData.split(" ", 2);
                    String icono = parts[0];
                    String name = parts[1];
                    
                    CategoryEntity category = new CategoryEntity();
                    category.userUid = userUid;
                    category.name = name;
                    category.icono = icono;
                    category.isActive = true;
                    category.updatedAt = DateTimeUtil.getCurrentEpochMillis();
                    category.syncState = "PENDING";
                    
                    // Insertar en Room
                    long categoryId = categoryDao.insertCategory(category);
                    category.idLocal = categoryId;
                    
                    Log.d(TAG, "Categoría por defecto creada: " + name + " (ID: " + categoryId + ")");
                }
                Log.d(TAG, "Categorías por defecto creadas exitosamente para usuario: " + userUid);
            } catch (Exception e) {
                Log.e(TAG, "Error al crear categorías por defecto", e);
            }
        });
    }

    /**
     * Verifica si los datos del usuario necesitan sincronización y los sincroniza si es necesario.
     */
    private void checkAndSyncUserData(String userUid) {
        executor.execute(() -> {
            try {
                // Contar categorías y gastos locales
                List<CategoryEntity> localCategories = categoryDao.getAllCategoriesByUserSync(userUid);
                int categoryCount = localCategories != null ? localCategories.size() : 0;
                
                // Sincronizar categorías inmediatamente (siempre incremental usando lastSync)
                long lastCategoriesSync = SyncPrefs.getLastSyncCategoriesMillis(context, userUid);
                if (categoryCount == 0) {
                    lastCategoriesSync = 0L; // Forzar sincronización completa si no hay datos locales
                }
                
                if (categoriesSyncInProgress.compareAndSet(false, true)) {
                    long finalLastCategoriesSync = lastCategoriesSync;
                    Log.d(TAG, "Sincronizando categorías desde Firestore (lastSync: " + finalLastCategoriesSync + ")");
                    CategoryRepositoryImpl categoryRepo = new CategoryRepositoryImpl(context);
                    categoryRepo.syncFromFirestore(userUid, finalLastCategoriesSync, new CategoryRepository.RepositoryCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer count) {
                            Log.d(TAG, "Sincronización de categorías completada: " + count + " categorías");
                            SyncPrefs.setLastSyncCategoriesMillis(context, userUid, System.currentTimeMillis());
                            categoriesSyncInProgress.set(false);
                        }
                        
                        @Override
                        public void onError(Exception error) {
                            Log.e(TAG, "Error al sincronizar categorías desde Firestore", error);
                            categoriesSyncInProgress.set(false);
                        }
                    });
                } else {
                    Log.d(TAG, "Sincronización de categorías ya en progreso, se omite nueva solicitud.");
                }
                
                // Verificar si necesita sincronización de gastos
                // Necesitamos obtener el ExpenseDao para contar gastos
                com.example.gestorgastos.data.local.dao.ExpenseDao expenseDao = 
                    AppDatabase.getDatabase(context).expenseDao();
                List<com.example.gestorgastos.data.local.entity.ExpenseEntity> localExpenses = 
                    expenseDao.getExpensesByUserSync(userUid);
                int expenseCount = localExpenses != null ? localExpenses.size() : 0;
                
                long lastExpensesSync = SyncPrefs.getLastSyncExpensesMillis(context, userUid);
                if (expenseCount == 0) {
                    lastExpensesSync = 0L; // Forzar sincronización completa si no hay datos locales
                }
                
                if (expensesSyncInProgress.compareAndSet(false, true)) {
                    long finalLastExpensesSync = lastExpensesSync;
                    Log.d(TAG, "Sincronizando gastos desde Firestore (lastSync: " + finalLastExpensesSync + ")");
                    ExpenseRepositoryImpl expenseRepo = new ExpenseRepositoryImpl(context);
                    expenseRepo.syncFromFirestore(userUid, finalLastExpensesSync, new ExpenseRepository.RepositoryCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer count) {
                            Log.d(TAG, "Sincronización de gastos completada: " + count + " gastos");
                            SyncPrefs.setLastSyncExpensesMillis(context, userUid, System.currentTimeMillis());
                            expensesSyncInProgress.set(false);
                        }
                        
                        @Override
                        public void onError(Exception error) {
                            Log.e(TAG, "Error al sincronizar gastos desde Firestore", error);
                            expensesSyncInProgress.set(false);
                        }
                    });
                } else {
                    Log.d(TAG, "Sincronización de gastos ya en progreso, se omite nueva solicitud.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al verificar sincronización de datos", e);
            }
        });
    }

    /**
     * Sincroniza los datos básicos del usuario con Firestore en /users/{uid}
     * Usa set() (create/update) para evitar problemas de NOT_FOUND.
     */
    private void syncUserToFirestore(UserEntity userEntity) {
        if (userEntity == null || userEntity.uid == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("name", userEntity.name);
        data.put("email", userEntity.email);
        data.put("role", userEntity.role);
        data.put("plan_id", userEntity.planId);
        if (userEntity.planExpiresAt != null) {
            data.put("plan_expires_at", userEntity.planExpiresAt);
        }
        data.put("zona_horaria", userEntity.zonaHoraria);
        data.put("is_active", userEntity.isActive);
        data.put("updated_at", FieldValue.serverTimestamp());

        // set() con merge implícito via createUser (document().set())
        data.put("created_at", FieldValue.serverTimestamp());
        firestoreDataSource.createUser(userEntity.uid, data)
                .addOnSuccessListener(docRef ->
                        Log.d(TAG, "Usuario sincronizado en Firestore: " + userEntity.uid))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error al sincronizar usuario en Firestore", e));
    }
}
