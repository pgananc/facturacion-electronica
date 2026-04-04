# 🌍 Bloqueo Geográfico con CloudFlare WAF

## 📋 Overview

Implementación de bloqueo de acceso basado en geolocalización para proteger la aplicación de intentos de hackeo desde países con alto índice de amenazas cibernéticas.

---

## 🎯 Estrategia: CloudFlare WAF

**Ventajas:**

- ✅ No requiere cambios en el código
- ✅ Bloqueo antes de que las peticiones lleguen al servidor
- ✅ Logs detallados de intentos bloqueados
- ✅ Configuración visual, sin necesidad de programar
- ✅ Puede bloquear por: País, IP, ASN, User-Agent, etc.
- ✅ Reduce costos de servidor (menos peticiones procesadas)

---

## 🚀 Implementación CloudFlare WAF

### Paso 1: Configurar Dominio en CloudFlare

1. Ir a https://dash.cloudflare.com/
2. Agregar tu dominio (ej: `ebilling.wallparisoft.com`)
3. CloudFlare te dará 2 nameservers
4. Actualizar los nameservers en tu proveedor de dominio (GoDaddy, Namecheap, etc.)
5. Esperar propagación DNS (24-48 horas máximo)

### Paso 2: Activar Proxy de CloudFlare

1. En CloudFlare Dashboard → DNS
2. Para cada registro A/AAAA/CNAME:
   - Activar el **proxy naranja** (nube naranja) ☁️
   - Esto enruta el tráfico a través de CloudFlare

### Paso 3: Crear Regla de Firewall (Bloqueo por País)

#### Opción A: CloudFlare WAF (Recomendado para planes Pro+)

1. Ir a **Security** → **WAF**
2. Click en **Create rule**
3. Configurar la regla:

```
Rule name: Block High-Risk Countries

If...
  Field: Country
  Operator: is in
  Value:
    - China (CN)
    - Russia (RU)
    - North Korea (KP)
    - Iran (IR)
    - Vietnam (VN)
    - Indonesia (ID)
    - Pakistan (PK)
    - Bangladesh (BD)
    - Ukraine (UA)
    - Belarus (BY)

Then...
  Action: Block

Choose action:
  ☑ Block
  ☐ Log only (para testing primero)

Response:
  - Status Code: 403
  - Custom HTML (opcional):
    "Access from your country is temporarily restricted.
     If you believe this is an error, please contact support."
```

4. **Deploy**

#### Opción B: Firewall Rules (Disponible en todos los planes)

1. Ir a **Security** → **WAF** → **Custom rules**
2. Click en **Create rule**
3. Usar expresión similar:

```
(ip.geoip.country in {"CN" "RU" "KP" "IR" "VN" "ID" "PK" "BD" "UA" "BY"})
```

### Paso 4: Configuraciones Adicionales de Seguridad

#### 4.1. Rate Limiting (Limitar peticiones por IP)

```
Rule: Limit Login Attempts

If...
  URI Path: equals "/api/auth/login"
  Requests: > 5 requests
  Period: 10 minutes

Then...
  Block for 1 hour
```

#### 4.2. Bot Fight Mode

1. Ir a **Security** → **Bots**
2. Activar **Bot Fight Mode**
   - Bloquea bots maliciosos automáticamente
   - Permite bots buenos (Google, Bing)

#### 4.3. Challenge Passage

1. Ir a **Security** → **Settings**
2. Configurar **Security Level**:
   - **Medium** (recomendado para aplicaciones de negocios)
   - Presenta desafíos CAPTCHA a visitantes sospechosos

#### 4.4. Browser Integrity Check

1. Ir a **Security** → **Settings**
2. Activar **Browser Integrity Check**
   - Bloquea peticiones que no provienen de navegadores legítimos

---

## 📊 Monitoreo y Logs

### Ver Intentos Bloqueados

1. Ir a **Security** → **Events**
2. Filtrar por:
   - Action: Block
   - Service: Firewall Rules
   - Fecha: Últimos 7/30 días

### Métricas Importantes

- **Total Blocked**: Total de peticiones bloqueadas
- **Top Countries**: Países con más intentos
- **Top IPs**: IPs más agresivas
- **Attack Vectors**: Tipos de ataques detectados

---

## 🔧 Configuración Avanzada (Opcional)

### 1. Whitelist de IPs Conocidas

Si tienes clientes legítimos en países bloqueados:

```
Rule: Whitelist Known Clients

If...
  IP Address: is in {192.168.1.100, 203.45.67.89}

Then...
  Skip remaining rules
```

### 2. Bloqueo por ASN (Autonomous System Number)

Bloquear rangos de IPs de proveedores sospechosos:

```
Rule: Block Suspicious Hosting Providers

If...
  ASN: is in {AS12345, AS67890}  # Hosting providers conocidos por abusos

Then...
  Block
```

### 3. Bloqueo por User-Agent

Bloquear herramientas de hacking conocidas:

```
Rule: Block Hacking Tools

If...
  User Agent: contains "sqlmap"
  OR User Agent: contains "nikto"
  OR User Agent: contains "nmap"
  OR User Agent: contains "burp"

Then...
  Block
```

---

## 🎨 Dashboard de Seguridad (Futuro Frontend)

