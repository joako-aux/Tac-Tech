# TacTech — Gestión de Tickets y Guías de Servicio

Aplicación Android nativa (**Kotlin + Jetpack Compose**) para una empresa de servicio técnico de
equipos de frío, con tres roles. El **cliente** pide servicio. El **administrador** convierte cada
solicitud en un ticket y lo asigna a un técnico. El **técnico** atiende en terreno y registra un
reporte con foto y ubicación, incluso **sin internet**.

La app consume dos servicios por Retrofit:
- el microservicio propio **`tactech-backend`** (Spring Boot + MySQL), desplegado en **AWS EC2**;
- la API externa **Nominatim de OpenStreetMap**, que convierte las coordenadas GPS de la visita
  en una dirección legible.

Asignatura **DSY1105 — Desarrollo de Aplicaciones Móviles** · Evaluación Parcial 4 · Equipo N° [completar]

## Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| `TacTech/` | Aplicación Android (Kotlin + Jetpack Compose + MVVM) |
| `TacTech/release/` | APK firmado (`app-release.apk`) y llave de firma (`.jks`) |
| `tactech-backend/` | Microservicio Spring Boot + MySQL, desplegado en AWS EC2 (ver su README) |

## Historial de versiones

| Versión | Tecnología | Motivo |
|---|---|---|
| v1 | Activities en XML + SQLite (`DatabaseHelper`) | Primer MVP del equipo (commits de octubre) |
| v2 | **Jetpack Compose + MVVM**, Room, Retrofit, DataStore, microservicio propio en AWS | La v1 no cumplía con Jetpack Compose ni con el patrón MVVM exigidos; se migró la app completa a la nueva arquitectura |
| v5.3 (actual) | Pruebas sobre **todo** el proyecto (Room y pantallas Compose con Robolectric) | Cobertura de todo el código sin exclusiones: 273 pruebas, 92,8 % de líneas y 80,2 % de ramas. Acceso al servidor desde la consola web de AWS y comandos para revisar los datos en MySQL |

## Integrantes

| Nombre y apellido | Responsabilidad |
|---|---|
| Moise Fonseca | Base del proyecto, listado y formulario de tickets |
| Joaquin Perez | Detalle del ticket, cambio de estado y guías de servicio |
| Carlos Lobos  | Cámara, ubicación, permisos, animaciones, API externa, pruebas, despliegue y documentación |

- Repositorio (público): [completar enlace de GitHub]
- Planificación: [completar enlace del tablero de GitHub Projects] — autorizado por el docente
  en reemplazo de Trello.

## Funcionalidades

| Rol | Funcionalidad |
|---|---|
| Todos | Login contra el microservicio (BCrypt) con respaldo offline; sesión en DataStore; navegación según el rol |
| Cliente | Crear solicitudes (offline), ver su estado y el avance del ticket, cancelarlas mientras estén "Por confirmar" |
| Administrador | CRUD de usuarios y empresas (RUT validado); confirmar solicitudes creando el ticket y asignando técnico y prioridad, o rechazarlas; panel con filtros, búsqueda y avance por técnico; cancelar tickets |
| Técnico | Ver sus tickets, cambiar el estado, emitir el reporte de visita con **foto** (cámara) y **ubicación** (GPS) obligatorias, ver la **dirección del GPS** (API externa) y cerrar el ticket |
| Sistema | Sincronización offline-first con Room: sube lo pendiente y descarga lo de cada rol cuando hay red; banner de conexión |

## Endpoints utilizados

### Microservicio propio: `tactech-backend`
URL base: `http://44.215.178.124:8080/` (AWS EC2, IP elástica). Definidos en `data/remote/ApiService.kt`.

