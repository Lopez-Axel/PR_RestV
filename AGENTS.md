# AGENTS.md

Auth API REST (JWT + refresh + OTP por email) en Spring Boot 4.1.1 / Java 21 / Gradle 9.5.1. Paquete raíz: `com.example.demo_pam`.

## Comandos (PowerShell)

```powershell
.\gradlew.bat bootRun          # corre en :8080
.\gradlew.bat test             # todos los tests
.\gradlew.bat test --tests "com.example.demo_pam.DemoPamApplicationTests"   # un test
.\gradlew.bat compileJava      # verificación rápida de compilación
```

## Prerrequisitos y gotchas

- Requiere PostgreSQL local en `localhost:5432`, base `PR_RestV` (credenciales en `src/main/resources/application.yaml`). Sin DB levantada, `bootRun` y los tests fallan al arrancar.
- Hibernate maneja el esquema con `ddl-auto: update` para las entidades auth (User, RefreshToken, OtpVerification). EXCEPCIÓN: `mesa`/`silla` + la función/triggers `fn_sincronizar_sillas()` se versionan con **Flyway** en `src/main/resources/db/migration/` (Flyway corre ANTES que Hibernate al arrancar). No escribir migraciones SQL para tablas gestionadas por Hibernate.
- Flyway para PG requiere el módulo `flyway-database-postgresql` además de `flyway-core` (ambos sin versión, el BOM de Boot la aporta).
- **jjwt NO está en `build.gradle`**: agregarlo antes de crear `security/JwtService` (`io.jsonwebtoken:jjwt-api` + `jjwt-impl` + `jjwt-jackson`, runtime).
- Spring Boot 4 usa nombres nuevos de starters: `spring-boot-starter-webmvc` (no `-web`) y starters de test como `spring-boot-starter-data-jpa-test`. No reintroducir los nombres viejos.
- Lombok está activo (`@Getter`/`@Setter` en entidades); requiere annotation processor, ya configurado.
- Con `spring-boot-starter-security` y sin `SecurityConfig`, TODO endpoint pide auth por defecto; `/hello`, `/hola` y Swagger fallarán hasta crear la cadena de filtros.
- El código OTP se imprime en consola si no hay SMTP configurado (bloque `spring.mail` comentado en application.yaml).
- `bootRun` inyecta automáticamente las variables del `.env` de la raíz (configurado en `build.gradle`); `.env` está en `.gitignore`.
- Los endpoints `/api/**` (mesas, sillas) exigen JWT (`anyRequest().authenticated()`).
- Swagger UI vía springdoc 3.x: `/swagger-ui.html`.
- Hibernate genera check constraints nombradas `<tabla>_<columna>_check` al crear tablas con `@Enumerated(EnumType.STRING)`; `ddl-auto: update` NO las altera si cambia el enum, hay que dropearlas a mano en la DB.

## Estado del código

Auth completo (entity → repository → dto → security → config → service → exception → controller). Módulo de mesas/sillas con triggers gestionado por Flyway (`V1__mesa_y_silla.sql`). Las sillas se crean SOLO por trigger de DB, nunca por endpoint.

## Reglas obligatorias del repo

- Solo `@RestController` + DTOs JSON. Prohibido `@Controller` con vistas, Thymeleaf o JSP.
- Comentarios: ÚNICAMENTE una línea explicando cada `@Override` (por qué Spring lo invoca) y cada anotación no trivial (`@PrePersist`, `@PreUpdate`, `@Transactional`, `@Valid`, etc.). Ningún otro comentario.
- Los DTOs de response nunca exponen `passwordHash` ni entidades JPA directamente.
- Repositorios: solo métodos derivados (`findByEmail`, `findByTokenHash`...); `@Query` solo si un derivado no alcanza.
- Enums de User internos con `@Enumerated(EnumType.STRING)`.
- `@Transactional` en métodos de servicio que escriben en más de una tabla.
- Máximo 500 líneas por archivo.
- No agregar tests ni documentación salvo petición explícita.
