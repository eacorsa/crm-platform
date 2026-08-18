# CRM Clientes

Aplicación CRM para administrar contactos, empresas, oportunidades y tareas. Incluye una interfaz web construida con React y una API REST protegida con JWT, desarrollada con Spring Boot.

## Tecnologías

| Capa | Tecnologías |
| --- | --- |
| Frontend | React 18, TypeScript, Vite, React Router y Axios |
| Backend | Java 17, Spring Boot 3, Spring Security, Spring Data JPA y JWT |
| Base de datos | MySQL 8 |

## Funcionalidades

- Inicio de sesión con JWT.
- Gestión de contactos y empresas.
- Seguimiento de oportunidades de venta.
- Creación y control de tareas.
- Aislamiento de datos por usuario autenticado.
- Validación de solicitudes, CORS y encabezados de seguridad.

## Estructura del proyecto

```text
crm-spring/
├── backend/                 # API REST de Spring Boot
│   └── src/main/resources/  # Configuración de la aplicación
├── frontend/                # Cliente React + Vite
├── docs/                    # Diagramas de arquitectura y entidad-relación
└── README.md
```

Los diagramas disponibles son [arquitectura](docs/arquitectura.svg) y [modelo entidad-relación](docs/entidad-relacion.svg).

## Requisitos

- Java 17 o posterior.
- Maven 3.9 o posterior.
- Node.js 18 o posterior (incluye npm).
- MySQL 8 o posterior.

## Configuración local

1. Cree una base de datos en MySQL:

   ```sql
   CREATE DATABASE cec_crm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Configure la conexión de MySQL en `backend/src/main/resources/application.yml` con sus credenciales locales. Verifique especialmente `spring.datasource.url`, `username` y `password`.

3. Use una clave JWT propia y segura en `app.jwt.secret` antes de desplegar el sistema. No publique claves ni contraseñas en el repositorio.

> La aplicación usa `ddl-auto: update` en desarrollo, por lo que Spring crea o actualiza las tablas a partir de las entidades al iniciar.

## Ejecutar el proyecto

Abra dos terminales desde la raíz del repositorio.

### Backend

```powershell
cd backend
mvn spring-boot:run
```

La API quedará disponible en `http://localhost:8080/api`.

En Windows también puede ejecutar `backend\\start.cmd` si ajusta `JAVA_HOME` dentro del archivo a la instalación de Java de su equipo.

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173` en el navegador. Durante el desarrollo, Vite redirige las solicitudes de `/api` al backend local.

Para generar la versión de producción:

```powershell
cd frontend
npm run build
```

Los archivos generados se guardan en `frontend/dist`.

## Variables del frontend

Por defecto, el cliente utiliza `http://localhost:8080/api`. Para usar otra API, cree `frontend/.env.local`:

```dotenv
VITE_API_URL=https://api.ejemplo.com/api
```

El archivo está ignorado por Git; use un archivo `.env.example` sin secretos si necesita compartir una plantilla.

## API

Todas las rutas, salvo el inicio de sesión, requieren el encabezado:

```http
Authorization: Bearer <token>
```

| Método | Ruta | Descripción |
| --- | --- | --- |
| POST | `/api/auth/login` | Autentica un usuario y devuelve un JWT. |
| GET, POST | `/api/contacts` | Consulta o crea contactos. |
| PUT, DELETE | `/api/contacts/{id}` | Actualiza o elimina un contacto. |
| GET, POST | `/api/companies` | Consulta o crea empresas. |
| PUT, DELETE | `/api/companies/{id}` | Actualiza o elimina una empresa. |
| GET, POST | `/api/deals` | Consulta o crea oportunidades. |
| PUT, DELETE | `/api/deals/{id}` | Actualiza o elimina una oportunidad. |
| GET, POST | `/api/tasks` | Consulta o crea tareas. |
| PUT, DELETE | `/api/tasks/{id}` | Actualiza o elimina una tarea. |

Ejemplo de autenticación:

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "usuario@ejemplo.com",
  "password": "una-contrasena-de-al-menos-8-caracteres"
}
```

## Seguridad

- Las contraseñas se almacenan usando BCrypt.
- Las sesiones son sin estado y se validan mediante JWT.
- Las rutas de negocio requieren autenticación.
- El repositorio ignora archivos de entorno, configuraciones locales y artefactos generados. Consulte [`.gitignore`](.gitignore).

## Licencia

Este proyecto no incluye una licencia explícita. Agregue una antes de distribuirlo públicamente.
