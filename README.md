
# CAS REST Client (Spring Boot)

This project is a Spring Boot application that acts as a client for a Central Authentication Service (CAS) server using the REST protocol. It demonstrates how to authenticate users against a CAS server and manage authentication tickets programmatically.

## Features

- Authenticate users via CAS REST API
- Obtain Ticket Granting Ticket (TGT) and Service Ticket (ST)
- Validate service tickets
- Issue CASTGC cookie for SSO
- Exposes a RESTful login endpoint for frontend integration

## Technology Stack

- Java 21
- Spring Boot 3.5.x
- Spring Security
- Maven

## Quick Start

1. **Clone the repository**
2. **Configure CAS server URL and client service URL** in `src/main/resources/application.properties`:

   ```properties
   cas.server.url=https://localhost:8080/cas
   cas.client.service.url=http://localhost:8081
   server.port=8081
   ```

3. **Start your CAS server** (default: `https://localhost:8080/cas`)
4. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```
5. The backend will be available at `http://localhost:8081`

## API Usage

### Login Endpoint

- **URL:** `POST /api/auth/login`
- **Content-Type:** `application/json`
- **Request Body:**
  ```json
  {
    "username": "your_username",
    "password": "your_password"
  }
  ```
- **Success Response:**
  ```json
  {
    "success": true,
    "message": "Login successful",
    "serviceTicket": "ST-..."
  }
  ```

### Authentication Check Endpoint

- **URL:** `POST /api/auth/authen`
- **Description:** Validates the user's session using CASTGC cookie
- **Headers:** Requires CASTGC cookie to be present
- **Success Response:**
  ```json
  {
    "success": true,
    "message": "Authenticate successful",
    "serviceTicket": "ST-...",
    "username": "admin",
    "role": "ADMIN"
  }
  ```

### Service Ticket Validation Endpoint

- **URL:** `POST /api/auth/validate`
- **Content-Type:** `application/json`
- **Request Body:**
  ```json
  {
    "serviceTicket": "ST-...",
    "service": "http://localhost:8081"
  }
  ```
- **Success Response:**
  ```json
  {
    "success": true,
    "username": "admin",
    "role": "ADMIN"
  }
  ```

## How It Works

1. **Frontend sends credentials** to `/api/auth/login`.
2. **Backend requests TGT** from CAS server (`/v1/tickets`).
3. **Backend requests ST** using TGT (`/v1/tickets/{tgt}`).
4. **Backend validates ST** (`/serviceValidate`) and parses XML response to extract username and role.
5. **CASTGC cookie** is set in the response for SSO.
6. **Login response** is returned to the frontend.

## CAS XML Response Parsing

The application parses the CAS validation response XML to extract user information:

```xml
<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
    <cas:authenticationSuccess>
        <cas:user>admin</cas:user>
        <cas:attributes>
            <cas:groupMembership>ADMIN</cas:groupMembership>
            <!-- other attributes -->
        </cas:attributes>
    </cas:authenticationSuccess>
</cas:serviceResponse>
```

The service extracts:
- **Username** from `<cas:user>` element
- **Role** from `<cas:groupMembership>` element

## Role-Based API Access Control

The application implements role-based access control using Spring Security. After CAS validation, users are automatically authenticated with their roles:

### Available Endpoints by Role

#### Public Endpoints (No Authentication Required)
- `POST /api/auth/login` - User login
- `POST /api/auth/validate` - Service ticket validation

#### User Endpoints (Requires USER or ADMIN role)
- `GET /api/user/profile` - Get user profile
- `GET /api/user/dashboard` - Get user dashboard
- `POST /api/user/settings` - Update user settings

#### Admin Endpoints (Requires ADMIN role only)
- `GET /api/admin/users` - Get all users
- `POST /api/admin/system/config` - Update system configuration
- `GET /api/admin/reports` - Get admin reports

#### Authenticated Endpoints (Any valid user)
- `POST /api/auth/authen` - Check authentication status

### How Role-Based Filtering Works

1. **Request Interception**: The `RoleBasedAuthFilter` intercepts all requests
2. **Cookie Validation**: Extracts and validates the CASTGC cookie
3. **User Details Retrieval**: Gets username and role from CAS validation
4. **Spring Security Context**: Sets authentication with proper authorities
5. **Access Control**: Spring Security enforces role-based access rules

### Testing Role-Based Access

```bash
# Login as admin user
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' \
  -c cookies.txt

# Access admin endpoint (should work for ADMIN role)
curl -X GET http://localhost:8081/api/admin/users \
  -b cookies.txt

# Access user endpoint (should work for both USER and ADMIN roles)  
curl -X GET http://localhost:8081/api/user/profile \
  -b cookies.txt
```

## Project Structure

```
src/main/java/com/hust/restclient/
├── config/           # CAS, Security, SSL, and Web configuration
├── controller/       # REST API endpoints (AuthController, AdminController, UserController)
├── dto/              # Data Transfer Objects (LoginRequest, LoginResponse, etc.)
├── security/         # Role-based authentication filter
├── service/          # CAS REST client logic (CasRestClient)
└── RestclientApplication.java  # Main Spring Boot application
```

## Security Features

- **CAS REST Authentication**: Seamless integration with CAS server
- **Role-Based Access Control**: Automatic role extraction and enforcement
- **Session Management**: CASTGC cookie-based SSO
- **Method-Level Security**: `@PreAuthorize` annotations for fine-grained control
- **Automatic User Context**: Spring Security authentication context

## Dependencies

- spring-boot-starter-web
- spring-boot-starter-security
- spring-boot-starter-validation
- spring-boot-starter-webflux
- lombok
- spring-boot-devtools (optional, for development)
- spring-boot-starter-test (test scope)

## SSL & Security

The application is pre-configured for SSL and debug logging. See `application.properties` and `config/SslConfig.java` for details. You may need to trust the CAS server's certificate in your environment.

## License

MIT or as specified in the repository.