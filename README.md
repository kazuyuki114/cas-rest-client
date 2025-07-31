
# CAS REST Client - Spring Boot Authentication Service

A modern Spring Boot application implementing Central Authentication Service (CAS) REST protocol with session-based authentication and role-based access control.

## 🏗️ Architecture Overview

This application provides a complete CAS authentication solution featuring:

- **CAS REST Protocol**: Full TGT → ST → Validation flow implementation
- **Session Management**: Hybrid session + CASTGC cookie authentication for optimal performance  
- **Single Filter Design**: `SimpleAuthFilter` handles both session lookup and CAS validation
- **Role-Based Security**: Spring Security integration with automatic role enforcement
- **Performance Optimized**: Session caching minimizes CAS server calls

## 🚀 Features

- ✅ **Complete CAS Integration**: TGT/ST ticket flow with XML response parsing
- ✅ **Smart Authentication**: Session-first approach with CASTGC fallback
- ✅ **Role Extraction**: Automatic role parsing from `<cas:groupMembership>`
- ✅ **Session Caching**: 30-minute sessions for fast subsequent requests
- ✅ **Cookie Management**: Automatic CASTGC and JSESSIONID handling
- ✅ **SSL Support**: Configurable SSL/TLS for secure CAS communication
- ✅ **Clean Architecture**: Single filter replaces complex filter chains

## 📋 Prerequisites

- Java 21+
- Maven 3.8+
- Running CAS Server
- Spring Boot 3.5.x

## ⚙️ Configuration

### Application Properties

```properties
# CAS Server Configuration
cas.server.url=https://your-cas-server:8443/cas/
cas.client.service.url=http://localhost:8081

# Server Configuration
server.port=8081

# Logging (Optional)
logging.level.com.hust.restclient=INFO
```

### SSL Configuration (Optional)

```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:server.crt
server.ssl.key-store-password=changeit
```

## 🏃‍♂️ Quick Start

1. **Configure CAS Server URLs**
   ```bash
   # Edit src/main/resources/application.properties
   cas.server.url=https://your-cas-server:8443/cas/
   cas.client.service.url=http://localhost:8081
   ```

2. **Build and Run**
   ```bash
   ./mvnw clean compile
   ./mvnw spring-boot:run
   ```

3. **Access Application**
   - Application: `http://localhost:8081`
   - Login: `POST /api/auth/login`

## 📚 API Endpoints

### Authentication

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

**Response:**
```json
{
  "success": true,
  "serviceTicket": "ST-1-xxx",
  "message": "Login successful"
}
```

#### Logout
```http
POST /api/auth/logout
```

**Response:**
```json
{
  "message": "Logout successful",
  "action": "redirect_to_login"
}
```

### Protected Endpoints

#### User Endpoints (USER or ADMIN role)
```http
GET /api/user/profile         # User profile information
GET /api/user/dashboard       # User dashboard data
```

#### Admin Endpoints (ADMIN role only)
```http
GET /api/admin/dashboard      # Admin dashboard
GET /api/admin/users          # User management
```

## 🔐 Authentication Flow

### Login Process
```
Client              App                 CAS Server
  |                  |                      |
  | POST /login ---> |                      |
  |                  | -- Get TGT ------->  |
  |                  | <-- TGT-123 -------- |
  |                  | -- Get ST -------->  |
  |                  | <-- ST-456 --------- |
  |                  | -- Validate ST ---> |
  |                  | <-- User+Role ------ |
  | <-- Session +    |                      |
  |     CASTGC       |                      |
```

### API Access Process
```
Request with Cookies
        |
    SimpleAuthFilter
        |
   Check Session? -----> Session exists
        |                     |
        NO                   YES
        |                     |
   Check CASTGC? ---------> Use session data
        |                     |
        YES                  Allow access
        |
  Validate with CAS
        |
  Create new session
        |
    Allow access
```

## 🛡️ Security Configuration

### Current Filter Chain
```java
// SecurityConfig.java
http.addFilterBefore(simpleAuthFilter, UsernamePasswordAuthenticationFilter.class)
    .authorizeHttpRequests(authz -> authz
        .requestMatchers("/api/auth/login", "/api/auth/logout", "/public/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
        .anyRequest().authenticated()
    );
```

