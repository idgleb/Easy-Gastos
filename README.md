# 💰 Easy Gastos - Gestor de Gastos Personal

Aplicación Android para gestión de gastos personales con sincronización en tiempo real usando Firebase.

## 📱 Características Principales

### 🔐 Autenticación
- Inicio de sesión con email y contraseña
- Google Sign-In integrado
- Selección de cuenta Google en cada inicio de sesión
- Generación automática de nombre de usuario desde email
- Gestión segura de sesiones

### 💳 Gestión de Gastos
- Crear, editar y eliminar gastos
- Categorización personalizada de gastos
- Filtrado por mes y categoría
- Dashboard con resumen visual y estadísticas
- Modo offline completo con sincronización automática

### 🏷️ Categorías Personalizadas
- Crear categorías con iconos personalizados
- Activar/desactivar categorías
- Gestión completa CRUD
- Sincronización en tiempo real

### 👥 Panel de Administración
- Gestión completa de usuarios (solo para administradores)
- CRUD de usuarios con roles (admin/user)
- Asignación de planes (free/premium)
- Visualización de estado de sincronización
- Creación de usuarios con Cloud Functions

### 🔄 Sincronización Inteligente
- **Offline-First**: Todas las operaciones funcionan sin conexión
- **Sincronización Automática**: Se dispara al recuperar conexión
- **Tiempo Real**: Cambios se reflejan instantáneamente
- **Bidireccional**: Sincroniza desde y hacia Firestore
- **Indicadores Visuales**: Iconos de estado de sincronización

### 🌐 Manejo de Conexión
- Detección automática de estado de red
- Banner no intrusivo para errores de conexión
- Sincronización automática al recuperar conexión
- Modo offline completo con Room Database

### 🔃 Pull-to-Refresh
- Actualización manual en todas las pantallas
- Sincronización de datos desde Firestore
- Feedback visual con indicador de carga

## 🏗️ Arquitectura

### Patrón MVVM (Model-View-ViewModel)
```
UI (Activity/Fragment)
    ↓
ViewModel (LiveData)
    ↓
Repository (Lógica de negocio)
    ↓
DataSources (Room + Firestore)
```

### Componentes Principales

#### **Room Database (SQLite)**
- Base de datos local para modo offline
- Sincronización automática con Firestore
- LiveData para actualizaciones reactivas
- Migraciones automáticas de esquema

#### **Firebase Firestore**
- Base de datos en la nube
- Listeners en tiempo real para el usuario actual
- Sincronización bidireccional
- Reglas de seguridad robustas

#### **Firebase Authentication**
- Autenticación con email/password
- Google Sign-In
- Gestión de sesiones

#### **Cloud Functions**
- Creación de usuarios por administradores
- Eliminación de usuarios con limpieza de datos
- Triggers automáticos para nuevos usuarios

#### **WorkManager**
- Sincronización en background
- Ejecución garantizada de tareas
- Respeta batería y recursos del sistema

## 🚀 Características Técnicas Avanzadas

### Offline-First Pattern
```java
// 1. Guardar en Room inmediatamente
userDao.updateUserFields(...);
callback.onSuccess(user); // UI se actualiza

// 2. Sincronizar con Firestore en background
syncUserWithFirestore(user);

// 3. Listener detecta confirmación
firestoreListener → actualiza estado de sincronización
```

### Firestore Listeners (Tiempo Real)
```java
// Escucha cambios automáticamente
userListener = firestore.collection("users")
    .document(uid)
    .addSnapshotListener((snapshot, error) -> {
        // Se ejecuta cuando:
        // - Primera vez (datos actuales)
        // - Cualquier cambio en el documento
        // - Cambios desde cualquier dispositivo
        
        // Actualiza Room y notifica a la UI
        userLiveData.postValue(updatedUser);
    });
```

### Sincronización Automática al Recuperar Conexión
```java
NetworkMonitor detecta conexión
    ↓
Dispara SyncWorker automáticamente
    ↓
Sincroniza todos los datos pendientes:
    - Usuarios (crear/actualizar/eliminar)
    - Categorías (crear/actualizar/eliminar)
    - Gastos (crear/actualizar/eliminar)
```

### Gestión de Usuarios Huérfanos
```java
// Durante refresh, limpia usuarios que:
// - Existen en Room pero no en Firestore
// - No son temporales (temp_*)
// - No están marcados como eliminados
// - No tienen cambios pendientes
```

### Deduplicación Offline
```java
// Evita duplicados al sincronizar
// 1. Buscar por remoteId
// 2. Si no existe, buscar por atributos (email, nombre, etc.)
// 3. Actualizar en lugar de insertar
```

## 📦 Dependencias Principales

