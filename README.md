# ms-productos-ab

Microservicio CRUD de productos: Spring Boot 4.1 · Java 21 · PostgreSQL · Swagger (springdoc).

## Ejecutar

La tabla se crea con [productos.sql](productos.sql). Las credenciales se pasan por variables de entorno:

```powershell
$env:DB_URL="jdbc:postgresql://<host>.oregon-postgres.render.com:5432/bd_postgresql_productos?sslmode=require"
$env:DB_USERNAME="bd_postgresql_productos_user"
$env:DB_PASSWORD="<password>"
.\mvnw spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Pruebas manuales: [productos.http](productos.http)

## Endpoints

| Método | Ruta | Respuesta |
|---|---|---|
| GET | `/api/productos?categoria=` | 200 lista |
| GET | `/api/productos/{id}` | 200 / 404 |
| POST | `/api/productos` | 201 + Location / 400 / 409 |
| PUT | `/api/productos/{id}` | 200 / 400 / 404 / 409 |
| DELETE | `/api/productos/{id}` | 204 / 404 |

Los errores se devuelven como `application/problem+json` (RFC 9457).

## Tests

```powershell
.\mvnw test
```

Los tests usan H2 en memoria (modo PostgreSQL), no tocan la BD real.

## CI/CD (GitHub Actions + Render)

### Ramas

```
feature/*  ──PR──►  dev  ──PR──►  main
                     │              │
                     ▼              ▼
            Render (dev)    Render (producción)

hotfix/*   ──PR──►  main   (y luego merge de main a dev)
```

| Rama | Tests | Docker build | Deploy Render |
|---|---|---|---|
| `feature/*` | ✅ | — | — |
| `dev` | ✅ | ✅ | ✅ `ms-productos-ab-dev` |
| `hotfix/*` | ✅ | ✅ | — |
| `main` | ✅ | ✅ | ✅ `ms-productos-ab` (producción) |

Ambos deploys son automáticos y solo ocurren si todos los checks pasan.

En los PR, el job **Política de ramas** rechaza integraciones fuera del flujo:
a `main` solo desde `dev` o `hotfix/*`; a `dev` solo desde `feature/*`, `hotfix/*` o `main`.

```
push a main ──► GitHub Actions: build + 35 tests ──► docker build
                                                  │
                                    todos los checks en verde
                                                  ▼
                   Render (autoDeployTrigger: checksPass) ──► build Dockerfile ──► deploy
                                                  │
                                    health check /actuator/health
```

### Protección de ramas (GitHub > Settings > Branches)

Para `main` y `dev`: *Require a pull request before merging* y *Require status checks to pass*
marcando **Build y tests** y **Política de ramas** (y **Build imagen Docker** en `main`).

| Archivo | Función |
|---|---|
| [.github/workflows/ci-cd.yml](.github/workflows/ci-cd.yml) | CI: tests + reportes + build de la imagen Docker |
| [Dockerfile](Dockerfile) | Imagen multi-stage Java 21 (Render no tiene runtime Java nativo) |
| [render.yaml](render.yaml) | Blueprint de Render: web service Docker, plan free, región oregon |

### Configuración inicial (una vez)

1. Subir el repo a GitHub con las ramas `main` y `dev`.
2. En Render: **New > Blueprint** → elegir el repositorio. Render lee `render.yaml` y crea
   los dos servicios (`ms-productos-ab` y `ms-productos-ab-dev`).
3. Completar las variables de cada servicio (usar el **host interno** de la BD, misma región):
   - `DB_URL` = `jdbc:postgresql://<hostname-interno>:5432/<base>`
   - `DB_USERNAME`, `DB_PASSWORD`

   Se recomienda que `dev` use una BD distinta para no mezclar datos de prueba con producción.
4. Desde ahí, cada push a `main` o `dev` con los checks en verde se despliega solo en su ambiente.
   Si un test falla, Render no despliega.

Swagger:
- Producción: `https://ms-productos-ab.onrender.com/swagger-ui.html`
- Desarrollo: `https://ms-productos-ab-dev.onrender.com/swagger-ui.html`

(La URL exacta la asigna Render; puede llevar un sufijo si el nombre ya está tomado.)

| Tipo | Clase | Qué prueba |
|---|---|---|
| Unitario | `ProductoServiceTest` | Lógica del servicio con el repositorio mockeado (Mockito) |
| Unitario web | `ProductoControllerTest` | Controlador con `@WebMvcTest`: status, JSON, validaciones, errores |
| Persistencia | `ProductoRepositoryTest` | Consultas JPA con `@DataJpaTest` |
| Integración | `ProductoIntegrationTest` | Contexto completo sin mocks: flujo CRUD de punta a punta |
| Servicios (E2E) | `ProductoApiServiceTest` | Servidor HTTP real en puerto aleatorio + Swagger/OpenAPI disponibles |
