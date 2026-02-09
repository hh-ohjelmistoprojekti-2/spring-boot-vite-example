# Authentication Flow Documentation

This document describes the JWT-based authentication flow in the Spring Boot + Vite messenger application. The authentication flow starts when a user submits the login form and ends when the user receives an access token that can be used for subsequent authenticated requests.

## Overview

The application uses **JWT (JSON Web Token)** authentication with a stateless session management approach. The frontend stores the JWT token in local storage and includes it in subsequent requests, while the backend validates the token on each request.

## Authentication Flow Steps

### 1. Frontend: User Login UI

File: [`frontend/src/pages/Login.jsx`](frontend/src/pages/Login.jsx)

The login process begins at the Login component where the user enters their credentials:

1. User enters `username` and `password` in the form
2. When the form is submitted, `handleSubmitLogin` is called
3. The function calls `login({ username, password })` service function
4. On success, the user is navigated to the home page and the page is reloaded
5. On error, an error message is displayed

```javascript
function handleSubmitLogin(event) {
  event.preventDefault();
  
  login({ username, password })
    .then(() => {
      navigate("/");
      window.location.reload();
    })
    .catch((error) => {
      if (error.response.data) {
        setError(error.response.data.message);
      }
    });
}
```

### 2. Frontend: Login Service

File: [`frontend/src/services/user.js`](frontend/src/services/user.js)

The `login` function handles the HTTP request to the backend:

1. Makes a POST request to `/api/auth/login` endpoint with credentials
2. If the response contains an `accessToken`, stores it using `setAccessToken()`
3. Returns the response data

```javascript
export function login(credentials) {
  return api.post("/api/auth/login", credentials).then((response) => {
    if (response.data.accessToken) {
      setAccessToken(response.data.accessToken);
    }
    return response.data;
  });
}
```

### 3. Frontend: API Client and Token Storage

File: [`frontend/src/services/api.js`](frontend/src/services/api.js)

The API client handles token storage and automatic inclusion in requests:

1. **Token Storage**: The `setAccessToken()` function stores the JWT token in browser's `localStorage`
   - Storage key: `MESSENGER_ACCESS_TOKEN`
   
2. **Token Retrieval**: The `getAccessToken()` function retrieves the token from localStorage

