# 🚀 Pasos para Agregar Nueva App en Firebase

## 📋 Información que Necesitas

- **Package Name**: `com.glebursol.registrogastos`
- **SHA-1**: `40:e2:c5:9e:c7:10:33:11:0f:9b:e9:b6:a1:e6:0f:79:07:cd:37:6f`
- **SHA-1 (sin dos puntos)**: `40e2c59ec71033110f9be9b6a1e60f7907cd376f`
- **SHA-256**: `b7:5b:02:ec:c2:d9:40:5b:05:c4:4e:28:d1:83:0f:db:25:6c:07:a4:9d:0c:9f:89:39:0d:65:be:da:a6:e0:bd`

## 📝 Paso 1: Ir a Firebase Console

1. Abre tu navegador y ve a: **https://console.firebase.google.com/**
2. Inicia sesión con tu cuenta de Google
3. Selecciona el proyecto: **gestor-gastos-app-6e1d9**

## 📝 Paso 2: Ir a Configuración del Proyecto

1. En la parte superior izquierda, haz clic en el **ícono de engranaje (⚙️)** junto al nombre del proyecto
2. Selecciona **"Configuración del proyecto"** del menú desplegable

## 📝 Paso 3: Agregar Nueva App Android

1. En la página de configuración, desplázate hasta la sección **"Tus aplicaciones"**
2. Busca la sección **"Apps para Android"**
3. Haz clic en el botón **"Agregar app"** (botón azul en la parte superior derecha)

## 📝 Paso 4: Completar el Formulario

1. Se abrirá un formulario para agregar una nueva app Android
2. Completa los campos:
   - **Nombre del paquete de Android**: 
     ```
     com.glebursol.registrogastos
     ```
   - **Sobrenombre de la app** (opcional, pero recomendado):
     ```
     Gestor Gastos
     ```
     o
     ```
     Registro de Gastos Offline
     ```
   - **Certificado de firma de depuración SHA-1** (opcional, puedes agregarlo después):
     ```
     40:e2:c5:9e:c7:10:33:11:0f:9b:e9:b6:a1:e6:0f:79:07:cd:37:6f
     ```
     O sin dos puntos:
     ```
     40e2c59ec71033110f9be9b6a1e60f7907cd376f
     ```

3. Haz clic en **"Registrar app"**

## 📝 Paso 5: Agregar SHA-1 (si no lo agregaste antes)

1. Una vez creada la app, verás su página de configuración
2. Desplázate hasta la sección **"Huellas digitales del certificado SHA"**
3. Haz clic en el botón **"Agregar huella digital"** (texto azul)
4. Pega el SHA-1:
   ```
   40:e2:c5:9e:c7:10:33:11:0f:9b:e9:b6:a1:e6:0f:79:07:cd:37:6f
   ```
   O sin dos puntos:
   ```
   40e2c59ec71033110f9be9b6a1e60f7907cd376f
   ```
5. Haz clic en **"Guardar"** o presiona Enter

## 📝 Paso 6: Agregar SHA-256 (Opcional pero Recomendado)

1. En la misma sección, haz clic nuevamente en **"Agregar huella digital"**
2. Pega el SHA-256:
   ```
   b7:5b:02:ec:c2:d9:40:5b:05:c4:4e:28:d1:83:0f:db:25:6c:07:a4:9d:0c:9f:89:39:0d:65:be:da:a6:e0:bd
   ```
   O sin dos puntos:
   ```
   b75b02ecc2d9405b05c44e28d1830fdb256c07a49d0c9f89390d65beda6e0bd
   ```
3. Haz clic en **"Guardar"**

## 📝 Paso 7: Descargar google-services.json

1. En la misma página de configuración de la app
2. Desplázate hasta la sección **"Configuración del SDK"**
3. Haz clic en el botón **"google-services.json"** (botón con icono de descarga)
4. El archivo se descargará automáticamente

## 📝 Paso 8: Reemplazar el Archivo en el Proyecto

1. Abre la carpeta del proyecto: `C:\Users\idgle\AndroidStudioProjects\GestorGastos\app\`
2. **Copia** el archivo `google-services.json` descargado
3. **Pega** y **reemplaza** el archivo existente en: `app/google-services.json`
4. Asegúrate de que el archivo se haya reemplazado correctamente

## 📝 Paso 9: Verificar el Archivo

Abre el archivo `app/google-services.json` y verifica que contenga:

```json
{
  "client": [
    {
      "client_info": {
        "android_client_info": {
          "package_name": "com.glebursol.registrogastos"
        }
      },
      "oauth_client": [
        {
          "android_info": {
            "package_name": "com.glebursol.registrogastos",
            "certificate_hash": "40e2c59ec71033110f9be9b6a1e60f7907cd376f"
          }
        }
      ]
    }
  ]
}
```

## 📝 Paso 10: Sincronizar en Android Studio

1. Abre Android Studio
2. Haz clic en **"Sync Project with Gradle Files"** (icono de elefante en la barra superior)
   - O ve a: **File → Sync Project with Gradle Files**
3. Espera a que termine la sincronización

## 📝 Paso 11: Limpiar y Recompilar

En la terminal de Android Studio o PowerShell, ejecuta:

```bash
.\gradlew clean
.\gradlew assembleDebug
```

## 📝 Paso 12: Probar Google Sign-In

1. Ejecuta la app en modo debug
2. Intenta iniciar sesión con Google
3. El error debería estar resuelto ✅

## ✅ Verificación Final

Después de seguir todos los pasos, verifica que:

- ✅ Existe una app en Firebase con package: `com.glebursol.registrogastos`
- ✅ El SHA-1 está registrado en esa app
- ✅ El `google-services.json` tiene el package name correcto
- ✅ El `applicationId` en `build.gradle.kts` es `com.glebursol.registrogastos`
- ✅ La app compila sin errores
- ✅ Google Sign-In funciona correctamente

## 🐛 Si Algo Sale Mal

### Error: "Package name already exists"
- Esto significa que ya existe una app con ese package name
- Ve a la lista de apps y busca `com.glebursol.registrogastos`
- Si existe, descarga su `google-services.json` y úsalo

### Error: "Invalid SHA-1"
- Asegúrate de copiar el SHA-1 completo
- Puedes usar con o sin dos puntos, ambos funcionan
- Verifica que no haya espacios extra

### Error después de reemplazar google-services.json
- Asegúrate de haber reemplazado el archivo correcto
- Sincroniza el proyecto nuevamente
- Limpia y recompila: `.\gradlew clean assembleDebug`

## 📞 ¿Necesitas Ayuda?

Si encuentras algún problema, verifica:
1. Que el package name sea exacto: `com.glebursol.registrogastos`
2. Que el SHA-1 sea correcto
3. Que el `google-services.json` esté en la ubicación correcta: `app/google-services.json`
4. Que hayas sincronizado el proyecto en Android Studio

