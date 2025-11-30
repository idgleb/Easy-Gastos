#!/bin/bash
# Script para Linux/Mac para actualizar la versión en Firestore
# 
# Uso:
#   ./scripts/update-version.sh <versionCode> <versionName> [message]
#
# Ejemplo:
#   ./scripts/update-version.sh 2 "1.1" "Nueva versión con mejoras"

if [ -z "$1" ]; then
    echo "Error: Falta versionCode"
    echo ""
    echo "Uso: ./scripts/update-version.sh <versionCode> <versionName> [message]"
    echo "Ejemplo: ./scripts/update-version.sh 2 \"1.1\" \"Nueva versión con mejoras\""
    exit 1
fi

if [ -z "$2" ]; then
    echo "Error: Falta versionName"
    echo ""
    echo "Uso: ./scripts/update-version.sh <versionCode> <versionName> [message]"
    echo "Ejemplo: ./scripts/update-version.sh 2 \"1.1\" \"Nueva versión con mejoras\""
    exit 1
fi

node scripts/update-version.js "$1" "$2" "$3"

