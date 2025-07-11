# Event Management System API

A scalable, production-ready RESTful API for event management built with Spring Boot 3, featuring JWT authentication, role-based access control, caching, rate limiting, and comprehensive event management capabilities.

Api Collection - https://lively-rocket-514967.postman.co/workspace/public-api-collection~90eaf496-50de-4355-a29c-fe5ac46c0922/collection/24118288-d9874c7e-f717-43cd-b5d7-604d71b2f964?action=share&source=copy-link&creator=24118288

## Features

### Core Features
- **User Management**: Registration, authentication with JWT tokens
- **Event Management**: Create, read, update, delete events with rich filtering
- **Attendance Management**: RSVP to events with different status options
- **Role-Based Access Control**: USER and ADMIN roles with appropriate permissions
- **Soft Delete**: Events are archived instead of permanently deleted

### Advanced Features
- **JWT Authentication**: Secure token-based authentication
- **Caching**: Caffeine cache for improved performance
- **Rate Limiting**: Request throttling per user/IP
- **Pagination & Sorting**: Efficient data retrieval
- **Input Validation**: Comprehensive request validation
- **Exception Handling**: Structured error responses
- **Database Migrations**: Flyway for schema management

## Tech Stack

- **Java 17**
- **Spring Boot 3.5.3**
- **Spring Security 6** (JWT)
- **Spring Data JPA** (Hibernate)
- **PostgreSQL** (Production)
- **H2 Database** (Testing)
- **Maven** (Build tool)
- **MapStruct** (Object mapping)
- **Caffeine** (Caching)
- **Flyway** (Database migrations)
- **Lombok** (Boilerplate reduction)

## API Endpoints

### Authentication (`/auth`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/auth/register` | Register new user | Public |
| POST | `/auth/login` | User login | Public |

### Events (`/events`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/events` | Create event | Authenticated |
| GET | `/events/{id}` | Get event details | Authenticated |
| PUT | `/events/{id}` | Update event | Host/Admin |
| DELETE | `/events/{id}` | Delete event | Host/Admin |
| GET | `/events` | List events with filters | Public |
| GET | `/events/upcoming` | List upcoming events | Public |
| GET | `/events/my-hosted` | List user's hosted events | Authenticated |
| GET | `/events/my-attending` | List user's attending events | Authenticated |
| GET | `/events/my-events` | List all user's events | Authenticated |

### Attendance (`/attendances`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/attendances` | RSVP to event | Authenticated |
| GET | `/attendances/my-attendances` | Get user's RSVPs | Authenticated |
| GET | `/attendances/event/{id}` | Get event attendees | Admin |

## Quick Start

### Prerequisites
- Java 17
- PostgreSQL 12
- Maven 3.6

### Setup Database
```sql
CREATE DATABASE event_management;
```

### Configuration
Update `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/event_management
spring.datasource.username=event_user
spring.datasource.password=your_password
app.jwt.secret=your_secret_key_here
```

### Run Application
```bash
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080/api/v1`

## Sample API Usage

### 1. Register User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123",
    "role": "USER"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

### 3. Create Event
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "title": "Spring Boot Workshop",
    "description": "Learn Spring Boot best practices",
    "startTime": "2025-08-15T10:00:00",
    "endTime": "2025-08-15T17:00:00",
    "location": "Tech Conference Center",
    "visibility": "PUBLIC"
  }'
```

### 4. Get Upcoming Events
```bash
curl -X GET "http://localhost:8080/api/v1/events/upcoming?page=0&size=10&sort=startTime"
```

### 5. RSVP to Event
```bash
curl -X POST http://localhost:8080/api/v1/attendances \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "eventId": "EVENT_UUID",
    "status": "GOING"
  }'
```

## Data Models

### User
```json
{
  "id": "UUID",
  "name": "string",
  "email": "string",
  "role": "USER|ADMIN",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### Event
```json
{
  "id": "UUID",
  "title": "string",
  "description": "string",
  "host": "UserResponse",
  "startTime": "timestamp",
  "endTime": "timestamp",
  "location": "string",
  "visibility": "PUBLIC|PRIVATE",
  "createdAt": "timestamp",
  "updatedAt": "timestamp",
  "attendeeCount": "number"
}
```

### Attendance
```json
{
  "event": "EventResponse",
  "user": "UserResponse",
  "status": "GOING|MAYBE|DECLINED",
  "respondedAt": "timestamp"
}
```

## Testing

### Run Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn test -Dtest=*IntegrationTest
```

## Architecture

The application follows a layered architecture with clear separation of concerns:

- **Controller Layer**: REST endpoints and request/response handling
- **Service Layer**: Business logic and transaction management
- **Repository Layer**: Data access and persistence
- **Security Layer**: Authentication and authorization
- **Exception Layer**: Global exception handling
- **Configuration Layer**: Application configuration and beans

## Security

- **JWT Authentication**: Stateless authentication with secure tokens
- **Role-Based Access**: USER and ADMIN roles with method-level security
- **Password Encryption**: BCrypt for secure password storage
- **Input Validation**: Comprehensive validation with custom error messages
- **Rate Limiting**: Request throttling to prevent abuse

## Performance

- **Caching**: Caffeine cache for frequently accessed data
- **Database Indexing**: Optimized queries with strategic indexes
- **Pagination**: Efficient data retrieval for large datasets
- **Connection Pooling**: HikariCP for database connections
- **Query Optimization**: Custom queries for complex filtering

## Monitoring

- **Actuator Endpoints**: Health checks and metrics
- **Logging**: Structured logging with different levels
- **Error Tracking**: Comprehensive exception handling

## Default Credentials

After running the application with sample data:
- **Admin**: sachini.karunaratne@eventmanager.com / admin123
- **User**: chanuth.silva@techconf.com / user123

