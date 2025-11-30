# 🚀 Guía Práctica: Subir App a Google Play Store

Ya tienes cuenta de desarrollador, así que vamos directo al grano.

## ✅ Estado Actual Verificado

- ✅ `applicationId`: `com.glebursol.registrogastos` (correcto, no es `com.example.*`)
- ✅ `versionCode`: `1` (correcto para primera publicación)
- ✅ `versionName`: `1.0` (correcto)
- ✅ Firebase configurado correctamente

---

## 📋 Paso 1: Crear Keystore (Firma de la App)

**⚠️ CRÍTICO**: Sin keystore no puedes publicar. Si lo pierdes, no podrás actualizar la app.

### Opción A: Desde Android Studio (Recomendado)

1. Abre Android Studio
2. **Build → Generate Signed Bundle / APK**
3. Selecciona **"Android App Bundle"**
4. Clic en **"Create new..."** (crear nuevo keystore)
5. Completa el formulario:
   - **Key store path**: `C:\Users\idgle\keystore\gestorgastos.jks`
     - (Crea la carpeta `keystore` si no existe)
   - **Password**: Crea una contraseña fuerte (GUÁRDALA BIEN)
   - **Key alias**: `gestorgastos-key`
   - **Key password**: Otra contraseña (puede ser la misma)
   - **Validity**: 25 años (máximo: 10000 días)
   - **First and Last Name**: Gleb Ursol
   - **Organizational Unit**: (opcional)
   - **Organization**: (opcional)
   - **City**: Tu ciudad
   - **State**: Tu estado/provincia
   - **Country Code**: AR
6. Clic en **"OK"**
7. **GUARDA EL KEYSTORE Y LAS CONTRASEÑAS EN UN LUGAR SEGURO**

### Opción B: Desde Línea de Comandos

```bash
keytool -genkey -v -keystore C:\Users\idgle\keystore\gestorgastos.jks -alias gestorgastos-key -keyalg RSA -keysize 2048 -validity 10000
```

---

## 📝 Paso 2: Configurar Firma en el Proyecto

### 2.1 Crear `keystore.properties`

Crea el archivo `keystore.properties` en la **raíz del proyecto** (mismo nivel que `build.gradle.kts`):

```properties
storePassword=TU_PASSWORD_DEL_KEYSTORE
keyPassword=TU_PASSWORD_DEL_KEY
keyAlias=gestorgastos-key
storeFile=C:\\Users\\idgle\\keystore\\gestorgastos.jks
```

**⚠️ IMPORTANTE**: 
- Reemplaza `TU_PASSWORD_DEL_KEYSTORE` y `TU_PASSWORD_DEL_KEY` con tus contraseñas reales
- El archivo ya está en `.gitignore`, no se subirá a Git

### 2.2 Actualizar `app/build.gradle.kts`

Agrega esto al **inicio** del archivo (antes de `android {`):

```kotlin
// Cargar propiedades del keystore
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = java.util.Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
}
```

Y luego, dentro de `android {`, agrega:

```kotlin
android {
    namespace = "com.example.gestorgastos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.glebursol.registrogastos"
        // ... resto de la configuración ...
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    // ... resto de la configuración ...
}
```

### 2.3 Verificar que Compila

```bash
.\gradlew clean
.\gradlew bundleRelease
```

Si todo está bien, verás:
```
BUILD SUCCESSFUL
```

El AAB estará en: `app/build/outputs/bundle/release/app-release.aab`

---

## 🎨 Paso 3: Preparar Materiales de la App

### 3.1 Icono de la App (512x512 px)

- **Tamaño**: 512x512 píxeles
- **Formato**: PNG (sin transparencia)
- **Ubicación**: Puedes usar el icono actual y redimensionarlo

### 3.2 Capturas de Pantalla

