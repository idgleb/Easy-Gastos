package com.example.gestorgastos.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class FirestoreDataSource {
    private final FirebaseFirestore firestore;
    
    public FirestoreDataSource() {
        this.firestore = FirebaseFirestore.getInstance();
    }
    
    // Users
    public Task<DocumentReference> createUser(String uid, Map<String, Object> userData) {
        return firestore.collection("users").document(uid).set(userData)
                .continueWith(task -> firestore.collection("users").document(uid));
    }
    
    public Task<DocumentSnapshot> getUser(String uid) {
        return firestore.collection("users").document(uid).get();
    }
    
    /**
     * Obtiene un listener de tiempo real para el documento del usuario.
     * Detecta cambios automáticamente cuando el webhook actualiza el plan.
     */
    public ListenerRegistration getUserSnapshotListener(String uid, com.google.firebase.firestore.EventListener<com.google.firebase.firestore.DocumentSnapshot> listener) {
        return firestore.collection("users").document(uid).addSnapshotListener(listener);
    }
    
    public Task<Void> updateUser(String uid, Map<String, Object> updates) {
        return firestore.collection("users").document(uid).update(updates);
    }
    
    public Task<Void> deleteUser(String uid) {
        return firestore.collection("users").document(uid).delete();
    }
    
    public Task<QuerySnapshot> getAllUsers() {
        return firestore.collection("users").get();
    }
    
    // Categories
    public Task<DocumentReference> createCategory(String userUid, Map<String, Object> categoryData) {
        return firestore.collection("users").document(userUid)
                .collection("categories").add(categoryData);
    }
    
    public Task<QuerySnapshot> getCategories(String userUid) {
        return firestore.collection("users").document(userUid)
                .collection("categories").get();
    }
    
    public Task<QuerySnapshot> getCategoriesAfter(String userUid, Timestamp lastSync) {
        return firestore.collection("users").document(userUid)
                .collection("categories")
                .whereGreaterThan("updated_at", lastSync)
                .get();
    }
    
    public Task<Void> updateCategory(String userUid, String categoryId, Map<String, Object> updates) {
        return firestore.collection("users").document(userUid)
                .collection("categories").document(categoryId).update(updates);
    }
    
    public Task<Void> deleteCategory(String userUid, String categoryId) {
        return firestore.collection("users").document(userUid)
                .collection("categories").document(categoryId).delete();
    }
    
    // Expenses
    public Task<DocumentReference> createExpense(String userUid, Map<String, Object> expenseData) {
        return firestore.collection("users").document(userUid)
                .collection("expenses").add(expenseData);
    }
    
    public Task<QuerySnapshot> getExpenses(String userUid) {
        return firestore.collection("users").document(userUid)
                .collection("expenses").get();
    }
    
    public Task<QuerySnapshot> getExpensesAfter(String userUid, Timestamp lastSync) {
        return firestore.collection("users").document(userUid)
                .collection("expenses")
                .whereGreaterThan("updated_at", lastSync)
                .get();
    }
    
    public Task<Void> updateExpense(String userUid, String expenseId, Map<String, Object> updates) {
        return firestore.collection("users").document(userUid)
                .collection("expenses").document(expenseId).update(updates);
    }
    
    public Task<Void> deleteExpense(String userUid, String expenseId) {
        return firestore.collection("users").document(userUid)
                .collection("expenses").document(expenseId).delete();
    }
    
    // Plans
    public Task<QuerySnapshot> getPlans() {
        return firestore.collection("plans").get();
    }
    
    public Task<DocumentSnapshot> getPlan(String planId) {
        return firestore.collection("plans").document(planId).get();
    }
    
    // Batch operations
    public WriteBatch batch() {
        return firestore.batch();
    }
}





