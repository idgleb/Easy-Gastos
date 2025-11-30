/**
 * Script para actualizar la versión de la app en Firestore
 * 
 * Uso:
 *   node scripts/update-version.js <versionCode> <versionName> [message]
 * 
 * Ejemplo:
 *   node scripts/update-version.js 2 "1.1" "Nueva versión con mejoras"
 */

const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// Verificar que existe serviceAccountKey.json
const serviceAccountPath = path.join(__dirname, '..', 'serviceAccountKey.json');
if (!fs.existsSync(serviceAccountPath)) {
  console.error('❌ Error: No se encontró serviceAccountKey.json');
  console.log('');
  console.log('Para obtener el archivo:');
  console.log('1. Ve a Firebase Console → Configuración del proyecto');
  console.log('2. Pestaña "Cuentas de servicio"');
  console.log('3. Clic en "Generar nueva clave privada"');
  console.log('4. Descarga el JSON y renómbralo a serviceAccountKey.json');
  console.log('5. Colócalo en la raíz del proyecto');
  process.exit(1);
}

// Inicializar Firebase Admin SDK
const serviceAccount = require(serviceAccountPath);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function updateVersion(versionCode, versionName, message) {
  try {
    const versionRef = db.doc('app_info/version');
    
    const versionData = {
      latestVersionCode: parseInt(versionCode),
      latestVersionName: versionName,
      updateMessage: message || `Nueva versión ${versionName} disponible con mejoras y correcciones.`,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    };
    
    // Obtener minVersionCode actual si existe, o usar el mismo que latestVersionCode
    const currentDoc = await versionRef.get();
    if (currentDoc.exists) {
      const currentData = currentDoc.data();
      if (currentData.minVersionCode) {
        versionData.minVersionCode = currentData.minVersionCode;
      } else {
        versionData.minVersionCode = parseInt(versionCode);
      }
    } else {
      versionData.minVersionCode = parseInt(versionCode);
    }
    
    await versionRef.set(versionData, { merge: true });
    
    console.log('✅ Versión actualizada exitosamente en Firestore:');
    console.log(`   - latestVersionCode: ${versionData.latestVersionCode}`);
    console.log(`   - latestVersionName: ${versionData.latestVersionName}`);
    console.log(`   - updateMessage: ${versionData.updateMessage}`);
    console.log(`   - minVersionCode: ${versionData.minVersionCode}`);
    
    process.exit(0);
  } catch (error) {
    console.error('❌ Error al actualizar versión:', error);
    process.exit(1);
  }
}

// Obtener argumentos de la línea de comandos
const args = process.argv.slice(2);

if (args.length < 2) {
  console.error('❌ Uso incorrecto');
  console.log('');
  console.log('Uso:');
  console.log('  node scripts/update-version.js <versionCode> <versionName> [message]');
  console.log('');
  console.log('Ejemplos:');
  console.log('  node scripts/update-version.js 2 "1.1"');
  console.log('  node scripts/update-version.js 2 "1.1" "Nueva versión con mejoras en el dashboard"');
  process.exit(1);
}

const versionCode = args[0];
const versionName = args[1];
const message = args[2];

updateVersion(versionCode, versionName, message);

