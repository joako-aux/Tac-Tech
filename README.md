# 📱 Tac-Tech (TacticalDEV - MVP)

> **Evaluación de Desarrollo de Aplicaciones Móviles (DSY1105)**  
> **Duoc UC - Escuela de Informática y Telecomunicaciones CITT**  

---

## 📋 1. Descripción del Proyecto y Contexto del Caso

**TacticalDEV** es una solución tecnológica diseñada para resolver los desafíos de trazabilidad y verificación de visitas en terreno para empresas distribuidoras y puntos de venta de productos refrigerados (vitrinas freezer y conservadoras). 

Tradicionalmente, el registro de estas revisiones se realizaba de manera informal a través de WhatsApp, planillas de Excel o papel, lo que generaba problemas críticos como:
* **"Visitas fantasma"**: Ausencia de un mecanismo confiable para comprobar la presencia real del operario en el punto de venta.
* **Fallas no detectadas a tiempo**: Un alto porcentaje de equipos presentaba incidencias anuales sin registro oportuno.
* **Información dispersa**: Falta de un historial unificado y consultable de los activos y mantenciones.

**Tac-Tech** materializa un **MVP (Producto Mínimo Viable)** enfocado en estandarizar este flujo mediante una interfaz móvil nativa en Kotlin, asegurando la trazabilidad de las órdenes de trabajo, validación de técnicos y administradores, y sincronización de datos locales con SQLite.

---

## 👥 2. Equipo de Trabajo (Integrantes)
* **Joaquín Pérez**
* **Moise Fonseca**
* **Carlos Lobos**

---

## 🚀 3. Arquitectura y Tecnologías Utilizadas
El desarrollo se estructuró siguiendo los lineamientos técnicos de la asignatura:
* **Lenguaje**: Kotlin
* **Entorno de Desarrollo**: Android Studio
* **Arquitectura**: MVVM (Model-View-ViewModel) y Patrón de Repositorios de Datos.
* **Persistencia de Datos**: SQLite / `DatabaseHelper` local para soporte en terreno con conectividad interintermitente.
* **Interfaz de Usuario**: XML / Material Design con navegación estructurada por roles (Administrador, Técnico de Soporte y Cliente).
* **Control de Versiones**: Git & GitHub.

---

## 🗂️ 4. Estructura y Componentes del Sistema

### Descripción de Módulos Principales
* **Módulo de Administración (`ui/admin`)**: Gestión de empresas, asignación de técnicos de soporte autorizados, registro centralizado y control de órdenes de trabajo.
* **Módulo de Clientes (`ui/client`)**: Creación, consulta y edición de solicitudes o incidencias sobre equipos y dispositivos asignados.
* **Módulo de Soporte / Técnico (`ui/support`)**: Visualización de órdenes asignadas, actualización de estados en terreno y registro de notas de cierre técnico.
* **Capa de Datos (`data`)**: Controladores de bases de datos relacionales (`DatabaseHelper`, `WorkOrderRepository`, `UserRepository`, `CompanyRepository`).

### Distribución de Archivos y Paquetes en la Aplicación
```text
com.example.tac_tech/
│
├── MainActivity.kt                      # Pantalla de acceso principal y selección de roles
│
├── data/                                # Capa de persistencia y conexión a base de datos SQLite
│   ├── DatabaseHelper.kt                # Creación de tablas, esquema relacional y cifrado SHA-256
│   ├── UserRepository.kt                # Gestión de usuarios, autenticación y filtros por empresa
│   ├── WorkOrderRepository.kt           # Gestión, creación, actualización y cierre de órdenes de trabajo
│   ├── CompanyRepository.kt             # Manejo de empresas asociadas al sistema
│   ├── ClientRepository.kt              # Repositorio de clientes en terreno
│   ├── EmployeeRepository.kt            # Repositorio de personal técnico y operarios
│   ├── ClientAccountRepository.kt       # Cuentas y accesos de clientes
│   └── SupportAccountRepository.kt      # Cuentas y accesos del personal de soporte
│
├── model/                               # Entidades de datos (Modelos de dominio)
│   ├── User.kt                          # Estructura de usuarios y roles (ADMIN, SOPORTE, CLIENTE)
│   ├── WorkOrder.kt                     # Estructura de solicitudes y órdenes de trabajo
│   ├── Company.kt                       # Datos de las empresas distribuidoras
│   ├── Client.kt                        # Perfil  clientes
│   ├── Employee.kt                      # Perfil de técnicos y operarios
│   └── Task.kt                          # Tareas asociadas a revisiones
│
└── ui/                                  # Capa de presentación (Interfaces de usuario por rol)
    ├── admin/                           # Módulo del Administrador
    │   ├── AdminActivity.kt
    │   ├── AdminWorkOrdersActivity.kt   # Creación y supervisión general de órdenes
    │   ├── CompaniesActivity.kt         # Gestión de empresas
    │   ├── ClientsActivity.kt           # Gestión de clientes 
    │   ├── SupportTechniciansActivity.kt# Asignación y control de técnicos
    │   └── [Gestión de cuentas y vistas auxiliares]
    │
    ├── client/                          # Módulo del Cliente 
    │   ├── ClientActivity.kt
    │   └── ClientWorkOrdersActivity.kt  # Solicitud y edición de requerimientos
    │
    └── support/                         # Módulo de Soporte / Técnico en Terreno
        ├── SupportActivity.kt
        └── SupportWorkOrdersActivity.kt # Visualización de órdenes asignadas y notas de cierre
```
---
### 5. Enlace del Repositorio Oficial

https://github.com/joako-aux/Tac-Tech.git
