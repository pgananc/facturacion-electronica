# 🤝 Guía de Contribución

Gracias por tu interés en contribuir a `ebill-authentication`. Este documento contiene las guías y mejores prácticas para el desarrollo.

---

## 📋 Tabla de contenidos

- [Código de Conducta](#-código-de-conducta)
- [Proceso de desarrollo](#-proceso-de-desarrollo)
- [Estándares de código](#-estándares-de-código)
- [Commits](#-commits)
- [Pull Requests](#-pull-requests)
- [Testing](#-testing)

---

## 🎯 Código de Conducta

- Sé respetuoso y profesional
- Ayuda a otros desarrolladores
- Escribe código limpio y mantenible
- Documenta tus cambios

---

## 🔄 Proceso de desarrollo

### 1. Crear rama

```bash
# Actualizar develop
git checkout develop
git pull origin develop

# Crear rama feature
git checkout -b feature/nombre-descriptivo

# O rama hotfix
git checkout -b hotfix/nombre-bug
```

### 2. Desarrollar

```bash
# Hacer cambios
# ...

# Compilar y verificar
mvn clean compile
mvn fmt:format

# Ejecutar tests
mvn test
```

### 3. Commit

```bash
git add .
git commit -m "feat: descripción del cambio"
```

### 4. Push y Merge Request

```bash
git push origin feature/nombre-descriptivo

# Crear Merge Request en GitLab
# Asignar reviewers
# Esperar aprobación
```

---

## 📝 Estándares de código

### Java Style Guide

Usamos **Google Java Style Guide**

#### Formato automático

```bash
# Formatear código
mvn fmt:format

# Verificar formato
mvn fmt:check
```

#### Reglas principales

```java
// ✅ Correcto
public class UserService {
    private final UserRepository userRepository;
    
    public UserDto findById(Long id) {
        return userRepository.findById(id)
            .map(userMapper::toDto)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }
}

// ❌ Incorrecto
public class userService {
  private UserRepository userRepository;
  
  public UserDto findById(Long id) 
  {
    return userRepository.findById(id).map(userMapper::toDto).orElseThrow(() -> new NotFoundException("User not found"));
  }
}
```

### Nomenclatura

| Tipo | Convención | Ejemplo |
|------|------------|---------|
| Clase | PascalCase | `UserService` |
| Método | camelCase | `findUserById` |
| Variable | camelCase | `userName` |
| Constante | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Paquete | lowercase | `com.wallparisoft.authentication` |

### Anotaciones

```java
// ✅ Orden correcto de anotaciones
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    // ...
}

// ✅ Anotaciones de validación
public void createUser(@Valid @NotNull UserDto userDto) {
    // ...
}
```

---

## 💬 Commits

Usamos **Conventional Commits**

### Formato

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

### Tipos

| Tipo | Descripción | Ejemplo |
|------|-------------|---------|
| `feat` | Nueva funcionalidad | `feat(auth): add JWT refresh token` |
| `fix` | Corrección de bug | `fix(user): resolve null pointer in findById` |
| `refactor` | Refactorización | `refactor(service): simplify user validation` |
| `docs` | Documentación | `docs: update README with setup instructions` |
| `test` | Tests | `test(user): add unit tests for UserService` |
| `chore` | Mantenimiento | `chore: upgrade Spring Boot to 3.4.5` |
| `style` | Formato | `style: apply Google Java Style` |
| `perf` | Performance | `perf(query): optimize user search query` |

### Ejemplos

```bash
# Feature
git commit -m "feat(auth): implement password recovery"

# Bug fix
git commit -m "fix(mapper): correct role mapping in UserMapper"

# Refactor
git commit -m "refactor(controller): extract common logic to base class"

# Breaking change
git commit -m "feat(api)!: change response format for user endpoints

BREAKING CHANGE: User API now returns camelCase instead of snake_case"
```

---

## 🔀 Pull Requests

### Checklist antes de crear PR

- [ ] Código compila sin errores
- [ ] Tests pasan (`mvn test`)
- [ ] Código formateado (`mvn fmt:format`)
- [ ] Documentación actualizada
- [ ] Sin warnings de linter
- [ ] Commits siguen Conventional Commits
- [ ] Rama actualizada con develop

### Plantilla de PR

```markdown
## 📝 Descripción

Breve descripción del cambio

## 🎯 Tipo de cambio

- [ ] Bug fix
- [ ] Nueva funcionalidad
- [ ] Breaking change
- [ ] Documentación

## ✅ Checklist

- [ ] Tests agregados/actualizados
- [ ] Documentación actualizada
- [ ] Sin warnings
- [ ] Revisión de código realizada

## 🖼️ Screenshots (si aplica)

## 📚 Contexto adicional
```

---

## 🧪 Testing

### Estructura de tests

```
src/test/java/
└── com/wallparisoft/authentication/
    ├── unit/           # Tests unitarios
    ├── integration/    # Tests de integración
    └── e2e/            # Tests end-to-end
```

### Ejemplo de test unitario

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    @DisplayName("Should find user by id successfully")
    void shouldFindUserById() {
        // Given
        Long userId = 1L;
        User user = User.builder().idUser(userId).name("Test").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        // When
        User result = userService.findById(userId);
        
        // Then
        assertNotNull(result);
        assertEquals(userId, result.getIdUser());
        verify(userRepository).findById(userId);
    }
}
```

### Ejecutar tests

```bash
# Todos los tests
mvn test

# Tests específicos
mvn test -Dtest=UserServiceTest

# Con coverage
mvn jacoco:report
```

---

## 🔍 Code Review

### Para el autor del PR

- Haz PRs pequeños y enfocados
- Describe bien los cambios
- Responde a comentarios constructivamente
- No tomes críticas personal

### Para el revisor

- Sé constructivo y específico
- Sugiere alternativas
- Aprueba si está listo
- Solicita cambios si es necesario

---

## 🚀 Release Process

1. Merge features a `develop`
2. Crear `release/x.y.z` desde `develop`
3. Tests y ajustes finales
4. Merge a `main`
5. Tag `vx.y.z`
6. Merge `main` a `develop`

---

## 📚 Recursos

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Spring Boot Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Git Flow](https://nvie.com/posts/a-successful-git-branching-model/)

---

## ❓ Preguntas

Si tienes dudas, contacta al equipo de desarrollo o crea un issue en GitLab.

---

**¡Gracias por contribuir! 🎉**


