# Gestor de Gastos - Android App

Una aplicación Android completa para gestionar gastos personales con sincronización offline-first usando Firebase y Room.

## Características

- **Autenticación**: Firebase Auth con email/contraseña
- **Offline-first**: Funciona sin conexión, sincroniza cuando hay internet
- **Base de datos local**: Room para almacenamiento local
- **Sincronización**: WorkManager para sincronización en segundo plano
- **Dashboard**: Gráficos de torta con MPAndroidChart
- **Categorías**: CRUD de categorías (solo plan Pro)
- **Material 3**: UI moderna y accesible

## Configuración del Proyecto

### 1. Configurar Firebase

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Crea un nuevo proyecto
3. Agrega una aplicación Android:
   - Package name: `com.example.gestorgastos`
   - Descarga el archivo `google-services.json`
4. Coloca `google-services.json` en la carpeta `app/`
5. Habilita Authentication:
   - Ve a Authentication > Sign-in method
   - Habilita Email/Password
6. Crea la base de datos Firestore:
   - Ve a Firestore Database
   - Crea base de datos en modo de prueba
   - Copia las reglas de `firestore.rules`

### 2. Configurar SHA-1

Para que Firebase Auth funcione correctamente:

```bash
# Para debug
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Para release (si tienes keystore)
keytool -list -v -keystore tu-keystore.jks -alias tu-alias
```

Agrega el SHA-1 a tu proyecto de Firebase en Project Settings > General > Your apps.

### 3. Configurar Planes

En Firestore, crea la colección `plans` con los siguientes documentos:

**Plan Free:**
```json
{
  "name": "Free",
  "price": 0,
  "description": "Plan gratuito con funcionalidades básicas",
  "features": ["Gastos ilimitados", "Categorías predefinidas"],
  "is_active": true,
  "updated_at": "timestamp"
}
```

**Plan Pro:**
```json
{
  "name": "Pro",
  "price": 9.99,
  "description": "Plan premium con todas las funcionalidades",
  "features": ["Gastos ilimitados", "Categorías personalizadas", "Exportar datos"],
  "is_active": true,
  "updated_at": "timestamp"
}
```

## Ejecutar la Aplicación

1. Abre el proyecto en Android Studio
2. Sincroniza el proyecto con Gradle
3. Conecta un dispositivo o emulador
4. Ejecuta la aplicación

## Credenciales de Prueba

Puedes crear una cuenta nueva desde la aplicación o usar estas credenciales de prueba:

- **Email**: `test@example.com`
- **Contraseña**: `123456`

## Cambiar Plan a Pro

Para cambiar un usuario a plan Pro:

1. Ve a Firestore Console
2. Navega a `users/{uid}`
3. Cambia el campo `plan_id` de `"free"` a `"pro"`
4. Actualiza `updated_at` con el timestamp actual

## Estructura del Proyecto

```
app/src/main/java/com/example/gestorgastos/
├── data/
│   ├── local/           # Room database, entities, DAOs
│   ├── remote/          # Firebase data sources
│   └── repository/      # Repository implementations
├── domain/
│   ├── model/           # POJOs
│   └── repository/      # Repository interfaces
├── ui/
│   ├── auth/            # Authentication screens
│   ├── home/            # Main expense list
│   ├── dashboard/       # Charts and analytics
│   └── categories/      # Category management
├── work/                # WorkManager sync worker
└── util/                # Utility classes
```

## Tecnologías Utilizadas

- **Java 17**: Lenguaje de programación
- **AndroidX**: Componentes modernos de Android
- **Material 3**: Diseño de UI
- **Room**: Base de datos local
- **Firebase**: Auth y Firestore
- **WorkManager**: Sincronización en segundo plano
- **MPAndroidChart**: Gráficos
- **MVVM**: Arquitectura de la aplicación

## Funcionalidades Implementadas

### ✅ Completadas
- [x] Autenticación con Firebase
- [x] Base de datos Room con entidades
- [x] Navegación entre pantallas
- [x] UI básica con Material 3
- [x] WorkManager para sincronización
- [x] Reglas de seguridad Firestore

### 🔄 En Progreso
- [ ] Implementación completa de repositorios
- [ ] ViewModels y LiveData
- [ ] Adapters para RecyclerView
- [ ] BottomSheets para entrada de datos
- [ ] Gráficos con MPAndroidChart
- [ ] Sincronización completa

### 📋 Pendientes
- [ ] Tests unitarios
- [ ] Tests de integración
- [ ] Manejo de errores robusto
- [ ] Optimizaciones de rendimiento
- [ ] Accesibilidad completa

## Troubleshooting

### Error de compilación con MPAndroidChart
Si tienes problemas con MPAndroidChart, asegúrate de que el repositorio JitPack esté configurado en `build.gradle.kts` del proyecto raíz.

### Error de Firebase
Verifica que:
1. El archivo `google-services.json` esté en la carpeta correcta
2. El SHA-1 esté configurado en Firebase Console
3. Las reglas de Firestore estén aplicadas

### Error de Room
Si hay errores de compilación de Room:
1. Limpia y reconstruye el proyecto
2. Verifica que las anotaciones estén correctas
3. Asegúrate de que el procesador de anotaciones esté configurado

## Contribuir

1. Fork el proyecto
2. Crea una rama para tu feature
3. Commit tus cambios
4. Push a la rama
5. Abre un Pull Request

## Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.


