# 🔑 Guía para Obtener serviceAccountKey.json

Esta guía te ayudará a obtener el archivo `serviceAccountKey.json` necesario para actualizar la versión de la app en Firestore automáticamente.

## 📋 Requisitos

- Acceso a Firebase Console
- Permisos de administrador en el proyecto Firebase

## 🚀 Pasos Detallados

### Paso 1: Acceder a Firebase Console

1. Abre tu navegador y ve a: **https://console.firebase.google.com/**
2. Inicia sesión con tu cuenta de Google (la misma que usas para Firebase)

### Paso 2: Seleccionar el Proyecto

1. En la lista de proyectos, busca y selecciona: **`gestor-gastos-app-6e1d9`**
   - Si no ves el proyecto, verifica que estés usando la cuenta correcta

### Paso 3: Ir a Configuración del Proyecto

1. En la parte superior izquierda, haz clic en el **ícono de engranaje (⚙️)** junto al nombre del proyecto
2. Selecciona **"Configuración del proyecto"** del menú desplegable

### Paso 4: Ir a la Pestaña "Cuentas de servicio"

1. En la página de configuración, busca la pestaña **"Cuentas de servicio"**
2. Haz clic en esa pestaña
3. Verás una sección llamada **"SDK de administración de Firebase"**

### Paso 5: Generar Nueva Clave Privada

1. En la sección "SDK de administración de Firebase", verás un botón que dice:
   **"Generar nueva clave privada"** o **"Generate new private key"**
2. Haz clic en ese botón
3. Aparecerá un diálogo de advertencia que dice algo como:
   > "¿Estás seguro de que deseas generar una nueva clave privada?"
   > "Esta acción no se puede deshacer. La clave privada anterior dejará de funcionar."
4. Haz clic en **"Generar clave"** o **"Generate key"**

### Paso 6: Descargar el Archivo

1. El navegador descargará automáticamente un archivo JSON
2. El nombre del archivo será algo como: `gestor-gastos-app-6e1d9-xxxxx.json`
   - Donde `xxxxx` es un código único

### Paso 7: Renombrar y Colocar el Archivo

1. **Renombra el archivo** a: `serviceAccountKey.json`
   - Elimina todo el nombre largo y déjalo solo como `serviceAccountKey.json`
2. **Mueve el archivo** a la raíz de tu proyecto
   - Debe estar en el mismo nivel que `build.gradle.kts`, `settings.gradle.kts`, etc.
   - Ruta completa: `C:\Users\idgle\AndroidStudioProjects\GestorGastos\serviceAccountKey.json`

### Paso 8: Verificar que el Archivo Está Correcto

El archivo debe tener una estructura similar a esta:

```json
{
  "type": "service_account",
  "project_id": "gestor-gastos-app-6e1d9",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n",
  "client_email": "...@gestor-gastos-app-6e1d9.iam.gserviceaccount.com",
  "client_id": "...",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "..."
}
```

## ✅ Verificación

Para verificar que el archivo está correctamente colocado, ejecuta:

```bash
# Windows
dir serviceAccountKey.json

# Linux/Mac
ls -la serviceAccountKey.json
```

Si el archivo existe, verás algo como:
```
serviceAccountKey.json
```

## 🧪 Probar el Script

Una vez que tengas el archivo, puedes probar el script:

```bash
# Windows
node scripts/update-version.js 1 "1.0" "Versión inicial"

# Linux/Mac
node scripts/update-version.js 1 "1.0" "Versión inicial"
```

Si todo está correcto, verás:
```
✅ Versión actualizada exitosamente en Firestore:
   - latestVersionCode: 1
   - latestVersionName: 1.0
   - updateMessage: Versión inicial
   - minVersionCode: 1
```

## ⚠️ Importante

- **NO compartas este archivo** con nadie
- **NO lo subas a Git** (ya está en `.gitignore`)
- **Mantén este archivo seguro** - contiene credenciales de administrador
- Si pierdes o comprometes este archivo, puedes generar uno nuevo desde Firebase Console

## 🔄 Si Necesitas Generar un Nuevo Archivo

Si necesitas generar un nuevo archivo (por ejemplo, si perdiste el anterior):

1. Ve a Firebase Console → Configuración → Cuentas de servicio
2. En la lista de "Cuentas de servicio", verás una cuenta que termina en `@gestor-gastos-app-6e1d9.iam.gserviceaccount.com`
3. Puedes eliminar la clave anterior y generar una nueva
4. O simplemente generar una nueva clave (puedes tener múltiples claves activas)

## 📞 ¿Problemas?

Si tienes problemas:

1. **No encuentras la opción "Cuentas de servicio"**:
   - Asegúrate de tener permisos de administrador en el proyecto
   - Verifica que estés en el proyecto correcto

2. **El archivo no se descarga**:
   - Verifica la configuración de descargas de tu navegador
   - Intenta con otro navegador

3. **El script no funciona**:
   - Verifica que el archivo esté en la raíz del proyecto
   - Verifica que el nombre sea exactamente `serviceAccountKey.json`
   - Verifica que Node.js esté instalado: `node --version`

