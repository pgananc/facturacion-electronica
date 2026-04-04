# 📱 Guía para Probar la App en tu Celular

## Requisitos Previos

1. **Flutter instalado** y configurado correctamente
2. **Dispositivo Android o iOS** conectado por USB o WiFi
3. **Backend corriendo** en tu computadora
4. **Mismo WiFi**: Tu celular y computadora deben estar en la misma red WiFi

## Paso 1: Configurar la IP del Backend

La app está configurada para usar `localhost`, pero en un dispositivo físico necesitas usar la IP de tu computadora en la red local.

### Obtener tu IP local:

**En macOS/Linux:**
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

**En Windows:**
```bash
ipconfig
```

Busca la IP que empieza con `192.168.x.x` o `10.x.x.x`

### Configurar las URLs:

Ejecuta la app con las variables de entorno:

```bash
flutter run --dart-define=API_BASE_URL=http://TU_IP:8180 \
  --dart-define=API_AUTH_URL=http://TU_IP:8180/api/auth \
  --dart-define=API_CUSTOMER_URL=http://TU_IP:8280/api/customers \
  --dart-define=API_PRODUCT_URL=http://TU_IP:8380/api/products \
  --dart-define=API_BILLING_URL=http://TU_IP:8480/api/billing
```

**Ejemplo con IP 192.168.100.227:**
```bash
flutter run --dart-define=API_BASE_URL=http://192.168.100.227:8180 \
  --dart-define=API_AUTH_URL=http://192.168.100.227:8180/api/auth \
  --dart-define=API_CUSTOMER_URL=http://192.168.100.227:8280/api/customers \
  --dart-define=API_PRODUCT_URL=http://192.168.100.227:8380/api/products \
  --dart-define=API_BILLING_URL=http://192.168.100.227:8480/api/billing
```

## Paso 2: Preparar el Dispositivo

### Android:

1. **Habilitar Modo Desarrollador:**
   - Ve a `Configuración` > `Acerca del teléfono`
   - Toca 7 veces en "Número de compilación"
   - Aparecerá "Eres un desarrollador"

2. **Habilitar Depuración USB:**
   - Ve a `Configuración` > `Opciones de desarrollador`
   - Activa "Depuración USB"

3. **Conectar por USB:**
   - Conecta tu celular con un cable USB
   - Acepta el diálogo de "Permitir depuración USB" en el celular

### iOS:

1. **Conectar por USB:**
   - Conecta tu iPhone/iPad con un cable USB

2. **Confiar en la computadora:**
   - En el dispositivo, acepta "Confiar en esta computadora"

3. **Configurar certificado de desarrollo:**
   - Abre Xcode
   - Ve a `Preferences` > `Accounts`
   - Agrega tu Apple ID
   - En el proyecto, selecciona tu equipo de desarrollo

## Paso 3: Verificar Dispositivos Conectados

```bash
flutter devices
```

Deberías ver tu dispositivo listado. Ejemplo:
```
2 connected devices:

sdk gphone64 arm64 (mobile) • emulator-5554 • android-arm64  • Android 13 (API 33)
iPhone 14 Pro (mobile)      • 00008030-001... • ios            • com.apple.CoreSimulator.SimRuntime.iOS-16-2
```

## Paso 4: Ejecutar la App

### Opción 1: Ejecutar directamente

```bash
# Primero instala dependencias
flutter pub get

# Ejecuta con la IP de tu computadora
flutter run --dart-define=API_BASE_URL=http://192.168.100.227:8180 \
  --dart-define=API_AUTH_URL=http://192.168.100.227:8180/api/auth \
  --dart-define=API_CUSTOMER_URL=http://192.168.100.227:8280/api/customers \
  --dart-define=API_PRODUCT_URL=http://192.168.100.227:8380/api/products \
  --dart-define=API_BILLING_URL=http://192.168.100.227:8480/api/billing
```

### Opción 2: Crear un script (más fácil)

Crea un archivo `run_mobile.sh`:

```bash
#!/bin/bash
IP="192.168.100.227"  # Cambia esto por tu IP

flutter run \
  --dart-define=API_BASE_URL=http://$IP:8180 \
  --dart-define=API_AUTH_URL=http://$IP:8180/api/auth \
  --dart-define=API_CUSTOMER_URL=http://$IP:8280/api/customers \
  --dart-define=API_PRODUCT_URL=http://$IP:8380/api/products \
  --dart-define=API_BILLING_URL=http://$IP:8480/api/billing
```

Luego ejecuta:
```bash
chmod +x run_mobile.sh
./run_mobile.sh
```

## Paso 5: Verificar que el Backend esté Accesible

Antes de ejecutar la app, verifica que puedas acceder al backend desde tu celular:

1. **Abre un navegador en tu celular**
2. **Ve a:** `http://TU_IP:8180/api/auth` (o el endpoint que corresponda)
3. **Deberías ver una respuesta** (aunque sea un error, significa que la conexión funciona)

## Solución de Problemas

### Error: "No devices found"
- Verifica que el dispositivo esté conectado: `flutter devices`
- En Android, acepta el diálogo de depuración USB
- En iOS, confía en la computadora

### Error: "Connection refused" o "Network error"
- Verifica que el backend esté corriendo
- Verifica que la IP sea correcta: `ifconfig` o `ipconfig`
- Verifica que el celular y la computadora estén en la misma red WiFi
- Verifica el firewall de tu computadora (debe permitir conexiones en los puertos 8180, 8280, 8380, 8480)

### Error: "Build failed"
- Ejecuta `flutter clean`
- Ejecuta `flutter pub get`
- Ejecuta `flutter pub run build_runner build --delete-conflicting-outputs`
- Vuelve a intentar

### La app se conecta pero no carga datos
- Verifica que el backend esté respondiendo correctamente
- Revisa los logs: `flutter run -v` (modo verbose)
- Verifica que las URLs en `app_config.dart` sean correctas

## Hot Reload

Una vez que la app esté corriendo:
- Presiona `r` en la terminal para hot reload
- Presiona `R` para hot restart
- Presiona `q` para salir

## Build para Producción

### Android APK:
```bash
flutter build apk --release \
  --dart-define=API_BASE_URL=http://TU_IP:8180 \
  --dart-define=API_AUTH_URL=http://TU_IP:8180/api/auth \
  --dart-define=API_CUSTOMER_URL=http://TU_IP:8280/api/customers \
  --dart-define=API_PRODUCT_URL=http://TU_IP:8380/api/products \
  --dart-define=API_BILLING_URL=http://TU_IP:8480/api/billing
```

### iOS:
```bash
flutter build ios --release \
  --dart-define=API_BASE_URL=http://TU_IP:8180 \
  --dart-define=API_AUTH_URL=http://TU_IP:8180/api/auth \
  --dart-define=API_CUSTOMER_URL=http://TU_IP:8280/api/customers \
  --dart-define=API_PRODUCT_URL=http://TU_IP:8380/api/products \
  --dart-define=API_BILLING_URL=http://TU_IP:8480/api/billing
```

## Notas Importantes

⚠️ **IMPORTANTE**: 
- La IP puede cambiar cada vez que te conectas a una red WiFi diferente
- Si cambias de red, necesitarás actualizar la IP en los comandos
- Para producción, considera usar un dominio o IP estática