### Página `/admin/security-dashboard`

**Componentes a implementar (OPCIONAL):**

1. **Mapa de Intentos Bloqueados**

   - Mapa mundial con puntos rojos indicando origen de ataques
   - Requiere: CloudFlare Analytics API

2. **Lista de IPs Bloqueadas**

   - Tabla con IPs, país, número de intentos, última fecha
   - Acciones: Agregar a whitelist, Bloquear permanentemente

3. **Configuración de Lista Negra**
   - Interfaz para agregar/quitar países de la lista negra
   - Requiere: Backend endpoint + CloudFlare API

**Endpoints necesarios (si se implementa):**

```
GET  /api/security/blocked-attempts       - Obtener intentos bloqueados
POST /api/security/blacklist/countries    - Agregar país a lista negra
DELETE /api/security/blacklist/countries  - Quitar país de lista negra
```

---

## 🧪 Testing del Bloqueo

### 1. Usar VPN para Simular

1. Instalar VPN (ProtonVPN, NordVPN, TunnelBear)
2. Conectarse a un país bloqueado (ej: China, Rusia)
3. Intentar acceder a tu dominio
4. Deberías ver: **Error 403 Forbidden**

### 2. Verificar con Herramientas Online

- https://www.whatismyip.com/ - Ver tu IP y país
- https://tools.keycdn.com/geo - Verificar geolocalización de tu IP

### 3. Revisar Logs en CloudFlare

1. Ir a **Security** → **Events**
2. Deberías ver el intento bloqueado con:
   - IP de origen
   - País: CN/RU/etc.
   - Action: Block
   - Rule: Block High-Risk Countries

---

## 📝 Países de Alto Riesgo Sugeridos

### Tier 1 - Bloqueo Obligatorio

- 🇨🇳 China (CN) - Mayor fuente de ataques DDoS
- 🇷🇺 Rusia (RU) - Ransomware, phishing
- 🇰🇵 Corea del Norte (KP) - Ataques patrocinados por estado
- 🇮🇷 Irán (IR) - Ataques patrocinados por estado

### Tier 2 - Bloqueo Recomendado

- 🇻🇳 Vietnam (VN) - Spam, phishing
- 🇮🇩 Indonesia (ID) - Bots, scraping
- 🇵🇰 Pakistán (PK) - Brute force attacks
- 🇧🇩 Bangladesh (BD) - Click fraud
- 🇺🇦 Ucrania (UA) - Hosting de malware
- 🇧🇾 Bielorrusia (BY) - Ataques patrocinados

### Tier 3 - Considerar según Análisis

- 🇧🇷 Brasil (BR) - Alto volumen de bots (pero también clientes legítimos)
- 🇮🇳 India (IN) - Spam (pero también muchos clientes legítimos)
- 🇹🇷 Turquía (TR) - Phishing

**Nota:** Para aplicación de facturación en Ecuador, es seguro bloquear Tier 1 y Tier 2. Es muy improbable tener clientes legítimos de esos países.

---

## 💰 Costos CloudFlare

### Plan Free ($0/mes)

- ✅ 5 Firewall Rules
- ✅ DDoS protection básica
- ✅ SSL/TLS gratis
- ✅ Suficiente para bloqueo geográfico básico

### Plan Pro ($20/mes)

- ✅ 20 Firewall Rules
- ✅ WAF (Web Application Firewall)
- ✅ Rate Limiting avanzado
- ✅ Analytics detallados

### Plan Business ($200/mes)

- ✅ 100 Firewall Rules
- ✅ Managed WAF Rulesets
- ✅ Bot Management
- ✅ Log retention extendido

**Recomendación para MVP:** Plan Free es suficiente

---

## ✅ Checklist de Implementación

- [ ] Agregar dominio a CloudFlare
- [ ] Actualizar nameservers
- [ ] Activar proxy (nube naranja)
- [ ] Crear regla de bloqueo por país
- [ ] Probar con VPN
- [ ] Activar Bot Fight Mode
- [ ] Configurar Rate Limiting para /login
- [ ] Activar Browser Integrity Check
- [ ] Revisar logs semanalmente
- [ ] (Opcional) Implementar dashboard de seguridad

---

## 🚨 Alternativa: Bloqueo en Backend (NO Recomendado)

Si por alguna razón NO puedes usar CloudFlare:

```java
// IpGeolocationFilter.java (Spring Boot)
@Component
public class IpGeolocationFilter implements Filter {
    private static final List<String> BLOCKED_COUNTRIES =
        Arrays.asList("CN", "RU", "KP", "IR");

    @Autowired
    private GeoLocationService geoService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String clientIp = getClientIp(httpRequest);
        String country = geoService.getCountryCode(clientIp);

        if (BLOCKED_COUNTRIES.contains(country)) {
            ((HttpServletResponse) response).setStatus(403);
            return;
        }

        chain.doFilter(request, response);
    }
}
```

**Desventajas:**

- ❌ Usa recursos del servidor
- ❌ Requiere librería de geolocalización (MaxMind GeoIP2)
- ❌ Costo de API de geolocalización
- ❌ Latencia adicional en cada petición

---

**Recomendación Final:** 🎯 **Usar CloudFlare WAF** - Es gratis, efectivo, y no requiere cambios en el código.
