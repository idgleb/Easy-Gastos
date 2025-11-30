package com.example.gestorgastos.ui.dialogs;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.gestorgastos.R;
import com.example.gestorgastos.databinding.DialogUpdateBinding;
import com.example.gestorgastos.util.UpdateChecker;

/**
 * Diálogo que muestra información sobre una actualización disponible.
 * Permite al usuario actualizar la app o posponer la actualización.
 */
public class UpdateDialog extends DialogFragment {
    private static final String ARG_VERSION_NAME = "version_name";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_IS_MANDATORY = "is_mandatory";
    
    private DialogUpdateBinding binding;
    private boolean isMandatory;
    
    public static UpdateDialog newInstance(String versionName, String message, boolean isMandatory) {
        UpdateDialog dialog = new UpdateDialog();
        Bundle args = new Bundle();
        args.putString(ARG_VERSION_NAME, versionName);
        args.putString(ARG_MESSAGE, message);
        args.putBoolean(ARG_IS_MANDATORY, isMandatory);
        dialog.setArguments(args);
        return dialog;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        
        // Si es obligatorio, no se puede cerrar
        if (getArguments() != null) {
            isMandatory = getArguments().getBoolean(ARG_IS_MANDATORY, false);
        }
        
        dialog.setCancelable(!isMandatory);
        dialog.setCanceledOnTouchOutside(!isMandatory);
        return dialog;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogUpdateBinding.inflate(inflater, container, false);
        
        if (getArguments() != null) {
            String versionName = getArguments().getString(ARG_VERSION_NAME, getString(R.string.update_version_default));
            String message = getArguments().getString(ARG_MESSAGE, getString(R.string.update_message_default));
            isMandatory = getArguments().getBoolean(ARG_IS_MANDATORY, false);
            
            binding.tvVersionName.setText(getString(R.string.update_version_format, versionName));
            binding.tvMessage.setText(message);
            
            if (isMandatory) {
                binding.btnLater.setVisibility(View.GONE);
                binding.btnUpdate.setText(R.string.update_button_mandatory);
            } else {
                binding.btnLater.setVisibility(View.VISIBLE);
                binding.btnUpdate.setText(R.string.update_button_update);
            }
        }
        
        binding.btnUpdate.setOnClickListener(v -> {
            UpdateChecker.openPlayStore(requireContext());
            dismiss();
        });
        
        binding.btnLater.setOnClickListener(v -> {
            dismiss();
        });
        
        return binding.getRoot();
    }
}

