# 🔧 Guía para Resolver Error de Google Sign-In (StatusCode 10)

## ✅ Verificación Actual

### SHA-1 del Certificado de Debug
```
SHA1: 40:E2:C5:9E:C7:10:33:11:0F:9B:E9:B6:A1:E6:0F:79:07:CD:37:6F
SHA-1 (sin dos puntos): 40e2c59ec71033110f9be9b6a1e60f7907cd376f
```

### Web Client ID Configurado
```
933557167534-sungdr1r29j7b9bbc84ugfsickr11ce3.apps.googleusercontent.com
```

### Package Name
```
com.glebursol.registrogastos
```

## 🔍 Pasos para Resolver el Error

### Paso 1: Verificar SHA-1 en Firebase Console

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona el proyecto: **gestor-gastos-app-6e1d9**
3. Haz clic en el **ícono de engranaje (⚙️)** → **Configuración del proyecto**
4. Ve a la pestaña **"General"**
5. Desplázate hasta **"Tus aplicaciones"**
6. Busca la app Android con package: `com.glebursol.registrogastos`
7. Verifica que el SHA-1 esté registrado:
   - Debe aparecer: `40e2c59ec71033110f9be9b6a1e60f7907cd376f`
   - Si NO está, haz clic en **"Agregar huella digital"** y agrega:
     ```
     40:E2:C5:9E:C7:10:33:11:0F:9B:E9:B6:A1:E6:0F:79:07:CD:37:6F
     ```
8. **IMPORTANTE**: Después de agregar el SHA-1, descarga el nuevo `google-services.json` y reemplaza el actual

### Paso 2: Verificar OAuth en Google Cloud Console

1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Selecciona el proyecto: **gestor-gastos-app-6e1d9** (o el que corresponda)
3. Ve a **"APIs y servicios"** → **"Credenciales"**
4. Busca el **"ID de cliente de OAuth 2.0"** con el ID:
   ```
   933557167534-sungdr1r29j7b9bbc84ugfsickr11ce3.apps.googleusercontent.com
   ```
5. Haz clic en el cliente para editarlo
6. Verifica que:
   - **Tipo de aplicación**: "Aplicación web"
   - **Orígenes autorizados de JavaScript**: Debe estar vacío o tener URLs válidas
   - **URI de redirección autorizados**: Debe tener al menos:
     ```
     https://gestor-gastos-app-6e1d9.firebaseapp.com/__/auth/handler
     ```
7. Si falta alguna configuración, agrégalo y guarda

### Paso 3: Verificar Pantalla de Consentimiento OAuth

1. En Google Cloud Console, ve a **"APIs y servicios"** → **"Pantalla de consentimiento de OAuth"**
2. Verifica que:
   - **Tipo de usuario**: "Externo" o "Interno" (según tu caso)
   - **Información de la aplicación**:
     - Nombre de la app: "Gestor de Gastos" (o el que prefieras)
     - Correo electrónico de soporte: Tu email
     - Logo: (opcional)
   - **Dominios autorizados**: Debe incluir:
     ```
     gestor-gastos-app-6e1d9.firebaseapp.com
     ```
   - **Alcances**: Debe incluir al menos:
     - `openid`
     - `email`
     - `profile`
3. Si falta algo, complétalo y guarda
4. Si es la primera vez, deberás **"Publicar"** la app

### Paso 4: Verificar APIs Habilitadas

1. En Google Cloud Console, ve a **"APIs y servicios"** → **"Biblioteca"**
2. Busca y verifica que estén **habilitadas**:
   - ✅ **Google Sign-In API** (Identity Toolkit API)
   - ✅ **Firebase Authentication API**
3. Si no están habilitadas, haz clic en cada una y presiona **"Habilitar"**

### Paso 5: Actualizar google-services.json

1. En Firebase Console, ve a **Configuración del proyecto** → **General**
2. En **"Tus aplicaciones"**, busca la app Android
3. Haz clic en **"Descargar google-services.json"**
4. Reemplaza el archivo en: `app/google-services.json`
5. **Sincroniza el proyecto** en Android Studio

### Paso 6: Limpiar y Recompilar

```bash
# Limpiar el proyecto
.\gradlew clean

# Recompilar
.\gradlew assembleDebug
```

### Paso 7: Probar de Nuevo

1. Ejecuta la app en modo debug
2. Intenta iniciar sesión con Google
3. Si el error persiste, verifica los logs en Android Studio:
   ```
   Logcat → Filtrar por: "AuthActivity" o "GoogleSignIn"
   ```

## 🐛 Solución de Problemas

### Error persiste después de seguir los pasos

1. **Verifica que el package name sea exacto**:
   - En `app/build.gradle.kts`: `applicationId = "com.glebursol.registrogastos"`
   - En Firebase Console: debe coincidir exactamente

2. **Verifica que el SHA-1 sea correcto**:
   ```bash
   .\gradlew signingReport
   ```
   - Copia el SHA-1 de la variante "debug"
   - Asegúrate de que esté en Firebase Console (sin dos puntos)

3. **Verifica que el Web Client ID sea correcto**:
   - En `strings.xml`: `default_web_client_id`
   - Debe coincidir con el `client_type: 3` en `google-services.json`

4. **Espera unos minutos**:
   - Los cambios en Google Cloud Console pueden tardar 5-10 minutos en propagarse

5. **Revisa los logs detallados**:
   ```bash
   adb logcat | findstr /C:"GoogleSignIn" /C:"AuthActivity" /C:"FirebaseAuth"
   ```

## 📝 Notas Importantes

- ⚠️ El SHA-1 de **debug** es diferente al SHA-1 de **release**
- ⚠️ Si vas a publicar en Play Store, necesitarás agregar el SHA-1 del certificado de release también
- ⚠️ Después de cambiar configuración en Firebase, siempre descarga el nuevo `google-services.json`
- ⚠️ Los cambios en Google Cloud Console pueden tardar en aplicarse

## 🔗 Enlaces Útiles

- [Firebase Console](https://console.firebase.google.com/)
- [Google Cloud Console](https://console.cloud.google.com/)
- [Documentación de Google Sign-In](https://developers.google.com/identity/sign-in/android/start)

