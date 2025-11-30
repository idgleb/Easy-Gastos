# 🔍 Verificar Configuración en Firebase

## Situación Actual

- **applicationId en build.gradle.kts**: `com.glebursol.registrogastos`
- **google-services.json**: Tiene `com.glebursol.registrogastos`
- **Firebase Console**: Solo muestra `com.example.gestorgastos`

## ⚠️ Problema

El `google-services.json` puede estar desactualizado o puede haber una app en Firebase que no estás viendo.

## 🔍 Pasos para Verificar

### 1. Verificar Todas las Apps en Firebase

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona el proyecto: **gestor-gastos-app-6e1d9**
3. Ve a **Configuración del proyecto** (⚙️)
4. En la sección **"Tus aplicaciones"**, revisa **TODAS** las apps Android
5. Busca si existe una app con package: `com.glebursol.registrogastos`

### 2. Si NO Existe la App con `com.glebursol.registrogastos`

**Opción A: Crear Nueva App (Recomendado para Play Store)**
- Agrega una nueva app Android con package: `com.glebursol.registrogastos`
- Agrega el SHA-1: `40:e2:c5:9e:c7:10:33:11:0f:9b:e9:b6:a1:e6:0f:79:07:cd:37:6f`
- Descarga el nuevo `google-services.json`
- Reemplaza el archivo actual

**Opción B: Cambiar applicationId a `com.example.gestorgastos`**
- Cambia `applicationId` en `build.gradle.kts` a `com.example.gestorgastos`
- Descarga el `google-services.json` de la app existente
- Reemplaza el archivo actual
- ⚠️ **Desventaja**: No podrás usar `com.glebursol.registrogastos` en Play Store

### 3. Si SÍ Existe la App con `com.glebursol.registrogastos`

- Descarga el `google-services.json` de esa app
- Verifica que tenga el SHA-1 correcto
- Reemplaza el archivo actual
- Sincroniza el proyecto

## ✅ Solución Recomendada

**Crear nueva app en Firebase** porque:
- ✅ Mantienes `com.glebursol.registrogastos` para Play Store
- ✅ No afecta la app existente (`com.example.gestorgastos`)
- ✅ Puedes tener ambas apps en el mismo proyecto Firebase
- ✅ Es la solución más limpia y profesional

## 📝 Nota Importante

Firebase **NO permite** cambiar el package name de una app existente. Es un identificador inmutable. Por eso la única opción es:
- Crear una nueva app con el package name correcto, O
- Cambiar el `applicationId` para que coincida con la app existente

