package com.example.gestorgastos.ui.auth;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.gestorgastos.R;
import com.example.gestorgastos.databinding.ActivityAuthBinding;
import com.example.gestorgastos.ui.main.MainActivity;
import com.example.gestorgastos.util.NavBarUtils;
import com.example.gestorgastos.util.AnimationConstants;
import com.example.gestorgastos.ui.dialogs.AuthMessageDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.Objects;

public class AuthActivity extends AppCompatActivity {
    private static final String TAG = "AuthActivity";
    
    private ActivityAuthBinding binding;
    private AuthViewModel authViewModel;
    private boolean isSignUpMode = false;
    private boolean isAnimating = false;
    private View legacyBlurOverlay;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Forzar modo claro
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Configurar ventana para fondo animado full screen
        if (getWindow() != null) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }

        // Configurar WindowInsets para mejor manejo de la UI
        setupWindowInsets();

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        // Configurar Google Sign-In
        setupGoogleSignIn();
        
        setupViews();
        observeViewModel();
        
        // Configurar estado inicial de la UI
        updateUI();
        
        // Animar entrada de la UI
        animateUIEntry();
    }
    
    private void setupGoogleSignIn() {
        // Configurar opciones de Google Sign-In
        // No especificamos setAccountName para que siempre muestre el selector de cuentas
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Asegurarse de que no haya una cuenta guardada al iniciar
        // Esto se hace automáticamente cuando se llama a signOut() al cerrar sesión
        
        // Configurar ActivityResultLauncher para Google Sign-In
        googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                
                if (result.getResultCode() == RESULT_OK && data != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null && account.getIdToken() != null) {
                            Log.d(TAG, "Google Sign-In exitoso: " + account.getEmail());
                            // El spinner ya está visible, el ViewModel lo manejará
                            authViewModel.signInWithGoogle(account.getIdToken());
                        } else {
                            Log.e(TAG, "Google Sign-In: cuenta o token nulo");
                            showLoadingStateImmediate(false); // Ocultar spinner en caso de error
                            showErrorDialog("¡Ups! No pudimos obtener tu información de Google. Intenta de nuevo 😅");
                        }
                    } catch (ApiException e) {
                        Log.e(TAG, "Error en Google Sign-In", e);
                        showLoadingStateImmediate(false); // Ocultar spinner en caso de error
                        String errorMessage = "¡Ups! No pudimos iniciar sesión con Google. ";
                        int statusCode = e.getStatusCode();
                        if (statusCode == 12500) {
                            errorMessage += "Por favor, verifica tu conexión a internet.";
                        } else if (statusCode == 10) {
                            errorMessage += "Error de configuración. Verifica:\n" +
                                    "• El SHA-1 en Firebase Console\n" +
                                    "• El Web Client ID en strings.xml\n" +
                                    "• La configuración de OAuth";
                        } else if (statusCode == 7) {
                            errorMessage += "Error de red. Verifica tu conexión a internet.";
                        } else if (statusCode == 8) {
                            errorMessage += "Error interno. Intenta de nuevo más tarde.";
                        } else {
                            errorMessage += "Error código: " + statusCode + ". Intenta de nuevo.";
                        }
                        showErrorDialog(errorMessage);
                    }
                } else if (data != null) {
                    // Intentar obtener el error incluso si el resultado no es OK
                    // Solo para detectar errores de configuración
                    try {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        if (task.isComplete()) {
                            try {
                                GoogleSignInAccount account = task.getResult(ApiException.class);
                                // Si llegamos aquí, no hubo error, pero el resultado no fue OK
                                Log.d(TAG, "Google Sign-In: resultado no OK pero sin error");
                            } catch (ApiException e) {
                                Log.e(TAG, "Error en Google Sign-In (resultado cancelado)", e);
                                int statusCode = e.getStatusCode();
                                showLoadingStateImmediate(false); // Ocultar spinner siempre
                                
                                // Solo mostrar errores importantes, no cancelaciones
                                if (statusCode == 10) {
                                    // Error de configuración - muy importante mostrar
                                    String errorMessage = "⚠️ Error de configuración de Google Sign-In\n\n" +
                                            "El Web Client ID no está configurado correctamente o el SHA-1 de tu certificado no está registrado en Firebase.\n\n" +
                                            "Verifica:\n" +
                                            "• El Web Client ID en strings.xml\n" +
                                            "• El SHA-1 en Firebase Console > Configuración del proyecto\n" +
                                            "• La configuración de OAuth en Google Cloud Console";
                                    showErrorDialog(errorMessage);
                                } else if (statusCode == 7) {
                                    // NETWORK_ERROR
                                    showErrorDialog("¡Ups! Error de red. Verifica tu conexión a internet e intenta de nuevo.");
                                } else if (statusCode == 12500) {
                                    showErrorDialog("¡Ups! Verifica tu conexión a internet.");
                                } else if (statusCode == 12501) {
                                    // SIGN_IN_CANCELLED - usuario canceló, no mostrar error
                                    Log.d(TAG, "Usuario canceló Google Sign-In");
                                } else {
                                    // Otros errores
                                    String errorMessage = "¡Ups! No pudimos iniciar sesión con Google.\n\n" +
                                            "Error código: " + statusCode + ". Intenta de nuevo.";
                                    showErrorDialog(errorMessage);
                                }
                            }
                        } else {
                            Log.d(TAG, "Google Sign-In cancelado por el usuario");
                            showLoadingStateImmediate(false); // Ocultar spinner
                            // No mostrar error si el usuario canceló intencionalmente
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al procesar resultado de Google Sign-In", e);
                        showLoadingStateImmediate(false); // Ocultar spinner
                        // No mostrar error genérico para evitar confusión
                    }
                } else {
                    Log.d(TAG, "Google Sign-In cancelado (sin datos)");
                    showLoadingStateImmediate(false); // Ocultar spinner
                    // No mostrar error si el usuario canceló intencionalmente
                }
            }
        );
    }

    private void setupViews() {
        // Botón de inicio de sesión
        binding.btnSignIn.setOnClickListener(v -> {
            String email = Objects.requireNonNull(binding.etEmail.getText()).toString().trim();
            String password = Objects.requireNonNull(binding.etPassword.getText()).toString().trim();
            
            if (isSignUpMode) {
                // El nombre se genera automáticamente desde el email en el repositorio
                if (validateInput(email, password)) {
                    // Mostrar spinner inmediatamente
                    showLoadingStateImmediate(true);
                    authViewModel.signUp(email, password, "");
                }
            } else {
                if (validateInput(email, password)) {
                    // Mostrar spinner inmediatamente
                    showLoadingStateImmediate(true);
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

        // Botón de Google Sign-In
        binding.btnGoogleSignIn.setOnClickListener(v -> {
            // Mostrar spinner inmediatamente
            showLoadingStateImmediate(true);
            signInWithGoogle();
        });

        // Configurar toggle de visibilidad de contraseña
        setupPasswordVisibilityToggle();
    }
    
    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }
    
    /**
     * Desconecta Google Sign-In para permitir seleccionar otra cuenta la próxima vez
     */
    public static void signOutGoogle(Context context) {
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(context, gso);
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Log.d(TAG, "Google Sign-In desconectado exitosamente");
            });
        } catch (Exception e) {
            Log.e(TAG, "Error al desconectar Google Sign-In", e);
        }
    }

    private void updateUI() {
        // Limpiar errores antes de cambiar la UI
        clearErrors();
        
        if (isSignUpMode) {
            binding.tvForgotPassword.setVisibility(View.GONE);
            binding.btnSignIn.setText("Crear Cuenta");
            binding.tvMode.setText("¿Ya tienes cuenta? Inicia sesión");
        } else {
            binding.tvForgotPassword.setVisibility(View.VISIBLE);
            binding.btnSignIn.setText("Iniciar Sesión");
            binding.tvMode.setText("¿No tienes cuenta? Regístrate");
        }
    }

    private void clearErrors() {
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
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
        if (!isValidEmail(email)) {
            binding.tilEmail.setError("¡Hmmm! Ese email no parece válido. Revísalo por favor 📧");
            binding.etEmail.requestFocus();
            return false;
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError("¡Oye! Tu contraseña es importante 🔐");
            binding.etPassword.requestFocus();
            return false;
        }
        return true;
    }
    
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void observeViewModel() {
        authViewModel.getCurrentUser().observe(this, user -> {
            Log.d("AuthActivity", "getCurrentUser observer called, user: " + (user != null ? user.email : "null"));
            if (user != null) {
                // Usuario autenticado, navegar a MainActivity con animación
                Log.d("AuthActivity", "Navegando a MainActivity");
                // Ocultar spinner antes de navegar
                showLoadingStateImmediate(false);
                animateTransitionToMain();
            }
        });

        authViewModel.getIsLoading().observe(this, isLoading -> {
            // El ViewModel maneja el estado de carga, pero solo animamos si no está ya visible
            // para evitar conflictos con showLoadingStateImmediate
            if (!isLoading || binding.progressBar.getVisibility() != View.VISIBLE) {
                animateLoadingState(isLoading);
            }
            binding.btnSignIn.setEnabled(!isLoading);
            binding.btnGoogleSignIn.setEnabled(!isLoading);
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
        View scroll = findViewById(R.id.scroll_view);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            androidx.core.graphics.Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            int bottomPadding = imeVisible ? ime.bottom : systemBars.bottom;

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);

            if (imeVisible) {
                View focused = getCurrentFocus();
                if (focused != null) {
                    v.post(() -> {
                        // Desplazar para que el campo enfocado quede visible
                        int[] location = new int[2];
                        focused.getLocationOnScreen(location);
                        v.scrollTo(0, Math.max(0, focused.getBottom()));
                    });
                }
            }
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
        
        // Animar botón de Google Sign-In
        if (binding.btnGoogleSignIn != null) {
            binding.btnGoogleSignIn.setAlpha(0f);
            binding.btnGoogleSignIn.animate()
                    .alpha(1f)
                    .setDuration(AnimationConstants.ScreenEntry.DURATION_MODE_TEXT)
                    .setStartDelay(AnimationConstants.ScreenEntry.DELAY_MODE_TEXT + 100)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
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
        binding.btnSignIn.clearAnimation();
        
        // Actualizar la UI sin animaciones de campo de nombre
            updateUI();
        
        // Animar solo el botón
                        animateButtonOnly();
    }

    /**
     * Anima solo el botón
     */
    private void animateButtonOnly() {
        // Cancelar animaciones previas del botón
        binding.btnSignIn.clearAnimation();
        
        // Animar botón
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
     * Muestra u oculta el estado de carga inmediatamente (sin animación de entrada)
     * Útil para mostrar feedback instantáneo cuando el usuario presiona un botón
     */
    private void showLoadingStateImmediate(boolean isLoading) {
        if (isLoading) {
            // Mostrar spinner inmediatamente sin animación de fade-in
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.progressBar.setAlpha(1f);
            binding.progressBar.bringToFront();
            
            // Deshabilitar botones inmediatamente
            binding.btnSignIn.setEnabled(false);
            if (binding.btnGoogleSignIn != null) {
                binding.btnGoogleSignIn.setEnabled(false);
            }
            
            // Animar botones a estado deshabilitado
            binding.btnSignIn.animate()
                    .alpha(AnimationConstants.AuthButton.ALPHA_DISABLED)
                    .setDuration(AnimationConstants.AuthButton.DURATION)
                    .start();
            if (binding.btnGoogleSignIn != null) {
                binding.btnGoogleSignIn.animate()
                        .alpha(AnimationConstants.AuthButton.ALPHA_DISABLED)
                        .setDuration(AnimationConstants.AuthButton.DURATION)
                        .start();
            }
        } else {
            // Ocultar spinner con animación
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

            // Rehabilitar botones
            binding.btnSignIn.setEnabled(true);
            if (binding.btnGoogleSignIn != null) {
                binding.btnGoogleSignIn.setEnabled(true);
            }
            
            // Animar botones a estado normal
            binding.btnSignIn.animate()
                    .alpha(1f)
                    .setDuration(AnimationConstants.AuthButton.DURATION)
                    .start();
            if (binding.btnGoogleSignIn != null) {
                binding.btnGoogleSignIn.animate()
                        .alpha(1f)
                        .setDuration(AnimationConstants.AuthButton.DURATION)
                        .start();
            }
        }
    }

    /**
     * Anima el estado de carga (usado por el ViewModel)
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
            if (binding.btnGoogleSignIn != null) {
                binding.btnGoogleSignIn.animate()
                        .alpha(AnimationConstants.AuthButton.ALPHA_DISABLED)
                        .setDuration(AnimationConstants.AuthButton.DURATION)
                        .start();
            }
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
            if (binding.btnGoogleSignIn != null) {
                binding.btnGoogleSignIn.animate()
                        .alpha(1f)
                        .setDuration(AnimationConstants.AuthButton.DURATION)
                        .start();
            }
        }
    }

    private void showMessageDialog(String title, String message, String type, String buttonText) {
        AuthMessageDialog dialog = AuthMessageDialog.newInstance(title, message, type, buttonText);
        dialog.setOnDialogActionListener(new AuthMessageDialog.OnDialogActionListener() {
            @Override
            public void onActionClicked() {
                // Acción cuando se presiona el botón principal
                applyBackgroundBlur(false);
            }
            
            @Override
            public void onDialogClosed() {
                // Acción cuando se cierra el diálogo
                applyBackgroundBlur(false);
            }
        });
        dialog.show(getSupportFragmentManager(), AuthMessageDialog.TAG);
        applyBackgroundBlur(true);
    }

    private void applyBackgroundBlur(boolean enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.getRoot().setRenderEffect(enable ?
                    RenderEffect.createBlurEffect(40f, 40f, Shader.TileMode.CLAMP) : null);
        } else {
            // Fallback para APIs < 31: añadir/quitar overlay semitransparente
            if (enable) {
                if (legacyBlurOverlay == null) {
                    legacyBlurOverlay = new View(this);
                    legacyBlurOverlay.setBackgroundResource(R.drawable.blur_overlay);
                    legacyBlurOverlay.setClickable(true);
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT);
                    addContentView(legacyBlurOverlay, lp);
                } else {
                    legacyBlurOverlay.setVisibility(View.VISIBLE);
                }
            } else if (legacyBlurOverlay != null) {
                legacyBlurOverlay.setVisibility(View.GONE);
            }
        }
    }
    
    private void showSuccessDialog(String message) {
        showMessageDialog("¡Genial! 🎉", message, AuthMessageDialog.TYPE_SUCCESS, "¡Perfecto!");
    }
    
    private void showErrorDialog(String message) {
        showMessageDialog("¡Ups! 😅", message, AuthMessageDialog.TYPE_INFO, "Intentar de nuevo");
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
