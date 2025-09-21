package com.example.gestorgastos.ui.auth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.gestorgastos.databinding.ActivityAuthBinding;
import com.example.gestorgastos.ui.main.MainActivity;
import com.example.gestorgastos.util.NavBarUtils;
import com.example.gestorgastos.util.AnimationConstants;
import com.example.gestorgastos.ui.dialogs.AuthMessageDialog;
import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

public class AuthActivity extends AppCompatActivity {
    private ActivityAuthBinding binding;
    private AuthViewModel authViewModel;
    private boolean isSignUpMode = false;
    private boolean isAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Forzar modo claro
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configurar NavigationBar consistente
        if (getWindow() != null) {
            getWindow().setNavigationBarColor(getResources().getColor(android.R.color.black));
        }

        // Configurar WindowInsets para mejor manejo de la UI
        setupWindowInsets();

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        setupViews();
        observeViewModel();
        
        // Configurar estado inicial de la UI
        updateUI();
        
        // Animar entrada de la UI
        animateUIEntry();
    }

    private void setupViews() {
        // Botón de inicio de sesión
        binding.btnSignIn.setOnClickListener(v -> {
            String email = Objects.requireNonNull(binding.etEmail.getText()).toString().trim();
            String password = Objects.requireNonNull(binding.etPassword.getText()).toString().trim();
            
            if (isSignUpMode) {
                String name = Objects.requireNonNull(binding.etName.getText()).toString().trim();
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
            animateModeChange();
        });

        // Enlace "¿Olvidaste tu contraseña?"
        binding.tvForgotPassword.setOnClickListener(v -> {
            handleForgotPassword();
        });

        // Configurar toggle de visibilidad de contraseña
        setupPasswordVisibilityToggle();
    }

    private void updateUI() {
        // Limpiar errores antes de cambiar la UI
        clearErrors();
        
        if (isSignUpMode) {
            binding.tilName.setVisibility(View.VISIBLE);
            binding.tvForgotPassword.setVisibility(View.GONE);
            binding.btnSignIn.setText("Crear Cuenta");
            binding.tvMode.setText("¿Ya tienes cuenta? Inicia sesión");
        } else {
            binding.tilName.setVisibility(View.GONE);
            binding.tvForgotPassword.setVisibility(View.VISIBLE);
            binding.btnSignIn.setText("Iniciar Sesión");
            binding.tvMode.setText("¿No tienes cuenta? Regístrate");
        }
    }

    private void clearErrors() {
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilName.setError(null);
    }

    private boolean validateInput(String email, String password) {
        return validateInput(email, password, "");
    }

    private boolean validateInput(String email, String password, String name) {
        if (email.isEmpty()) {
            binding.tilEmail.setError("¡Ups! Necesitamos tu email para continuar 📧");
            binding.etEmail.requestFocus();
            return false;
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError("¡Oye! Tu contraseña es importante 🔐");
            binding.etPassword.requestFocus();
            return false;
        }
        if (isSignUpMode && name.isEmpty()) {
            binding.tilName.setError("¡Hola! ¿Cómo te gustaría que te llamemos? 👋");
            binding.etName.requestFocus();
            return false;
        }
        return true;
    }

    private void observeViewModel() {
        authViewModel.getCurrentUser().observe(this, user -> {
            Log.d("AuthActivity", "getCurrentUser observer called, user: " + (user != null ? user.email : "null"));
            if (user != null) {
                // Usuario autenticado, navegar a MainActivity con animación
                Log.d("AuthActivity", "Navegando a MainActivity");
                animateTransitionToMain();
            }
        });

        authViewModel.getIsLoading().observe(this, isLoading -> {
            animateLoadingState(isLoading);
            binding.btnSignIn.setEnabled(!isLoading);
        });

        authViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                // Determinar si es un mensaje de éxito o error
                if (errorMessage.contains("enviado un enlace") || 
                    errorMessage.contains("enlace mágico") || 
                    errorMessage.contains("¡Perfecto!") ||
                    errorMessage.contains("éxito") ||
                    errorMessage.contains("✨")) {
                    showSuccessDialog(errorMessage);
                } else {
                    showErrorDialog(errorMessage);
                }
            }
        });
    }

    /**
     * Configura WindowInsets para mejor manejo de la UI
     */
    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Anima la entrada inicial de la UI
     */
    private void animateUIEntry() {
        // Inicialmente ocultar elementos
        binding.ivAppLogo.setAlpha(0f);
        binding.tvAppTitle.setAlpha(0f);
        binding.tvAppSubtitle.setAlpha(0f);
        binding.cardAuthForm.setAlpha(0f);
        binding.tvMode.setAlpha(0f);

        // Animar logo
        binding.ivAppLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(AnimationConstants.ScreenEntry.DURATION_LOGO)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Animar título
        binding.tvAppTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(AnimationConstants.ScreenEntry.DURATION_TITLE)
                .setStartDelay(AnimationConstants.ScreenEntry.DELAY_TITLE)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Animar subtítulo
        binding.tvAppSubtitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(AnimationConstants.ScreenEntry.DURATION_SUBTITLE)
                .setStartDelay(AnimationConstants.ScreenEntry.DELAY_SUBTITLE)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Animar card del formulario
        binding.cardAuthForm.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(AnimationConstants.ScreenEntry.DURATION_FORM)
                .setStartDelay(AnimationConstants.ScreenEntry.DELAY_FORM)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // Animar texto de modo
        binding.tvMode.animate()
                .alpha(1f)
                .setDuration(AnimationConstants.ScreenEntry.DURATION_MODE_TEXT)
                .setStartDelay(AnimationConstants.ScreenEntry.DELAY_MODE_TEXT)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Anima el cambio entre modo login y registro
     */
    private void animateModeChange() {
        // Evitar múltiples animaciones simultáneas
        if (isAnimating) {
            return;
        }
        
        isAnimating = true;
        
        // Cancelar cualquier animación en curso
        binding.tilName.clearAnimation();
        binding.btnSignIn.clearAnimation();
        
        // Animar salida del campo nombre si está visible
        if (binding.tilName.getVisibility() == View.VISIBLE) {
            binding.tilName.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(AnimationConstants.DURATION_FAST)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            binding.tilName.setVisibility(View.GONE);
                            updateUI();
                            animateButtonOnly();
                        }
                    })
                    .start();
        } else {
            updateUI();
            // Si el campo de nombre debe aparecer, animarlo
            if (binding.tilName.getVisibility() == View.VISIBLE) {
                animateNameFieldIn();
            } else {
                animateButtonOnly();
            }
        }
    }

    /**
     * Anima solo el campo de nombre cuando aparece
     */
    private void animateNameFieldIn() {
        // Cancelar animaciones previas del campo nombre
        binding.tilName.clearAnimation();
        binding.tilName.setAlpha(0f);
        binding.tilName.setTranslationY(-20f);
        binding.tilName.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(AnimationConstants.FormField.DURATION)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Después de animar el campo, animar el botón
                        animateButtonOnly();
                    }
                })
                .start();
    }

    /**
     * Anima solo el botón
     */
    private void animateButtonOnly() {
        // Cancelar animaciones previas del botón
        binding.btnSignIn.clearAnimation();
        
        // Animar botón con un pequeño delay para evitar superposición
        binding.btnSignIn.postDelayed(() -> {
            binding.btnSignIn.animate()
                    .scaleX(AnimationConstants.AuthButton.SCALE_FACTOR)
                    .scaleY(AnimationConstants.AuthButton.SCALE_FACTOR)
                    .setDuration(AnimationConstants.AuthButton.DURATION)
                    .withEndAction(() -> {
                        binding.btnSignIn.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(AnimationConstants.AuthButton.DURATION)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        // Resetear la variable de control al final de todas las animaciones
                                        isAnimating = false;
                                    }
                                })
                                .start();
                    })
                    .start();
        }, AnimationConstants.AuthButton.DELAY);
    }

    /**
     * Configura el toggle de visibilidad de contraseña
     */
    private void setupPasswordVisibilityToggle() {
        binding.tilPassword.setEndIconOnClickListener(v -> {
            if (binding.etPassword.getTransformationMethod() == null) {
                // Mostrar contraseña
                binding.etPassword.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                binding.tilPassword.setEndIconDrawable(getDrawable(com.example.gestorgastos.R.drawable.ic_visibility_off_auth));
            } else {
                // Ocultar contraseña
                binding.etPassword.setTransformationMethod(null);
                binding.tilPassword.setEndIconDrawable(getDrawable(com.example.gestorgastos.R.drawable.ic_visibility_auth));
            }
            binding.etPassword.setSelection(Objects.requireNonNull(binding.etPassword.getText()).length());
        });
    }

    /**
     * Anima la transición a MainActivity
     */
    private void animateTransitionToMain() {
        // Animar salida de la UI
        binding.getRoot().animate()
                .alpha(0f)
                .scaleX(AnimationConstants.ScreenTransition.SCALE_FACTOR)
                .scaleY(AnimationConstants.ScreenTransition.SCALE_FACTOR)
                .setDuration(AnimationConstants.ScreenTransition.DURATION)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Navegar a MainActivity
                        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }
                })
                .start();
    }

    /**
     * Anima el estado de carga
     */
    private void animateLoadingState(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.progressBar.setAlpha(0f);
            binding.progressBar.animate()
                    .alpha(1f)
                    .setDuration(AnimationConstants.Loading.DURATION)
                    .start();

            binding.btnSignIn.animate()
                    .alpha(AnimationConstants.AuthButton.ALPHA_DISABLED)
                    .setDuration(AnimationConstants.AuthButton.DURATION)
                    .start();
        } else {
            binding.progressBar.animate()
                    .alpha(0f)
                    .setDuration(AnimationConstants.Loading.DURATION)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            binding.progressBar.setVisibility(View.GONE);
                        }
                    })
                    .start();

            binding.btnSignIn.animate()
                    .alpha(1f)
                    .setDuration(AnimationConstants.AuthButton.DURATION)
                    .start();
        }
    }

    private void showMessageDialog(String title, String message, String type, String buttonText) {
        AuthMessageDialog dialog = AuthMessageDialog.newInstance(title, message, type, buttonText);
        dialog.setOnDialogActionListener(new AuthMessageDialog.OnDialogActionListener() {
            @Override
            public void onActionClicked() {
                // Acción cuando se presiona el botón principal
            }
            
            @Override
            public void onDialogClosed() {
                // Acción cuando se cierra el diálogo
            }
        });
        dialog.show(getSupportFragmentManager(), AuthMessageDialog.TAG);
    }
    
    private void showSuccessDialog(String message) {
        showMessageDialog("¡Genial! 🎉", message, AuthMessageDialog.TYPE_SUCCESS, "¡Perfecto!");
    }
    
    private void showErrorDialog(String message) {
        showMessageDialog("¡Ups! 😅", message, AuthMessageDialog.TYPE_ERROR, "Intentar de nuevo");
    }
    
    private void showInfoDialog(String title, String message) {
        showMessageDialog(title, message, AuthMessageDialog.TYPE_INFO, "¡Entendido!");
    }

    private void handleForgotPassword() {
        String email = Objects.requireNonNull(binding.etEmail.getText()).toString().trim();
        
        if (email.isEmpty()) {
            binding.tilEmail.setError("¡Ay! Necesitamos tu email para ayudarte 🔑");
            binding.etEmail.requestFocus();
            return;
        }
        
        // Limpiar error después de validar
        binding.tilEmail.setError(null);
        
        // Llamar al método del ViewModel para resetear la contraseña
        authViewModel.resetPassword(email);
    }
}
