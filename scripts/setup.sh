#!/bin/bash
# Script de configuración inicial para los scripts de actualización

echo "🔧 Configurando scripts de actualización de versión..."
echo ""

# Verificar si Node.js está instalado
if ! command -v node &> /dev/null; then
    echo "❌ Node.js no está instalado"
    echo "   Por favor, instala Node.js desde: https://nodejs.org/"
    exit 1
fi

echo "✅ Node.js está instalado: $(node --version)"

# Verificar si npm está instalado
if ! command -v npm &> /dev/null; then
    echo "❌ npm no está instalado"
    exit 1
fi

echo "✅ npm está instalado: $(npm --version)"
echo ""

# Instalar firebase-admin si no está instalado
if [ ! -d "node_modules" ]; then
    echo "📦 Instalando dependencias..."
    npm install firebase-admin
    echo "✅ Dependencias instaladas"
else
    echo "✅ Dependencias ya instaladas"
fi

echo ""
echo "📋 Próximos pasos:"
echo "1. Obtén serviceAccountKey.json desde Firebase Console"
echo "2. Colócalo en la raíz del proyecto"
echo "3. Ejecuta: ./scripts/update-version.sh 2 \"1.1\" \"Mensaje\""
echo ""
echo "📖 Lee scripts/README.md para más información"

