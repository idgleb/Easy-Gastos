package com.example.gestorgastos.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.gestorgastos.R;
import com.google.android.material.button.MaterialButton;

public class AuthMessageDialog extends DialogFragment {
    
    public static final String TAG = "AuthMessageDialog";
    
    // Tipos de mensaje
    public static final String TYPE_SUCCESS = "success";
    public static final String TYPE_INFO = "info";
    public static final String TYPE_ERROR = "error";
    
    
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_TYPE = "type";
    private static final String ARG_BUTTON_TEXT = "button_text";
    
    private TextView tvMessageTitle;
    private TextView tvMessageText;
    private TextView ivMessageIcon;
    private MaterialButton btnAction;
    private ImageButton btnClose;
    
    private OnDialogActionListener listener;
    
    public interface OnDialogActionListener {
        void onActionClicked();
        void onDialogClosed();
    }
    
    public static AuthMessageDialog newInstance(String title, String message, String type, String buttonText) {
        AuthMessageDialog dialog = new AuthMessageDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        args.putString(ARG_TYPE, type);
        args.putString(ARG_BUTTON_TEXT, buttonText);
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Usar tema sin marco para permitir fondo transparente y tarjeta personalizada
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Material3DialogTheme);
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        
        // Configurar el diálogo
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            // Oscurecer el fondo detrás del diálogo para efecto glass
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.3f);
            // Aplicar blur del contenido detrás en Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                dialog.getWindow().setBackgroundBlurRadius(60);
                } catch (Exception e) {
                    // Ignorar si falla el blur - no es crítico para la funcionalidad
                    android.util.Log.w(TAG, "No se pudo aplicar blur de fondo", e);
                }
            }
        }
        
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        
        return dialog;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_auth_message, container, false);
        
        initViews(view);
        setupListeners();
        configureContent();
        
        return view;
    }
    
    private void initViews(View view) {
        tvMessageTitle = view.findViewById(R.id.tvMessageTitle);
        tvMessageText = view.findViewById(R.id.tvMessageText);
        ivMessageIcon = view.findViewById(R.id.ivMessageIcon);
        btnAction = view.findViewById(R.id.btnAction);
        btnClose = view.findViewById(R.id.btnClose);
    }
    
    private void setupListeners() {
        btnAction.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActionClicked();
            }
            dismiss();
        });
        
        btnClose.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDialogClosed();
            }
            dismiss();
        });
        
        // También cerrar al tocar fuera del diálogo
        getDialog().setOnCancelListener(dialog -> {
            if (listener != null) {
                listener.onDialogClosed();
            }
        });
    }
    
    private void configureContent() {
        Bundle args = getArguments();
        if (args == null) return;
        
        String title = args.getString(ARG_TITLE, "Mensaje");
        String message = args.getString(ARG_MESSAGE, "");
        String type = args.getString(ARG_TYPE, TYPE_INFO);
        String buttonText = args.getString(ARG_BUTTON_TEXT, "Entendido");
        
        tvMessageTitle.setText(title);
        tvMessageText.setText(message);
        btnAction.setText(buttonText);
        
        // Configurar icono y colores según el tipo
        configureType(type);
    }
    
    private void configureType(String type) {
        Context context = getContext();
        if (context == null) return;
        
        if (TYPE_SUCCESS.equals(type)) {
            ivMessageIcon.setText("✅");
            ivMessageIcon.setTextColor(context.getColor(R.color.white));
            btnAction.setBackgroundColor(context.getColor(R.color.dialog_button_glass_bg));
        } else if (TYPE_ERROR.equals(type)) {
            ivMessageIcon.setText("❌");
            ivMessageIcon.setTextColor(context.getColor(R.color.white));
            btnAction.setBackgroundColor(context.getColor(R.color.dialog_button_glass_bg));
        } else {
            // TYPE_INFO por defecto
            ivMessageIcon.setText("ℹ️");
            ivMessageIcon.setTextColor(context.getColor(R.color.white));
            btnAction.setBackgroundColor(context.getColor(R.color.dialog_button_glass_bg));
        }
    }
    
    public void setOnDialogActionListener(OnDialogActionListener listener) {
        this.listener = listener;
    }
}
