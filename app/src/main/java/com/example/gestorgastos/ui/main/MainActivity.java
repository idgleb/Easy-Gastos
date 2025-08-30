package com.example.gestorgastos.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.gestorgastos.R;
import com.example.gestorgastos.databinding.ActivityMainBinding;
import com.example.gestorgastos.ui.auth.AuthActivity;
import com.example.gestorgastos.ui.categories.CategoriesFragment;
import com.example.gestorgastos.ui.dashboard.DashboardFragment;
import com.example.gestorgastos.ui.expenses.ExpensesFragment;
import com.example.gestorgastos.ui.dialogs.AccountBottomSheet;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements AccountBottomSheet.OnAccountActionListener {
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Forzar modo claro
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
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
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            
            if (item.getItemId() == R.id.nav_home) {
                fragment = new ExpensesFragment();
            } else if (item.getItemId() == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
            } else if (item.getItemId() == R.id.nav_categories) {
                fragment = new CategoriesFragment();
            }
            
            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            
            return false;
        });
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
    
    private void showAccountSheet() {
        AccountBottomSheet bottomSheet = AccountBottomSheet.newInstance("Usuario", "usuario@email.com");
        bottomSheet.setOnAccountActionListener(this);
        bottomSheet.show(getSupportFragmentManager(), "AccountBottomSheet");
    }
    
    private void observeViewModel() {
        viewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                // Actualizar el saludo en el AppBar personalizado
                binding.customAppbar.tvUserGreeting.setText("Hola, " + user.name);
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
}


