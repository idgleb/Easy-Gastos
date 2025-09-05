package com.example.gestorgastos.util;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.view.Window;
import android.view.WindowInsetsController;
import android.content.Context;

public final class NavBarUtils {
    private NavBarUtils() { /* Utility class */ }

    public static void aplicarEstiloNavBar(Object source) {
        Window window = null;
        if (source instanceof Activity) {
            window = ((Activity) source).getWindow();
        } else if (source instanceof Dialog) {
            window = ((Dialog) source).getWindow();
        } else if (source instanceof Window) {
            window = (Window) source;
        }
        if (window == null) return;

        Context context = window.getContext();
        int blackColor;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            blackColor = context.getColor(android.R.color.black);
        } else {
            blackColor = context.getResources().getColor(android.R.color.black);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            window.setNavigationBarContrastEnforced(false);
            window.setNavigationBarColor(blackColor);
            WindowInsetsController insetsController = window.getInsetsController();
            if (insetsController != null) {
                insetsController.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
            }
        } else {
            // Android 8.1 - 10
            window.setNavigationBarColor(blackColor);
            window.getDecorView().setSystemUiVisibility(0);
        }
    }
}

