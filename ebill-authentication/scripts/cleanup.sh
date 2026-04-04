#!/bin/bash

# Script para limpiar el entorno de desarrollo
# Autor: Wallparisoft Dev Team

echo "🧹 Limpiando entorno de desarrollo..."
echo ""

# Detener servicios Docker
echo "⏹️  Deteniendo servicios Docker..."
docker-compose down

# Limpiar target
echo "🗑️  Limpiando directorio target..."
mvn clean

# Eliminar logs
echo "📄 Limpiando logs..."
rm -f *.log

# Opcional: Eliminar volúmenes Docker (descomentar si quieres borrar datos)
# echo "💾 Eliminando volúmenes Docker..."
# docker-compose down -v

echo ""
echo "✅ Limpieza completada"

