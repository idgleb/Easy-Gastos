package com.example.gestorgastos.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.gestorgastos.databinding.ActivityAuthBinding;
import com.example.gestorgastos.ui.main.MainActivity;
import com.google.android.material.snackbar.Snackbar;

public class AuthActivity extends AppCompatActivity {
    private ActivityAuthBinding binding;
    private AuthViewModel authViewModel;
    private boolean isSignUpMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Forzar modo claro
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        setupViews();
        observeViewModel();
    }

    private void setupViews() {
        // Botón de inicio de sesión
        binding.btnSignIn.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            
            if (isSignUpMode) {
                String name = binding.etName.getText().toString().trim();
                if (validateInput(email, password, name)) {
                    authViewModel.signUp(email, password, name);
                }
            } else {
                if (validateInput(email, password)) {
                    authViewModel.signIn(email, password);
                }
            }
        });

        // Cambio de modo (login/registro)
        binding.tvMode.setOnClickListener(v -> {
            isSignUpMode = !isSignUpMode;
            updateUI();
        });
    }

    private void updateUI() {
        if (isSignUpMode) {
            binding.tilName.setVisibility(View.VISIBLE);
            binding.btnSignIn.setText("Crear Cuenta");
            binding.btnSignUp.setVisibility(View.GONE);
            binding.tvMode.setText("¿Ya tienes cuenta? Inicia sesión");
        } else {
            binding.tilName.setVisibility(View.GONE);
            binding.btnSignIn.setText("Iniciar Sesión");
            binding.btnSignUp.setVisibility(View.GONE);
            binding.tvMode.setText("¿No tienes cuenta? Regístrate");
        }
    }

    private boolean validateInput(String email, String password) {
        return validateInput(email, password, "");
    }

    private boolean validateInput(String email, String password, String name) {
        if (email.isEmpty()) {
            binding.etEmail.setError("El email es requerido");
            return false;
        }
        if (password.isEmpty()) {
            binding.etPassword.setError("La contraseña es requerida");
            return false;
        }
        if (isSignUpMode && name.isEmpty()) {
            binding.etName.setError("El nombre es requerido");
            return false;
        }
        return true;
    }

    private void observeViewModel() {
        authViewModel.getCurrentUser().observe(this, user -> {
            Log.d("AuthActivity", "getCurrentUser observer called, user: " + (user != null ? user.email : "null"));
            if (user != null) {
                // Usuario autenticado, navegar a MainActivity
                Log.d("AuthActivity", "Navegando a MainActivity");
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        authViewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnSignIn.setEnabled(!isLoading);
        });

        authViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Snackbar.make(binding.getRoot(), errorMessage, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
