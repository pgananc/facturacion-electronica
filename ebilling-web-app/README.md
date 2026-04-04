# 📱 E-Billing Web App - Sistema de Facturación Electrónica

<div align="center">

![Angular](https://img.shields.io/badge/Angular-18.2.3-red?logo=angular)
![TypeScript](https://img.shields.io/badge/TypeScript-5.5.3-blue?logo=typescript)
![Material](https://img.shields.io/badge/Material-18.2.3-blue?logo=material-design)
![License](https://img.shields.io/badge/License-Proprietary-yellow)

**Sistema web moderno para gestión de facturación electrónica desarrollado con Angular 18**

</div>

---

## 📋 Tabla de Contenidos

- [Descripción](#-descripción)
- [Características](#-características)
- [Arquitectura](#-arquitectura)
- [Tecnologías](#-tecnologías)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación](#-instalación)
- [Configuración](#-configuración)
- [Comandos Disponibles](#-comandos-disponibles)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Módulos Principales](#-módulos-principales)
- [Documentación Adicional](#-documentación-adicional)
- [Desarrollo](#-desarrollo)
- [Producción](#-producción)

---

## 🎯 Descripción

Sistema web profesional para la gestión completa de facturación electrónica, diseñado con arquitectura modular y optimizado para alto rendimiento. Incluye gestión de clientes, productos, usuarios, empresas y generación de facturas con cálculos automáticos de impuestos (IVA, ICE).

---

## ✨ Características

### **Funcionalidades Principales:**

- ✅ **Facturación Electrónica**: Creación y gestión de facturas con cálculos automáticos
- ✅ **Gestión de Clientes**: CRUD completo con búsqueda avanzada
- ✅ **Gestión de Productos**: Catálogo con tipos de IVA y ICE
- ✅ **Gestión de Usuarios**: Control de acceso y permisos
- ✅ **Gestión de Empresas**: Multi-empresa con establecimientos y puntos de emisión
- ✅ **Firma Digital**: Integración con certificados digitales
- ✅ **Autenticación JWT**: Sistema seguro de autenticación

### **Características Técnicas:**

- 🚀 **Lazy Loading**: Carga diferida de módulos (-64% bundle inicial)
- ⚡ **Optimizado**: Bundle inicial de 241 KB (sin vendor)
- 🔒 **Seguro**: Interceptor HTTP con manejo de errores
- 📊 **Logging Estructurado**: Sistema de logs con niveles (debug, info, warn, error)
- 🌐 **URLs Limpias**: Path Location Strategy (sin #)
- 🎨 **Material Design**: UI moderna con Angular Material
- 📱 **Responsive**: Diseño adaptable a diferentes dispositivos

---

## 🏗️ Arquitectura

### **Frontend (Esta aplicación):**

```
Angular 18 SPA
├── Lazy Loading (6 módulos)
├── Reactive Forms
├── RxJS (State Management)
└── JWT Authentication
```

### **Backend (Microservicios):**

```
┌─────────────────────────────────────┐
│         Frontend Angular            │
│      (localhost:4200)               │
└──────────────┬──────────────────────┘
               │
    ┌──────────┴──────────┐
    │   Proxy Config      │
    │  (proxy.conf.json)  │
    └──────────┬──────────┘
               │
    ┌──────────┴──────────────────────┐
    │                                  │
    ▼                                  ▼
┌────────────┐              ┌──────────────┐
│   Auth     │              │  Customers   │
│ Service    │              │   Service    │
│ :8180      │              │   :8280      │
└────────────┘              └──────────────┘
    │                                  │
    ▼                                  ▼
┌────────────┐              ┌──────────────┐
│  Products  │              │  E-Billing   │
│  Service   │              │   Service    │
│  :8380     │              │   :8480      │
└────────────┘              └──────────────┘
```

**Servicios Backend:**

- **Auth Service** (`:8180`): Autenticación y autorización
- **Customers Service** (`:8280`): Gestión de clientes y empresas
- **Products Service** (`:8380`): Catálogo de productos
- **E-Billing Service** (`:8480`): Facturación electrónica

---

## 🛠️ Tecnologías

### **Core:**

- **Angular**: 18.2.3
- **TypeScript**: 5.5.3
- **RxJS**: 7.8.0
- **Zone.js**: 0.14.10

### **UI/UX:**

- **Angular Material**: 18.2.3
- **Angular CDK**: 18.2.3
- **Flex Layout**: 15.0.0-beta.42

### **Autenticación:**

- **@auth0/angular-jwt**: JWT token management

### **Development:**

- **Angular CLI**: 18.2.3
- **Angular DevKit**: 18.2.3

---

## 📦 Requisitos Previos

```bash
Node.js: >= 18.x
npm: >= 9.x
Angular CLI: 18.2.3
```

**Verificar versiones:**

```bash
node --version    # v18.x o superior
npm --version     # 9.x o superior
ng version        # Angular CLI 18.2.3
```

**Instalar Angular CLI (si no lo tienes):**

```bash
npm install -g @angular/cli@18.2.3
```

---

## 🚀 Instalación

### **1. Clonar el repositorio:**

```bash
git clone <repository-url>
cd ebilling-web-app
```

### **2. Instalar dependencias:**

```bash
npm install
```

### **3. Verificar instalación:**

```bash
npm run ng version
```

---

## ⚙️ Configuración

### **1. Configurar Backend (proxy.conf.json):**

El archivo `proxy.conf.json` ya está configurado para los microservicios:

```json
{
  "/api/auth/**": {
    "target": "http://localhost:8180",
    "secure": false
  },
  "/api/customers/**": {
    "target": "http://localhost:8280",
    "secure": false
  },
  "/api/products/**": {
    "target": "http://localhost:8380",
    "secure": false
  },
  "/api/billing/**": {
    "target": "http://localhost:8480",
    "secure": false
  }
}
```

### **2. Configurar Ambientes:**

**Desarrollo (`src/environments/environment.ts`):**

```typescript
export const environment = {
  production: false,
  REINTENTOS: 2,
  TOKEN: "token",
  ID_COMPANY: "idCompany",
  // ... más configuración
};
```

**Producción (`src/environments/environment.prod.ts`):**

```typescript
export const environment = {
  production: true,
  REINTENTOS: 1,
  TOKEN: "token",
  ID_COMPANY: "idCompany",
  // ... más configuración
};
```

---

## 🎮 Comandos Disponibles

### **Desarrollo:**

```bash
# Iniciar servidor de desarrollo (puerto 4200)
npm start
# o
npm run start:local

# Iniciar en modo producción local
npm run start:prod

# Parar servidor (libera puerto 4200)
npm run stop
```

### **Build:**

```bash
# Build de desarrollo
npm run build:dev

# Build de producción
npm run build
```

### **Testing:**

```bash
# Ejecutar tests unitarios
npm test

# Ejecutar tests con coverage
npm run test:coverage
```

### **Otros:**

```bash
# Watch mode (recompila automáticamente)
npm run watch

# Generar componente
ng generate component components/my-component

# Generar servicio
ng generate service services/my-service
```

---

## 📁 Estructura del Proyecto

```
ebilling-web-app/
├── src/
│   ├── app/
│   │   ├── _constants/          # Constantes globales
│   │   ├── _functions/          # Funciones utilitarias
│   │   ├── _model/              # Modelos e interfaces
│   │   │   ├── auth/           # Modelos de autenticación
│   │   │   ├── customer/       # Modelos de clientes
│   │   │   ├── invoice/        # Modelos de facturación
│   │   │   └── product/        # Modelos de productos
│   │   ├── _service/            # Servicios
│   │   │   ├── auth/           # Servicios de autenticación
│   │   │   ├── customer/       # Servicios de clientes
│   │   │   ├── invoice/        # Servicios de facturación
│   │   │   ├── logger/         # Sistema de logging
│   │   │   ├── product/        # Servicios de productos
│   │   │   └── user/           # Servicios de usuarios
│   │   ├── _shared/             # Recursos compartidos
│   │   │   ├── server-errors.interceptor.ts
│   │   │   └── http-skip-interceptor.ts
│   │   ├── material/            # Configuración Angular Material
│   │   ├── pages/               # Páginas/Módulos (Lazy Loaded)
│   │   │   ├── billing/        # Módulo de facturación
│   │   │   ├── company/        # Módulo de empresas
│   │   │   ├── customer/       # Módulo de clientes
│   │   │   ├── product/        # Módulo de productos
│   │   │   ├── user/           # Módulo de usuarios
│   │   │   ├── setting/        # Módulo de configuración
│   │   │   ├── login/          # Login y reset password
│   │   │   ├── welcome/        # Dashboard principal
│   │   │   └── error/          # Páginas de error (403, 404)
│   │   ├── app-routing.module.ts
│   │   ├── app.component.ts
│   │   └── app.module.ts
│   ├── assets/                  # Recursos estáticos
│   ├── environments/            # Configuraciones de ambiente
│   └── styles.css               # Estilos globales
├── .vscode/                     # Configuración VS Code/Cursor
│   ├── launch.json             # Debug configuration
│   ├── tasks.json              # Tasks
│   └── settings.json           # Settings
├── angular.json                 # Configuración Angular
├── proxy.conf.json             # Configuración de proxy
├── tsconfig.json               # Configuración TypeScript
├── package.json                # Dependencias
├── DATABASE.md                 # Guía de base de datos
├── DEBUG.md                    # Guía de debugging
├── HTTP-ERROR-HANDLING.md      # Guía de manejo de errores
├── LAZY-LOADING.md             # Guía de lazy loading
├── LOGGER.md                   # Guía del sistema de logs
├── PATH-LOCATION-STRATEGY.md   # Guía de URLs limpias
└── README.md                   # Este archivo
```

---

## 📦 Módulos Principales

### **Módulos Lazy Loaded:**

| Módulo       | Ruta       | Descripción                   | Bundle |
| ------------ | ---------- | ----------------------------- | ------ |
| **Billing**  | `/billing` | Facturación electrónica       | 87 KB  |
| **Company**  | `/company` | Gestión de empresas           | 121 KB |
| **Customer** | `/client`  | Gestión de clientes           | 79 KB  |
| **Product**  | `/product` | Catálogo de productos         | 63 KB  |
| **User**     | `/user`    | Gestión de usuarios           | 76 KB  |
| **Setting**  | `/setting` | Configuración y firma digital | 27 KB  |

**Beneficio:** Los módulos se cargan solo cuando el usuario los visita, mejorando el tiempo de carga inicial en un **64%**.

---

## 📚 Documentación Adicional

| Documento                                                    | Descripción                                            |
| ------------------------------------------------------------ | ------------------------------------------------------ |
| [BILLING-IMPROVEMENTS.md](./BILLING-IMPROVEMENTS.md)         | Mejoras en validaciones y UX del módulo de facturación |
| [DATABASE.md](./DATABASE.md)                                 | Guía para visualizar y gestionar la base de datos      |
| [DEBUG.md](./DEBUG.md)                                       | Cómo debuggear la aplicación en Cursor/VS Code         |
| [HTTP-ERROR-HANDLING.md](./HTTP-ERROR-HANDLING.md)           | Sistema de manejo de errores HTTP                      |
| [LAZY-LOADING.md](./LAZY-LOADING.md)                         | Explicación completa del Lazy Loading                  |
| [LOGGER.md](./LOGGER.md)                                     | Sistema de logging estructurado                        |
| [ON-PUSH-CHANGE-DETECTION.md](./ON-PUSH-CHANGE-DETECTION.md) | Optimización de performance con OnPush                 |
| [PATH-LOCATION-STRATEGY.md](./PATH-LOCATION-STRATEGY.md)     | Configuración de URLs limpias                          |

---

## 💻 Desarrollo

### **Iniciar aplicación:**

```bash
# 1. Asegúrate de que los servicios backend estén corriendo
# 2. Inicia el frontend
npm start

# La aplicación se abrirá en http://localhost:4200
```

### **Flujo de desarrollo:**

1. **Login:** Usa las credenciales del sistema
2. **Dashboard:** Selecciona empresa y punto de emisión
3. **Módulos:** Navega a los diferentes módulos (lazy loaded)

### **Debugging:**

Ver [DEBUG.md](./DEBUG.md) para instrucciones completas de debugging en:

- Chrome DevTools
- VS Code / Cursor IDE
- Network inspection

### **Sistema de Logging:**

```typescript
// Uso del LoggerService
constructor(private logger: LoggerService) {}

this.logger.debug('Mensaje de debug');  // Solo en desarrollo
this.logger.info('Información general');
this.logger.warn('Advertencia');
this.logger.error('Error crítico', error);
this.logger.table(data, 'Descripción');
```

Ver [LOGGER.md](./LOGGER.md) para más detalles.

---

## 🚀 Producción

### **Build:**

```bash
# Build de producción
npm run build

# Los archivos se generan en: dist/ebilling-web-app/
```

### **Configuración del Servidor:**

La aplicación usa **PathLocationStrategy** (URLs limpias). El servidor debe redirigir todas las rutas a `index.html`.

**Nginx:**

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

**Apache:**

```apache
RewriteEngine On
RewriteCond %{REQUEST_FILENAME} !-f
RewriteCond %{REQUEST_FILENAME} !-d
RewriteRule ^ index.html [L]
```

Ver [PATH-LOCATION-STRATEGY.md](./PATH-LOCATION-STRATEGY.md) para más configuraciones (Docker, Firebase, Netlify, Vercel, AWS, etc.).

---

## 🐛 Troubleshooting

### **Puerto 4200 ocupado:**

```bash
npm run stop
```

### **Error de compilación:**

```bash
# Limpiar caché y reinstalar
rm -rf node_modules package-lock.json .angular
npm install
```

### **Errores de proxy:**

```bash
# Verificar que los servicios backend estén corriendo
# Verificar proxy.conf.json
```

---

## 🤝 Contribución

1. Crea un branch para tu feature: `git checkout -b feature/amazing-feature`
2. Commit tus cambios: `git commit -m 'Add amazing feature'`
3. Push al branch: `git push origin feature/amazing-feature`
4. Abre un Pull Request

---

## 📄 Licencia

Este proyecto es propiedad de **WallpariSoft** y está protegido por derechos de autor.

---

## 👥 Equipo

### **Desarrolladores:**

- **Jonathan Guerrero** - Sr. Software Developer
- **Pablo Ganán** - Sr. Software Developer

Desarrollado por **WallpariSoft**

---

## 📞 Soporte

Para soporte técnico, contacta al equipo de desarrollo.

---

<div align="center">

**Hecho con ❤️ por WallpariSoft**

</div>
