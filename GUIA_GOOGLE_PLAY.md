# 📱 Guía Completa: Publicar App en Google Play Store

Esta guía te ayudará a publicar tu app "Registro de Gastos Offline" en Google Play Store paso a paso.

## 📋 Requisitos Previos

1. **Cuenta de Google Play Console** ($25 USD - pago único)
   - Ve a: https://play.google.com/console/signup
   - Crea una cuenta de desarrollador
   - Paga la tarifa única de $25 USD

2. **App preparada para producción**
   - Icono de la app
   - Capturas de pantalla
   - Descripción de la app
   - Política de privacidad (URL)

---

## 🔧 Paso 1: Preparar la App para Producción

### 1.1 Cambiar el Application ID (IMPORTANTE)

⚠️ **CRÍTICO**: El `applicationId` actual es `com.example.gestorgastos` que es solo para desarrollo.

**Debes cambiarlo a un ID único antes de publicar:**

1. Abre `app/build.gradle.kts`
2. Cambia:
   ```kotlin
   applicationId = "com.example.gestorgastos"
   ```
   Por algo como:
   ```kotlin
   applicationId = "com.glebursol.gestorgastos"
   ```
   O:
   ```kotlin
   applicationId = "com.easygastos.app"
   ```

**⚠️ ADVERTENCIA**: Una vez que publiques con un `applicationId`, NO puedes cambiarlo. Elige bien.

### 1.2 Verificar Versión

En `app/build.gradle.kts`:
```kotlin
versionCode = 1        // Debe ser 1 para la primera publicación
versionName = "1.0"    // Versión visible para usuarios
```

### 1.3 Generar Keystore (Firma de la App)

Google Play requiere que la app esté firmada. Necesitas crear un keystore:

**Opción A: Desde Android Studio (Recomendado)**

1. Build → Generate Signed Bundle / APK
2. Selecciona "Android App Bundle"
3. Clic en "Create new..." para crear un nuevo keystore
4. Completa el formulario:
   - **Key store path**: Elige una ubicación segura (ej: `C:\Users\idgle\keystore\gestorgastos.jks`)
   - **Password**: Crea una contraseña fuerte (GUÁRDALA BIEN)
   - **Key alias**: `gestorgastos-key`
   - **Key password**: Otra contraseña (puede ser la misma)
   - **Validity**: 25 años (máximo)
   - **First and Last Name**: Tu nombre
   - **Organizational Unit**: (opcional)
   - **Organization**: (opcional)
   - **City**: Tu ciudad
   - **State**: Tu estado/provincia
   - **Country Code**: AR (o tu país)
5. Guarda el keystore en un lugar SEGURO
6. **IMPORTANTE**: Guarda las contraseñas en un lugar seguro (si las pierdes, no podrás actualizar la app)

**Opción B: Desde línea de comandos**

```bash
keytool -genkey -v -keystore gestorgastos.jks -alias gestorgastos-key -keyalg RSA -keysize 2048 -validity 10000
```

### 1.4 Configurar Firma en build.gradle.kts

Crea o edita el archivo `keystore.properties` en la raíz del proyecto:

```properties
storePassword=TU_PASSWORD_DEL_KEYSTORE
keyPassword=TU_PASSWORD_DEL_KEY
keyAlias=gestorgastos-key
storeFile=C:\\Users\\idgle\\keystore\\gestorgastos.jks
```

**⚠️ IMPORTANTE**: Agrega `keystore.properties` a `.gitignore` (no subirlo a Git)

Luego, agrega esto en `app/build.gradle.kts`:

```kotlin
// Agregar al inicio del archivo
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = java.util.Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
}

android {
    // ... código existente ...
    
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
}
```

---

## 📦 Paso 2: Generar Android App Bundle (AAB)

Google Play requiere un **AAB** (Android App Bundle), no un APK.

### Desde Android Studio:

1. **Build → Generate Signed Bundle / APK**
2. Selecciona **"Android App Bundle"**
3. Selecciona tu keystore y completa las contraseñas
4. Selecciona **"release"** como build variant
5. Clic en **"Finish"**
6. El AAB se generará en: `app/build/outputs/bundle/release/app-release.aab`

### Desde línea de comandos:

```bash
./gradlew bundleRelease
```

El AAB estará en: `app/build/outputs/bundle/release/app-release.aab`

---

## 🎨 Paso 3: Preparar Materiales de la App

### 3.1 Icono de la App

- **Tamaño requerido**: 512x512 píxeles (PNG, sin transparencia)
- Tu app ya tiene iconos, pero necesitas crear uno de 512x512 para Play Store

### 3.2 Capturas de Pantalla

Necesitas capturas de pantalla en diferentes tamaños:

**Mínimo requerido:**
- **Teléfono**: Al menos 2 capturas (mínimo 320px de altura)
- **Tablet (7")**: Al menos 1 captura (mínimo 320px de altura)
- **Tablet (10")**: Al menos 1 captura (mínimo 320px de altura)

**Tamaños recomendados:**
- Teléfono: 1080x1920 px (vertical) o 1920x1080 px (horizontal)
- Tablet 7": 1200x1920 px
- Tablet 10": 1600x2560 px

**Cómo tomarlas:**
1. Ejecuta la app en un emulador o dispositivo
2. Navega por las pantallas principales:
   - Pantalla de inicio (Auth)
   - Dashboard
   - Lista de gastos
   - Lista de categorías
   - Pantalla de configuración
3. Toma capturas de pantalla (Power + Volumen Abajo en Android)
4. Edítalas si es necesario (recortar, agregar texto, etc.)

### 3.3 Descripción de la App

Prepara textos en español:

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

### 3.4 Política de Privacidad

Necesitas una URL pública con tu política de privacidad. Opciones:

1. **Crear página en GitHub Pages** (gratis)
2. **Crear página en tu sitio web** (si tienes uno)
3. **Usar un servicio como Privacy Policy Generator**

**Ejemplo de URL:**
```
https://github.com/idgleb/Easy-Gastos/blob/main/PRIVACY.md
```

O crea un archivo `PRIVACY.md` en tu repositorio de GitHub.

---

## 🚀 Paso 4: Crear App en Google Play Console

### 4.1 Acceder a Google Play Console

1. Ve a: https://play.google.com/console
2. Inicia sesión con tu cuenta de Google
3. Si es tu primera vez, paga la tarifa de $25 USD

### 4.2 Crear Nueva App

1. Clic en **"Crear app"** o **"Create app"**
2. Completa el formulario:
   - **Nombre de la app**: "Registro de Gastos Offline"
   - **Idioma predeterminado**: Español (España) o Español (Latinoamérica)
   - **Tipo de app**: App
   - **Gratis o de pago**: Gratis
   - **Declaraciones**: Marca las casillas requeridas
     - ✅ Contenido de la app
     - ✅ Política de privacidad
     - ✅ Normas de la tienda
3. Clic en **"Crear app"**

### 4.3 Configurar Store Listing

1. Ve a **"Store listing"** en el menú lateral
2. Completa todos los campos:

   **App name**: Registro de Gastos Offline
   
   **Short description**: Gestiona tus gastos fácilmente, incluso sin internet
   
   **Full description**: (Pega la descripción completa que preparaste)
   
   **App icon**: Sube el icono de 512x512 px
   
   **Feature graphic**: 1024x500 px (imagen promocional)
   
   **Screenshots**: Sube las capturas de pantalla
   
   **Categoría**: Finanzas
   
   **Contact details**:
   - Email: tu-email@ejemplo.com
   - Teléfono: (opcional)
   - Sitio web: https://github.com/idgleb/Easy-Gastos
   
   **Privacy Policy**: URL de tu política de privacidad

3. Guarda los cambios

### 4.4 Configurar Contenido de la App

1. Ve a **"Content rating"**
2. Completa el cuestionario sobre el contenido de tu app
3. Generalmente para una app de finanzas será: **"Everyone"** o **"Para todos"**

### 4.5 Configurar Precios y Distribución

1. Ve a **"Pricing & distribution"**
2. Selecciona **"Free"** (Gratis)
3. Selecciona los países donde quieres distribuir (o "Todos los países")
4. Marca las declaraciones requeridas
5. Guarda

---

## 📤 Paso 5: Subir el AAB

### 5.1 Ir a Producción

1. En el menú lateral, ve a **"Production"** (o **"Producción"**)
2. Clic en **"Create new release"** (o **"Crear nueva versión"**)

### 5.2 Subir el AAB

1. En la sección **"App bundles"**, clic en **"Upload"**
2. Selecciona tu archivo `app-release.aab`
3. Espera a que se procese (puede tardar unos minutos)

### 5.3 Completar Información de la Versión

1. **Release name**: "1.0 - Versión inicial"
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
- Si hay problemas, Google te notificará

---

## ✅ Paso 7: Después de la Aprobación

Una vez aprobada:

1. Tu app estará disponible en Google Play Store
2. Los usuarios podrán descargarla
3. Recibirás estadísticas de descargas, calificaciones, etc.

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
   ./gradlew bundleRelease
   ```

3. **Actualiza Firestore** (usando el script):
   ```bash
   node scripts/update-version.js 2 "1.1" "Nueva versión con mejoras"
   ```

4. **Sube el nuevo AAB** en Google Play Console → Production → Create new release

5. **Agrega notas de la versión** y publica

---

## ⚠️ Checklist Antes de Publicar

- [ ] Application ID cambiado (no usar `com.example.*`)
- [ ] Keystore creado y guardado de forma segura
- [ ] Passwords del keystore guardadas de forma segura
- [ ] AAB generado y probado
- [ ] Icono de 512x512 px preparado
- [ ] Capturas de pantalla preparadas
- [ ] Descripción de la app escrita
- [ ] Política de privacidad publicada (URL)
- [ ] Cuenta de Google Play Console creada y pagada
- [ ] Store listing completado
- [ ] Content rating completado
- [ ] Pricing & distribution configurado

---

## 📞 Recursos Útiles

- **Google Play Console**: https://play.google.com/console
- **Documentación oficial**: https://developer.android.com/distribute/googleplay
- **Guía de políticas**: https://play.google.com/about/developer-content-policy/

---

## 🆘 Problemas Comunes

### "Application ID ya está en uso"
- El ID que elegiste ya está tomado
- Elige otro ID único

### "Error al firmar"
- Verifica que el keystore y passwords sean correctos
- Asegúrate de que `keystore.properties` esté configurado

### "App rechazada"
- Lee los motivos en Google Play Console
- Corrige los problemas y vuelve a subir

---

¡Buena suerte con tu publicación! 🚀

