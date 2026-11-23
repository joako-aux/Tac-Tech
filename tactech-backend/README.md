# tactech-backend

Microservicio de persistencia remota de **TacTech**. Centraliza en **MySQL** lo que la app
Android guarda offline (Room) y modela el flujo de los tres roles:

1. El **cliente** crea solicitudes, que nacen en `POR_CONFIRMAR`, y puede cancelarlas mientras
   sigan así.
2. El **administrador**:
   - gestiona usuarios y empresas;
   - **confirma** cada solicitud, creando el ticket asignado a un técnico, o la **rechaza**;
   - supervisa el estado de todas las tareas.
3. El **técnico** atiende sus tickets y sube el reporte con **foto y geolocalización**, ambas
   obligatorias.

Asignatura DSY1105 · Integrantes: Moise Fonseca, Joaquin Perez, Carlos Lobos.

## Stack

| Elemento | Uso |
|---|---|
| Java 17 + Spring Boot 3.3 | Base del servicio |
| Gradle | Dependencias y ejecución |
| Spring Web | Controladores REST |
| Spring Data JPA + MySQL | Persistencia (`com.mysql.cj.jdbc.Driver`) |
| Lombok | `@Data`, `@Builder`, `@RequiredArgsConstructor`… |
| Spring Validation | `@NotBlank`, `@Size`, `@Pattern`, `@Email` en los DTO |
| Spring WebFlux / WebClient | Cliente HTTP reactivo (`/api/salud/verificar`) |
| spring-security-crypto | Hash BCrypt de las claves |

Toda la configuración está en `src/main/resources/application.yml` (no se usan `.properties`).

## Estructura

```
src/main/java/com/example/tactech_backend/
├── entity/        Tablas JPA: Empresa, Usuario, Solicitud, Equipo, Ticket, Guia, FotoGuia + enums
├── repository/    Interfaces JpaRepository (consultas derivadas del nombre del método)
├── service/       Lógica de negocio (reglas, upsert, mapeo entidad <-> DTO)
├── controller/    Endpoints REST
├── dto/           Objetos de entrada/salida validados
├── exception/     Excepciones propias + GlobalExceptionHandler (JSON de error único)
├── validation/    Anotación propia @RutValido (dígito verificador del RUT)
└── config/        BCrypt, WebClient y usuarios de prueba
```

Flujo de una petición: **controller → service → repository → MySQL**. El controlador no tiene
lógica: valida el DTO con `@Valid` y delega en el servicio.

## Cómo ejecutar (IntelliJ IDEA)

1. Tener MySQL corriendo en `localhost:3306` (XAMPP, MySQL Server o Workbench).
2. Crear la base una vez: ejecutar `database/crear_base_de_datos.sql`
   (`CREATE DATABASE tactech_db;`). Las tablas las crea Hibernate (`ddl-auto: update`).
3. Si tu usuario `root` tiene clave, defínela en la variable de entorno `DB_PASSWORD` (o cambia el valor por defecto de `password: ${DB_PASSWORD:}` en `application.yml`).
4. Abrir la carpeta `tactech-backend` en IntelliJ → *Open* → esperar que Gradle sincronice.
   Usar JDK 17 (*File → Project Structure → SDK*).
5. Ejecutar `TactechBackendApplication` (botón ▶) o `./gradlew bootRun`.
6. Probar en el navegador: <http://localhost:8080/api/salud>.

Al primer arranque se crean los usuarios de prueba:

| Correo | Clave | Rol |
|---|---|---|
| admin@tactech.cl | admin123 | ADMINISTRADOR |
| tecnico@tactech.cl | tecnico123 | TECNICO |
| tecnico2@tactech.cl | tecnico123 | TECNICO |
| cliente@tactech.cl | cliente123 | CLIENTE (empresa Clinica Las Condes, RUT 76123456-0) |

Pruebas unitarias: `./gradlew test` (reglas de `TicketService` con Mockito, sin base de datos).
Peticiones de ejemplo: `docs/pruebas.http`.

## Despliegue en AWS (Learner Lab)

El servicio corre en una instancia **EC2** del AWS Academy Learner Lab, junto con su MySQL, y
la app lo consume en `http://44.215.178.124:8080/`.

| Elemento | Valor |
|---|---|
| Instancia | `tactech-backend` · Ubuntu Server 24.04 · t3.small · us-east-1 |
| IP fija | Elastic IP `44.215.178.124` (no cambia al apagar y encender) |
| Grupo de seguridad | `tactech-sg`: 8080 abierto a internet (la app); 22 (SSH) solo desde la IP del administrador; 3306 cerrado |
| Base de datos | MySQL 8 en la misma máquina, base `tactech_db`, usuario `tactech` (solo `localhost`) |
| Ejecución | Servicio `systemd` `tactech.service`: arranca solo al encender la instancia y se reinicia si falla |
| Configuración | Variables en `/etc/tactech.env` (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `SHOW_SQL`), leídas por `application.yml` |

