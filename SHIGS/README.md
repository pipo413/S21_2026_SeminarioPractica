# SIGHS — TP3 (Login)

Prototipo Java SE 17 del login del Sistema de Gestion de Higiene y Seguridad (SIGHS).

## Estructura

```
Tp3/
├── pom.xml                                          <- Maven, Java 17, mysql-connector-j 8.3
├── scripts/
│   └── actualizar-hashes-prueba.sql                <- actualiza password_hash para que pipo14 funcione
└── src/main/java/ar/com/zyasoc/sighs/
    ├── Main.java
    ├── modelo/
    │   ├── Usuario.java                             <- abstract
    │   ├── UsuarioTecnico.java                      <- subclase 1
    │   ├── UsuarioResponsable.java                  <- subclase 2
    │   ├── UsuarioAdministrador.java                <- subclase 3
    │   ├── Rol.java
    │   ├── Bitacora.java
    │   ├── EstadoUsuario.java                       <- enum
    │   └── Permiso.java                             <- enum (para polimorfismo)
    ├── persistencia/
    │   ├── ConexionBD.java
    │   ├── UsuarioDAO.java                          <- JDBC + JOIN a usuario_rol/rol
    │   └── BitacoraDAO.java
    ├── control/
    │   ├── ServicioAutenticacion.java               <- SHA-256 + sal real
    │   ├── ControladorUsuario.java                  <- orquesta CU-USR-01
    │   └── ResultadoAutenticacion.java              <- DTO del resultado
    └── vista/
        ├── VistaLogin.java                          <- Swing JFrame
        ├── VistaMenuPrincipal.java                  <- placeholder (prox. TP3)
        └── CallbackLogin.java
```

## POO — pilares justificados

- **Abstraccion**: `Usuario` es `abstract` con un metodo abstracto `calcularPermisos()`. No se puede instanciar.
- **Encapsulamiento**: todos los atributos son `private`, se accede por getters/setters.
- **Herencia**: `UsuarioTecnico`, `UsuarioResponsable`, `UsuarioAdministrador` extienden `Usuario`.
- **Polimorfismo**: cada subclase redefine `calcularPermisos()` y devuelve una lista distinta de `Permiso`.

## Prerrequisitos

1. **MySQL 8 corriendo** en `localhost:3306`. Levantar con Docker:
   ```powershell
   docker run --name mysql-sighs -e MYSQL_ROOT_PASSWORD=pipo14 -e MYSQL_DATABASE=sighs_db -p 3306:3306 -d mysql:8.0 --default-authentication-plugin=mysql_native_password
   ```

2. **Esquema y semillas de la base `sighs_db`**, en este orden:
   ```powershell
   # 1) DDL: modelo final del TP3, alineado 100% con el codigo Java.
   #    Consolida el DDL del TP2 + las migraciones 02/03/05/06.
   Get-Content '.\SHIGS\sql\01-creacion-de-tablas.sql' | docker exec -i mysql-sighs mysql -uroot -ppipo14

   # 2) Semillas: 5 usuarios demo (contrasena 123456), catalogo de
   #    tipos de riesgo, 3 empresas con sus establecimientos,
   #    sectores, puestos, tareas y 9 visitas. Idempotente.
   Get-Content '.\SHIGS\sql\02-semillas.sql' | docker exec -i mysql-sighs mysql -uroot -ppipo14 sighs_db

   # 3) Consultas del TP2 (referencia, no se ejecuta al levantar
   #    la base; alimenta la trazabilidad CU por CU en la fase 2).
   # Get-Content '.\SHIGS\sql\03-consultas.sql' | docker exec -i mysql-sighs mysql -uroot -ppipo14 sighs_db
   ```

3. **Java 17** + **Maven 3.8+** instalados. Verificar:
   ```powershell
   java --version
   mvn --version
   ```

**Credenciales de prueba** (definidas en `02-semillas.sql`):

| `nombre_usuario` | contrasena | rol |
|---|---|---|
| `tecnico1` | `123456` | Tecnico en HyS |
| `tecnico2` | `123456` | Tecnico en HyS |
| `resp.hys` | `123456` | Responsable del Servicio |
| `admin`    | `123456` | Administrador |
| `demo`     | `123456` | Tecnico (demo) |

## Como correrlo

Desde la carpeta `Tp3/`:

```powershell
mvn clean compile
mvn exec:java -Dexec.mainClass="ar.com.zyasoc.sighs.Main"
```

O empaquetar y correr el JAR:

```powershell
mvn clean package
java -jar target/sighs-1.0.0-SNAPSHOT.jar
```

## Como probarlo

Usuario de prueba (los 4 tienen contrasena `pipo14`):

| nombre_usuario | rol | permisos |
|---|---|---|
| `tecnico1` | Tecnico en HyS | Cargar ARPT, Controlar EPP, Gestionar planes |
| `tecnico2` | Tecnico en HyS | igual |
| `resp.hys` | Responsable del Servicio | los anteriores + Enviar informe mensual |
| `admin` | Administrador | todos los anteriores + Administrar usuarios y catalogos |

**Casos de prueba del CU-USR-01** (cubiertos por `ServicioAutenticacion`):

1. Login exitoso: `tecnico1` / `pipo14` -> mensaje verde "Bienvenido...", abre menu con 3 permisos.
2. Login fallido: `tecnico1` / `xxx` -> "Usuario o contrasena invalidos".
3. Tres fallidos seguidos: tras el tercero aparece "Cuenta bloqueada", el usuario pasa a `inactivo`.
4. Login de bloqueado: `tecnico1` / `pipo14` (ya inactivo) -> "Usuario inactivo...".
5. Login de inexistente: `noexiste` / `xxx` -> "Usuario o contrasena invalidos".

## Variables de entorno opcionales

Para apuntar a otra base:

| Variable | Default |
|---|---|
| `SIGHS_DB_HOST` | `127.0.0.1` |
| `SIGHS_DB_PORT` | `3306` |
| `SIGHS_DB_NAME` | `sighs_db` |
| `SIGHS_DB_USER` | `root` |
| `SIGHS_DB_PASS` | `pipo14` |