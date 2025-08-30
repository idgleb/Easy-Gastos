package com.example.gestorgastos.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthDataSource {
    private final FirebaseAuth firebaseAuth;
    
    public FirebaseAuthDataSource() {
        this.firebaseAuth = FirebaseAuth.getInstance();
    }
    
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
    
    public Task<com.google.firebase.auth.AuthResult> signInWithEmailAndPassword(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }
    
    public Task<com.google.firebase.auth.AuthResult> createUserWithEmailAndPassword(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }
    
    public void signOut() {
        firebaseAuth.signOut();
    }
    
    public Task<Void> deleteUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            return user.delete();
        }
        // Retornar null si no hay usuario
        return null;
    }
}
