# TableGo API

API REST para la gestión de restaurantes, menús, mesas, usuarios y reservas. Incluye autenticación con JWT y permisos según el rol del usuario (administrador, recepcionista y cliente).

## Requisitos

- JDK 21
- MySQL en ejecución
- No hace falta instalar Maven: el proyecto incluye Maven Wrapper.

## Configuración

La configuración local está en `src/main/resources/application.properties`. Por defecto, la aplicación se conecta a `jdbc:mysql://localhost:3306/tablego` y crea la base de datos si no existe. Asegúrate de que el usuario configurado tenga permisos para conectarse y crearla.

Puedes configurar las credenciales de MySQL y el secreto JWT mediante variables de entorno antes de arrancar. Por ejemplo, en PowerShell:

```powershell
$env:DB_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "tu-clave-de-MySQL"
$env:JWT_SECRET = "un-secreto-local-de-al-menos-32-caracteres"
```

No uses secretos de desarrollo en producción. La inicialización de usuarios de prueba está activada por defecto (`app.seed.enabled=true`); desactívala en entornos de producción.

## Arrancar

Desde la raíz del proyecto, en Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

En macOS o Linux:

```bash
./mvnw spring-boot:run
```

Al iniciar correctamente, la API queda disponible en `http://localhost:8080`.

## Comprobar la API

El endpoint público de prueba no requiere autenticación:

```text
GET http://localhost:8080/api/public/ping
```

Para obtener un token, registra un usuario o inicia sesión mediante `POST /api/auth/register` o `POST /api/auth/login`. La mayoría de los demás endpoints requieren enviar el token JWT en el encabezado:

```text
Authorization: Bearer <token>
```

Las rutas principales incluyen autenticación, consulta de restaurantes y productos, reservas y operaciones administrativas para gestionar restaurantes, mesas, productos y usuarios.

## Pruebas

En Windows:

```powershell
.\mvnw.cmd test
```

En macOS o Linux:

```bash
./mvnw test
```
