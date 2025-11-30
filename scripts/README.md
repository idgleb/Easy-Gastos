# Scripts de Utilidad

## update-version.js

Script para actualizar automáticamente la versión de la app en Firestore.

### Requisitos previos

1. **Instalar Node.js** (si no lo tienes):
   - Descarga desde: https://nodejs.org/
   - Instala la versión LTS

2. **Obtener serviceAccountKey.json**:
   - Ve a Firebase Console → Configuración del proyecto → Cuentas de servicio
   - Clic en "Generar nueva clave privada"
   - Descarga el archivo JSON
   - Renómbralo a `serviceAccountKey.json`
   - Colócalo en la raíz del proyecto (mismo nivel que `package.json`)

3. **Instalar dependencias**:
   ```bash
   npm install firebase-admin
   ```

### Uso

#### Windows:
```bash
scripts\update-version.bat 2 "1.1" "Nueva versión con mejoras"
```

#### Linux/Mac:
```bash
chmod +x scripts/update-version.sh
./scripts/update-version.sh 2 "1.1" "Nueva versión con mejoras"
```

#### Directo con Node.js:
```bash
node scripts/update-version.js 2 "1.1" "Nueva versión con mejoras"
```

### Parámetros

- `versionCode`: Número de versión (debe coincidir con `versionCode` en `build.gradle.kts`)
- `versionName`: Nombre de versión visible (debe coincidir con `versionName` en `build.gradle.kts`)
- `message`: (Opcional) Mensaje personalizado para mostrar en el diálogo

### Ejemplos

```bash
# Actualizar a versión 1.1
node scripts/update-version.js 2 "1.1"

# Actualizar a versión 1.1 con mensaje personalizado
node scripts/update-version.js 2 "1.1" "Nueva versión con mejoras en colores y sistema de actualizaciones"

# Actualizar a versión 2.0
node scripts/update-version.js 3 "2.0" "Gran actualización con nuevas funcionalidades"
```

### Flujo de trabajo recomendado

1. **Actualizar `build.gradle.kts`**:
   ```kotlin
   versionCode = 2
   versionName = "1.1"
   ```

2. **Compilar y publicar en Google Play**

3. **Ejecutar el script**:
   ```bash
   node scripts/update-version.js 2 "1.1" "Nueva versión disponible"
   ```

4. **Verificar en Firebase Console** que el documento se actualizó correctamente

### Seguridad

⚠️ **IMPORTANTE**: 
- El archivo `serviceAccountKey.json` contiene credenciales de administrador
- **NO** lo subas a Git (debe estar en `.gitignore`)
- Mantén este archivo seguro y privado