`application.yml` usa variables de entorno con valores por defecto
(`${DB_USER:root}`): en local funciona igual que antes (root sin clave) y en el servidor toma
los valores de `/etc/tactech.env`. La clave del servidor no queda en el repositorio.

**Uso diario:** Learner Lab → *Start Lab* (la instancia, MySQL y el backend encienden solos) →
probar <http://44.215.178.124:8080/api/salud> → al terminar, *End Lab*.

**Actualizar el backend:**

```bash
# En el PC (carpeta tactech-backend)
./gradlew clean bootJar
scp -i labsuser.pem build/libs/tactech-backend-0.0.1-SNAPSHOT.jar ubuntu@44.215.178.124:/home/ubuntu/app.jar

# En el servidor (ssh -i labsuser.pem ubuntu@44.215.178.124)
sudo mv ~/app.jar /opt/tactech/app.jar
sudo systemctl restart tactech
sudo journalctl -u tactech -n 50        # revisar el arranque
```

**Ver los datos guardados:** `mysql -u tactech -p tactech_db -e "SELECT titulo, estado FROM solicitudes;"`

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/salud` | Estado del servicio |
| GET | `/api/salud/verificar` | Auto-verificación con WebClient |
| POST | `/api/auth/login` | Login → usuario y rol (401 si falla) |
| GET/POST | `/api/usuarios?rol=` | Listar (filtro opcional por rol) / crear usuarios |
| GET/PUT/DELETE | `/api/usuarios/{id}` | Ver / actualizar / eliminar |
| GET/POST | `/api/empresas` | Listar / crear empresas (RUT validado) |
| GET/PUT/DELETE | `/api/empresas/{id}` | Ver / actualizar / eliminar |
| GET | `/api/solicitudes?estado=&cliente=` | Listar solicitudes |
| POST | `/api/solicitudes` | El cliente crea una solicitud (`POR_CONFIRMAR`) |
| PUT | `/api/solicitudes/{id}` | Upsert desde la app del cliente (409 si ya fue procesada) |
| PUT | `/api/solicitudes/{id}/cancelar` | El cliente la anula (solo `POR_CONFIRMAR`) |
| PUT | `/api/solicitudes/{id}/rechazar` | El administrador la rechaza con motivo |
| POST | `/api/solicitudes/{id}/confirmar` | El administrador crea el ticket y lo asigna a un técnico |
| GET/POST | `/api/equipos` | Listar / crear |
| GET/PUT/DELETE | `/api/equipos/{id}` | Ver / crear-o-actualizar / eliminar |
| GET | `/api/tickets?estado=&tecnico=&cliente=` | Listar; `estado` = `PENDIENTES`, `TERMINADOS` o un estado |
| GET | `/api/tickets/resumen` | Totales para el panel del administrador |
| POST | `/api/tickets` | Crear |
| GET/PUT/DELETE | `/api/tickets/{id}` | Ver / crear-o-actualizar / eliminar |
| PUT | `/api/tickets/{id}/estado` | Cambiar estado con reglas de negocio |
| GET | `/api/guias?ticketId=&tecnico=` | Listar reportes (guías) |
| POST | `/api/guias` | Crear |
| GET/PUT/DELETE | `/api/guias/{id}` | Ver / crear-o-actualizar / eliminar |

**Por qué PUT `/{id}` para sincronizar:** el id (UUID) lo genera el teléfono cuando trabaja sin
conexión. Si la app reenvía el mismo registro (por ejemplo, se cortó la red justo después de
guardar), el servidor lo actualiza en vez de duplicarlo: la operación es idempotente.

## Códigos de respuesta

| Código | Cuándo |
|---|---|
| 200 / 201 / 204 | Correcto |
| 400 | Falla una validación: el JSON trae `errores` campo por campo |
| 401 | Credenciales inválidas |
| 404 | No existe el recurso |
| 409 | Lo impide una regla de negocio (cerrar sin reportes, solicitud ya procesada, correo o RUT repetido, cliente sin empresa) |

## Reglas de negocio principales

- Toda solicitud nace `POR_CONFIRMAR`. Una vez confirmada, rechazada o cancelada, ya no cambia.
- Al confirmar, en **una sola transacción**:
  - se registra el equipo (o se reutiliza por número de serie);
  - se crea el ticket con número correlativo asignado por el servidor, estado `ABIERTO` y el
    técnico elegido, que debe existir y estar activo;
  - la solicitud pasa a `CONFIRMADA`.
- Un usuario `CLIENTE` debe pertenecer a una empresa. Los demás roles no tienen empresa.
- No se elimina un usuario con tickets o solicitudes (se sugiere desactivarlo), ni una empresa
  con clientes.
- El reporte del técnico exige entre 1 y 15 fotos y latitud/longitud.
- Un ticket se cierra solo si tiene al menos un reporte.
