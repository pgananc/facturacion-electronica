# 🏗️ Arquitectura del Proyecto - eBill Mobile

Este documento describe la arquitectura del proyecto eBill Mobile.

## 📐 Arquitectura General

El proyecto sigue **Clean Architecture** con separación en 3 capas principales:

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

## 📁 Estructura de Carpetas

```
lib/
├── core/                    # Funcionalidades core de la app
│   ├── config/             # Configuración (API URLs, etc)
│   ├── constants/          # Constantes globales
│   ├── di/                 # Dependency Injection (GetIt)
│   ├── error/              # Manejo de errores (Failures, Exceptions)
│   ├── network/            # Cliente HTTP (Dio) e interceptores
│   ├── storage/            # Almacenamiento local (SharedPreferences)
│   └── utils/              # Utilidades (DateUtils, etc)
│
├── data/                    # Capa de datos
│   ├── datasources/        # Fuentes de datos (Remote, Local)
│   ├── models/             # Modelos de datos (JSON serializables)
│   └── repositories/       # Implementación de repositorios
│
├── domain/                  # Capa de dominio (lógica de negocio)
│   ├── entities/           # Entidades de dominio (puras)
│   ├── repositories/       # Interfaces de repositorios
│   └── usecases/           # Casos de uso (opcional, para lógica compleja)
│
└── presentation/            # Capa de presentación (UI)
    ├── bloc/               # BLoCs (gestión de estado)
    ├── pages/              # Pantallas de la aplicación
    ├── routes/             # Configuración de rutas (GoRouter)
    ├── theme/              # Tema y estilos
    └── widgets/            # Widgets reutilizables
```

## 🔄 Flujo de Datos

### Ejemplo: Cargar Facturas

```
1. UI (InvoicesPage)
   ↓
2. BLoC (InvoiceBloc) - Dispara evento LoadInvoicesEvent
   ↓
3. Repository (InvoiceRepository) - Interfaz del dominio
   ↓
4. Repository Implementation (InvoiceRepositoryImpl) - Implementación
   ↓
5. DataSource (InvoiceRemoteDataSource) - Llama a la API
   ↓
6. API Response → Model (InvoiceModel) → Entity (Invoice)
   ↓
7. Repository retorna Either<Failure, List<Invoice>>
   ↓
8. BLoC emite nuevo estado (InvoiceLoaded o InvoiceError)
   ↓
9. UI se actualiza automáticamente (BlocBuilder)
```

## 🎯 Principios de Diseño

### 1. Separación de Responsabilidades

- **Presentation:** Solo UI y gestión de estado
- **Domain:** Lógica de negocio pura (sin dependencias externas)
- **Data:** Acceso a datos (API, BD local)

### 2. Inversión de Dependencias

- La capa de dominio define las interfaces
- La capa de datos implementa esas interfaces
- La capa de presentación depende de las interfaces, no de las implementaciones

### 3. Single Source of Truth

- Los BLoCs son la única fuente de verdad para el estado
- No hay estado duplicado en widgets

### 4. Manejo de Errores

- Uso de `Either<Failure, Success>` para manejo funcional de errores
- Failures específicos por tipo de error (NetworkFailure, ServerFailure, etc.)

## 🔌 Integración con Backend

### Configuración de API

Las URLs de los microservicios se configuran en `core/config/app_config.dart`:

```dart
static const String authServiceUrl = 'http://localhost:8280/api/auth';
static const String customerServiceUrl = 'http://localhost:8180/api/customers';
static const String productServiceUrl = 'http://localhost:8380/api/products';
static const String billingServiceUrl = 'http://localhost:8480/api/billing';
```

### Interceptores HTTP

El proyecto incluye interceptores para:

1. **AuthInterceptor:** Agrega token JWT automáticamente
2. **LoggingInterceptor:** Registra requests/responses (solo en desarrollo)
3. **ErrorInterceptor:** Maneja errores HTTP de forma centralizada

## 💾 Gestión de Estado

### BLoC Pattern

Cada feature tiene su propio BLoC:

- `AuthBloc`: Maneja autenticación (login, logout)
- `InvoiceBloc`: Maneja facturas (listar, detalle)
- `CustomerBloc`: Maneja clientes (futuro)
- `ProductBloc`: Maneja productos (futuro)

### Estados

Cada BLoC tiene estados específicos:

```dart
// Ejemplo: InvoiceState
- InvoiceInitial
- InvoiceLoading
- InvoiceLoaded(List<Invoice>)
- InvoiceError(String message)
```

## 🔐 Autenticación

### Flujo de Autenticación

1. Usuario ingresa credenciales en `LoginPage`
2. `AuthBloc` dispara `LoginEvent`
3. `AuthRepository` llama al endpoint de login
4. Si es exitoso, se guarda el token en `StorageService`
5. `AuthBloc` emite `AuthAuthenticated`
6. La app navega al `DashboardPage`

### Persistencia de Sesión

- El token se guarda en `SharedPreferences`
- Al iniciar la app, `AuthBloc` verifica si hay un token válido
- Si existe, el usuario permanece autenticado

## 📦 Dependencias Principales

| Paquete | Propósito |
|---------|-----------|
| `flutter_bloc` | Gestión de estado |
| `dio` | Cliente HTTP |
| `get_it` | Inyección de dependencias |
| `go_router` | Navegación |
| `shared_preferences` | Almacenamiento local |
| `dartz` | Programación funcional (Either) |
| `json_annotation` | Serialización JSON |

## 🚀 Extensibilidad

### Agregar una Nueva Feature

1. **Crear Entity** en `domain/entities/`
2. **Crear Repository Interface** en `domain/repositories/`
3. **Crear Model** en `data/models/` (con `@JsonSerializable`)
4. **Crear DataSource** en `data/datasources/`
5. **Implementar Repository** en `data/repositories/`
6. **Crear BLoC** en `presentation/bloc/`
7. **Crear Página** en `presentation/pages/`
8. **Registrar en DI** en `core/di/injection.dart`
9. **Agregar Ruta** en `presentation/routes/app_router.dart`

## 📝 Convenciones de Código

- **Nombres de archivos:** snake_case (ej: `auth_bloc.dart`)
- **Nombres de clases:** PascalCase (ej: `AuthBloc`)
- **Nombres de variables:** camelCase (ej: `userName`)
- **Constantes:** camelCase con `const` (ej: `const appName = 'eBill'`)

## 🔍 Testing (Futuro)

La arquitectura está preparada para testing:

- **Unit Tests:** Para BLoCs, Repositories, Use Cases
- **Widget Tests:** Para páginas y widgets
- **Integration Tests:** Para flujos completos

Cada capa puede testearse de forma independiente gracias a la inversión de dependencias.