| Método | Ruta | Uso en la app |
|---|---|---|
| POST | `/api/auth/login` | Iniciar sesión |
| GET / POST | `/api/usuarios?rol=` | Listar (filtro por rol) / crear usuarios |
| PUT / DELETE | `/api/usuarios/{id}` | Editar / eliminar usuario |
| GET / POST | `/api/empresas` | Listar / crear empresas |
| PUT / DELETE | `/api/empresas/{id}` | Editar / eliminar empresa |
| GET | `/api/solicitudes?estado=&cliente=` | Descargar solicitudes (todas o las de un cliente) |
| PUT | `/api/solicitudes/{id}` | Subir (crear o actualizar) una solicitud del cliente |
| PUT | `/api/solicitudes/{id}/rechazar` | Rechazar con motivo |
| POST | `/api/solicitudes/{id}/confirmar` | Confirmar: crea el ticket y lo asigna |
| GET / PUT | `/api/equipos`, `/api/equipos/{id}` | Descargar / subir equipos |
| GET | `/api/tickets?estado=&tecnico=&cliente=` | Descargar tickets según el rol |
| PUT | `/api/tickets/{id}` | Subir cambios de un ticket |
| GET / PUT | `/api/guias?ticketId=&tecnico=`, `/api/guias/{id}` | Descargar / subir reportes de visita |
| GET | `/api/salud` | Comprobar que el servicio está arriba |

### API externa: Nominatim (OpenStreetMap)
Definida en `data/remote/geocodificacion/NominatimApi.kt`, con su propio cliente Retrofit
(`ApiExternaClient`) para no interferir con el microservicio.

| Método | URL | Uso |
|---|---|---|
| GET | `https://nominatim.openstreetmap.org/reverse?lat={lat}&lon={lon}&format=jsonv2&accept-language=es&addressdetails=1` | Geocodificación inversa: coordenadas de la visita → dirección |

- **Dónde se ve:** en el *Reporte de visita*, al registrar la ubicación aparece "Dirección según
  GPS" con el botón *Usar esta dirección en el reporte*. En el *Detalle de la guía*, junto a las
  coordenadas guardadas.
- **No interfiere** con los datos locales ni con el microservicio: solo consulta y muestra.
  El técnico decide si copia la dirección al campo del reporte. Sin internet, las coordenadas
  quedan registradas igual y se puede reintentar.
- Es gratuita y sin API key. Se respeta su política de uso: `User-Agent` propio, una consulta
  por ubicación registrada y atribución "© colaboradores de OpenStreetMap" en la interfaz.

## Roles y flujo de trabajo

```
CLIENTE                    ADMINISTRADOR                         TECNICO
Crea solicitud ──► "Por confirmar" ──► Crea ticket y asigna ──► Ve sus tickets asignados
   │  (offline OK)          │  tecnico + prioridad                 │
   └─ Puede cancelarla      └─ o la rechaza con motivo             ├─ Avanza el estado
      mientras siga                                                ├─ Reporte de visita con
      "Por confirmar"                                              │  FOTO y UBICACION obligatorias
                                                                   └─ Cierra el ticket
        ◄──────────── el avance se ve en los tres paneles (sincronizacion) ────────────►
```

**Cliente**
- Crea solicitudes para sus equipos de frío. Toda solicitud nace **"Por confirmar"**.
- Ve el estado de cada solicitud y, cuando se confirma, el ticket, el técnico asignado y su avance.
- Puede **cancelar** la solicitud por error mientras siga "Por confirmar".

**Administrador (centro de control)**
- **Gestión total de usuarios:** crea, edita, lista y elimina técnicos, clientes y
  administradores. Por ejemplo, da de alta a un nuevo soporte técnico.
- **CRUD de empresas** cliente, con validación del RUT (dígito verificador).
- Bandeja de **solicitudes por confirmar:** con cada una crea el ticket formal, asignando
  técnico y prioridad, o la rechaza con un motivo.
- Panel de respaldo con las tareas **pendientes, en proceso, terminadas y canceladas**, y el
  avance por técnico. Puede cambiar el estado de cualquier ticket y cancelarlo.

**Técnico (soporte en terreno)**
- Recibe en su panel solo los tickets que le asignaron.
- Avanza el estado del ticket y llena el **reporte de visita**, que exige al menos **una
  fotografía** de evidencia (cámara) y la **geolocalización** de la visita (GPS).
- Todo funciona sin internet y se sincroniza al recuperar la señal.

## Arquitectura

```
Pantalla (Compose) ──► ViewModel ──► Repositorio ──► DAO ──► Room (SQLite)
                                          │
                                          ├──► ApiService (Retrofit) ──► tactech-backend (AWS EC2) ──► MySQL
                                          └──► NominatimApi (Retrofit) ──► OpenStreetMap (API externa)
```

