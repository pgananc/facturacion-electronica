# 📱 eBill Mobile - Aplicación Móvil de Facturación Electrónica

Aplicación móvil desarrollada con Flutter para el sistema de facturación electrónica eBill.

## 🏗️ Arquitectura

El proyecto sigue **Clean Architecture** con **BLoC** como patrón de gestión de estado:

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (UI - Widgets, Pages, BLoC)           │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Domain Layer                    │
│  (Entities, Use Cases, Repositories)   │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Data Layer                      │
│  (API, Local Cache, Models)             │
└─────────────────────────────────────────┘
```

## 🚀 Tecnologías

- **Flutter**: Framework multiplataforma
- **BLoC**: Gestión de estado reactiva
- **Dio + Retrofit**: Cliente HTTP REST
- **Hive**: Almacenamiento local
- **GetIt**: Inyección de dependencias
- **GoRouter**: Navegación declarativa
- **Freezed**: Modelos inmutables

## 📦 Estructura del Proyecto

```
lib/
├── core/
│   ├── config/           # Configuración (API URLs, etc)
│   ├── constants/        # Constantes globales
│   ├── error/            # Manejo de errores
│   ├── network/          # Dio client, interceptores
│   ├── storage/          # Hive/SharedPreferences
│   └── utils/            # Utilidades
├── data/
│   ├── datasources/      # API y Local datasources
│   ├── models/          # Modelos de datos (JSON)
│   └── repositories/    # Implementación de repositorios
├── domain/
│   ├── entities/        # Entidades de dominio
│   ├── repositories/    # Interfaces de repositorios
│   └── usecases/        # Casos de uso
├── presentation/
│   ├── bloc/            # BLoCs (estado)
│   ├── pages/           # Pantallas
│   ├── widgets/         # Widgets reutilizables
│   └── theme/           # Tema de la app
└── main.dart
```

## 🔧 Configuración

### 1. Instalar dependencias

```bash
flutter pub get
```

### 2. Generar código

```bash
# Generar todos los archivos necesarios
flutter pub run build_runner build --delete-conflicting-outputs

# O en modo watch (desarrollo)
flutter pub run build_runner watch --delete-conflicting-outputs
```

### 3. Configurar variables de entorno

Crea un archivo `.env` en la raíz del proyecto:

```env
# API Configuration
API_BASE_URL=http://localhost:8180
API_AUTH_URL=http://localhost:8180/api/auth
API_CUSTOMER_URL=http://localhost:8280/api/customers
API_PRODUCT_URL=http://localhost:8380/api/products
API_BILLING_URL=http://localhost:8480/api/billing

# App Configuration
APP_NAME=eBill Mobile
APP_VERSION=1.0.0
```

## 🎯 MVP - Funcionalidades

### Fase 1 (MVP Core)
- ✅ Autenticación (Login/Logout)
- ✅ Dashboard básico
- ✅ Listar facturas
- ✅ Ver detalle de factura
- ✅ Listar clientes
- ✅ Listar productos

### Fase 2 (Próximamente)
- Crear facturas
- CRUD de clientes
- CRUD de productos
- Modo offline
- Notificaciones push

## 🏃 Ejecutar la aplicación

```bash
# Desarrollo
flutter run

# Con hot reload
flutter run --hot

# Release standalone contra producción (sin debug; ENV=prod por defecto en código)
./scripts/build_release_prod.sh apk    # → build/app/outputs/flutter-apk/app-release.apk
./scripts/build_release_prod.sh aab   # bundle Play Store
./scripts/build_release_prod.sh ios   # sin firmar; firmar en Xcode
./scripts/build_release_prod.sh ipa   # requiere cuenta Apple / firma

# Equivalente manual
flutter build apk --release --dart-define=ENV=prod
```

## 📱 Plataformas Soportadas

- ✅ Android
- ✅ iOS

## 🤝 Integración con Microservicios

La aplicación se conecta a los microservicios backend existentes:

- **Auth Service** (`:8280`): Autenticación y autorización
- **Customers Service** (`:8180`): Gestión de clientes y empresas
- **Products Service** (`:8380`): Catálogo de productos
- **E-Billing Service** (`:8480`): Facturación electrónica

## 📄 Licencia

Copyright © 2025 Wallparisoft. Todos los derechos reservados.

## 👥 Equipo

Desarrollado por el equipo de Wallparisoft.

