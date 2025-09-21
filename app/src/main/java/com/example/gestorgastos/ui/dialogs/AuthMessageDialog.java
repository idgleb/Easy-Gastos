package com.example.gestorgastos.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
    public static final String TYPE_ERROR = "error";
    public static final String TYPE_INFO = "info";
    public static final String TYPE_WARNING = "warning";
    
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
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Material3DialogTheme);
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        
        // Configurar el diálogo
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
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
        
        switch (type) {
            case TYPE_SUCCESS:
                ivMessageIcon.setText("✅");
                ivMessageIcon.setTextColor(context.getColor(R.color.green));
                btnAction.setBackgroundColor(context.getColor(R.color.green));
                break;
                
            case TYPE_ERROR:
                ivMessageIcon.setText("❌");
                ivMessageIcon.setTextColor(context.getColor(R.color.red));
                btnAction.setBackgroundColor(context.getColor(R.color.red));
                break;
                
            case TYPE_WARNING:
                ivMessageIcon.setText("⚠️");
                ivMessageIcon.setTextColor(context.getColor(R.color.orange));
                btnAction.setBackgroundColor(context.getColor(R.color.orange));
                break;
                
            case TYPE_INFO:
            default:
                ivMessageIcon.setText("ℹ️");
                ivMessageIcon.setTextColor(context.getColor(R.color.blue));
                btnAction.setBackgroundColor(context.getColor(R.color.blue));
                break;
        }
    }
    
    public void setOnDialogActionListener(OnDialogActionListener listener) {
        this.listener = listener;
    }
}
