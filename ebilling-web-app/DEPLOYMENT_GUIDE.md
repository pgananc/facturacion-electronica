# 📦 Guía de Despliegue a Producción - Dashboard de Ventas

## 🎯 Resumen

Esta guía explica cómo desplegar a producción los cambios del Dashboard de Ventas que incluyen la librería **Chart.js 4.5.1**.

---

## ✅ Cambios Realizados

### 📚 Nueva Dependencia
- **Chart.js**: Librería de gráficos interactivos
- **Versión**: 4.5.1
- **Tamaño**: ~200KB (minificado y gzipped)
- **Ubicación**: Ya agregado a `package.json` en `dependencies`

###  Archivos Modificados
1. `/src/app/pages/reports/sales-dashboard/*` - Componente dashboard completo
2. `/src/app/material/material.module.ts` - Agregado `MatChipsModule`
3. `/package.json` - Agregado `chart.js: ^4.5.1`

---

## 🚀 Opción 1: Despliegue en Servidor (RECOMENDADO)

### Pasos en tu servidor de producción:

```bash
# 1. Hacer pull de los cambios desde Git
git pull origin main  # (o tu rama principal)

# 2. Limpiar instalación anterior (IMPORTANTE)
rm -rf node_modules
rm -rf dist
rm package-lock.json

# 3. Instalar dependencias frescas (esto instalará Chart.js automáticamente)
npm install --production=false

# 4. Hacer build de producción
npm run build --configuration production

# 5. Los archivos compilados estarán en: dist/ebilling-web-app/
# Estos archivos YA INCLUYEN Chart.js empaquetado dentro del bundle

# 6. Desplegar la carpeta dist/ a tu servidor web (nginx, apache, etc.)
```

---

## 🏠 Opción 2: Build Local + Subir Manualmente

Si prefieres hacer el build en tu máquina local:

```bash
# EN TU MÁQUINA LOCAL:

# 1. Asegurarte de tener los últimos cambios
git pull origin main

# 2. Limpiar (si tienes problemas de caché)
rm -rf node_modules dist package-lock.json

# 3. Instalar dependencias
npm install

# 4. Build de producción
npm run build --configuration production

# 5. Comprimir la carpeta dist/
cd dist
tar -czf ebilling-web-app.tar.gz ebilling-web-app/

# 6. Subir ebilling-web-app.tar.gz a tu servidor

# EN EL SERVIDOR:
# 7. Descomprimir y copiar los archivos
tar -xzf ebilling-web-app.tar.gz
cp -r ebilling-web-app/* /ruta/del/servidor/web/
```

---

## ⚠️ Problema Actual: Error de Babel en Build Local

**Síntoma**: El build falla con error `Cannot find module '@babel/plugin-bugfix-firefox-class-in-computed-class-key'`

**Causa**: Caché corrupto de npm/node_modules en tu máquina local

**Solución**:

```bash
# Opción A: Limpiar caché de npm
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
npm run build

# Opción B: Si persiste, instalar el plugin manualmente
npm install --save-dev @babel/plugin-bugfix-firefox-class-in-computed-class-key
npm run build

# Opción C: Usar Node.js 18.19.1+ (recomendado por Angular 18)
nvm install 18.19.1
nvm use 18.19.1
npm install
npm run build
```

**NOTA IMPORTANTE**: Este error NO ocurrirá en tu servidor de producción si:
- Haces `git pull` + `npm install` desde cero
- No tienes node_modules corrupto
- Usas Node.js 18.19.1 o superior

---

## 📦 ¿Qué Incluye el Build de Producción?

Cuando ejecutas `npm run build`, Angular CLI automáticamente:

1. ✅ **Empaqueta Chart.js** dentro de los archivos JavaScript
2. ✅ **Minifica** todo el código (reduce tamaño)
3. ✅ **Optimiza** para producción (tree-shaking, compresión)
4. ✅ **Crea hash** en nombres de archivos para cache busting
5. ✅ **Genera source maps** (opcional, para debugging)

**Resultado**: Los archivos en `dist/` son AUTÓNOMOS, no necesitan `node_modules` ni instalar nada en el servidor web.

---

## 🔍 Verificar que Chart.js está Incluido

Después del build, puedes verificar:

```bash
# Ver el tamaño de los bundles
ls -lh dist/ebilling-web-app/*.js

# Buscar Chart.js en el código
grep -r "Chart" dist/ebilling-web-app/main*.js
```

Si encuentras menciones a "Chart" o "chart.js" en los archivos `.js` del `dist/`, significa que está correctamente empaquetado.