3. **Request Interceptor**: An [Axios](https://axios-http.com/docs/intro) interceptor automatically adds the token to all requests
   - Before each request, it retrieves the token from localStorage
   - If a token exists, adds it to the `Authorization` header
   - The token is sent in the format `Bearer <TOKEN>`, which is a common format

```javascript
export function setAccessToken(token) {
  localStorage.setItem(ACCESS_TOKEN_KEY, token);
}

api.interceptors.request.use((config) => {
  const token = getAccessToken();
  
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  
  return config;
});
```

### 4. Backend: Authentication Endpoint

File: [`src/main/java/fi/haagahelia/messenger/controller/AuthRestController.java`](src/main/java/fi/haagahelia/messenger/controller/AuthRestController.java)

The `/api/auth/login` endpoint handles authentication requests:

1. **Validation**: Validates the `LoginUserDto` request body
   - Checks that username and password are not blank
   - Returns HTTP 400 if validation fails

2. **Authentication Token Creation**: Creates a `UsernamePasswordAuthenticationToken` with credentials

3. **Authentication**: Uses Spring Security's `AuthenticationManager` to authenticate
   - The `AuthenticationManager` delegates to `UserDetailsServiceImpl`
   - Throws exception if credentials are invalid

4. **JWT Generation**: On successful authentication, generates a JWT token using `JwtService`

5. **Response**: Returns an `AccessTokenPayloadDto` containing the token and expiration time

```java
@PostMapping("/login")
public ResponseEntity<?> login(@Valid @RequestBody LoginUserDto login, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                bindingResult.getAllErrors().get(0).getDefaultMessage());
    }

    UsernamePasswordAuthenticationToken credentials = 
        new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword());

    try {
        Authentication auth = authenticationManager.authenticate(credentials);
        AccessTokenPayloadDto accessTokenPayload = jwtService.getAccessToken(auth.getName());
        
        return ResponseEntity.ok().body(accessTokenPayload);
    } catch (Exception exception) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid username or password");
    }
}
```

### 5. Backend: User Verification

File: [`src/main/java/fi/haagahelia/messenger/service/UserDetailsServiceImpl.java`](src/main/java/fi/haagahelia/messenger/service/UserDetailsServiceImpl.java)

The `UserDetailsServiceImpl` is called by Spring Security's `AuthenticationManager` during authentication:

1. Implements Spring Security's `UserDetailsService` interface
2. The `loadUserByUsername` method queries the database for the user
3. Throws `UsernameNotFoundException` if user doesn't exist
4. Returns a Spring Security `UserDetails` object with username, password hash, and roles
5. Spring Security automatically verifies the password using BCrypt

**Key Code**:
```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findOneByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
            .password(user.getPasswordHash()).roles(user.getRole()).build();
}
```

### 6. Backend: JWT Token Generation

File: [`src/main/java/fi/haagahelia/messenger/service/JwtService.java`](src/main/java/fi/haagahelia/messenger/service/JwtService.java)

The `JwtService` generates JWT tokens:

1. **Token Creation**: Creates a JWT with an 8-hour expiration
   - Sets the username as the subject (`setSubject`)
   - Sets expiration time (8 hours from now)
   - Signs the token with HMAC using the secret key

2. **Signing Key**: Uses a secret key from application properties (`auth.jwt-secret`)
   - Converts the secret to bytes
   - Creates an HMAC key for signing

3. **Response**: Returns an `AccessTokenPayloadDto` with the token string and expiration timestamp

```java
public AccessTokenPayloadDto getAccessToken(String username) {
    Instant expiresAt = Instant.now().plusMillis(EXPIRATION_TIME);

    String accessToken = Jwts.builder()
        .setSubject(username)
        .setExpiration(Date.from(expiresAt))
        .signWith(getSigningKey())
        .compact();

    return new AccessTokenPayloadDto(accessToken, expiresAt);
}

private Key getSigningKey() {
    byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
}
```

## Token Usage in Subsequent Requests

After successful login, the JWT token is used to authenticate all subsequent requests.

### Frontend: Automatic Token Inclusion

The Axios request interceptor (in `api.js`) automatically includes the token from localStorage in the `Authorization` header for all API requests.

### Backend: Authentication Filter

File: [`src/main/java/fi/haagahelia/messenger/config/AuthenticationFilter.java`](src/main/java/fi/haagahelia/messenger/config/AuthenticationFilter.java)

The `AuthenticationFilter` validates the JWT token on every request:

1. **Execution**: Runs before every request as a `OncePerRequestFilter`

2. **Token Extraction**: Extracts the JWT token from the `Authorization` header

3. **Token Validation**: Uses `JwtService.getAuthUser()` to parse and validate the token
   - Parses the JWT
   - Verifies the signature
   - Extracts the username from the token's subject

4. **Security Context**: If valid, creates an `Authentication` object and sets it in Spring Security's `SecurityContextHolder`
   - This makes the user authenticated for the current request

5. **Filter Chain**: Continues the filter chain

```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, java.io.IOException {
    String jws = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (jws != null) {
        String user = jwtService.getAuthUser(request);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, List.of());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
}
```

### Backend: JWT Token Validation

File: [`src/main/java/fi/haagahelia/messenger/service/JwtService.java`](src/main/java/fi/haagahelia/messenger/service/JwtService.java)

The `getAuthUser` method validates and extracts the username from a JWT token:

1. Extracts the `Authorization` header value
2. Returns null if no authorization header exists
3. Parses the JWT token
   - Attempts to remove the "Bearer " prefix (using `.replace(PREFIX, "")`)
   - If no prefix exists, the replace has no effect and the raw token is parsed
   - Verifies the signature using the signing key
   - Extracts the subject (username) from the token
4. Returns null if token is invalid or expired (caught in the exception handler)

```java
public String getAuthUser(HttpServletRequest request) {
    String authorizationHeaderValue = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (authorizationHeaderValue == null) {
        return null;
    }

    JwtParser parser = getJwtParser();

    try {
        String user = parser
                .parseClaimsJws(authorizationHeaderValue.replace(PREFIX, ""))
                .getBody().getSubject();

        return user;
    } catch (Exception e) {
        return null;
    }
}
```

### Backend: Security Configuration

File: [`src/main/java/fi/haagahelia/messenger/config/SecurityConfig.java`](src/main/java/fi/haagahelia/messenger/config/SecurityConfig.java)

The security configuration defines which endpoints require authentication:

1. **Session Management**: Configured as STATELESS
   - No server-side sessions are created
   - Each request must include the JWT token

2. **Public Endpoints**: Certain endpoints are accessible without authentication
   - `POST /api/auth/login` - login endpoint
   - `POST /api/users` - user registration
   - `GET /api/messages/**` - viewing messages
   - Swagger documentation endpoints

3. **Protected Endpoints**: All other endpoints require authentication

4. **Filter Registration**: Adds `AuthenticationFilter` before Spring Security's default authentication filter

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable()).cors(withDefaults())
            .sessionManagement(
                    sessionManagement -> sessionManagement
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                    .requestMatchers(
                            antMatcher(HttpMethod.POST, "/api/auth/login"),
                            antMatcher(HttpMethod.POST, "/api/users"),
                            antMatcher(HttpMethod.GET, "/api/messages"),
                            antMatcher(HttpMethod.GET, "/api/messages/*"),
                            antMatcher("/error"))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```
