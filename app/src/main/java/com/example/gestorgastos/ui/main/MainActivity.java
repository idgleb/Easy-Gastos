package com.example.gestorgastos.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.gestorgastos.R;
import com.example.gestorgastos.databinding.ActivityMainBinding;
import com.example.gestorgastos.ui.auth.AuthActivity;
import com.example.gestorgastos.ui.categories.CategoriesFragment;
import com.example.gestorgastos.ui.categories.CategoryViewModel;
import com.example.gestorgastos.ui.dashboard.DashboardFragment;
import com.example.gestorgastos.ui.expenses.ExpensesFragment;
import com.example.gestorgastos.ui.dialogs.AccountBottomSheet;
import com.example.gestorgastos.data.local.entity.CategoryEntity;
import java.util.List;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AccountBottomSheet.OnAccountActionListener {
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    
    // Constantes para identificar fragmentos
    private static final int FRAGMENT_EXPENSES = 0;
    private static final int FRAGMENT_DASHBOARD = 1;
    private static final int FRAGMENT_CATEGORIES = 2;
    
    // Variable para rastrear el fragmento actual
    private int currentFragment = FRAGMENT_EXPENSES;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Asegurar que el sistema dibuje la status bar con el color indicado
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        // Opción A: sin padding por insets en el root para que la status bar muestre su color
        // Aplicar solo el inset top al AppBar para que no se solape con la status bar
        View appbar = findViewById(R.id.appbar);
        if (appbar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(appbar, (v, insets) -> {
                Insets status = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), status.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
        // Configurar barras del sistema para MainActivity
        if (getWindow() != null) {
            // Asegurar que podamos colorear la status bar
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // Status bar con fondo fondo_principal
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.fondo_principal));
            // Navigation bar en negro
            getWindow().setNavigationBarColor(getResources().getColor(android.R.color.black));
            // Asegurar iconos claros sobre status bar oscura
            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            controller.setAppearanceLightStatusBars(false);
        }
        // Forzar modo claro
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        initHeightDeSvInfo();
        setupToolbar();
        setupNavigation();
        setupUserInfo();
        observeViewModel();
        
        // Cargar fragmento inicial
        if (savedInstanceState == null) {
            loadFragment(new ExpensesFragment());
        }
        
        // Configurar comportamiento del BottomNavigationView
        setupBottomNavigationBehavior();
    }

    private void initHeightDeSvInfo() {
        binding.constrConteiner.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.mainAdmin.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int scrollViewHeight = binding.mainAdmin.getHeight();
                // Calcula el 55% de alto y lo aplica a vFondo
                int newHeight = (int) (scrollViewHeight * 0.55);
                ViewGroup.LayoutParams params = binding.vFondo.getLayoutParams();
                params.height = newHeight;
                binding.vFondo.setLayoutParams(params);
            }
        });
    }
    
    private void setupToolbar() {
        // Configurar click listener para el AppBar personalizado
        binding.customAppbar.getRoot().setOnClickListener(v -> {
            showAccountSheet();
        });
    }
    
    private void setupNavigation() {
        // Configurar el item seleccionado inicialmente
        binding.bottomNavigation.setSelectedItemId(R.id.nav_expenses);
        // Inicializar el fragmento actual
        currentFragment = FRAGMENT_EXPENSES;
    }
    
    private void setupUserInfo() {
        // La configuración del usuario se maneja en setupToolbar()
    }
    
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
    
    /**
     * Carga un fragmento con animaciones inteligentes basadas en la dirección de navegación
     * @param fragment El fragmento a cargar
     * @param targetFragment El ID del fragmento destino
     */
    private void loadFragmentWithSmartAnimation(Fragment fragment, int targetFragment) {
        int enterAnim, exitAnim, popEnterAnim, popExitAnim;
        
        // Determinar la dirección de la animación basada en la navegación
        if (currentFragment < targetFragment) {
            // Navegando hacia la derecha (Gastos -> Dashboard -> Categorías)
            enterAnim = R.anim.slide_in_from_right;
            exitAnim = R.anim.slide_out_to_left;
            popEnterAnim = R.anim.slide_in_from_left;
            popExitAnim = R.anim.slide_out_to_right;
        } else if (currentFragment > targetFragment) {
            // Navegando hacia la izquierda (Categorías -> Dashboard -> Gastos)
            enterAnim = R.anim.slide_in_from_left;
            exitAnim = R.anim.slide_out_to_right;
            popEnterAnim = R.anim.slide_in_from_right;
            popExitAnim = R.anim.slide_out_to_left;
        } else {
            // Mismo fragmento (no debería pasar, pero por seguridad)
            enterAnim = R.anim.slide_in_right;
            exitAnim = R.anim.slide_out_left;
            popEnterAnim = R.anim.slide_in_left;
            popExitAnim = R.anim.slide_out_right;
        }
        
        // Aplicar la transición con animaciones inteligentes
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
                .replace(R.id.fragment_container, fragment)
                .commit();
        
        // Actualizar el fragmento actual
        currentFragment = targetFragment;

    }
    
    private void setupBottomNavigationItemBackground() {
        // Configurar el fondo del botón seleccionado usando programación
        binding.bottomNavigation.setItemActiveIndicatorEnabled(true);
        binding.bottomNavigation.setItemActiveIndicatorColor(
            android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.bottom_nav_primary_container)
            )
        );
        binding.bottomNavigation.setItemActiveIndicatorShapeAppearance(
            com.google.android.material.shape.ShapeAppearanceModel.builder()
                .setAllCornerSizes(16f)
                .build()
        );
    }
    
    private void setupBottomNavigationBehavior() {
        // Personalizar el fondo del botón seleccionado
        setupBottomNavigationItemBackground();
        
        // Configurar comportamiento del BottomNavigationView
        binding.bottomNavigation.setOnItemReselectedListener(item -> {
            // Manejar cuando se selecciona el mismo item (scroll to top, refresh, etc.)
            Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            
            if (currentFragment != null) {
                // Aquí puedes agregar lógica específica para cada fragmento
                // Por ejemplo, scroll to top en RecyclerView
                if (currentFragment instanceof ExpensesFragment) {
                    // TODO: Implementar scroll to top en ExpensesFragment
                } else if (currentFragment instanceof CategoriesFragment) {
                    // TODO: Implementar scroll to top en CategoriesFragment
                } else if (currentFragment instanceof DashboardFragment) {
                    // TODO: Implementar refresh en DashboardFragment
                }
            }
        });
        
        // Configurar animación de selección
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            // Agregar feedback háptico
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                binding.bottomNavigation.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
            }
            
            Fragment fragment = null;
            int targetFragment = -1;
            
            if (item.getItemId() == R.id.nav_expenses) {
                fragment = new ExpensesFragment();
                targetFragment = FRAGMENT_EXPENSES;
            } else if (item.getItemId() == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
                targetFragment = FRAGMENT_DASHBOARD;
            } else if (item.getItemId() == R.id.nav_categories) {
                fragment = new CategoriesFragment();
                targetFragment = FRAGMENT_CATEGORIES;
            }
            
            if (fragment != null && targetFragment != -1) {
                loadFragmentWithSmartAnimation(fragment, targetFragment);
                return true;
            }
            
            return false;
        });
    }
    
    private void showAccountSheet() {
        AccountBottomSheet bottomSheet = AccountBottomSheet.newInstance("Usuario", "usuario@email.com");
        bottomSheet.setOnAccountActionListener(this);
        bottomSheet.show(getSupportFragmentManager(), "AccountBottomSheet");
    }
    
    private void observeViewModel() {
        viewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                // Actualizar el saludo en el AppBar personalizado usando recurso string
                String saludo = getString(R.string.greeting_user, user.name);
                binding.customAppbar.tvUserGreeting.setText(saludo);

            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            viewModel.signOut();
            Intent intent = new Intent(this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // Implementación de AccountBottomSheet.OnAccountActionListener
    @Override
    public void onSettingsClicked() {
        // TODO: Implementar navegación a configuración
        android.widget.Toast.makeText(this, "Configuración", android.widget.Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onAboutClicked() {
        // TODO: Implementar navegación a acerca de
        android.widget.Toast.makeText(this, "Acerca de", android.widget.Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onLogoutClicked() {
        viewModel.signOut();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    
    // Método para obtener el UID del usuario actual
    public String getCurrentUserUid() {
        return viewModel.getCurrentUserUid();
    }
}