```gradle
// Firebase
implementation 'com.google.firebase:firebase-auth:22.3.1'
implementation 'com.google.firebase:firebase-firestore:24.10.1'

// Room Database
implementation 'androidx.room:room-runtime:2.6.1'
kapt 'androidx.room:room-compiler:2.6.1'

// Architecture Components
implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'

// WorkManager
implementation 'androidx.work:work-runtime:2.9.0'

// Material Design
implementation 'com.google.android.material:material:1.11.0'

// SwipeRefreshLayout
implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'

// Google Sign-In
implementation 'com.google.android.gms:play-services-auth:20.7.0'
```

## 🔧 Configuración del Proyecto

### 1. Clonar el Repositorio
```bash
git clone https://github.com/idgleb/Easy-Gastos.git
cd Easy-Gastos
```

### 2. Configurar Firebase

#### a) Crear Proyecto en Firebase Console
1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Crea un nuevo proyecto
3. Agrega una aplicación Android

#### b) Descargar google-services.json
1. En Firebase Console → Configuración del proyecto
2. Descarga `google-services.json`
3. Colócalo en `app/google-services.json`

**⚠️ IMPORTANTE**: Este archivo contiene claves sensibles y está en `.gitignore`

Ver [README_GOOGLE_SERVICES.md](README_GOOGLE_SERVICES.md) para más detalles.

#### c) Configurar Authentication
1. Firebase Console → Authentication
2. Habilitar "Correo electrónico/contraseña"
3. Habilitar "Google"

#### d) Configurar Firestore
1. Firebase Console → Firestore Database
2. Crear base de datos en modo producción
3. Desplegar reglas de seguridad:
```bash
firebase deploy --only firestore:rules
```

#### e) Configurar Cloud Functions
```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

### 3. Configurar Google Sign-In

#### a) Obtener SHA-1 y SHA-256
```bash
cd android
gradlew signingReport
```

#### b) Agregar Huellas en Firebase
1. Firebase Console → Configuración del proyecto
2. Agrega SHA-1 y SHA-256

#### c) Configurar OAuth en Google Cloud
1. [Google Cloud Console](https://console.cloud.google.com/)
2. APIs y servicios → Credenciales
3. Configura pantalla de consentimiento OAuth
4. Crea credenciales OAuth 2.0

### 4. Compilar y Ejecutar
```bash
# Limpiar y compilar
gradlew clean assembleDebug

# Instalar en dispositivo
gradlew installDebug
```

## 📁 Estructura del Proyecto

```
app/
├── src/main/
│   ├── java/com/example/gestorgastos/
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── dao/           # Room DAOs
│   │   │   │   ├── entity/        # Entidades de Room
│   │   │   │   └── AppDatabase.java
│   │   │   ├── remote/
│   │   │   │   └── FirestoreDataSource.java
│   │   │   └── repository/        # Repositorios (lógica de negocio)
│   │   ├── domain/
│   │   │   └── repository/        # Interfaces de repositorios
│   │   ├── ui/
│   │   │   ├── admin/             # Pantalla de administración
│   │   │   ├── auth/              # Autenticación
│   │   │   ├── categories/        # Gestión de categorías
│   │   │   ├── dashboard/         # Dashboard principal
│   │   │   ├── dialogs/           # Diálogos y bottom sheets
│   │   │   ├── expenses/          # Gestión de gastos
│   │   │   └── main/              # Activity principal
│   │   ├── util/                  # Utilidades
│   │   │   ├── ConnectionErrorNotifier.java
│   │   │   ├── NetworkMonitor.java
│   │   │   └── SyncPrefs.java
│   │   └── work/
│   │       └── SyncWorker.java    # Worker de sincronización
│   └── res/
│       ├── layout/                # Layouts XML
│       ├── values/
│       │   └── strings.xml        # Todos los textos centralizados
│       └── drawable/              # Recursos gráficos
│
├── firestore.rules                # Reglas de seguridad de Firestore
├── functions/                     # Cloud Functions
│   ├── index.js
│   └── package.json
└── build.gradle.kts
```

## 🔐 Seguridad

### Firestore Security Rules
```javascript
// Solo admins pueden crear/eliminar usuarios
match /users/{userId} {
  allow read: if isAuthenticated();
  allow update: if isUser(userId) || isAdmin();
  allow create, delete: if isAdmin();
}

// Usuarios solo pueden ver/modificar sus propios datos
match /users/{userId}/categories/{categoryId} {
  allow read, write: if isUser(userId);
}

match /users/{userId}/expenses/{expenseId} {
  allow read, write: if isUser(userId);
}
```

### Cloud Functions con Admin SDK
```javascript
// Verificación de token y rol de admin
const idToken = request.headers.authorization?.split('Bearer ')[1];
const decodedToken = await admin.auth().verifyIdToken(idToken);
const adminUser = await admin.firestore()
  .collection('users')
  .doc(decodedToken.uid)
  .get();

