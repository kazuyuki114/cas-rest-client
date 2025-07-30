# CAS Client with REST Protocol

This is a Spring Boot application that implements a CAS (Central Authentication Service) client using the REST protocol.

## Flow Overview

1. **Credentials from frontend to backend** - Frontend sends username/password to the backend
2. **TGT request** - Backend requests a Ticket Granting Ticket from CAS server
3. **ST request** - Backend requests a Service Ticket using the TGT
4. **ST validation** - Backend validates the Service Ticket with CAS server
5. **Browser receives CASTGC cookie** - On successful validation, CASTGC cookie is set
6. **Backend returns login success** - Backend returns success response to frontend

## Configuration

Update `src/main/resources/application.properties`:

```properties
# CAS Configuration
cas.server.url=https://localhost:8080/cas
cas.client.service.url=http://localhost:8081
server.port=8081
```

## API Endpoints

### Login API
- **URL**: `POST /api/auth/login`
- **Content-Type**: `application/json`
- **Request Body**:
```json
{
  "username": "your_username",
  "password": "your_password"
}
```

- **Success Response**:
```json
{
  "success": true,
  "message": "Login successful",
  "serviceTicket": "ST-1234567890abcdef",
  "castgcCookie": "CASTGC=TGT-1234567890abcdef; Path=/; Secure; HttpOnly"
}
```

- **Failure Response**:
```json
{
  "success": false,
  "message": "Failed to obtain TGT"
}
```

### Health Check
- **URL**: `GET /api/auth/health`
- **Response**: `"CAS Client is running"`

## Testing

### Using curl
```bash
# Test login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"your_username","password":"your_password"}' \
  -v

# Test health check
curl http://localhost:8081/api/auth/health
```

### Using Postman
1. Create a new POST request to `http://localhost:8081/api/auth/login`
2. Set Content-Type header to `application/json`
3. Add request body:
```json
{
  "username": "your_username",
  "password": "your_password"
}
```

## Running the Application

1. Make sure your CAS server is running on `https://localhost:8080/cas`
2. Start the Spring Boot application:
```bash
./mvnw spring-boot:run
```
3. The application will start on `http://localhost:8081`

## Project Structure

```
src/main/java/com/hust/restclient/
├── config/
│   ├── CasConfig.java          # CAS configuration
│   ├── SecurityConfig.java     # Security configuration
│   └── WebConfig.java          # Web configuration
├── controller/
│   └── AuthController.java     # REST API endpoints
├── dto/
│   ├── LoginRequest.java       # Login request DTO
│   └── LoginResponse.java      # Login response DTO
├── service/
│   └── CasRestClient.java      # CAS REST client service
└── RestclientApplication.java  # Main application class
```

## CAS REST Protocol Implementation

The application implements the CAS REST protocol with the following steps:

1. **TGT Request**: POST to `/v1/tickets` with username/password
2. **ST Request**: POST to `/v1/tickets/{tgt}` with service URL
3. **ST Validation**: GET to `/serviceValidate` with ticket and service parameters

The CASTGC cookie is automatically set in the response headers for successful logins. 