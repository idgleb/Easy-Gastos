# Configuración de google-services.json

## ⚠️ IMPORTANTE: Este archivo NO debe subirse al repositorio

El archivo `google-services.json` contiene información sensible (claves API) y está configurado en `.gitignore` para que NO se suba al repositorio.

## 📥 Cómo obtener el archivo

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto: **gestor-gastos-app-6e1d9**
3. Haz clic en el ícono de engranaje ⚙️ (Configuración del proyecto)
4. En la pestaña "General", desplázate hasta la sección "Tus aplicaciones"
5. Busca la aplicación Android con el package name: `com.example.gestorgastos`
6. Haz clic en "Descargar google-services.json"
7. Coloca el archivo descargado en: `app/google-services.json`

## ✅ Verificación

Después de colocar el archivo, verifica que:
- El archivo está en `app/google-services.json`
- El archivo NO aparece en `git status` (está en `.gitignore`)
- La aplicación compila correctamente

## 🔒 Seguridad

- ✅ El archivo está en `.gitignore` - NO se subirá al repositorio
- ✅ El historial de Git fue limpiado - el archivo ya no está en commits anteriores
- ⚠️ **IMPORTANTE**: Si Google detectó la clave API expuesta, debes rotarla en Google Cloud Console