if (adminUser.data()?.role !== 'admin') {
  throw new functions.https.HttpsError('permission-denied', 'No autorizado');
}
```

## 🎨 Características de UX

### Indicadores de Estado
- ✅ **Sincronizado**: Sin icono, datos actualizados
- ⟳ **Pendiente**: Icono de sincronización, esperando conexión
- 📡 **Sin conexión**: Banner informativo no intrusivo

### Feedback Visual
- Spinners en botones durante operaciones
- Pull-to-refresh en todas las listas
- Animaciones suaves de transición
- Diálogos informativos para errores

### Modo Offline
- Todas las operaciones funcionan sin conexión
- Datos guardados localmente en Room
- Sincronización automática al recuperar conexión
- Indicadores claros de estado de sincronización

## 📊 Flujos Principales

### Crear Usuario (Admin)
```
1. Admin abre pantalla de Administración
2. Presiona FAB → Diálogo de creación
3. Ingresa datos (email, nombre, rol, plan)
4. Guarda → Room (inmediato) + Cloud Function (background)
5. Cloud Function crea usuario en Firebase Auth
6. Cloud Function crea documento en Firestore
7. Listener detecta nuevo usuario
8. UI se actualiza con usuario sincronizado
```

### Agregar Gasto (Offline)
```
1. Usuario sin conexión abre app
2. Selecciona categoría y monto
3. Guarda → Room (syncState = "PENDING")
4. UI muestra gasto inmediatamente
5. Usuario recupera conexión
6. NetworkMonitor detecta conexión
7. SyncWorker se dispara automáticamente
8. Gasto se sincroniza con Firestore
9. syncState cambia a "SYNCED"
```

### Cambiar Plan de Usuario
```
1. Admin edita usuario → Cambia plan
2. Room se actualiza (offline-first)
3. Firestore se actualiza (background)
4. Listener de Firestore detecta cambio
5. LiveData notifica a todos los observadores
6. AccountBottomSheet se actualiza automáticamente
7. Usuario ve nuevo plan sin reiniciar
```

## 🐛 Solución de Problemas

### Google Sign-In no funciona
1. Verifica que `google-services.json` esté actualizado
2. Confirma que SHA-1 y SHA-256 estén registrados en Firebase
3. Verifica OAuth en Google Cloud Console
4. Limpia y reconstruye el proyecto

### Datos no se sincronizan
1. Verifica conexión a internet
2. Revisa logs de `SyncWorker`
3. Confirma reglas de Firestore
4. Verifica que Cloud Functions estén desplegadas

### Error PERMISSION_DENIED
1. Verifica que el usuario esté autenticado
2. Confirma que las reglas de Firestore sean correctas
3. Verifica el rol del usuario (admin/user)
4. Redeploy de reglas: `firebase deploy --only firestore:rules`

## 📝 Logs Importantes

### Sincronización
```
AdminRepositoryImpl: 🔄 Sincronizando usuario con Firestore - UID: ..., Plan: ...
AdminRepositoryImpl: 📤 Datos a enviar a Firestore: plan_id=...
AdminRepositoryImpl: ✅ Usuario sincronizado con Firestore
```

### Conexión de Red
```
NetworkMonitor: ✅ Red con internet validado
NetworkMonitor: 🔄 Conexión recuperada - disparando sincronización automática
SyncWorker: Iniciando sincronización en segundo plano
```

### Firestore Listeners
```
AuthRepositoryImpl: 📥 Snapshot recibido de Firestore (desde: SERVER)
AuthRepositoryImpl: 📋 Datos de Firestore - planId: premium
AuthRepositoryImpl: 📤 Posteando usuario al LiveData
AccountBottomSheet: 👤 Usuario recibido - Plan: premium
AccountBottomSheet: ✅ Plan actualizado en UI
```

## 🚧 Roadmap

### Próximas Características
- [ ] Listeners en tiempo real para categorías
- [ ] Listeners en tiempo real para gastos (con paginación)
- [ ] Exportación de datos a CSV/PDF
- [ ] Gráficos y estadísticas avanzadas
- [ ] Notificaciones push para recordatorios
- [ ] Presupuestos mensuales por categoría
- [ ] Modo oscuro
- [ ] Soporte multi-idioma completo

## 👥 Contribuir

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto es de código abierto y está disponible bajo la licencia MIT.

## 👨‍💻 Autor

**Gleb Ursol**
- GitHub: [@idgleb](https://github.com/idgleb)
- Proyecto: [Easy-Gastos](https://github.com/idgleb/Easy-Gastos)

## 🙏 Agradecimientos

- Firebase por la infraestructura backend
- Material Design por los componentes UI
- Android Jetpack por las herramientas de arquitectura
- Comunidad de Stack Overflow por el soporte

---

**Última actualización**: Noviembre 2025

**Versión**: 1.0.0

**Estado**: ✅ Producción
