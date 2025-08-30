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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthRepositoryImpl implements AuthRepository {
    private static final String TAG = "AuthRepositoryImpl";
    
    private final FirebaseAuthDataSource authDataSource;
    private final FirestoreDataSource firestoreDataSource;
    private final UserDao userDao;
    private final CategoryDao categoryDao;
    private final ExecutorService executor;
    
    public AuthRepositoryImpl(Context context) {
        this.authDataSource = new FirebaseAuthDataSource();
        this.firestoreDataSource = new FirestoreDataSource();
        this.userDao = AppDatabase.getDatabase(context).userDao();
        this.categoryDao = AppDatabase.getDatabase(context).categoryDao();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    @Override
    public LiveData<UserEntity> getCurrentUser() {
        MutableLiveData<UserEntity> userLiveData = new MutableLiveData<>();
        
        FirebaseUser firebaseUser = authDataSource.getCurrentUser();
        
        if (firebaseUser != null) {
            // Primero buscar en Room
            executor.execute(() -> {
                UserEntity existingUser = userDao.getUserByUidSync(firebaseUser.getUid());
                
                if (existingUser != null) {
                    // Usuario existe en Room, devolverlo
                    Log.d(TAG, "Usuario encontrado en Room: " + existingUser.name);
                    userLiveData.postValue(existingUser);
                } else {
                    // Usuario no existe en Room, crearlo y guardarlo
                    UserEntity userEntity = new UserEntity();
                    userEntity.uid = firebaseUser.getUid();
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
                    userLiveData.postValue(userEntity);
                }
            });
        } else {
            Log.d(TAG, "No hay usuario autenticado");
        }
        
        return userLiveData;
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
    public void signOut() {
        authDataSource.signOut();
        // Limpiar datos locales si es necesario
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
    
    private void checkAndCreateUser(FirebaseUser firebaseUser, AuthCallback callback) {
        String uid = firebaseUser.getUid();
        
        // Verificar si el usuario ya existe en Room
        executor.execute(() -> {
            UserEntity existingUser = userDao.getUserByUidSync(uid);
            
            if (existingUser != null) {
                // Usuario ya existe, solo devolverlo
                Log.d(TAG, "Usuario existente encontrado: " + existingUser.name);
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
    
    private void createNewUser(FirebaseUser firebaseUser, String name, String email, AuthCallback callback) {
        String uid = firebaseUser.getUid();
        String userName = name != null ? name : email.split("@")[0];
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
        
        UserEntity userEntity = new UserEntity();
        userEntity.uid = uid;
        userEntity.name = name != null ? name : "";
        userEntity.email = email != null ? email : "";
        userEntity.role = role != null ? role : "user";
        userEntity.planId = planId != null ? planId : "free";
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
}