- **MVVM:** la pantalla solo pinta el estado (`StateFlow`) y envía eventos al ViewModel.
- **Repositorios:** única puerta a los datos. Ninguna pantalla toca Room ni Retrofit.
- **domain/validation:** reglas en Kotlin puro, probadas con JUnit.

### Estrategia offline-first

1. Toda escritura va a Room con `pendienteSync = true`.
2. `MonitorConexion` escucha la red con `ConnectivityManager.NetworkCallback` y la expone como
   `Flow<Boolean>`.
3. `SincronizacionRepository` combina "hay internet" con "hay pendientes". Cuando ambas se
   cumplen:
   - **sube** solicitudes, equipos, tickets y reportes con `PUT /{id}`;
   - **descarga** solo lo que le corresponde a cada rol: el cliente, sus solicitudes y los
     tickets que nacieron de ellas; el técnico, sus tickets asignados y sus reportes; el
     administrador, todo.
   - **Conflictos:** si el servidor responde 409 (por ejemplo, el cliente canceló offline una
     solicitud que el administrador ya había confirmado), gana el servidor.
   - Las acciones del administrador (crear tickets, usuarios y empresas) van directo al
     servidor, porque deben ser atómicas y únicas.
4. Lo que se descarga nunca pisa un registro que todavía tiene cambios locales sin enviar.
5. Un `Mutex` evita dos sincronizaciones al mismo tiempo.

### Persistencia local

| Qué | Dónde | Por qué |
|---|---|---|
| Solicitudes, tickets, equipos, reportes, fotos | Room (`AppDatabase`, versión 6) | Datos relacionales con consultas y `Flow` |
| Sesión (correo, nombre, rol) | DataStore Preferences | Pares clave-valor asíncronos; reemplaza a SharedPreferences |
| Archivos de fotos | Almacenamiento privado + `FileProvider` | En la base solo se guarda la URI |

## Estructura del proyecto

```
app/src/main/java/cl/duoc/serviciotecnico/
├── data/
│   ├── local/          Room: entidades, DAO, base de datos y conversores
│   ├── remote/         Retrofit: ApiService, RetrofitClient (Singleton), MonitorConexion, DTO
│   │   └── geocodificacion/  API externa: NominatimApi + ApiExternaClient (OpenStreetMap)
│   └── repository/     Tickets, equipos, guías, sesión (DataStore), sincronización y geocodificación
├── domain/
│   ├── model/          Enum del negocio, Rol y Usuario
│   └── validation/     Validadores de cada formulario (sin Android)
├── ui/
│   ├── components/     Campo validado, banner de conexión, tarjetas, cámara, GPS, galería
│   ├── navigation/     Rutas y grafo; el inicio depende del rol guardado
│   ├── screens/        Login · Cliente (solicitudes) · Técnico (tickets, reporte) ·
│   │                   Admin (panel, solicitudes, tickets, usuarios, empresas)
│   ├── theme/          Material 3: colores, tipografía y formas
│   └── viewmodel/      Un ViewModel por pantalla + AdminViewModel
└── util/               Archivos de foto y permisos
```

## Usuarios de prueba

| Correo | Clave | Rol |
|---|---|---|
| admin@tactech.cl | admin123 | Administrador |
| tecnico@tactech.cl | tecnico123 | Técnico |
| tecnico2@tactech.cl | tecnico123 | Técnico |
| cliente@tactech.cl | cliente123 | Cliente (Clinica Las Condes) |

Con el backend encendido, el login se valida contra MySQL (clave con BCrypt). Sin conexión,
la app acepta estos mismos usuarios para que el técnico pueda trabajar igual.

## Pasos para ejecutar

### Opción A — Backend en la nube (AWS), la que usa la APK
1. En **AWS Academy → Learner Lab** apretar **Start Lab** y esperar el punto verde. La instancia
   EC2 `tactech-backend` enciende sola, junto con MySQL y el backend (servicio `systemd`).
2. Comprobar en el navegador: <http://44.215.178.124:8080/api/salud> → `"estado":"OK"`.
3. Abrir la carpeta `TacTech/` en Android Studio (Gradle JDK 17) y ejecutar, o instalar el APK
   firmado `TacTech/release/app-release.apk`.
   La URL sale de `API_URL` en `gradle.properties`.
