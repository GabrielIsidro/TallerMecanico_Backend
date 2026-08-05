<div align="center">
  <h1>🔧 TallerMecanico_Backend</h1>
  <p><em>Sistema Integral de Gestión para Talleres Mecánicos</em></p>
</div>

---

## 📖 Sobre el Proyecto

El proyecto nace de una necesidad real: optimizar y digitalizar la administración de un taller mecánico familiar especializado en autos, camiones y camionetas. 
Este backend provee una **API REST** robusta y segura que resuelve los problemas organizativos típicos de un taller en crecimiento, reemplazando la gestión manual o en papel por un sistema digital centralizado.

---

## ⚙️ Características Principales

- 🚗 **Gestión de Vehículos**: Registro detallado de unidades de diferente porte (autos, utilitarios, camiones).
- 📜 **Historial de Reparaciones**: Seguimiento integral de los servicios realizados a cada vehículo, facilitando el mantenimiento preventivo.
- 👥 **Administración de Clientes**: Base de datos de dueños y empresas.
- 📋 **Control de Órdenes de Trabajo**: Trazabilidad del estado de las reparaciones (Pendiente, En Proceso, Terminado, Entregado).
- 🔒 **Seguridad y Autenticación**: Sistema seguro basado en tokens JWT (JSON Web Tokens) y roles de usuario.
- ✉️ **Notificaciones por Correo**: Integración con un servidor SMTP (Gmail) para enviar alertas y comunicaciones.
- 📊 **Importación de Datos**: Soporte para la lectura de archivos CSV e ingesta inicial de información.
- 💳 **Suscripciones y Pagos**: Integración con la pasarela de pagos Mercado Pago para gestionar planes de suscripción del modelo SaaS.
- 🏢 **Multi-Taller (SaaS)**: Soporte para la administración de múltiples talleres mecánicos desde una misma plataforma centralizada.

---

## 🏗️ Arquitectura del Sistema

El proyecto está diseñado bajo una **Arquitectura en Capas (Layered Architecture)**, asegurando la separación de responsabilidades, alta escalabilidad y facilidad de mantenimiento:

1. **Controllers (`/controller`)**: Exponen los endpoints de la API REST y manejan las peticiones HTTP.
2. **Services (`/service`)**: Contienen la lógica de negocio pura, reglas del taller y cálculos.
3. **Repositories (`/repository`)**: Interfaces de Spring Data JPA para la persistencia y acceso a datos.
4. **Models (`/model`)**: Entidades que mapean directamente las tablas en la base de datos (Ej: `Vehiculo`, `Cliente`, `OrdenTrabajo`).
5. **DTOs (`/dto`)**: Objetos de Transferencia de Datos utilizados para desacoplar las entidades de la base de datos de las respuestas y peticiones de la API.
6. **Security (`/security`)**: Filtros y utilidades para la autenticación y autorización mediante JWT.

---

## 🛠️ Tecnologías Utilizadas

### Core & Frameworks
- **Java 17+**
- **Spring Boot 4.0.3**
- **Spring Data JPA** (Persistencia)
- **Spring Web / WebMVC** (API REST)
- **Spring Security** (Protección de endpoints)

### Herramientas de Apoyo
- **Lombok**: Para reducir el código repetitivo (getters, setters, constructores).
- **JJWT (io.jsonwebtoken)**: Para la generación y validación de tokens.
- **OpenCSV**: Para el procesamiento de archivos `.csv`.
- **JavaMail Sender**: Para el envío de correos electrónicos.
- **MercadoPago SDK / API**: Para procesar pagos y gestionar suscripciones.

### Base de Datos
- **MySQL** 
- **Hibernate** (ORM)

---

## 🚀 Instalación y Ejecución

### Prerrequisitos
- **Java 17** (o superior) instalado en tu sistema.
- **MySQL Server** en ejecución.
- (Opcional) Un IDE como IntelliJ IDEA, Eclipse o VS Code.

### Pasos para levantar el entorno local

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/tu-usuario/taller-mecanico-backend.git
   cd TallerMecanico_Backend
   ```

2. **Configurar la Base de Datos:**
   Crea una base de datos en MySQL llamada `taller_db`:
   ```sql
   CREATE DATABASE taller_db;
   ```
   *Nota: Por defecto, Hibernate (configurado con `update`) creará todas las tablas automáticamente al iniciar la aplicación.*

3. **Configurar Propiedades (Credenciales):**
   Edita el archivo `src/main/resources/application.properties` con tu usuario y contraseña de MySQL, y tus credenciales de Gmail para el envío de correos.

4. **Compilar e Instalar dependencias:**
   Ejecuta el Wrapper de Maven (incluido en el proyecto):
   ```bash
   # En Windows
   .\mvnw.cmd clean install -DskipTests
   
   # En Linux/Mac
   ./mvnw clean install -DskipTests
   ```

5. **Ejecutar el Servidor:**
   ```bash
   # En Windows
   .\mvnw.cmd spring-boot:run
   
   # En Linux/Mac
   ./mvnw spring-boot:run
   ```
   La API estará disponible en: `http://localhost:8080/`

---

## 🔌 Endpoints Principales

Aquí se detallan algunos de los recursos clave de la API. Para acceder a la mayoría, es necesario adjuntar el token JWT en la cabecera `Authorization: Bearer <token>`.

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Autenticar usuario y recibir JWT |
| `GET` | `/api/clientes` | Listar todos los clientes registrados |
| `POST` | `/api/vehiculos` | Registrar un nuevo auto, camión o utilitario |
| `GET` | `/api/vehiculos/{id}/historial` | Obtener el historial de reparaciones de un vehículo |
| `POST` | `/api/ordenes` | Crear una nueva orden de trabajo |
| `PUT` | `/api/ordenes/{id}/estado` | Actualizar el estado de una orden |

---

## 👥 Contribución y Soporte

Este es un proyecto personal con fines académicos y de implementación real.
Si deseas sugerir mejoras, detectar errores o contribuir al desarrollo:
1. Realiza un Fork del proyecto.
2. Crea una rama para tu feature (`git checkout -b feature/NuevaFuncionalidad`).
3. Haz un commit de tus cambios (`git commit -m 'Agrega nueva funcionalidad'`).
4. Sube la rama (`git push origin feature/NuevaFuncionalidad`).
5. Abre un Pull Request.

---

<div align="center">
  <b>Desarrollado por: Gabriel Isidro Garcia</b><br>
  <i>Estudiante de Analista en Informática</i>
</div>