**Mínimo requerido:**
- **Teléfono**: Al menos 2 capturas (mínimo 320px de altura)
- **Tablet (7")**: Al menos 1 captura (mínimo 320px de altura)
- **Tablet (10")**: Al menos 1 captura (mínimo 320px de altura)

**Tamaños recomendados:**
- Teléfono: 1080x1920 px (vertical)
- Tablet 7": 1200x1920 px
- Tablet 10": 1600x2560 px

**Cómo tomarlas:**
1. Ejecuta la app en un emulador o dispositivo
2. Toma capturas de las pantallas principales:
   - Dashboard
   - Lista de gastos
   - Lista de categorías
   - Configuración
3. Edítalas si es necesario (recortar, agregar texto, etc.)

### 3.3 Feature Graphic (1024x500 px)

- Imagen promocional para la página de la app
- Opcional pero recomendado

### 3.4 Descripción de la App

**Título corto** (máx. 50 caracteres):
```
Registro de Gastos Offline
```

**Descripción corta** (máx. 80 caracteres):
```
Gestiona tus gastos fácilmente, incluso sin internet
```

**Descripción completa** (máx. 4000 caracteres):
```
Registro de Gastos Offline es una aplicación intuitiva para gestionar tus finanzas personales de manera eficiente.

✨ CARACTERÍSTICAS PRINCIPALES:

📊 Dashboard Interactivo
- Visualiza tus gastos del mes con gráficos interactivos
- Analiza tus gastos por categoría
- Monitorea tu presupuesto mensual

💰 Gestión de Gastos
- Registra tus gastos rápidamente
- Organiza por categorías personalizables
- Historial completo de tus transacciones

📱 Funciona Offline
- Registra gastos sin conexión a internet
- Sincronización automática cuando hay conexión
- Tus datos siempre seguros y disponibles

🎨 Modo Oscuro
- Interfaz adaptativa con modo claro y oscuro
- Personaliza la apariencia según tu preferencia

🔒 Seguridad
- Tus datos están protegidos con Firebase
- Sincronización segura entre dispositivos
- Respaldos automáticos en la nube

📈 Análisis Detallado
- Estadísticas mensuales
- Top categorías de gastos
- Progreso de presupuesto

¡Comienza a controlar tus finanzas hoy mismo!
```

### 3.5 Política de Privacidad

Necesitas una URL pública. Opciones:

1. **GitHub Pages** (gratis):
   - Crea un archivo `PRIVACY.md` en tu repositorio
   - Activa GitHub Pages
   - URL: `https://tu-usuario.github.io/tu-repo/PRIVACY.md`

2. **Crear página simple**:
   - Puedes usar servicios como [Privacy Policy Generator](https://www.privacypolicygenerator.info/)

---

## 🚀 Paso 4: Crear App en Google Play Console

### 4.1 Acceder a Google Play Console

1. Ve a: **https://play.google.com/console**
2. Inicia sesión con tu cuenta de desarrollador

### 4.2 Crear Nueva App

1. Clic en **"Crear app"** o **"Create app"** (botón grande)
2. Completa el formulario:
   - **Nombre de la app**: `Registro de Gastos Offline`
   - **Idioma predeterminado**: Español (España) o Español (Latinoamérica)
   - **Tipo de app**: App
   - **Gratis o de pago**: Gratis
   - **Declaraciones**: Marca las casillas requeridas
     - ✅ Contenido de la app
     - ✅ Política de privacidad
     - ✅ Normas de la tienda
3. Clic en **"Crear app"**

### 4.3 Configurar Store Listing

1. En el menú lateral, ve a **"Store listing"**
2. Completa todos los campos:

   **App name**: `Registro de Gastos Offline`
   
   **Short description**: `Gestiona tus gastos fácilmente, incluso sin internet`
   
   **Full description**: (Pega la descripción completa que preparaste)
   
   **App icon**: Sube el icono de 512x512 px
   
   **Feature graphic**: 1024x500 px (opcional pero recomendado)
   
   **Screenshots**: 
   - Sube las capturas de teléfono (mínimo 2)
   - Sube las capturas de tablet si las tienes
   
   **Categoría**: Finanzas
   
   **Contact details**:
   - Email: tu-email@ejemplo.com
   - Teléfono: (opcional)
   - Sitio web: (opcional, puede ser tu GitHub)
   
   **Privacy Policy**: URL de tu política de privacidad

3. Clic en **"Guardar"** (arriba a la derecha)

### 4.4 Configurar Contenido de la App

1. Ve a **"Content rating"** (Clasificación de contenido)
2. Completa el cuestionario sobre el contenido de tu app
3. Para una app de finanzas generalmente será: **"Everyone"** o **"Para todos"**
4. Guarda

### 4.5 Configurar Precios y Distribución

1. Ve a **"Pricing & distribution"** (Precios y distribución)
2. Selecciona **"Free"** (Gratis)
3. Selecciona los países donde quieres distribuir:
   - **"All countries"** (Todos los países) - Recomendado
   - O selecciona países específicos
4. Marca las declaraciones requeridas:
   - ✅ Contenido de la app
   - ✅ Política de privacidad
   - ✅ Normas de la tienda
5. Clic en **"Guardar"**

---

## 📤 Paso 5: Subir el AAB

### 5.1 Generar el AAB Firmado

**Desde Android Studio:**
1. **Build → Generate Signed Bundle / APK**
2. Selecciona **"Android App Bundle"**
3. Selecciona tu keystore y completa las contraseñas
4. Selecciona **"release"** como build variant
5. Clic en **"Finish"**
6. El AAB estará en: `app/build/outputs/bundle/release/app-release.aab`

**Desde línea de comandos:**
```bash
.\gradlew bundleRelease
```

### 5.2 Subir a Google Play Console

1. En Google Play Console, ve a **"Production"** (Producción) en el menú lateral
2. Clic en **"Create new release"** (Crear nueva versión)
3. En la sección **"App bundles"**, clic en **"Upload"**
4. Selecciona tu archivo `app-release.aab`
5. Espera a que se procese (puede tardar unos minutos)

### 5.3 Completar Información de la Versión

1. **Release name**: `1.0 - Versión inicial`
2. **Release notes** (Notas de la versión):
   ```
   🎉 Primera versión de Registro de Gastos Offline
   
   ✨ Características:
   - Gestión completa de gastos
   - Dashboard con gráficos interactivos
   - Funciona offline
   - Modo oscuro
   - Sincronización automática
   ```

### 5.4 Revisar y Publicar

1. Revisa toda la información
2. Clic en **"Review release"** (Revisar versión)
3. Si todo está correcto, clic en **"Start rollout to Production"** (Iniciar publicación en Producción)
4. Confirma la publicación

---

## ⏳ Paso 6: Proceso de Revisión

Google revisará tu app. Esto puede tardar:

- **Primera publicación**: 1-7 días
- **Actualizaciones**: 1-3 días

Durante la revisión:
- Tu app aparecerá como "En revisión"
- Recibirás notificaciones por email sobre el estado
- Si hay problemas, Google te notificará con los motivos

---

## ✅ Checklist Final

Antes de publicar, verifica:

- [ ] Keystore creado y guardado de forma segura
- [ ] Passwords del keystore guardadas de forma segura
- [ ] `keystore.properties` configurado correctamente
- [ ] `build.gradle.kts` actualizado con signingConfigs
- [ ] AAB generado y probado (`.\gradlew bundleRelease`)
- [ ] Icono de 512x512 px preparado
- [ ] Capturas de pantalla preparadas (mínimo 2 para teléfono)
- [ ] Descripción de la app escrita
- [ ] Política de privacidad publicada (URL)
- [ ] Store listing completado
- [ ] Content rating completado
- [ ] Pricing & distribution configurado
- [ ] AAB subido a Google Play Console

---

## 🔄 Actualizar la App (Versiones Futuras)

Cuando quieras publicar una actualización:

1. **Actualiza `build.gradle.kts`**:
   ```kotlin
   versionCode = 2        // Incrementar
   versionName = "1.1"    // Nueva versión
   ```

2. **Genera nuevo AAB**:
   ```bash
   .\gradlew bundleRelease
   ```

3. **Actualiza Firestore** (usando el script):
   ```bash
   node scripts/update-version.js 2 "1.1" "Nueva versión con mejoras"
   ```

4. **Sube el nuevo AAB** en Google Play Console → Production → Create new release

5. **Agrega notas de la versión** y publica

---

## 🆘 Problemas Comunes

### "Error al firmar"
- Verifica que el keystore y passwords sean correctos
- Asegúrate de que `keystore.properties` esté configurado
- Verifica que la ruta del keystore sea correcta

### "Application ID ya está en uso"
- El ID `com.glebursol.registrogastos` ya está tomado
- Necesitarás elegir otro ID único
- ⚠️ Esto requiere cambiar el `applicationId` y crear nueva app en Firebase

### "App rechazada"
- Lee los motivos en Google Play Console
- Corrige los problemas y vuelve a subir
- Revisa las políticas de contenido

---

## 📞 Recursos Útiles

- **Google Play Console**: https://play.google.com/console
- **Documentación oficial**: https://developer.android.com/distribute/googleplay
- **Guía de políticas**: https://play.google.com/about/developer-content-policy/

---

¡Buena suerte con tu publicación! 🚀