4. Al terminar: **End Lab** para no gastar créditos.

El paso a paso del despliegue está en el README de `tactech-backend` (sección *Despliegue en AWS*).

#### Revisar los datos guardados en MySQL (desde cualquier computador)
1. **EC2 → Instancias → `tactech-backend` → Conectar → EC2 Instance Connect**, usuario `ubuntu`.
   Se abre una terminal en el navegador; no se necesita la llave `.pem` ni una IP fija.
2. En esa terminal:

| Comando | Muestra |
|---|---|
| `ver-datos` | Cuántos registros hay en cada tabla |
| `ver-ultimo solicitudes` | La última solicitud creada por un cliente, campo por campo |
| `ver-ultimo tickets` | El último ticket, con su equipo y el técnico asignado |
| `ver-ultimo guias` | El último reporte de visita: coordenadas GPS y cantidad de fotos |
| `ver-ultimo usuarios` / `empresas` | El último usuario (clave cifrada con BCrypt) o empresa |
| `ver-datos <tabla>` | Todos los registros de una tabla |

Así se comprueba que lo ingresado en la app llegó igual a la base de datos del microservicio.
El puerto de MySQL (3306) no está abierto a internet: solo se consulta desde el propio servidor.

### Opción B — Backend local (desarrollo)
1. Levantar `tactech-backend` en IntelliJ con MySQL local (ver su README) y comprobar
   <http://localhost:8080/api/salud>.
2. En `gradle.properties`, comentar la línea `API_URL=...` y hacer *Sync*: la app usa
   `http://10.0.2.2:8080/` (el PC visto desde el emulador).
3. Abrir la carpeta `TacTech/` en Android Studio y ejecutar en un emulador con API 26+.

## Pruebas unitarias

**273 pruebas** en `TacTech/app/src/test`, con **JUnit 4**, **MockK**, **kotlinx-coroutines-test**,
**Robolectric** y **Compose UI Test** (todas corren en la JVM, sin emulador), organizadas por capa:

| Capa | Archivos de prueba | Qué validan |
|---|---|---|
| Dominio | `ValidadoresTest`, `ValidacionesTest`, `ModelosTest` | Reglas de cada formulario (RUT, correo, serie, evidencia obligatoria, cambio de estado) y modelos |
| Datos | `ConvertersTest`, `LlamadaRemotaTest`, `MapeosTest` | Conversores de Room, traducción de errores HTTP del backend, mapeo entidad ↔ DTO |
| Repository | `TicketRepositoryTest`, `SolicitudRepositoryTest`, `SincronizacionRepositoryTest`, `SesionRepositoryTest`, `GuiaYEquipoRepositoryTest`, `GeocodificacionRepositoryTest`, `EmpresaYUsuarioRepositoryTest` (MockK) | Reglas de negocio, sincronización offline-first (orden de subida, conflicto 409, descarga por rol, no pisar pendientes, mutex), login con respaldo offline, API externa |
| ViewModel | Los 14 ViewModels (`LoginViewModelTest`, `FormularioGuiaViewModelTest`, `AdminViewModelTest`, etc.) + `EstadoFormularioTest` + `FabricasDeViewModelTest` | Estados de pantalla, validaciones visibles, flujos completos de cada rol, manejo de errores y factories |
| Room (Robolectric) | `TicketDaoTest`, `EquipoDaoTest`, `GuiaYFotoDaoTest`, `SolicitudDaoTest`, `AppDatabaseTest` | Consultas SQL reales sobre una base en memoria: orden, filtros por rol, marcado de sincronizacion y upsert |
| Pantallas Compose (Robolectric) | Una clase por pantalla (`LoginScreenTest`, `DetalleTicketScreenTest`, `FormularioGuiaScreenTest`, etc.) + `ComponentesTest` | Lo que ve el usuario: textos, formularios, dialogos, filtros, navegacion entre pantallas y componentes reutilizables |
| App y Android | `AplicacionTest`, `ClientesYMonitorTest`, `UtilidadesTest` | MainActivity y navegacion inicial, clientes Retrofit, monitor de red, FileProvider y permisos |

