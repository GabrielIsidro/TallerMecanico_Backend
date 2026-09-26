<div align="center">
  <h1>🔧 TallerMecanico_Backend</h1>
  <p><em>Sistema Integral Multi-Tenant de Gestión para Talleres Mecánicos (SaaS)</em></p>
</div>

---

## 📖 Sobre el Proyecto

El proyecto nace de una necesidad real: optimizar y digitalizar la administración integral de un taller mecánico familiar especializado en autos, camiones y utilitarios, escalable como plataforma **SaaS Multi-Tenant**.
Este backend provee una **API REST** robusta, segura y versionada (`/api/v1/...`) con aislamiento estricto de datos por taller a nivel de base de datos (`TenantId`), reemplazando la gestión manual o en papel por un sistema centralizado de alto rendimiento.

---

## ⚙️ Características Principales

- 🏢 **Multi-Taller (SaaS Multi-Tenancy)**: Aislamiento completo de información entre talleres con Hibernate Discriminator (`@TenantId`) y validación de seguridad transversal en capa de servicios.
- 🚗 **Gestión de Vehículos**: Registro de unidades (marca, modelo, patente, kilometraje, historial de mantenimientos).
- 👥 **Administración de Clientes**: Gestión de particulares y cuentas corrientes de empresas (CUIT, razón social, contactos).
- 📋 **Control Integral de Órdenes de Trabajo**: Trazabilidad completa del ciclo de reparación (Pendiente, En Proceso, Finalizado, Entregado), cálculo dinámico de costos y generación de comprobantes.
- 🛠️ **Catálogo de Servicios y Repuestos**: Control de inventario de repuestos con alertas de stock mínimo y tarifario de mano de obra organizado por grupos.
- 🔒 **Seguridad y Autenticación**: Tokens JWT (JSON Web Tokens) con separación estricta de roles (`SUPER_ADMIN`, `ADMIN_TALLER`, `MECANICO`).
- ✉️ **Notificaciones Automatizadas por Correo**: Alertas inmediatas al cliente cuando su vehículo está listo para ser retirado y correos de bienvenida con credenciales para nuevos talleres.
- 📊 **Importación Masiva de Datos**: Procesamiento de archivos `.csv` para actualización rápida de tarifas y precios de servicios.
- 💳 **Suscripciones y Pagos (Mercado Pago)**: Integración con checkout y webhooks automáticos para gestión de planes mensuales y anuales (`BASE`, `PRO`).
- 👔 **Backoffice SaaS**: Panel de control administrativo para supervisar talleres activos, estados de suscripción, extensiones de vigencia y bajas controladas.

---

## 🏗️ Arquitectura del Sistema

El proyecto implementa una **Arquitectura en Capas y Módulos** orientada al dominio:

1. **Core (`com.taller.backend.core`)**:
   - **`config`**: Configuración de seguridad, CORS, inicialización de datos de arranque (`DataInitializer`).
   - **`security`**: Filtros JWT (`JwtRequestFilter`), `SecurityHelper` para resolución del taller autenticado, UserDetailsService.
   - **`multitenancy`**: Contexto de hilo (`TenantContext`) y resolución de identificador de tenant para Hibernate.
   - **`exception`**: Excepciones de dominio tipadas (`ResourceNotFoundException`, `UnauthorizedAccessException`, `BusinessRuleException`, `DuplicateResourceException`) gestionadas centralmente por `GlobalExceptionHandler`.
   - **`service`**: Servicios transversales de correo (`EmailService`) y generación de documentos (`PdfService`).

2. **Módulo Talleres (`com.taller.backend.modules.talleres`)**:
   - Gestión operativa de cada taller: clientes, vehículos, órdenes de trabajo, inventario de repuestos, servicios y usuarios/mecánicos.
   - Todos los servicios validan la pertenencia de las entidades al taller autenticado.

3. **Módulo Backoffice (`com.taller.backend.modules.backoffice`)**:
   - Operaciones exclusivas del SaaS: alta y administración de talleres (`TallerService`), planes (`PlanSuscripcion`), pagos y webhooks (`SuscripcionService`, `MercadoPagoService`).

---

## 🛠️ Tecnologías Utilizadas

### Core & Frameworks
- **Java 17 LTS**
- **Spring Boot 3.4.3**
- **Spring Data JPA** (Persistencia & ORM)
- **Spring Web / WebMVC** (API REST)
- **Spring Security 6** (Protección de endpoints y control de roles)
- **Spring Boot Starter Validation** (Validación declarativa con Bean Validation)

### Herramientas de Apoyo
- **Lombok**: Reducción de código boilerplate (getters, setters, builders).
- **JJWT (io.jsonwebtoken 0.11.5)**: Firma y verificación segura de tokens JWT.
- **OpenCSV**: Procesamiento y parseo masivo de archivos `.csv`.
- **JavaMail Sender**: Envío de correos electrónicos transaccionales por SMTP.
- **MercadoPago SDK**: Generación de preferencias de pago y pasarela de cobro.
- **iText / OpenPDF**: Generación de presupuestos y comprobantes de trabajo en PDF.

### Base de Datos
- **MySQL 8+**
- **Hibernate Multi-Tenancy** (`@TenantId`)

---

## 🚀 Instalación y Ejecución