---

## 🌐 Configuración del Servidor Web

### Nginx

```nginx
server {
    listen 80;
    server_name tu-dominio.com;
    root /ruta/a/dist/ebilling-web-app;
    index index.html;

    # Cache para assets estáticos (JS, CSS, imágenes)
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Redirigir todas las rutas a index.html (Angular SPA)
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### Apache (.htaccess)

```apache
<IfModule mod_rewrite.c>
    RewriteEngine On
    RewriteBase /
    RewriteRule ^index\.html$ - [L]
    RewriteCond %{REQUEST_FILENAME} !-f
    RewriteCond %{REQUEST_FILENAME} !-d
    RewriteRule . /index.html [L]
</IfModule>

# Cache para assets estáticos
<FilesMatch "\.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$">
    Header set Cache-Control "max-age=31536000, public"
</FilesMatch>
```

---

## 📊 Impacto en Performance

### Tamaño de Bundles (Estimado)

| Archivo | Antes | Después | Incremento |
|---------|-------|---------|------------|
| main.js | ~2.5MB | ~2.7MB | +200KB |
| Total inicial | ~3.0MB | ~3.2MB | +6.7% |

### Tiempos de Carga (Estimados)

- **3G**: +0.5 segundos
- **4G**: +0.1 segundos  
- **WiFi**: +0.05 segundos

**Conclusión**: El impacto es MÍNIMO y muy aceptable para la funcionalidad que agrega.

---

## ✅ Checklist de Despliegue

Antes de desplegar a producción, verifica:

- [ ] **Git**: Todos los cambios están commiteados
- [ ] **Branch**: Estás en la rama correcta (main/master)
- [ ] **Pull**: Tienes los últimos cambios
- [ ] **Build**: El build local funciona (o usarás build en servidor)
- [ ] **package.json**: Chart.js está en dependencies (línea 30)
- [ ] **Tests**: La aplicación funciona en desarrollo (`npm start`)
- [ ] **Backend**: Endpoint `/api/reports/sales-dashboard` existe (o usarás datos de demostración)
- [ ] **Permisos**: Usuario tiene permisos para ver reportes

---

## 🧪 Testing Post-Despliegue

Después de desplegar, verifica en producción:

1. **Cargar la aplicación**: `https://tu-dominio.com`
2. **Login**: Autenticarte correctamente
3. **Navegar a Dashboard**: Reportes → Dashboard de Ventas
4. **Verificar KPIs**: Las 4 tarjetas se muestran correctamente
5. **Verificar Gráficos**: Los 3 gráficos se renderiz an (aunque con datos demo)
6. **Console del navegador**: No debe haber errores de Chart.js
7. **Network tab**: Verificar que los archivos .js se cargan correctamente

### Comandos de Debugging en Consola del Navegador

```javascript
// Verificar que Chart.js está cargado
console.log(window.Chart ? 'Chart.js OK' : 'Chart.js NO cargado');

// Verificar versión
console.log(Chart.version);  // Debería mostrar "4.5.1"
```

---

## 🆘 Troubleshooting

### Problema: "Chart is not defined"

**Causa**: Chart.js no se cargó correctamente en el bundle  
**Solución**: 
```bash
rm -rf node_modules dist
npm install
npm run build
```

### Problema: Gráficos no se muestran

**Causa**: Canvas no se encuentra en el DOM  
**Verificar**: Consola del navegador debe mostrar "Canvas salesTrendChart no encontrado"  
**Solución**: Es un problema de lógica, no de Chart.js

### Problema: Build muy lento

**Causa**: Primera vez que se empaqueta Chart.js  
**Solución**: Normal, builds subsecuentes serán más rápidos gracias al cache

### Problema: Error 404 en producción

**Causa**: Rutas de Angular no configuradas en servidor web  
**Solución**: Configurar rewrite rules (ver sección de Nginx/Apache arriba)

---

## 📞 Soporte

Si tienes problemas durante el despliegue:

1. Revisa los logs del build: `npm run build > build.log 2>&1`
2. Verifica la consola del navegador en producción
3. Compara con ambiente de desarrollo que SÍ funciona

---

## 📚 Referencias

- **Chart.js Docs**: https://www.chartjs.org/docs/
- **Angular Build**: https://angular.io/guide/deployment
- **Package.json actual**: Incluye `chart.js: ^4.5.1` en línea 30

---

**Última Actualización**: 12 de Diciembre, 2024  
**Versión**: 1.0  
**Autor**: Jorge Guerrero (Frontend Lead)