**Dobles de prueba** (`TacTech/app/src/test/.../testutil`): DAO en memoria en lugar de Room, un
microservicio falso, un DataStore en memoria, una red falsa y la API de OpenStreetMap falsa.
Así se prueba la lógica real sin dispositivo, sin red y sin base de datos.

**Cobertura con Kover sobre todo el proyecto** (sin exclusiones: lógica, Room, pantallas,
componentes, navegación, tema, Activity y Application):

```bash
cd TacTech
./gradlew testDebugUnitTest          # ejecutar las pruebas
./gradlew koverHtmlReportDebug       # reporte: app/build/reports/kover/htmlDebug/index.html
./gradlew koverLogDebug              # muestra el % total en la terminal
./gradlew koverVerifyDebug           # falla si la cobertura baja del 80%
```

Cobertura obtenida (Kover, todo el proyecto sin exclusiones): **92,8 % de líneas · 80,2 % de ramas · 93,7 % de clases** (meta: ≥ 80 %).

## APK firmado

| Elemento | Valor |
|---|---|
| APK firmado | `TacTech/release/app-release.apk` (release, firma APK v2) |
| Llave | `TacTech/release/tactech-release.jks` · alias `[completar]` |
| Certificado | `CN=joaquin moise carlos, C=cl` · válido 2026–2051 |
| Huella SHA-256 | `CA:40:3A:C2:6D:F8:6F:8F:65:BA:7C:DB:D2:7D:43:89:95:A3:32:B3:62:C1:3A:DC:45:20:A9:EA:A1:3C:E1:65` |

Configuración de firma en `TacTech/app/build.gradle.kts` (`signingConfigs.release`). Las contraseñas
se leen de `TacTech/keystore.properties`, que no se sube al repositorio (ver `keystore.properties.example`).
Para generar el APK firmado: `cd TacTech && ./gradlew assembleRelease`.

[Captura del APK firmado y del .jks]

## Planificación y control de versiones

- **GitHub:** commits de cada integrante sobre `master` (migración en tres entregas: backend y
  base de la app; datos, navegación y pruebas; interfaz, APK firmado y documentación).
- **GitHub Projects** (autorizado en reemplazo de Trello): tareas distribuidas por integrante
  con estados *Por hacer → En curso → Hecho*.

### Demostrar el flujo completo (los tres paneles)

1. **Cliente:** ingresa con `cliente@tactech.cl` y crea una solicitud. Queda "Por confirmar".
   Si activas el modo avión antes de crearla, el banner muestra "1 cambio guardado" y se envía
   sola al volver la red.
2. **Administrador:** entra con `admin@tactech.cl` → *Solicitudes por confirmar* → abre la
   solicitud → elige técnico y prioridad → *Crear ticket*.
   Opcional: en *Usuarios*, crea antes un técnico nuevo y asígnaselo.
3. **Técnico:** entra con el técnico asignado. El ticket aparece en su panel. Cambia el estado
   y crea el *Reporte de visita*: sin foto o sin ubicación no deja enviar. Al registrar la
   ubicación aparece la **dirección según GPS (API externa)**; con *Usar esta dirección* se copia
   al reporte. Luego cierra el ticket.
4. **Cliente** de nuevo: la solicitud muestra el ticket, el técnico y el estado "Cerrado".
5. Para probar la cancelación: el cliente crea otra solicitud y la cancela antes de que el
   administrador la procese.

## Recursos nativos y permisos

| Recurso | Uso | Permiso |
|---|---|---|
| Cámara | Foto de evidencia del reporte (obligatoria, hasta 15) | `CAMERA` (se pide al usarla) |
| Ubicación (GPS) | Geolocalización de la visita (obligatoria) y dirección por API externa | `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` |
| Red | Sincronización con el backend y API externa | `INTERNET`, `ACCESS_NETWORK_STATE` |

Si se deniega un permiso, la app explica para qué sirve y ofrece abrir los ajustes. Como la
evidencia es obligatoria, el reporte no se puede enviar sin foto y sin ubicación.

HTTP sin cifrar se permite solo hacia el backend (`network_security_config.xml`): la IP de AWS
`44.215.178.124`, y `10.0.2.2` / `localhost` para desarrollo. La API externa usa HTTPS.