### Prerrequisitos
- **Java 17** (o superior) instalado en el sistema.
- **MySQL Server 8+** en ejecución.

### Pasos para levantar el entorno local

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/tu-usuario/taller-mecanico-backend.git
   cd TallerMecanico_Backend
   ```

2. **Configurar la Base de Datos:**
   Crea la base de datos en MySQL:
   ```sql
   CREATE DATABASE taller_db;
   ```
   *Nota: Hibernate (`ddl-auto=update`) inicializará las tablas y `DataInitializer` sembrará automáticamente el usuario SuperAdmin inicial y los planes de suscripción.*

3. **Configurar Variables de Entorno:**
   Copia la plantilla `.env.example` y configura tus credenciales locales:
   ```bash
   cp .env.example .env
   ```
   O define las variables de entorno (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `JWT_SECRET`).

4. **Compilar el Proyecto:**
   ```bash
   # En Linux / macOS
   ./mvnw clean compile -DskipTests
   
   # En Windows
   .\mvnw.cmd clean compile -DskipTests
   ```

5. **Iniciar la Aplicación:**
   ```bash
   # En Linux / macOS
   ./mvnw spring-boot:run
   
   # En Windows
   .\mvnw.cmd spring-boot:run
   ```
   El backend estará disponible en: `http://localhost:8080/`

---

## 🔌 Endpoints de la API REST (`/api/v1`)

Todos los endpoints (excepto login y webhooks públicos) requieren la cabecera `Authorization: Bearer <token_jwt>`.

### Módulo Talleres (`/api/v1/talleres`)

| Método | Endpoint | Rol Requerido | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | Público | Autenticación de taller (ADMIN_TALLER / MECANICO) |
| `GET` | `/usuarios/me` | Autenticado | Perfil del usuario y taller logueado |
| `POST` | `/usuarios/actualizar-perfil` | Autenticado | Actualización de datos y cambio de contraseña |
| `GET` | `/usuarios/equipo` | ADMIN_TALLER | Listado de mecánicos del taller |
| `POST` | `/usuarios/equipo` | ADMIN_TALLER | Alta de nuevo mecánico para el equipo |
| `GET` | `/clientes` | Autenticado | Listado paginado y búsqueda de clientes |
| `POST` | `/clientes` | Autenticado | Registrar cliente para el taller |
| `PUT` | `/clientes/{id}` | Autenticado | Actualizar cliente |
| `DELETE` | `/clientes/{id}` | Autenticado | Eliminar cliente |
| `GET` | `/vehiculos` | Autenticado | Listar vehículos del taller |
| `POST` | `/vehiculos` | Autenticado | Registrar vehículo asignado al taller |
| `PUT` | `/vehiculos/{id}` | Autenticado | Actualizar vehículo |
| `DELETE` | `/vehiculos/{id}` | Autenticado | Eliminar vehículo |
| `GET` | `/ordenes` | Autenticado | Listado de órdenes de trabajo del taller |
| `POST` | `/ordenes` | Autenticado | Crear nueva orden de trabajo con ítems |
| `PUT` | `/ordenes/{id}/estado` | Autenticado | Actualizar estado y notificar por email |
| `GET` | `/repuestos` | Autenticado | Catálogo e inventario de repuestos |
| `POST` | `/repuestos` | Autenticado | Registrar repuesto con stock mínimo |
| `GET` | `/servicios` | Autenticado | Lista de tipos de servicios y precios |
| `POST` | `/importar-precios/csv` | Autenticado | Carga masiva de servicios desde archivo CSV |

### Módulo Backoffice SaaS (`/api/v1/backoffice`)

| Método | Endpoint | Rol Requerido | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | Público | Autenticación del SuperAdmin |
| `GET` | `/admin/saas/me` | SUPER_ADMIN | Perfil del SuperAdmin logueado |
| `GET` | `/admin/saas/talleres` | SUPER_ADMIN | Listar todos los talleres registrados |
| `POST` | `/admin/saas/talleres` | SUPER_ADMIN | Alta de taller y su administrador |
| `PUT` | `/admin/saas/talleres/{id}/suscripcion`| SUPER_ADMIN | Modificar suscripción o prorrogar vencimiento |
| `DELETE` | `/admin/saas/talleres/{id}` | SUPER_ADMIN | Baja en cascada de taller y datos asociados |
| `GET` | `/planes` | Público | Listar planes de suscripción disponibles |
| `POST` | `/suscripciones/checkout` | ADMIN_TALLER | Generar preferencia de pago en Mercado Pago |
| `POST` | `/suscripciones/webhook` | Público | Webhook de acreditación de suscripción |

---

## 👥 Contribución y Soporte

Este es un proyecto con fines académicos y de implementación comercial SaaS.
Para colaborar o reportar incidencias:
1. Realiza un Fork del repositorio.
2. Crea tu rama para la nueva característica (`git checkout -b feature/NuevaMejora`).
3. Confirma tus cambios (`git commit -m 'feat: agrega nueva mejora'`).
4. Sube la rama (`git push origin feature/NuevaMejora`).
5. Abre un Pull Request.

---

<div align="center">
  <b>Desarrollado por: Gabriel Isidro Garcia</b><br>
  <i>Estudiante de Analista en Informática</i>
</div>
