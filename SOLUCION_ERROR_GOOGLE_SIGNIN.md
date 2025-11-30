# ✅ Solución para Error de Google Sign-In

## 🔍 Problema Identificado

En Firebase Console, la app está registrada con:
- **Package name**: `com.example.gestorgastos` ❌

Pero tu aplicación actual usa:
- **applicationId**: `com.glebursol.registrogastos` ✅

**Esto causa el error de Google Sign-In porque Firebase no encuentra la app con el package name correcto.**

## 🚀 Solución: Agregar Nueva App en Firebase

### Paso 1: Agregar Nueva App Android

1. En Firebase Console, en la página de **Configuración del proyecto**
2. En la sección **"Apps para Android"**, haz clic en **"Agregar app"** (botón azul en la parte superior)
3. Completa el formulario:
   - **Nombre del paquete de Android**: `com.glebursol.registrogastos`
   - **Sobrenombre de la app** (opcional): "Gestor Gastos" o "Registro de Gastos Offline"
   - **Certificado de firma de depuración SHA-1** (opcional por ahora):
     ```
     40:e2:c5:9e:c7:10:33:11:0f:9b:e9:b6:a1:e6:0f:79:07:cd:37:6f
     ```
4. Haz clic en **"Registrar app"**

### Paso 2: Agregar SHA-1 a la Nueva App

1. Una vez creada la app, verás su configuración
2. En la sección **"Huellas digitales del certificado SHA"**
3. Haz clic en **"Agregar huella digital"**
4. Pega el SHA-1:
   ```
   40:e2:c5:9e:c7:10:33:11:0f:9b:e9:b6:a1:e6:0f:79:07:cd:37:6f
   ```
5. Haz clic en **"Guardar"**

### Paso 3: Descargar Nuevo google-services.json

1. En la misma página de configuración de la nueva app
2. Haz clic en el botón **"google-services.json"** (botón de descarga)
3. Reemplaza el archivo actual en: `app/google-services.json`
4. **IMPORTANTE**: Verifica que el nuevo archivo tenga:
   - `package_name`: `com.glebursol.registrogastos`
   - El SHA-1 correcto en `certificate_hash`

### Paso 4: Sincronizar en Android Studio

1. En Android Studio, haz clic en **"Sync Project with Gradle Files"** (icono de elefante)
2. O ejecuta: `File → Sync Project with Gradle Files`

### Paso 5: Limpiar y Recompilar

```bash
.\gradlew clean
.\gradlew assembleDebug
```

### Paso 6: Probar Google Sign-In

1. Ejecuta la app
2. Intenta iniciar sesión con Google
3. El error debería estar resuelto ✅

## 📝 Notas Importantes

- ⚠️ **NO elimines la app antigua** (`com.example.gestorgastos`) si tienes datos en producción
- ⚠️ Puedes tener **múltiples apps** en Firebase para el mismo proyecto
- ⚠️ Cada app debe tener su propio `google-services.json` con el package name correcto
- ⚠️ El SHA-1 debe estar registrado en la app correcta (`com.glebursol.registrogastos`)

## 🔍 Verificación

Después de seguir los pasos, verifica que:

1. ✅ Existe una app en Firebase con package: `com.glebursol.registrogastos`
2. ✅ El SHA-1 está registrado en esa app
3. ✅ El `google-services.json` tiene el package name correcto
4. ✅ El `applicationId` en `build.gradle.kts` coincide con el package name en Firebase

## 🐛 Si el Error Persiste

1. Verifica que el `google-services.json` descargado sea el de la app correcta
2. Asegúrate de que el SHA-1 esté en formato correcto (con o sin dos puntos, ambos funcionan)
3. Espera 5-10 minutos después de agregar la app (puede tardar en propagarse)
4. Verifica en Google Cloud Console que el OAuth client esté configurado para el nuevo package name

