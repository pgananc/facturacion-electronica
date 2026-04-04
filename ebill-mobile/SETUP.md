# 🚀 Guía de Configuración - eBill Mobile

Esta guía te ayudará a configurar y ejecutar el proyecto Flutter eBill Mobile.

## 📋 Requisitos Previos

### 1. Instalar Flutter

```bash
# Verificar si Flutter está instalado
flutter --version

# Si no está instalado, descarga desde:
# https://flutter.dev/docs/get-started/install
```

### 2. Verificar Configuración

```bash
# Verificar que todo esté correcto
flutter doctor

# Asegúrate de tener:
# - Flutter SDK
# - Android Studio / Xcode (para iOS)
# - Android SDK / iOS SDK
```

## 🔧 Configuración del Proyecto

### 1. Instalar Dependencias

```bash
cd ebill-mobile
flutter pub get
```

### 2. Generar Código

El proyecto usa code generation para modelos JSON y otros archivos:

```bash
# Generar todos los archivos necesarios
flutter pub run build_runner build --delete-conflicting-outputs

# O en modo watch (recomendado durante desarrollo)
flutter pub run build_runner watch --delete-conflicting-outputs
```

### 3. Configurar URLs de API

Edita `lib/core/config/app_config.dart` o crea variables de entorno:

```dart
// Para desarrollo local
static const String baseUrl = 'http://localhost:8180';

// Para servidor remoto
static const String baseUrl = 'http://tu-servidor.com';
```

**Nota:** En Android, usa `10.0.2.2` en lugar de `localhost` para acceder al servidor local.

### 4. Configurar Android

Si vas a ejecutar en Android, asegúrate de tener configurado el `AndroidManifest.xml`:

```xml
<!-- android/app/src/main/AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET"/>
```

### 5. Configurar iOS

Si vas a ejecutar en iOS, configura el `Info.plist`:

```xml
<!-- ios/Runner/Info.plist -->
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsArbitraryLoads</key>
    <true/>
</dict>
```

## 🏃 Ejecutar la Aplicación

### Desarrollo

```bash
# Ver dispositivos disponibles
flutter devices

# Ejecutar en dispositivo/emulador
flutter run

# Ejecutar con hot reload (ya está activo por defecto)
# Presiona 'r' para hot reload
# Presiona 'R' para hot restart
```

### Build para Producción

```bash
# Android APK
flutter build apk --release

# Android App Bundle
flutter build appbundle --release

# iOS
flutter build ios --release
```

## 🐛 Solución de Problemas

### Error: "No devices found"

```bash
# Verificar dispositivos conectados
flutter devices

# Si usas Android, asegúrate de tener un emulador corriendo o un dispositivo conectado
# Si usas iOS, asegúrate de tener un simulador o dispositivo conectado
```

### Error: "Connection refused" al conectar con la API

- **Android Emulator:** Usa `10.0.2.2` en lugar de `localhost`
- **iOS Simulator:** Usa `localhost` normalmente
- **Dispositivo físico:** Usa la IP de tu máquina en la red local

### Error: "Package not found"

```bash
# Limpiar y reinstalar
flutter clean
flutter pub get
flutter pub run build_runner build --delete-conflicting-outputs
```

### Error: "Build failed"

```bash
# Limpiar build
flutter clean

# Reinstalar dependencias
flutter pub get

# Regenerar código
flutter pub run build_runner build --delete-conflicting-outputs

# Intentar de nuevo
flutter run
```

## 📱 Próximos Pasos

1. **Conectar con Backend:** Ajusta las URLs en `app_config.dart`
2. **Probar Login:** Usa credenciales válidas del sistema backend
3. **Explorar Funcionalidades:** Navega por las diferentes pantallas
4. **Agregar Features:** Sigue la arquitectura establecida para nuevas funcionalidades

## 🔗 Enlaces Útiles

- [Documentación Flutter](https://flutter.dev/docs)
- [BLoC Pattern](https://bloclibrary.dev/)
- [GoRouter](https://pub.dev/packages/go_router)
- [Dio HTTP Client](https://pub.dev/packages/dio)