### CAS XML Response Parsing
Your application extracts user information from CAS validation responses:

```xml
<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
    <cas:authenticationSuccess>
        <cas:user>admin</cas:user>
        <cas:attributes>
            <cas:groupMembership>ADMIN</cas:groupMembership>
        </cas:attributes>
    </cas:authenticationSuccess>
</cas:serviceResponse>
```

## 🔧 How It Works

### SimpleAuthFilter Logic
```java
1. Skip public endpoints (/api/auth/login, /public/**)
2. Check HTTP session for cached authentication
   - If found: Set Spring Security context → Continue
3. Check CASTGC cookie
   - If found: Request new ST → Validate → Create session → Continue  
4. No authentication: Return 401 Unauthorized
```

### Session Management
- **Session Duration**: 30 minutes
- **Session Storage**: Username, role, CAS TGT
- **Performance**: Session hits ~1-5ms vs CAS validation ~100-500ms

## 🧪 Testing

### Manual Testing with curl

```bash
# 1. Login and save cookies
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' \
  -c cookies.txt -v

# 2. Access admin endpoint
curl -X GET http://localhost:8081/api/admin/dashboard \
  -b cookies.txt -v

# 3. Access user endpoint  
curl -X GET http://localhost:8081/api/user/profile \
  -b cookies.txt -v

# 4. Logout
curl -X POST http://localhost:8081/api/auth/logout \
  -b cookies.txt -v
```

### Expected Behavior
- **Admin user**: Can access both `/api/admin/**` and `/api/user/**`
- **Regular user**: Can only access `/api/user/**`
- **No session**: Gets 401 for protected endpoints

## 🛠️ Project Structure

```
src/main/java/com/hust/restclient/
├── config/
│   ├── CasConfig.java           # CAS server URLs configuration
│   ├── SecurityConfig.java      # Spring Security + filter setup
│   ├── SslConfig.java          # SSL/TLS configuration
│   └── WebConfig.java          # CORS and web settings
├── controller/
│   ├── AuthController.java     # Login/logout endpoints
│   ├── UserController.java     # USER role endpoints
│   └── AdminController.java    # ADMIN role endpoints
├── dto/
│   ├── CasLoginResult.java     # Login result with user details
│   ├── CasUserDetail.java      # User info from CAS
│   ├── LoginRequest.java       # Login request payload
│   └── LoginResponse.java      # Login response payload
├── security/
│   └── SimpleAuthFilter.java   # Main authentication filter
└── service/
    └── CasRestClient.java       # CAS REST protocol implementation
```

## 📊 Performance Characteristics

### Authentication Speed
- **Fresh login**: ~3-8s (full CAS flow)
- **Session hit**: ~1-5ms (cached)
- **CASTGC validation**: ~100-500ms (when session expired)

### Optimizations Implemented
- Session-first authentication strategy
- Single filter instead of multiple filter chain
- Configurable connection timeouts
- XML parsing optimization

## 🔍 Troubleshooting

### Common Issues

1. **Long Login Times (14s+)**
   - Check CAS server connectivity
   - Verify SSL certificate trust
   - Consider connection pooling

2. **Wrong Roles**
   - Verify CAS server returns `<cas:groupMembership>`
   - Check XML parsing in logs

3. **Session Issues**
   - Verify JSESSIONID and CASTGC cookies
   - Check session timeout (30 min default)

4. **Service Ticket Errors**
   - Remember: Service Tickets can only be used once
   - Check CAS server logs for validation failures

### Debug Logging

```properties
logging.level.com.hust.restclient=DEBUG
logging.level.org.springframework.security=DEBUG
```

## 🚀 Deployment

### Production Checklist
- [ ] Update CAS server URLs
- [ ] Configure SSL certificates
- [ ] Set secure cookie settings
- [ ] Configure appropriate session timeout
- [ ] Enable production logging levels

### Environment Variables
```bash
export CAS_SERVER_URL="https://prod-cas:8443/cas/"
export CAS_CLIENT_SERVICE_URL="https://your-app.com"
export SERVER_PORT=8081
```

## 📄 Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📜 License

This project is licensed under the MIT License.

---

**Built with ❤️ using Spring Boot 3.5.x, Java 21, and CAS REST Protocol**