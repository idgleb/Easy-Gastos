package com.example.gestorgastos.ui.auth;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RadialGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;

import androidx.annotation.Nullable;

import com.example.gestorgastos.R;

public class AnimatedGradientBackground extends View {

    private Paint paint;
    private ValueAnimator animator;
    
    // Colores individuales del gradiente (se inicializan en init())
    private int centerColor;
    private int midColor;
    private int edgeColor;
    private int outerColor;
    
    private float[] positions = {0f, 0.1f, 0.4f, 1.0f}; // more concentrated white in center
    private float baseRadius;
    private float animatedRadius;
    private ValueAnimator radiusAnimator;

    public AnimatedGradientBackground(Context context) {
        super(context);
        init();
    }

    public AnimatedGradientBackground(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // ========================================
        // CONFIGURACIÓN INICIAL DE COLORES
        // ========================================
        // Cargar colores desde recursos para mantener consistencia
        // y facilitar cambios sin modificar código
        centerColor = ContextCompat.getColor(getContext(), R.color.gradient_center);
        midColor = ContextCompat.getColor(getContext(), R.color.gradient_mid);
        edgeColor = ContextCompat.getColor(getContext(), R.color.gradient_edge);
        outerColor = ContextCompat.getColor(getContext(), R.color.gradient_outer);
        
        // Configurar el pincel para el gradiente radial
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        // ========================================
        // ANIMACIÓN DE COLORES (INTERPOLACIÓN)
        // ========================================
        // Crear animador que interpola entre 0 y 1 para la transición de colores
        animator = ValueAnimator.ofFloat(0f, 1f);
        
        // Duración: 3 segundos por ciclo completo (ida y vuelta)
        // Tiempo más largo para un efecto más suave y menos agresivo
        animator.setDuration(2000);
        
        // Repetir infinitamente para mantener la animación constante
        animator.setRepeatCount(ValueAnimator.INFINITE);
        
        // Modo REVERSE: cuando llega al final, regresa al inicio suavemente
        // Esto crea un efecto de "respiración" en los colores
        animator.setRepeatMode(ValueAnimator.REVERSE);
        
        // Listener que se ejecuta en cada frame de la animación
        animator.addUpdateListener(animation -> {
            // Obtener el valor actual de la animación (0.0 a 1.0)
            float fraction = (float) animation.getAnimatedValue();
            
            // Cargar colores base desde recursos para la interpolación
            int baseMidColor = ContextCompat.getColor(getContext(), R.color.gradient_mid);
            int baseEdgeColor = ContextCompat.getColor(getContext(), R.color.gradient_edge);
            
            // Interpolar entre colores base usando ArgbEvaluator
            // midColor: transición de azul a púrpura
            midColor = (int) new ArgbEvaluator().evaluate(fraction, baseMidColor, baseEdgeColor);
            // edgeColor: transición de púrpura a azul (inverso)
            edgeColor = (int) new ArgbEvaluator().evaluate(fraction, baseEdgeColor, baseMidColor);
            
            // Forzar redibujado del view con los nuevos colores
            invalidate();
        });
        // Iniciar la animación de colores
        animator.start();

        // ========================================
        // ANIMACIÓN DE RADIO (PULSO)
        // ========================================
        // Crear animador para el radio del gradiente radial
        // Valores: 0.8f (80%) a 1.2f (120%) del radio base
        radiusAnimator = ValueAnimator.ofFloat(0.2f, 0.6f);
        
        // Duración: 1.5 segundos por ciclo (más rápido que la animación de colores)
        // Esto crea un efecto de "pulso" más dinámico
        radiusAnimator.setDuration(1500);
        
        // Repetir infinitamente
        radiusAnimator.setRepeatCount(ValueAnimator.INFINITE);
        
        // Modo REVERSE: cuando llega al máximo, regresa al mínimo
        // Crea efecto de "respiración" en el tamaño del gradiente
        radiusAnimator.setRepeatMode(ValueAnimator.REVERSE);
        
        // Listener que se ejecuta en cada frame de la animación del radio
        radiusAnimator.addUpdateListener(animation -> {
            // Solo aplicar si ya se calculó el radio base
            if (baseRadius > 0) {
                // Multiplicar el radio base por el factor de animación (0.8 a 1.2)
                animatedRadius = baseRadius * (float) animation.getAnimatedValue();
                // Forzar redibujado con el nuevo radio
                invalidate();
            }
        });
        // Iniciar la animación del radio
        radiusAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // ========================================
        // CONFIGURACIÓN DE DIMENSIONES
        // ========================================
        // Obtener dimensiones actuales del view
        int width = getWidth();
        int height = getHeight();
        
        // Calcular radio base solo una vez (optimización)
        // Radio = 120% de la dimensión más grande (width o height)
        // Esto asegura que el gradiente cubra toda la pantalla
        if (baseRadius == 0) {
            baseRadius = Math.max(width, height) / 2f * 2.2f;
        }
        
        // ========================================
        // POSICIONAMIENTO DEL CENTRO
        // ========================================
        // Centro horizontal: mitad del ancho
        float centerX = width / 2f;
        // Centro vertical: 115% de la altura (más abajo del centro)
        // Esto crea un efecto visual donde el punto más brillante
        // aparece en la parte inferior de la pantalla
        float centerY = height * 0.6f;
        
        // ========================================
        // RADIO ANIMADO
        // ========================================
        // Usar radio animado si está disponible, sino usar el base
        // El radio animado cambia entre 80% y 120% del radio base
        float currentRadius = animatedRadius > 0 ? animatedRadius : baseRadius;
        
        // ========================================
        // CREACIÓN DEL GRADIENTE RADIAL
        // ========================================
        // Crear gradiente radial con:
        // - Centro en (centerX, centerY)
        // - Radio actual (animado o base)
        // - Colores: centro → medio → borde → exterior
        // - Posiciones: 0% → 10% → 40% → 100%
        RadialGradient gradient = new RadialGradient(centerX, centerY, currentRadius, 
            new int[]{centerColor, midColor, edgeColor, outerColor}, positions, Shader.TileMode.CLAMP);
        
        // Aplicar el gradiente al pincel
        paint.setShader(gradient);
        
        // ========================================
        // DIBUJADO DEL GRADIENTE
        // ========================================
        // Dibujar un rectángulo que cubra toda la pantalla
        // El gradiente se aplicará como fondo
        canvas.drawRect(0, 0, width, height, paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        
        // ========================================
        // LIMPIEZA DE RECURSOS
        // ========================================
        // Cancelar animación de colores para liberar memoria
        // y evitar memory leaks cuando el view se destruye
        if (animator != null) {
            animator.cancel();
        }
        
        // Cancelar animación de radio para liberar memoria
        // y evitar memory leaks cuando el view se destruye
        if (radiusAnimator != null) {
            radiusAnimator.cancel();
        }
    }
}
