# Spring Cloud Microservices - Complete Reference Guide

This document contains comprehensive notes for Spring Cloud Microservices architecture including properties, dependencies, annotations, and troubleshooting guides.

**Tech Stack:** Spring Boot 4.0.5 | Spring Cloud 2025.1.1 | Java 21

---

## Table of Contents

### Part 1: Reference Notes
1. [Architecture Overview](#architecture-overview)
2. [Service Ports](#service-ports)
3. [Dependencies Reference](#dependencies-reference)
4. [Annotations Reference](#annotations-reference)
5. [Properties Reference](#properties-reference)
6. [API Gateway Configuration](#api-gateway-configuration)
7. [Feign Client Configuration](#feign-client-configuration)
8. [Resilience4j - Fault Tolerance](#resilience4j----fault-tolerance)
9. [Distributed Tracing & Zipkin](#distributed-tracing--zipkin)
10. [Docker & Containerization](#docker--containerization)

### Part 2: Troubleshooting
10. [Config Server Hangs / Read Timeout](#issue-config-server-hangs--read-timeout-on-endpoints)
11. [Eureka Server Port Override Issue](#issue-eureka-server-port-not-working)
12. [Eureka Client Connection Failed](#issue-eureka-client-connection-failed--service-not-registering)
13. [Version Compatibility](#related-issues)
14. [Zipkin Connection Refused](#issue-zipkin-connection-refused-javanetconnectexception-from-zipkinhttpclientsender)
15. [Trailing Slash 404 Error](#issue-trailing-slash-in-url-causes-404-not-found)
16. [SLF4J Simple vs Logback Conflict](#issue-slf4j-simple-conflicts-with-logback--breaks-tracing)
17. [Feign Client Requests Not Showing in Zipkin](#issue-feign-client-requests-not-showing-in-zipkin-traces)
18. [RestTemplate Requests Not Showing in Zipkin](#issue-resttemplate--restclient-requests-not-showing-in-zipkin-traces)

---

# Part 1: Reference Notes

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              SPRING CLOUD ARCHITECTURE                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│    ┌──────────────────┐                                                      │
│    │   Config Server  │  ← Centralized configuration management              │
│    │    (port 8888)   │    Serves properties from Git/Local                  │
│    └────────┬─────────┘                                                      │
│             │                                                                │
│             │ Fetch Config                                                   │
│             ▼                                                                │
│    ┌──────────────────┐                                                      │
│    │  Naming Server   │  ← Service Registry (Eureka)                         │
│    │  (port 8761)     │    All services register here                        │
│    └────────┬─────────┘                                                      │
│             │                                                                │
│      ┌──────┴──────┬───────────────┐                                        │
│      │             │               │                                         │
│      ▼             ▼               ▼                                         │
│ ┌──────────┐ ┌──────────┐   ┌─────────────┐                                 │
│ │ Currency │ │ Currency │   │ API Gateway │ ← Single entry point             │
│ │ Exchange │ │Conversion│   │ (port 8765) │   Routes & Load Balances         │
│ │(port 8000)│ │(port 8100)│  └─────────────┘                                 │
│ │(port 9000)│ └──────────┘                                                   │
│ └──────────┘      │                                                          │
│      ▲            │ Feign Client Call                                        │
│      └────────────┘ (Load Balanced)                                          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Service Ports

| Service | Port | Purpose |
|---------|------|---------|
| **Naming Server (Eureka)** | 8761 | Service Discovery & Registry |
| **Config Server** | 8888 | Centralized Configuration |
| **API Gateway** | 8765 | Single Entry Point, Routing, Load Balancing |
| **Currency Exchange** | 8000, 9000 | Business Service (multiple instances for LB) |
| **Currency Conversion** | 8100 | Business Service (calls Currency Exchange) |

### Startup Order

```
1. Naming Server (Eureka)     → http://localhost:8761
2. Config Server              → http://localhost:8888
3. API Gateway                → http://localhost:8765
4. Currency Exchange          → http://localhost:8000 (and 9000 for LB testing)
5. Currency Conversion        → http://localhost:8100
```

---

## Dependencies Reference

### 1. Naming Server (Eureka Server)

```xml
<!-- EUREKA SERVER - Service Discovery & Registry -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

| Dependency | Purpose |
|------------|---------|
| `spring-cloud-starter-netflix-eureka-server` | Enables this application to act as a Eureka Server where other services can register themselves and discover other services |

---

### 2. Config Server

```xml
<!-- CONFIG SERVER - Centralized Configuration Management -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>

<!-- ACTUATOR - Health checks & monitoring endpoints -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

| Dependency | Purpose |
|------------|---------|
| `spring-cloud-config-server` | Enables this application to serve configuration properties to other microservices from Git repository or local filesystem |
| `spring-boot-starter-actuator` | Provides production-ready features like health checks, metrics, and monitoring endpoints |

---

### 3. API Gateway

```xml
<!-- GATEWAY - Reactive API Gateway for routing -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
</dependency>

<!-- EUREKA CLIENT - Register with & discover services from Eureka -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>

<!-- LOADBALANCER - Client-side load balancing -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>

<!-- ACTUATOR - Health checks & gateway routes endpoint -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

| Dependency | Purpose |
|------------|---------|
| `spring-cloud-starter-gateway-server-webflux` | Reactive API Gateway built on Spring WebFlux. Provides routing, filtering, and load balancing capabilities |
| `spring-cloud-starter-netflix-eureka-client` | Registers Gateway with Eureka and enables service discovery to route requests to registered services |
| `spring-cloud-starter-loadbalancer` | **CRITICAL!** Enables client-side load balancing. Without this, `lb://service-name` URIs won't work |
| `spring-boot-starter-actuator` | Provides `/actuator/gateway/routes` endpoint to view configured routes |

---

### 4. Currency Exchange Service (Business Service)

```xml
<!-- WEB MVC - REST API support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>

<!-- JPA - Database access with Hibernate -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- H2 DATABASE - In-memory database for development -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- H2 CONSOLE - Web-based database console -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-h2console</artifactId>
</dependency>

<!-- CONFIG CLIENT - Fetch config from Config Server -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>

<!-- EUREKA CLIENT - Register with Eureka -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-netflix-eureka-client</artifactId>
</dependency>
```

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-webmvc` | Provides Spring MVC for building REST APIs with `@RestController`, `@GetMapping`, etc. |
| `spring-boot-starter-data-jpa` | Spring Data JPA for database operations with repositories and entities |
| `h2` | Lightweight in-memory database for development |
| `spring-boot-h2console` | Web console to view H2 database at `/h2-console` |
| `spring-cloud-starter-config` | Enables fetching configuration from Config Server using `spring.config.import` |
| `spring-cloud-netflix-eureka-client` | Registers this service with Eureka for service discovery |

---

### 5. Currency Conversion Service (Feign Client)

```xml
<!-- WEB MVC - REST API support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>

<!-- OPENFEIGN - Declarative REST client -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>

<!-- CONFIG CLIENT - Fetch config from Config Server -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>

<!-- EUREKA CLIENT - Register with & discover services -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-netflix-eureka-client</artifactId>
</dependency>

<!-- LOADBALANCER - Client-side load balancing for Feign -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

| Dependency | Purpose |
|------------|---------|
| `spring-cloud-starter-openfeign` | Declarative REST client. Define interface with annotations, Feign implements it |
| `spring-cloud-starter-loadbalancer` | **CRITICAL!** Required for Feign to load balance between multiple service instances. Without this, you get `IllegalStateException: No Feign Client for loadBalancing defined` |

---

### Dependency Management (All Services)

```xml
<properties>
    <java.version>21</java.version>
    <spring-cloud.version>2025.1.1</spring-cloud.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

| Element | Purpose |
|---------|---------|
| `spring-cloud.version` | Defines Spring Cloud version. Must be compatible with Spring Boot version |
| `dependencyManagement` | Imports Spring Cloud BOM (Bill of Materials) to manage all Spring Cloud dependency versions automatically |

---

## Annotations Reference

### 1. `@SpringBootApplication`

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

| Annotation | Purpose |
|------------|---------|
| `@SpringBootApplication` | Combination of `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`. Entry point for Spring Boot application |

**Used in:** ALL services

---

### 2. `@EnableEurekaServer`

```java
@SpringBootApplication
@EnableEurekaServer
public class NamingServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(NamingServerApplication.class, args);
    }
}
```

| Annotation | Purpose |
|------------|---------|
| `@EnableEurekaServer` | Transforms application into a Eureka Server (Service Registry). Other services can register with this server |

**Used in:** Naming Server only

---

### 3. `@EnableConfigServer`

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

| Annotation | Purpose |
|------------|---------|
| `@EnableConfigServer` | Transforms application into a Config Server. Serves configuration properties from Git/Local to other services |

**Used in:** Config Server only

---

### 4. `@EnableFeignClients`

```java
@SpringBootApplication
@EnableFeignClients
public class CurrencyConversionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CurrencyConversionServiceApplication.class, args);
    }
}
```

| Annotation | Purpose |
|------------|---------|
| `@EnableFeignClients` | Enables Feign client scanning. Spring will look for interfaces annotated with `@FeignClient` and create implementations |

**Used in:** Currency Conversion Service (any service that calls other services via Feign)

---

### 5. `@FeignClient`

```java
@FeignClient(name = "currency-exchange")
public interface CurrencyExchangeProxy {

    @GetMapping("/currency-exchange/from/{from}/to/{to}")
    public CurrencyConversion retrieveExchangeValue(
            @PathVariable("from") String from,
            @PathVariable("to") String to);
}
```

| Annotation | Purpose |
|------------|---------|
| `@FeignClient(name = "service-name")` | Declares a REST client for the specified service. `name` must match the `spring.application.name` of target service registered in Eureka |

**Parameters:**
| Parameter | Description |
|-----------|-------------|
| `name` | Service name as registered in Eureka. Used for service discovery and load balancing |
| `url` | (Optional) Hardcoded URL. **Don't use this if you want load balancing!** |

**Used in:** Currency Conversion Service

---

### 6. `@RestController`

```java
@RestController
public class CurrencyExchangeController {

    @GetMapping("/currency-exchange/from/{from}/to/{to}")
    public CurrencyExchange retrieveExchangeValue(
            @PathVariable String from,
            @PathVariable String to) {
        // ...
    }
}
```

| Annotation | Purpose |
|------------|---------|
| `@RestController` | Combines `@Controller` and `@ResponseBody`. All methods return data (JSON) directly, not view names |
| `@GetMapping` | Maps HTTP GET requests to the method |
| `@PathVariable` | Extracts values from URL path (e.g., `/from/{from}` → `from` parameter) |

**Used in:** Currency Exchange Controller, Currency Conversion Controller

---

### 7. `@Configuration` and `@Bean`

```java
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("currency-exchange", r -> r
                        .path("/currency-exchange/**")
                        .uri("lb://currency-exchange"))
                .build();
    }
}
```

| Annotation | Purpose |
|------------|---------|
| `@Configuration` | Marks class as a source of bean definitions |
| `@Bean` | Method return value is registered as a Spring bean |

**Used in:** API Gateway (GatewayConfig)

---

### 8. `@Autowired`

```java
@RestController
public class CurrencyExchangeController {

    @Autowired
    private Environment environment;

    @Autowired
    private CurrencyExchangeRepository repository;
}
```

| Annotation | Purpose |
|------------|---------|
| `@Autowired` | Automatic dependency injection. Spring provides the required bean |

**Used in:** Controllers, Services

---

### Annotations Summary Table

| Annotation | Where Used | Purpose |
|------------|------------|---------|
| `@SpringBootApplication` | All main classes | Entry point |
| `@EnableEurekaServer` | Naming Server | Make it a Eureka Server |
| `@EnableConfigServer` | Config Server | Make it a Config Server |
| `@EnableFeignClients` | Currency Conversion | Enable Feign clients |
| `@FeignClient` | Proxy interfaces | Declare REST client |
| `@RestController` | Controllers | REST API endpoints |
| `@GetMapping` | Controller methods | Map GET requests |
| `@PathVariable` | Method parameters | Extract URL path values |
| `@Configuration` | Config classes | Define beans |
| `@Bean` | Config methods | Register beans |
| `@Autowired` | Fields/Constructors | Dependency injection |

---

## Properties Reference

### 1. Naming Server (Eureka) Properties

**File:** `naming-server/src/main/resources/application.properties`

```properties
spring.application.name=naming-server
server.port=8761
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

| Property | Value | Description |
|----------|-------|-------------|
| `spring.application.name` | `naming-server` | Unique name for this service |
| `server.port` | `8761` | Default Eureka port. Dashboard available at `http://localhost:8761` |
| `eureka.client.register-with-eureka` | `false` | Don't register itself with Eureka (it IS the server) |
| `eureka.client.fetch-registry` | `false` | Don't fetch registry (it IS the registry) |

---

### 2. Config Server Properties

**File:** `config-server/src/main/resources/application.properties`

```properties
spring.application.name=Config-Server
server.port=8888

# Git Configuration (Remote)
spring.cloud.config.server.git.uri=https://github.com/ravvaanilkumar/LearningRepository
spring.cloud.config.server.git.default-label=main
spring.cloud.config.server.git.clone-on-start=true
spring.cloud.config.server.git.timeout=10
spring.cloud.config.server.git.refresh-rate=0

# Local Configuration (Alternative)
#spring.profiles.active=native
#spring.cloud.config.server.native.search-locations=classpath:/config

# Disable config client on server itself
spring.cloud.config.enabled=false
spring.cloud.config.import-check.enabled=false

logging.level.org.springframework.cloud.config=DEBUG
```

| Property | Value | Description |
|----------|-------|-------------|
| `spring.application.name` | `Config-Server` | Unique name for this service |
| `server.port` | `8888` | Default Config Server port |
| `spring.cloud.config.server.git.uri` | GitHub URL | Git repository containing config files |
| `spring.cloud.config.server.git.default-label` | `main` | Git branch to use |
| `spring.cloud.config.server.git.clone-on-start` | `true` | Clone repo on startup for faster first request |
| `spring.cloud.config.server.git.timeout` | `10` | Git operation timeout in seconds |
| `spring.cloud.config.server.git.refresh-rate` | `0` | How often to check for updates (0 = every request) |
| `spring.profiles.active` | `native` | Use local filesystem instead of Git |
| `spring.cloud.config.server.native.search-locations` | `classpath:/config` | Local folder containing config files |
| `spring.cloud.config.enabled` | `false` | **IMPORTANT!** Disable config client on server itself to avoid infinite loop |
| `spring.cloud.config.import-check.enabled` | `false` | Disable import check on server |

---

### 3. API Gateway Properties

**File:** `api-gateway/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: api-gateway
  main:
    web-application-type: reactive
  web:
    resources:
      add-mappings: false
  cloud:
    discovery:
      reactive:
        enabled: true
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true

server:
  port: 8765

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
    fetch-registry: true
    register-with-eureka: true

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    gateway:
      enabled: true
```

| Property | Value | Description |
|----------|-------|-------------|
| `spring.application.name` | `api-gateway` | Service name registered in Eureka |
| `spring.main.web-application-type` | `reactive` | Use reactive web stack (required for Gateway) |
| `spring.web.resources.add-mappings` | `false` | Disable static resource handling to avoid 404 errors |
| `spring.cloud.discovery.reactive.enabled` | `true` | Enable reactive service discovery |
| `spring.cloud.gateway.discovery.locator.enabled` | `true` | Auto-create routes from Eureka services |
| `spring.cloud.gateway.discovery.locator.lower-case-service-id` | `true` | Allow lowercase service names in URLs |
| `server.port` | `8765` | Gateway port |
| `eureka.client.service-url.defaultZone` | `http://localhost:8761/eureka` | Eureka server URL (**must include `/eureka` suffix**) |
| `eureka.client.fetch-registry` | `true` | Fetch service registry to route requests |
| `eureka.client.register-with-eureka` | `true` | Register Gateway with Eureka |
| `management.endpoints.web.exposure.include` | `*` | Expose all actuator endpoints |
| `management.endpoint.gateway.enabled` | `true` | Enable `/actuator/gateway/routes` endpoint |

---

### 4. Currency Exchange Properties

**File:** `currency-exchange/src/main/resources/application.properties`

```properties
spring.application.name=currency-exchange

spring.config.import=optional:configserver:http://localhost:8888

eureka.client.service-url.defaultZone=http://localhost:8761/eureka

# Database (usually from Config Server)
spring.jpa.show-sql=true
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true
server.port=8000
spring.jpa.defer-datasource-initialization=true
```

| Property | Value | Description |
|----------|-------|-------------|
| `spring.application.name` | `currency-exchange` | **CRITICAL!** Must match `@FeignClient(name=...)` in other services |
| `spring.config.import` | `optional:configserver:http://localhost:8888` | Fetch config from Config Server. `optional:` means don't fail if unavailable |
| `eureka.client.service-url.defaultZone` | `http://localhost:8761/eureka` | Register with Eureka |
| `server.port` | `8000` | Service port (can run multiple instances on 8001, 9000, etc.) |
| `spring.jpa.show-sql` | `true` | Log SQL queries |
| `spring.datasource.url` | `jdbc:h2:mem:testdb` | H2 in-memory database URL |
| `spring.h2.console.enabled` | `true` | Enable H2 web console at `/h2-console` |
| `spring.jpa.defer-datasource-initialization` | `true` | Run `data.sql` after Hibernate creates tables |

---

### 5. Currency Conversion Properties

**File:** `currency-conversion-service/src/main/resources/application.properties`

```properties
spring.application.name=currency-conversion-service

spring.config.import=optional:configserver:http://localhost:8888

eureka.client.service-url.defaultZone=http://localhost:8761/eureka

server.port=8100
```

| Property | Value | Description |
|----------|-------|-------------|
| `spring.application.name` | `currency-conversion-service` | Unique service name |
| `spring.config.import` | `optional:configserver:http://localhost:8888` | Fetch config from Config Server |
| `eureka.client.service-url.defaultZone` | `http://localhost:8761/eureka` | Register with Eureka |
| `server.port` | `8100` | Service port |

---

### Properties Quick Reference Table

| Property | Purpose | Used In |
|----------|---------|---------|
| `spring.application.name` | Service name for Eureka | ALL |
| `server.port` | HTTP port | ALL |
| `spring.config.import=optional:configserver:URL` | Fetch config from Config Server | Clients only |
| `eureka.client.service-url.defaultZone` | Eureka server URL | Clients only |
| `eureka.client.register-with-eureka=false` | Don't register | Eureka Server only |
| `eureka.client.fetch-registry=false` | Don't fetch | Eureka Server only |
| `spring.cloud.config.enabled=false` | Disable config client | Config Server only |
| `spring.cloud.config.import-check.enabled=false` | Disable import check | Config Server only |
| `spring.cloud.gateway.discovery.locator.enabled` | Auto-discover routes | API Gateway only |

---

## API Gateway Configuration

### Programmatic Route Configuration (Recommended)

When YAML-based discovery locator doesn't work, use programmatic configuration:

**File:** `api-gateway/src/main/java/com/anil/springboot/api_gateway/GatewayConfig.java`

```java
package com.anil.springboot.api_gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Route for Currency Exchange Service
                .route("currency-exchange", r -> r
                        .path("/currency-exchange/**")
                        .uri("lb://currency-exchange"))
                // Route for Currency Conversion Service
                .route("currency-conversion-service", r -> r
                        .path("/currency-conversion/**")
                        .uri("lb://currency-conversion-service"))
                .build();
    }
}
```

### How Routes Work

```
        INCOMING REQUEST                         GATEWAY ROUTES TO
        ───────────────                         ─────────────────

http://localhost:8765/currency-exchange/from/USD/to/INR
                      └──────────┬──────────┘
                                 │
                      Matches: /currency-exchange/**
                                 │
                                 ▼
                      lb://currency-exchange
                      (Load balanced to port 8000 or 9000)


http://localhost:8765/currency-conversion/feign/from/USD/to/INR/quantity/10
                      └───────────┬───────────┘
                                  │
                      Matches: /currency-conversion/**
                                  │
                                  ▼
                      lb://currency-conversion-service
                      (Load balanced to port 8100)
```

### Key Points

| Element | Description |
|---------|-------------|
| `.path("/currency-exchange/**")` | URL pattern to match. `**` means any path after |
| `.uri("lb://currency-exchange")` | `lb://` = load balanced. `currency-exchange` = service name in Eureka |
| Route ID | Unique identifier for the route (used in logs/actuator) |

### Verify Routes

```
http://localhost:8765/actuator/gateway/routes
```

---

## Feign Client Configuration

### Basic Feign Client (Without Load Balancing)

```java
// ❌ HARDCODED URL - No load balancing
@FeignClient(name = "currency-exchange", url = "localhost:8000")
public interface CurrencyExchangeProxy {
    @GetMapping("/currency-exchange/from/{from}/to/{to}")
    public CurrencyConversion retrieveExchangeValue(
            @PathVariable("from") String from,
            @PathVariable("to") String to);
}
```

### Load Balanced Feign Client (Recommended)

```java
// ✅ LOAD BALANCED - Uses Eureka for service discovery
@FeignClient(name = "currency-exchange")
public interface CurrencyExchangeProxy {
    @GetMapping("/currency-exchange/from/{from}/to/{to}")
    public CurrencyConversion retrieveExchangeValue(
            @PathVariable("from") String from,
            @PathVariable("to") String to);
}
```

### Requirements for Load Balancing

1. **Remove `url` parameter** from `@FeignClient`
2. **Add dependency:**
   ```xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-loadbalancer</artifactId>
   </dependency>
   ```
3. **Add Eureka config:**
   ```properties
   eureka.client.service-url.defaultZone=http://localhost:8761/eureka
   ```

### How Load Balancing Works

```
┌────────────────────────────────┐
│    Currency Conversion         │
│         (port 8100)            │
│                                │
│  @FeignClient(name=            │
│    "currency-exchange")        │
└──────────────┬─────────────────┘
               │
               │ 1. "I need currency-exchange service"
               ▼
┌────────────────────────────────┐
│    Spring Cloud LoadBalancer   │
│                                │
│  Asks Eureka: "Where is        │
│  currency-exchange?"           │
└──────────────┬─────────────────┘
               │
               │ 2. Returns: [8000, 9000]
               ▼
┌────────────────────────────────┐
│    Round Robin Selection       │
│                                │
│  Request 1 → port 8000         │
│  Request 2 → port 9000         │
│  Request 3 → port 8000         │
│  ...                           │
└──────────────┬─────────────────┘
               │
        ┌──────┴──────┐
        ▼             ▼
┌─────────────┐ ┌─────────────┐
│  Currency   │ │  Currency   │
│  Exchange   │ │  Exchange   │
│  port 8000  │ │  port 9000  │
└─────────────┘ └─────────────┘
```

### Running Multiple Instances

```powershell
# Terminal 1 - Instance on port 8000
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8000"

# Terminal 2 - Instance on port 9000
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9000"
```

### Verify Load Balancing

The `environment` field in the response shows which instance handled the request:

```json
{
  "id": 10001,
  "from": "USD",
  "to": "INR",
  "conversionMultiple": 65,
  "environment": "8000"    // ← Changes between 8000 and 9000
}
```

---

## Resilience4j - Fault Tolerance

### Introduction

**Resilience4j** is a lightweight, easy-to-use fault tolerance library designed for Java 8+ and functional programming. It provides several patterns (Retry, Circuit Breaker, Rate Limiter, Bulkhead, Time Limiter) to make your microservices resilient against failures. Unlike Hystrix (which is in maintenance mode), Resilience4j is actively maintained and designed for Spring Boot 2/3+ and Java 8+ functional programming style.

---

### Required Dependencies (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.5</version>
        <relativePath/>
    </parent>
    
    <groupId>com.anil.springboot</groupId>
    <artifactId>resilience-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    
    <properties>
        <java.version>21</java.version>
    </properties>
    
    <dependencies>
        <!-- ACTUATOR - Health checks & resilience endpoints -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- WEB MVC - REST API support -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- RESILIENCE4J - Fault tolerance library with AOP support -->
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>2.2.0</version>
        </dependency>

        <!-- ASPECTJ WEAVER - Required for annotation-based AOP (@Retry, @CircuitBreaker, etc.) -->
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
        </dependency>

        <!-- SPRING WEB - For RestClient -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
        </dependency>

        <!-- TEST - For unit and integration tests -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-actuator` | Provides health checks and exposes Resilience4j metrics endpoints (`/actuator/circuitbreakers`, `/actuator/retries`) |
| `spring-boot-starter-webmvc` | Spring MVC for building REST APIs |
| `resilience4j-spring-boot3` | Core Resilience4j library with Spring Boot 3/4 auto-configuration and AOP support |
| `aspectjweaver` | **CRITICAL!** Required for annotation-based AOP. Without this, `@Retry`, `@CircuitBreaker` annotations won't work |
| `spring-web` | Provides `RestClient` for making HTTP calls |

---

### Main Application Class

**File:** `ResilienceDemoApplication.java`

```java
package com.anil.springboot.resilience_demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class ResilienceDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResilienceDemoApplication.class, args);
    }

    // RestClient bean for making HTTP calls
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
}
```

| Annotation/Bean | Purpose |
|-----------------|---------|
| `@SpringBootApplication` | Entry point - combines `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan` |
| `@Bean RestClient` | Creates a RestClient bean for making HTTP calls to external services |

> **Note:** No special annotation like `@EnableRetry` is needed. Resilience4j auto-configures via `resilience4j-spring-boot3` dependency.

---

### Controller with Resilience Patterns

**File:** `SampleController.java`

```java
package com.anil.springboot.resilience_demo;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class SampleController {

    private static final Logger logger = LoggerFactory.getLogger(SampleController.class);

    @Autowired
    private RestClient restClient;

    // ==================== RETRY PATTERN ====================
    @GetMapping("/data")
    @Retry(name = "sampleRetry", fallbackMethod = "getDataFallback")
    public String getDataDemoForRetry() {
        logger.info("Attempting to call the external service...");
        restClient.get().uri("http://localhost:8081/unavailable-endpoint")
                .retrieve()
                .body(String.class);
        return "Hello, World!";
    }

    public String getDataFallback(Exception e) {
        return "Fallback response due to: " + e.getMessage();
    }

    // ==================== CIRCUIT BREAKER PATTERN ====================
    @GetMapping("/circuit-breaker")
    @CircuitBreaker(name = "sampleCircuitBreaker", fallbackMethod = "getCircuitFallback")
    public String getDataForCircuitBreaker() {
        logger.info("Circuit Breaker: Attempting to call the external service...");
        restClient.get().uri("http://localhost:8081/unavailable-endpoint")
                .retrieve()
                .body(String.class);
        return "Hello, World!";
    }

    public String getCircuitFallback(Exception e) {
        return "Circuit Breaker Fallback response due to: " + e.getMessage();
    }

    // ==================== RATE LIMITER PATTERN ====================
    @GetMapping("/rate-limiter")
    @RateLimiter(name = "myRateLimiter", fallbackMethod = "getRateLimitFallback")
    public String getDataForRateLimiter() {
        logger.info("Rate Limiter: Processing request...");
        return "Rate Limiter Demo Response";
    }

    public String getRateLimitFallback(Exception e) {
        return "Rate Limiter Fallback response due to: " + e.getMessage();
    }

    // ==================== BULKHEAD PATTERN ====================
    @GetMapping("/bulkhead")
    @Bulkhead(name = "myBulkhead", fallbackMethod = "getBulkHeadFallback")
    public String getBulkHeadDemo() {
        logger.info("Bulkhead: Processing request...");
        return "Bulkhead Demo Response";
    }

    public String getBulkHeadFallback(Exception e) {
        return "Bulkhead Fallback response due to: " + e.getMessage();
    }
}
```

### Resilience4j Annotations Reference

| Annotation | Purpose | Parameters |
|------------|---------|------------|
| `@Retry` | Automatically retry failed operations | `name` = config name, `fallbackMethod` = method to call after all retries fail |
| `@CircuitBreaker` | Stop calling failing service, fail fast | `name` = config name, `fallbackMethod` = method when circuit is open |
| `@RateLimiter` | Limit calls per time period | `name` = config name, `fallbackMethod` = method when rate limit exceeded |
| `@Bulkhead` | Limit concurrent calls | `name` = config name, `fallbackMethod` = method when bulkhead is full |
| `@TimeLimiter` | Limit execution time | `name` = config name, `fallbackMethod` = method on timeout |

### Fallback Method Rules

1. **Same return type** as the original method
2. **Same parameters** as the original method + `Exception e` parameter
3. **Must be in the same class** (or a parent class)

```java
// Original method
@Retry(name = "myRetry", fallbackMethod = "fallback")
public String getData(String param1, int param2) { ... }

// Fallback method - same params + Exception
public String fallback(String param1, int param2, Exception e) {
    return "Fallback: " + e.getMessage();
}
```

---

### Application Properties Configuration

**File:** `application.properties`

```properties
spring.application.name=resilience-demo
server.port=8080

# ==================== RETRY CONFIGURATION ====================
resilience4j.retry.instances.sampleRetry.maxAttempts=3
resilience4j.retry.instances.sampleRetry.waitDuration=1s
resilience4j.retry.instances.sampleRetry.retryExceptions=java.io.IOException,java.net.ConnectException
resilience4j.retry.instances.sampleRetry.ignoreExceptions=java.lang.IllegalArgumentException

# ==================== CIRCUIT BREAKER CONFIGURATION ====================
resilience4j.circuitbreaker.instances.sampleCircuitBreaker.failureRateThreshold=50
resilience4j.circuitbreaker.instances.sampleCircuitBreaker.slowCallRateThreshold=50
resilience4j.circuitbreaker.instances.sampleCircuitBreaker.slowCallDurationThreshold=2s
resilience4j.circuitbreaker.instances.sampleCircuitBreaker.waitDurationInOpenState=10s
resilience4j.circuitbreaker.instances.sampleCircuitBreaker.permittedNumberOfCallsInHalfOpenState=3
resilience4j.circuitbreaker.instances.sampleCircuitBreaker.slidingWindowSize=10
resilience4j.circuitbreaker.instances.sampleCircuitBreaker.slidingWindowType=COUNT_BASED

# ==================== RATE LIMITER CONFIGURATION ====================
resilience4j.ratelimiter.instances.myRateLimiter.limitForPeriod=5
resilience4j.ratelimiter.instances.myRateLimiter.limitRefreshPeriod=1s
resilience4j.ratelimiter.instances.myRateLimiter.timeoutDuration=500ms

# ==================== BULKHEAD CONFIGURATION ====================
resilience4j.bulkhead.instances.myBulkhead.maxConcurrentCalls=3
resilience4j.bulkhead.instances.myBulkhead.maxWaitDuration=100ms

# ==================== TIME LIMITER CONFIGURATION ====================
resilience4j.timelimiter.instances.myTimeLimiter.timeoutDuration=2s
resilience4j.timelimiter.instances.myTimeLimiter.cancelRunningFuture=true

# ==================== ACTUATOR ENDPOINTS ====================
management.endpoints.web.exposure.include=health,info,circuitbreakers,retries,ratelimiters,bulkheads
management.health.circuitbreakers.enabled=true
management.endpoint.health.show-details=always
```

### Properties Explanation

#### Retry Properties

| Property | Example Value | Description |
|----------|---------------|-------------|
| `maxAttempts` | `3` | Maximum retry attempts (including initial call) |
| `waitDuration` | `1s` | Wait time between retries |
| `retryExceptions` | `java.io.IOException` | Exceptions that trigger a retry |
| `ignoreExceptions` | `java.lang.IllegalArgumentException` | Exceptions that should NOT trigger retry |

#### Circuit Breaker Properties

| Property | Example Value | Description |
|----------|---------------|-------------|
| `failureRateThreshold` | `50` | % of failures to open circuit (50 = 50%) |
| `slowCallRateThreshold` | `50` | % of slow calls to open circuit |
| `slowCallDurationThreshold` | `2s` | Duration to consider a call "slow" |
| `waitDurationInOpenState` | `10s` | Time to wait before trying again (OPEN → HALF_OPEN) |
| `permittedNumberOfCallsInHalfOpenState` | `3` | Test calls allowed in HALF_OPEN state |
| `slidingWindowSize` | `10` | Number of calls to calculate failure rate |
| `slidingWindowType` | `COUNT_BASED` | `COUNT_BASED` or `TIME_BASED` |

#### Rate Limiter Properties

| Property | Example Value | Description |
|----------|---------------|-------------|
| `limitForPeriod` | `5` | Max calls allowed per refresh period |
| `limitRefreshPeriod` | `1s` | Period after which limit resets |
| `timeoutDuration` | `500ms` | How long a thread waits for permission |

#### Bulkhead Properties

| Property | Example Value | Description |
|----------|---------------|-------------|
| `maxConcurrentCalls` | `3` | Maximum concurrent calls allowed |
| `maxWaitDuration` | `100ms` | Max time to wait for entry |

---

### Resilience4j Patterns Comparison

| Pattern | Purpose | Use Case | Behavior |
|---------|---------|----------|----------|
| **Retry** | Retry failed operations | Transient failures (network hiccup) | Attempts same operation multiple times |
| **Circuit Breaker** | Stop calling failing service, fail fast | Prolonged failures (service down) | Fails fast without calling service |
| **Rate Limiter** | Limit request rate | Prevent overload | Rejects requests exceeding rate |
| **Bulkhead** | Limit concurrent calls | Resource exhaustion | Isolates failures, limits threads |
| **Time Limiter** | Limit execution time | Slow dependencies | Cancels if taking too long |

### Retry vs Circuit Breaker

```
┌─────────────────────────────────────────────────────────────────────┐
│                     RETRY vs CIRCUIT BREAKER                         │
├────────────────────────────┬────────────────────────────────────────┤
│          RETRY             │           CIRCUIT BREAKER               │
├────────────────────────────┼────────────────────────────────────────┤
│ Stateless                  │ Stateful (CLOSED/OPEN/HALF_OPEN)       │
│ Tries N times              │ Fails fast when OPEN                   │
│ Good for transient errors  │ Good for prolonged failures            │
│ Each call is independent   │ Tracks failure rate over time          │
│ Retries same operation     │ Stops calling, returns fallback        │
├────────────────────────────┴────────────────────────────────────────┤
│                                                                      │
│  Request 1 ──→ [RETRY: Fail → Retry → Retry → Success] ──→ Response │
│                                                                      │
│  Request N ──→ [CIRCUIT BREAKER: OPEN] ──→ Fallback immediately     │
│                 (Doesn't even try to call the service)               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Circuit Breaker States

```
     ┌──────────┐    Failure Rate > Threshold    ┌──────────┐
     │  CLOSED  │ ─────────────────────────────→ │   OPEN   │
     │ (Normal) │                                │(Fail Fast)│
     └────┬─────┘                                └─────┬────┘
          │                                            │
          │                                            │ Wait Duration
          │                                            │ Elapsed
          │                                            ▼
          │                                     ┌───────────┐
          │      Success Rate OK                │ HALF_OPEN │
          └─────────────────────────────────────│  (Test)   │
                                                └───────────┘
```

---

### Testing Resilience4j

#### Basic Test Class

**File:** `ResilienceDemoApplicationTests.java`

```java
package com.anil.springboot.resilience_demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ResilienceDemoApplicationTests {

    @Test
    void contextLoads() {
        // Verifies application context loads successfully
    }
}
```

#### Manual Testing with cURL/Browser

**Test Retry Pattern:**
```powershell
# Call multiple times - observe retry attempts in logs
Invoke-RestMethod "http://localhost:8080/data"

# Expected: Fallback response after 3 retries
# Logs show: "Attempting to call the external service..." 3 times
```

**Test Circuit Breaker:**
```powershell
# Call rapidly to trigger circuit breaker
1..20 | ForEach-Object { Invoke-RestMethod "http://localhost:8080/circuit-breaker" }

# After ~10 failures (50% threshold), circuit opens
# Subsequent calls return fallback immediately without attempting
```

**Test Rate Limiter:**
```powershell
# Call rapidly to exceed rate limit (5 calls/second)
1..10 | ForEach-Object { Invoke-RestMethod "http://localhost:8080/rate-limiter" }

# After 5 calls, next calls return fallback
```

**Test Bulkhead:**
```powershell
# Run concurrent requests (requires multiple terminals or async calls)
# When maxConcurrentCalls (3) is exceeded, fallback is returned
```

#### Check Actuator Endpoints

```powershell
# View all circuit breakers
Invoke-RestMethod "http://localhost:8080/actuator/circuitbreakers"

# View circuit breaker state
Invoke-RestMethod "http://localhost:8080/actuator/circuitbreakers/sampleCircuitBreaker"

# View retry metrics
Invoke-RestMethod "http://localhost:8080/actuator/retries"

# View rate limiter metrics
Invoke-RestMethod "http://localhost:8080/actuator/ratelimiters"

# View bulkhead metrics
Invoke-RestMethod "http://localhost:8080/actuator/bulkheads"
```

#### Integration Test Example

```java
package com.anil.springboot.resilience_demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResilienceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testRetryFallback() {
        // Call endpoint that will fail and trigger fallback
        String response = restTemplate.getForObject(
            "http://localhost:" + port + "/data", 
            String.class
        );
        
        // Should return fallback response
        assertThat(response).contains("Fallback response");
    }

    @Test
    void testCircuitBreakerFallback() {
        String response = restTemplate.getForObject(
            "http://localhost:" + port + "/circuit-breaker", 
            String.class
        );
        
        assertThat(response).contains("Circuit Breaker Fallback");
    }

    @Test
    void testRateLimiterSuccess() {
        String response = restTemplate.getForObject(
            "http://localhost:" + port + "/rate-limiter", 
            String.class
        );
        
        assertThat(response).contains("Rate Limiter Demo Response");
    }

    @Test
    void testBulkheadSuccess() {
        String response = restTemplate.getForObject(
            "http://localhost:" + port + "/bulkhead", 
            String.class
        );
        
        assertThat(response).contains("Bulkhead Demo Response");
    }
}
```

---

### Combining Multiple Patterns

You can stack multiple resilience patterns on a single method:

```java
@GetMapping("/resilient-endpoint")
@Retry(name = "myRetry")
@CircuitBreaker(name = "myCircuitBreaker", fallbackMethod = "fallback")
@RateLimiter(name = "myRateLimiter")
@Bulkhead(name = "myBulkhead")
public String resilientEndpoint() {
    // External service call
}
```

**Default Order of Execution:**
```
Retry → CircuitBreaker → RateLimiter → TimeLimiter → Bulkhead
```

---

### Common Issues & Solutions

| Issue | Error Message | Solution |
|-------|---------------|----------|
| Annotations not working | No retry/circuit breaker behavior | Add `aspectjweaver` dependency |
| RestClient bean not found | `required a bean of type 'RestClient'` | Add `@Bean RestClient restClient()` in main class |
| JUnit test fails | `package org.junit.jupiter.api does not exist` | Ensure `spring-boot-starter-test` dependency is present |
| No actuator endpoints | 404 on `/actuator/circuitbreakers` | Add `management.endpoints.web.exposure.include` property |

---

## Distributed Tracing & Zipkin

### Introduction

**Distributed Tracing** allows you to follow a single request as it flows through multiple microservices. When a user calls the API Gateway, which calls Currency Conversion, which calls Currency Exchange — tracing links all these calls together under a single **Trace ID** so you can see the complete request journey, identify bottlenecks, and debug failures.

In Spring Boot 3.0+, **Spring Cloud Sleuth is deprecated and removed**. It has been replaced by **Micrometer Tracing** which provides the same trace/span ID generation and propagation, but through the Micrometer Observation API.

| Component | Role |
|-----------|------|
| **Micrometer Tracing** | Generates trace IDs and span IDs, propagates context between services |
| **Brave** | The tracing implementation (bridge) used by Micrometer. Handles the actual trace context creation and B3 header propagation |
| **Zipkin** | External server that collects, stores, and visualizes trace data. Provides a web UI to search and view traces |

> ⚠️ **Spring Cloud Sleuth vs Micrometer Tracing:**
> - Spring Boot **2.x** → Use `spring-cloud-starter-sleuth` + `spring-cloud-starter-zipkin`
> - Spring Boot **3.x / 4.x** → Use `spring-boot-micrometer-tracing-brave` + `micrometer-tracing-bridge-brave` + `spring-boot-starter-zipkin` (Sleuth is **NOT** used)

---

### Logging Framework: Why Logback and NOT SLF4J-Simple?

Spring Boot uses **SLF4J** as the logging **API** (facade) and **Logback** as the logging **implementation** (provider). They are NOT competing libraries — SLF4J is the interface, Logback is the implementation.

```
┌─────────────────────────────────────────────────────────┐
│                    Your Application Code                 │
│         Logger logger = LoggerFactory.getLogger(...)     │
│         logger.info("message");                          │
└──────────────────────┬──────────────────────────────────┘
                       │ (SLF4J API - the facade)
                       ▼
              ┌─────────────────┐
              │     SLF4J       │  ← Logging API / Interface
              │  (slf4j-api)    │     You code against this
              └────────┬────────┘
                       │ (binds to one implementation)
                       ▼
              ┌─────────────────┐
              │    Logback      │  ← Logging Implementation / Provider
              │ (logback-core)  │     Actually writes the logs
              └─────────────────┘
```

| Library | Type | Purpose |
|---------|------|---------|
| `slf4j-api` | API (Facade) | Provides `Logger`, `LoggerFactory` interfaces. Your code uses this |
| `logback-core` + `logback-classic` | Implementation | The actual logging engine. Reads `logback.xml`, formats output, writes to console/file |
| `slf4j-simple` | Implementation | A **basic** alternative to Logback. Only writes to `System.err`, no configuration support |

**Why NOT `slf4j-simple`?**

| Feature | Logback ✅ | SLF4J-Simple ❌ |
|---------|-----------|-----------------|
| Configuration file (`logback.xml`) | ✅ Yes | ❌ No |
| Log patterns (timestamps, colors) | ✅ Yes | ❌ No |
| MDC (traceId, spanId in logs) | ✅ Yes | ❌ No |
| Rolling file appenders | ✅ Yes | ❌ No |
| Spring Boot auto-configuration | ✅ Yes | ❌ No |
| Micrometer Tracing integration | ✅ Yes | ❌ No |

**Critical:** If `slf4j-simple` is on the classpath alongside Logback, Spring Boot **crashes** with:
```
IllegalStateException: LoggerFactory is not a Logback LoggerContext but Logback is on the classpath.
Either remove Logback or the competing implementation (class org.slf4j.simple.SimpleLoggerFactory)
```

> **Rule:** NEVER add `slf4j-simple` to a Spring Boot project. Spring Boot already includes SLF4J + Logback via `spring-boot-starter-logging` (which is pulled in by any starter like `spring-boot-starter-webmvc`).

**How traceId/spanId appear in logs:**

Micrometer Tracing stores the current `traceId` and `spanId` in the **MDC (Mapped Diagnostic Context)** — a thread-local map provided by SLF4J/Logback. The logging pattern then reads these values:

```properties
# This pattern reads traceId and spanId from MDC
logging.pattern.level=%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]
```

| Pattern Element | Meaning |
|-----------------|---------|
| `%5p` | Log level (INFO, DEBUG, etc.) right-padded to 5 chars |
| `${spring.application.name:}` | Application name from properties |
| `%X{traceId:-}` | MDC value for key `traceId`. If missing, empty string |
| `%X{spanId:-}` | MDC value for key `spanId`. If missing, empty string |

**Example log output:**
```
INFO [currency-conversion-service,69f04141aec6199a7ab37fa062e97aad,c7de3e113f5f29a0] ...
      └─── app name ──────────────┘ └──── traceId (32 hex) ──────────┘ └── spanId (16 hex) ┘
```

---

### How Trace ID and Span ID Are Generated

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        TRACE ID AND SPAN ID GENERATION                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  1. Browser calls API Gateway                                                │
│     → Brave generates: traceId=abc123, spanId=span1                          │
│     → Stored in MDC (thread-local) for logging                               │
│     → Logged as: [api-gateway, abc123, span1]                                │
│                                                                              │
│  2. API Gateway forwards to Currency Conversion                              │
│     → Gateway injects B3 headers into the HTTP request:                      │
│        X-B3-TraceId: abc123                                                  │
│        X-B3-SpanId: span1                                                    │
│        X-B3-ParentSpanId: (none - this is the root)                          │
│                                                                              │
│  3. Currency Conversion receives the request                                 │
│     → Brave extracts traceId=abc123 from headers (SAME trace!)               │
│     → Creates NEW spanId=span2                                               │
│     → Logged as: [currency-conversion-service, abc123, span2]                │
│                                                                              │
│  4. Currency Conversion calls Currency Exchange via Feign                     │
│     → Feign (with feign-micrometer) injects B3 headers:                      │
│        X-B3-TraceId: abc123                                                  │
│        X-B3-SpanId: span2                                                    │
│        X-B3-ParentSpanId: span1                                              │
│                                                                              │
│  5. Currency Exchange receives the request                                   │
│     → Brave extracts traceId=abc123 from headers (SAME trace!)               │
│     → Creates NEW spanId=span3                                               │
│     → Logged as: [currency-exchange, abc123, span3]                          │
│                                                                              │
│  ALL THREE SERVICES SHARE THE SAME traceId=abc123                            │
│  Each service has its OWN spanId (span1, span2, span3)                       │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

| Concept | What It Is | Who Creates It |
|---------|-----------|----------------|
| **Trace ID** | A unique ID for the entire request journey across ALL services. 32 hex characters. | Created by the **first service** (API Gateway or whoever receives the request first). All downstream services **reuse** the same trace ID. |
| **Span ID** | A unique ID for a single unit of work within a service. 16 hex characters. | Each service creates its **own** span ID. |
| **Parent Span ID** | The span ID of the calling service. | Set by the calling service in B3 headers so the trace tree can be reconstructed. |
| **B3 Headers** | HTTP headers (`X-B3-TraceId`, `X-B3-SpanId`, `X-B3-ParentSpanId`) used to propagate trace context between services. | Injected by the HTTP client (Feign, RestTemplate, WebClient) automatically when tracing is configured. |

---

### How Tracing and Zipkin Server Are Linked

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   API Gateway   │     │ Currency Conv.  │     │ Currency Exch.  │
│   (port 8765)   │     │  (port 8100)    │     │  (port 8000)    │
│                 │     │                 │     │                 │
│ Brave creates   │     │ Brave creates   │     │ Brave creates   │
│ spans locally   │     │ spans locally   │     │ spans locally   │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │   Async HTTP POST     │   Async HTTP POST     │   Async HTTP POST
         │   (spans as JSON)     │   (spans as JSON)     │   (spans as JSON)
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                        ZIPKIN SERVER                             │
│                     (port 9411)                                  │
│                                                                  │
│  1. Receives spans from all services                             │
│  2. Stores them (in-memory or database)                          │
│  3. Groups spans by traceId                                      │
│  4. Provides UI at http://localhost:9411                          │
│     - Search by service name, traceId, time range                │
│     - View trace timeline (waterfall diagram)                    │
│     - See which service is slow, where errors occurred           │
└─────────────────────────────────────────────────────────────────┘
```

**The link between your services and Zipkin is:**
1. `spring-boot-starter-zipkin` dependency in each service's `pom.xml`
2. `management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans` in `application.properties`
3. Each service's Brave reporter **asynchronously** sends completed spans (as JSON) to the Zipkin endpoint via HTTP POST
4. Zipkin server collects these spans, groups them by `traceId`, and shows the full trace in its UI

---

### Starting Zipkin Server

**Option 1: Docker (Recommended)**
```powershell
docker run -d -p 9411:9411 --name zipkin openzipkin/zipkin
```

**Option 2: Java JAR**
```powershell
# Download the latest Zipkin server JAR
curl -sSL https://zipkin.io/quickstart.sh | bash -s
# Run it
java -jar zipkin.jar
```

**Verify Zipkin is running:**
```
http://localhost:9411
```

You should see the Zipkin UI with a search page.

---

### Where to Check Traces in Zipkin

1. Open `http://localhost:9411` in your browser
2. Click **"Run Query"** (or select a service name from the dropdown)
3. Click on a trace to see the full timeline
4. The trace shows:
   - Which services were called
   - How long each service took
   - The parent-child relationship between spans
   - Any errors that occurred

**Zipkin UI Navigation:**

| Page | URL | What It Shows |
|------|-----|---------------|
| **Search** | `http://localhost:9411/zipkin/` | Search traces by service, time range, duration |
| **Trace Detail** | `http://localhost:9411/zipkin/traces/{traceId}` | Timeline view of a single trace across all services |
| **Dependencies** | `http://localhost:9411/zipkin/dependency` | Service dependency graph (which service calls which) |

---

### Required Dependencies for Tracing (pom.xml)

Add these to **every service** that should participate in distributed tracing:

```xml
<!-- 1. ACTUATOR - Required for tracing auto-configuration -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- 2. SPRING BOOT MICROMETER TRACING BRAVE - Spring Boot auto-configuration for Brave tracing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-micrometer-tracing-brave</artifactId>
</dependency>

<!-- 3. MICROMETER TRACING BRIDGE (Brave) - Core bridge between Micrometer and Brave -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>

<!-- 4. ZIPKIN STARTER - Reports spans to Zipkin server -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-zipkin</artifactId>
</dependency>
```

**Additional dependency for services using Feign clients:**

```xml
<!-- 5. FEIGN MICROMETER - Required for Feign to propagate trace headers to downstream services -->
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-micrometer</artifactId>
</dependency>
```

### Dependencies Explanation

| # | Dependency | GroupId | ArtifactId | Purpose |
|---|-----------|---------|------------|---------|
| 1 | **Actuator** | `org.springframework.boot` | `spring-boot-starter-actuator` | Activates `ObservationRegistry` and tracing auto-configuration. Without this, Micrometer tracing won't fully initialize |
| 2 | **Spring Boot Micrometer Tracing Brave** | `org.springframework.boot` | `spring-boot-micrometer-tracing-brave` | Spring Boot's auto-configuration module for Brave tracing. Auto-configures Brave `Tracer`, `Propagation`, and `SpanHandler` beans based on your `application.properties`. This is the **glue** between Spring Boot and Brave |
| 3 | **Micrometer Tracing Bridge** | `io.micrometer` | `micrometer-tracing-bridge-brave` | Core bridge that connects Micrometer's Observation API with Brave's tracing engine. Brave generates the actual trace IDs, span IDs, and injects/extracts B3 headers |
| 4 | **Zipkin Starter** | `org.springframework.boot` | `spring-boot-starter-zipkin` | Auto-configures the Zipkin span reporter. Sends completed spans as JSON to the Zipkin server endpoint |
| 5 | **Feign Micrometer** | `io.github.openfeign` | `feign-micrometer` | Integrates Feign HTTP clients with Micrometer's `ObservationRegistry`. Without this, Feign does **NOT** inject trace headers into outgoing requests |

### How the 3 Tracing Dependencies Relate to Each Other

```
spring-boot-micrometer-tracing-brave (org.springframework.boot)
│   → Spring Boot auto-configuration layer
│   → Reads your application.properties (sampling probability, etc.)
│   → Creates and configures Brave beans automatically
│
└── uses → micrometer-tracing-bridge-brave (io.micrometer)
        │   → Core bridge: Micrometer Observation API ↔ Brave
        │   → Generates traceId/spanId
        │   → Injects/extracts B3 headers on HTTP calls
        │
        └── reported by → spring-boot-starter-zipkin (org.springframework.boot)
                → Sends completed spans to Zipkin server
                → Async HTTP POST to http://localhost:9411/api/v2/spans
```

> **Why do we need BOTH `spring-boot-micrometer-tracing-brave` AND `micrometer-tracing-bridge-brave`?**
> - `micrometer-tracing-bridge-brave` = the **core library** (does the actual work)
> - `spring-boot-micrometer-tracing-brave` = the **Spring Boot auto-configuration** (configures the core library using your `application.properties`)
> - Without the auto-configuration, you'd have to manually create `Tracer`, `Propagation`, and `SpanHandler` beans yourself

### Which Service Needs Which Dependency?

| Service | actuator | spring-boot-micrometer-tracing-brave | micrometer-tracing-bridge-brave | spring-boot-starter-zipkin | feign-micrometer |
|---------|----------|--------------------------------------|-------------------------------------|---------------------------|------------------|
| **API Gateway** | ✅ | ✅ | ✅ | ✅ | ❌ (uses WebClient, not Feign) |
| **Currency Conversion** | ✅ | ✅ | ✅ | ✅ | ✅ (uses `@FeignClient`) |
| **Currency Exchange** | ✅ | ✅ | ✅ | ✅ | ❌ (doesn't call other services) |

---

### Application Properties for Tracing

Add these to **each service's** `application.properties`:

```properties
# ==================== TRACING CONFIGURATION ====================

# Sampling probability: 1.0 = trace 100% of requests, 0.1 = trace 10%
# Use 1.0 for development, 0.1 for production
management.tracing.sampling.probability=1.0

# Zipkin endpoint where spans are sent
management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans

# Expose all actuator endpoints (including tracing-related)
management.endpoints.web.exposure.include=*

# Show trace ID and span ID in log output
logging.pattern.level=%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]
```

| Property | Value | Description |
|----------|-------|-------------|
| `management.tracing.sampling.probability` | `1.0` | Fraction of requests to trace. `1.0` = all requests, `0.1` = 10%. Use `1.0` in development to see all traces |
| `management.zipkin.tracing.endpoint` | `http://localhost:9411/api/v2/spans` | URL where Zipkin server accepts spans. This is the link between your service and Zipkin |
| `management.endpoints.web.exposure.include` | `*` | Exposes all actuator endpoints including tracing-related ones |
| `logging.pattern.level` | `%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]` | Custom log pattern that includes the application name, trace ID, and span ID from MDC |

**For API Gateway** (uses `application.yml`):
```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

---

### No Special Annotations Required for Tracing

Unlike Resilience4j or Feign, distributed tracing requires **NO special annotations** on the main application class. It is entirely auto-configured by Spring Boot when the dependencies are on the classpath.

| What | Annotation Needed? | Why |
|------|-------------------|-----|
| Tracing (Micrometer + Brave) | ❌ **None** | Auto-configured by `spring-boot-starter-actuator` + `spring-boot-micrometer-tracing-brave` + `micrometer-tracing-bridge-brave` |
| Zipkin reporting | ❌ **None** | Auto-configured by `spring-boot-starter-zipkin` |
| Feign tracing | ❌ **None** (but need `feign-micrometer` dependency) | Auto-configured when `feign-micrometer` is on classpath |
| Log pattern (traceId in logs) | ❌ **None** (but need `logging.pattern.level` property) | Configured via `application.properties` |

The only thing you need is:
1. **Dependencies** in `pom.xml`
2. **Properties** in `application.properties`
3. ✅ That's it!

---

### Tracing with Feign Client

When Currency Conversion calls Currency Exchange via `@FeignClient`, trace headers must be propagated. Here's the complete setup:

**Step 1: Add `feign-micrometer` dependency** (in `currency-conversion-service/pom.xml`):
```xml
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-micrometer</artifactId>
</dependency>
```

**Step 2: No code changes needed.** The Feign proxy interface stays the same:
```java
@FeignClient(name = "currency-exchange")
public interface CurrencyExchangeProxy {
    @GetMapping("/currency-exchange/from/{from}/to/{to}")
    public CurrencyConversion retrieveExchangeValue(
            @PathVariable("from") String from,
            @PathVariable("to") String to);
}
```

**What `feign-micrometer` does under the hood:**
1. Registers `MicrometerObservationCapability` with Feign
2. When Feign makes an HTTP call, the capability wraps it in a Micrometer `Observation`
3. The observation triggers Brave to inject B3 trace headers (`X-B3-TraceId`, `X-B3-SpanId`) into the outgoing request
4. The downstream service (Currency Exchange) extracts these headers and continues the same trace

**Without `feign-micrometer`:**
```
Zipkin: [currency-conversion-service] → ❌ currency-exchange NOT linked
        (Feign makes a plain HTTP call with no trace headers)
        (Currency Exchange starts a NEW, unrelated trace)
```

**With `feign-micrometer`:**
```
Zipkin: [currency-conversion-service] → [currency-exchange] ✅ same traceId
        (Feign injects B3 headers → Currency Exchange continues the trace)
```

### ⚠️ Rules for Using Feign Client with Tracing

| # | Rule | Why |
|---|------|-----|
| 1 | Add `feign-micrometer` dependency | Without it, Feign does NOT inject trace headers |
| 2 | Add `spring-boot-starter-actuator` | Required for `ObservationRegistry` which `feign-micrometer` depends on |
| 3 | Add `spring-boot-micrometer-tracing-brave` | Spring Boot auto-configuration for Brave tracing beans |
| 4 | Add `micrometer-tracing-bridge-brave` | Provides the actual trace context creation and B3 header injection |
| 5 | Add `spring-boot-starter-zipkin` | Reports the spans to Zipkin server |
| 6 | Do NOT use `@FeignClient(url="...")` for tracing across Eureka | Hardcoded URL works for tracing, but you lose load balancing. Use `@FeignClient(name="service-name")` |

---

### Tracing with RestTemplate

When using `@LoadBalanced RestTemplate` to call other services, trace context must also be propagated. **The key rule is: use `RestTemplateBuilder` to create the RestTemplate, NOT `new RestTemplate()`.**

**❌ WRONG — No tracing (plain RestTemplate):**
```java
@Configuration
public class RestClientConfiguration {
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();  // ❌ No tracing interceptors attached!
    }
}
```

**✅ CORRECT — With tracing (uses RestTemplateBuilder):**
```java
@Configuration
public class RestClientConfiguration {
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();  // ✅ Spring auto-configures tracing interceptors
    }
}
```

**Why `RestTemplateBuilder` is required:**

Spring Boot auto-configures `RestTemplateBuilder` with tracing interceptors. When you call `builder.build()`, the resulting `RestTemplate` has:
1. **Load balancing interceptor** (from `@LoadBalanced`)
2. **Tracing interceptor** (from Micrometer/Brave auto-configuration)

When you use `new RestTemplate()`, you get a **plain** instance with no interceptors — no load balancing awareness for tracing, no B3 header injection.

**Controller usage (no changes needed):**
```java
@RestController
public class CurrencyConversionController {

    @Autowired
    private RestTemplate restTemplate;  // ← The traced, load-balanced bean

    @GetMapping("/currency-conversion/lb/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversionWithLB(...) {
        HashMap<String, String> uriVariables = new HashMap<>();
        uriVariables.put("from", from);
        uriVariables.put("to", to);

        // This call is now traced AND load-balanced
        CurrencyConversion currencyConversion = restTemplate.getForObject(
                "http://currency-exchange/currency-exchange/from/{from}/to/{to}",
                CurrencyConversion.class,
                uriVariables);
        // ...
    }
}
```

### ⚠️ Rules for Using RestTemplate with Tracing

| # | Rule | Why |
|---|------|-----|
| 1 | Use `RestTemplateBuilder` to create `RestTemplate` | `builder.build()` includes tracing interceptors. `new RestTemplate()` does NOT |
| 2 | Inject `RestTemplateBuilder` as a method parameter | Spring Boot auto-configures it with Micrometer tracing support |
| 3 | Add `spring-boot-starter-actuator` | Required for tracing auto-configuration |
| 4 | Add `spring-boot-micrometer-tracing-brave` | Spring Boot auto-configuration that sets up the tracing interceptor beans |
| 5 | Add `micrometer-tracing-bridge-brave` | Provides the tracing interceptor that `RestTemplateBuilder` attaches |
| 6 | Do NOT use `RestClient.create()` for traced calls | `RestClient.create()` is unmanaged — use `RestClient.Builder` bean instead (see below) |

---

### Tracing with RestClient (Spring Boot 3.2+)

If you use `RestClient` instead of `RestTemplate`, the same rule applies: **use the Spring-managed `RestClient.Builder` bean, NOT `RestClient.create()`.**

**❌ WRONG — No tracing:**
```java
// In controller method — this is a raw, unmanaged client
CurrencyConversion result = RestClient.create().get()
        .uri("http://localhost:8000/currency-exchange/from/{from}/to/{to}", uriVariables)
        .retrieve()
        .body(CurrencyConversion.class);
```

**✅ CORRECT — With tracing:**
```java
@RestController
public class CurrencyConversionController {

    @Autowired
    private RestClient.Builder restClientBuilder;  // Spring-managed, traced

    @GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversion(...) {
        CurrencyConversion result = restClientBuilder.build().get()
                .uri("http://localhost:8000/currency-exchange/from/{from}/to/{to}", uriVariables)
                .retrieve()
                .body(CurrencyConversion.class);
        // ...
    }
}
```

Spring Boot auto-configures a `RestClient.Builder` bean with Micrometer tracing support. When you call `restClientBuilder.build()`, the resulting `RestClient` has tracing interceptors attached.

---

### Complete Tracing Setup Summary

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    COMPLETE TRACING SETUP CHECKLIST                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  For EVERY service:                                                         │
│  ☐ pom.xml: spring-boot-starter-actuator                                    │
│  ☐ pom.xml: spring-boot-micrometer-tracing-brave                            │
│  ☐ pom.xml: micrometer-tracing-bridge-brave                                 │
│  ☐ pom.xml: spring-boot-starter-zipkin                                      │
│  ☐ application.properties: management.tracing.sampling.probability=1.0      │
│  ☐ application.properties: management.zipkin.tracing.endpoint=...           │
│  ☐ application.properties: logging.pattern.level=...                        │
│  ☐ Do NOT add slf4j-simple dependency                                       │
│                                                                             │
│  For services using Feign:                                                  │
│  ☐ pom.xml: feign-micrometer                                                │
│                                                                             │
│  For services using RestTemplate:                                           │
│  ☐ Create RestTemplate via RestTemplateBuilder (NOT new RestTemplate())      │
│                                                                             │
│  For services using RestClient:                                             │
│  ☐ Use @Autowired RestClient.Builder (NOT RestClient.create())              │
│                                                                             │
│  External:                                                                  │
│  ☐ Zipkin server running on port 9411                                       │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Verifying Tracing Works

1. **Start Zipkin:** `docker run -d -p 9411:9411 openzipkin/zipkin`
2. **Start all services** (Eureka → Config → Currency Exchange → Currency Conversion → API Gateway)
3. **Make a request:** `http://localhost:8765/currency-conversion/feign/from/USD/to/INR/quantity/10`
4. **Check service logs** — all services should show the **same traceId**:
   ```
   [api-gateway,         abc123def456..., span1]  ← Gateway
   [currency-conversion, abc123def456..., span2]  ← Conversion
   [currency-exchange,   abc123def456..., span3]  ← Exchange (same traceId!)
   ```
5. **Check Zipkin UI:** `http://localhost:9411` → Search → Click the trace → See all 3 services in the timeline

---

## Docker & Containerization

### Introduction

**Docker** packages your Spring Boot application and all its dependencies (JDK, libraries, config) into a single **image** that can run identically on any machine — your laptop, a test server, or production cloud. No more "it works on my machine" problems.

| Concept | What It Is |
|---------|-----------|
| **Image** | A read-only template (blueprint) containing your app + JDK + dependencies. Like a class in Java |
| **Container** | A running instance of an image. Like an object created from a class |
| **Dockerfile** | A text file with instructions to build an image step by step |
| **Docker Hub** | Public registry to store and share images (like Maven Central for JARs) |
| **Layer** | Each instruction in a Dockerfile creates a layer. Layers are cached for performance |

---

### Basic Docker Commands

```powershell
# ==================== IMAGE COMMANDS ====================

# List all images on your machine
docker images

# Build an image from Dockerfile in current directory
docker build -t my-app:v1 .

# Build with a specific Dockerfile path
docker build -t my-app:v1 -f path/to/Dockerfile .

# Remove an image
docker rmi my-app:v1

# Remove all unused images
docker image prune

# ==================== CONTAINER COMMANDS ====================

# Run a container from an image
docker run my-app:v1

# Run in detached mode (background) with port mapping
docker run -d -p 8080:8080 my-app:v1

# List running containers
docker ps

# List ALL containers (including stopped)
docker ps -a

# Stop a running container
docker stop <container-id>

# Remove a stopped container
docker rm <container-id>

# View logs of a container
docker logs <container-id>

# View logs in real-time (follow mode)
docker logs -f <container-id>

# Execute a command inside a running container
docker exec -it <container-id> /bin/sh

# ==================== CLEANUP COMMANDS ====================

# Stop all running containers
docker stop $(docker ps -q)

# Remove all stopped containers
docker container prune

# Remove everything (images, containers, volumes, networks)
docker system prune -a
```

### Command Flags Explained

| Flag | Full Form | Purpose |
|------|-----------|---------|
| `-t` | `--tag` | Name and tag for the image (e.g., `my-app:v1`) |
| `-d` | `--detach` | Run container in background (detached mode). Returns container ID immediately |
| `-p` | `--publish` | Map host port to container port. Format: `HOST_PORT:CONTAINER_PORT` |
| `-f` | `--file` | Specify Dockerfile path (if not in current directory) |
| `-it` | `--interactive --tty` | Interactive mode with terminal (for exec/debugging) |
| `--name` | `--name` | Assign a custom name to the container |
| `--rm` | `--rm` | Automatically remove container when it stops |

### Port Mapping (`-p`)

```
-p 9090:8080
    │     │
    │     └── Container port (what your Spring Boot app listens on)
    │
    └── Host port (what you access from your browser)

Browser → http://localhost:9090 → Docker forwards to → Container:8080
```

**Examples:**
```powershell
# Map host 8080 to container 8080 (same port)
docker run -d -p 8080:8080 my-app:v1

# Map host 9090 to container 8080 (different port)
docker run -d -p 9090:8080 my-app:v1

# Map multiple ports
docker run -d -p 8080:8080 -p 8081:8081 my-app:v1
```

### Detached Mode (`-d`)

```powershell
# WITHOUT -d: Terminal is blocked, logs shown directly
docker run -p 8080:8080 my-app:v1
# Ctrl+C stops the container

# WITH -d: Runs in background, terminal is free
docker run -d -p 8080:8080 my-app:v1
# Returns: a1b2c3d4e5f6...  (container ID)

# Check if it's running
docker ps

# View logs later
docker logs a1b2c3d4e5f6
```

---

### What is a Dockerfile?

A **Dockerfile** is a plain text file containing step-by-step instructions to build a Docker image. Each instruction creates a **layer** in the image.

**Basic Dockerfile structure:**
```dockerfile
# 1. Start from a base image (the OS + runtime)
FROM eclipse-temurin:17-jre

# 2. Copy your application JAR into the image
COPY target/my-app-0.0.1-SNAPSHOT.jar /app.jar

# 3. Tell Docker which port the app uses
EXPOSE 8080

# 4. Define the command to run when container starts
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

| Instruction | Purpose |
|-------------|---------|
| `FROM` | Base image to start from. Always the first instruction |
| `WORKDIR` | Set the working directory inside the container |
| `COPY` | Copy files from your machine into the image |
| `RUN` | Execute a command during image build (e.g., `mvn clean package`) |
| `EXPOSE` | Document which port the app listens on (informational, doesn't actually publish) |
| `ENTRYPOINT` | Command that runs when the container starts |
| `VOLUME` | Create a mount point for external storage |
| `ARG` | Build-time variable |
| `ENV` | Environment variable available at runtime |

---

### Approach 1: Simple Dockerfile (Pre-built JAR)

**Prerequisites:** Build the JAR first with `mvn clean package`

**File:** `dockerFileVersion1/Dockerfile`

```dockerfile
#Set the base image JDK.
FROM eclipse-temurin:17-jre
#Copy target folder jar file to the container.
COPY target/docker-helloworld-demo-0.0.1-SNAPSHOT.jar /helloworld.jar
#Expose the port 8080 to the outside world.
EXPOSE 8080
#Run the jar file.
ENTRYPOINT ["java","-jar","/helloworld.jar"]
```

**Build & Run:**
```powershell
# Step 1: Build the JAR first
cd C:\workspace\Intellij-workspace\springboot\docker-helloworld-demo
mvn clean package -DskipTests

# Step 2: Build Docker image
docker build -t docker-helloworld-demo:v1 -f dockerFileVersion1/Dockerfile .

# Step 3: Run the container
docker run -d -p 8080:8080 docker-helloworld-demo:v1

# Step 4: Test
# http://localhost:8080/api/hello → "Hello World from Docker Demo Application! V5.0"
```

**Flow:**
```
Your Machine                          Docker Image
─────────────                         ────────────
mvn clean package                     
    ↓                                 
target/*.jar created                  
    ↓                                 
docker build                          FROM eclipse-temurin:17-jre
    ↓                                 COPY target/*.jar → /helloworld.jar
    ↓                                 EXPOSE 8080
    ↓                                 ENTRYPOINT java -jar /helloworld.jar
    ↓                                 
Image created ✅                      
```

**Pros:** Simple, fast to build
**Cons:** Requires JAR to be built first on your machine. Any code change rebuilds the entire JAR layer.

---

### Approach 2: Multi-Stage Dockerfile (Build Inside Docker)

This approach builds the JAR **inside Docker** using Maven — no need to have Maven/JDK installed on your machine.

**File:** `Dockerfile` (root of docker-helloworld-demo)

```dockerfile
# ==================== STAGE 1: BUILD ====================
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Layered approach: copy pom.xml first, then source
COPY ./pom.xml /app/pom.xml
COPY ./src /app/src
RUN mvn -f /app/pom.xml clean package -DskipTests

# ==================== STAGE 2: RUN ====================
FROM eclipse-temurin:17-jre
VOLUME /tmp
EXPOSE 8080
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

**Build & Run:**
```powershell
cd C:\workspace\Intellij-workspace\springboot\docker-helloworld-demo

# Build image (Maven runs inside Docker — no local Maven needed!)
docker build -t docker-helloworld-demo:v2 .

# Run
docker run -d -p 8080:8080 docker-helloworld-demo:v2
```

**How Multi-Stage Works:**
```
┌─────────────────────────────────────────────────────────┐
│  STAGE 1: build (maven:3.8.5-openjdk-17)               │
│                                                          │
│  1. Start with Maven + JDK image (~400MB)               │
│  2. Copy pom.xml and src/                               │
│  3. Run mvn clean package → creates target/*.jar        │
│                                                          │
│  This stage is DISCARDED after build!                   │
│  Only the JAR is kept.                                  │
└──────────────────────┬──────────────────────────────────┘
                       │ COPY --from=build
                       ▼
┌─────────────────────────────────────────────────────────┐
│  STAGE 2: run (eclipse-temurin:17-jre)                  │
│                                                          │
│  1. Start with slim JRE image (~200MB)                  │
│  2. Copy ONLY the JAR from Stage 1                      │
│  3. Run java -jar app.jar                               │
│                                                          │
│  Final image is SMALL — only JRE + JAR                  │
└─────────────────────────────────────────────────────────┘
```

**Why Multi-Stage?**

| Aspect | Single Stage | Multi-Stage |
|--------|-------------|-------------|
| Final image size | ~400MB (includes Maven + full JDK) | ~200MB (only JRE + JAR) |
| Maven needed on host? | ✅ Yes | ❌ No |
| JDK needed on host? | ✅ Yes | ❌ No |
| Build reproducibility | Depends on local env | Always the same |

---

### Layered Approach: Why and How

#### The Problem Without Layers

Every time you change **one line of code**, Docker rebuilds the **entire JAR layer**:

```
Layer 1: FROM eclipse-temurin:17-jre          → Cached ✅ (never changes)
Layer 2: COPY target/my-app.jar /app.jar      → REBUILT ❌ (JAR changed!)
Layer 3: ENTRYPOINT java -jar /app.jar        → REBUILT ❌ (depends on Layer 2)
```

A typical Spring Boot JAR is **50-100MB**, and most of that is **dependencies** (libraries) that rarely change. But Docker doesn't know that — it sees the whole JAR as one blob.

#### The Layered Solution

Split the JAR into layers so Docker can **cache** the parts that don't change:

```
┌─────────────────────────────────────────────────────────────┐
│                     DOCKER LAYER CACHING                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Traditional (Single Layer):                                 │
│  ┌──────────────────────────────────┐                        │
│  │  app.jar (80MB)                  │ ← Rebuilt every time   │
│  │  = dependencies (70MB)           │                        │
│  │  + your code (10MB)              │                        │
│  └──────────────────────────────────┘                        │
│                                                              │
│  Layered (Split):                                            │
│  ┌──────────────────────────────────┐                        │
│  │  Layer 1: dependencies (70MB)    │ ← Cached ✅ (rarely   │
│  ├──────────────────────────────────┤    changes)            │
│  │  Layer 2: spring-boot-loader     │ ← Cached ✅            │
│  ├──────────────────────────────────┤                        │
│  │  Layer 3: snapshot-dependencies  │ ← Cached mostly       │
│  ├──────────────────────────────────┤                        │
│  │  Layer 4: application (10MB)     │ ← Rebuilt ❌ (your     │
│  └──────────────────────────────────┘    code changed)       │
│                                                              │
│  Result: Only 10MB rebuilt instead of 80MB!                  │
│  Build time: 30 seconds → 3 seconds                         │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

#### Why Go for Layered Approach?

| Benefit | Explanation |
|---------|-------------|
| **Faster builds** | Only the changed layer is rebuilt. Dependencies (70MB+) are cached |
| **Faster pushes** | Only changed layers are pushed to Docker registry |
| **Faster pulls** | When deploying, only new layers are downloaded |
| **Less bandwidth** | Saves network bandwidth in CI/CD pipelines |
| **Less storage** | Docker stores layers once, shares them across images |

#### Multi-Stage + Layered Dockerfile

In the multi-stage Dockerfile, the layered approach is achieved by copying `pom.xml` **before** `src/`:

```dockerfile
# ==================== STAGE 1: BUILD ====================
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# LAYER OPTIMIZATION: Copy pom.xml first (dependencies)
# This layer is cached as long as pom.xml doesn't change
COPY ./pom.xml /app/pom.xml

# Download dependencies (cached until pom.xml changes)
# This is the key optimization — 70MB+ of dependencies cached!
RUN mvn dependency:go-offline -B

# Now copy source code (changes frequently)
COPY ./src /app/src

# Build the JAR (only this runs when code changes)
RUN mvn -f /app/pom.xml clean package -DskipTests

# ==================== STAGE 2: RUN ====================
FROM eclipse-temurin:17-jre
VOLUME /tmp
EXPOSE 8080
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

**How caching works:**
```
First build:                          Second build (only code changed):
─────────────                         ──────────────────────────────────
COPY pom.xml    → Layer 1 (new)       COPY pom.xml    → Layer 1 (CACHED ✅)
RUN mvn deps    → Layer 2 (new)       RUN mvn deps    → Layer 2 (CACHED ✅)
COPY src/       → Layer 3 (new)       COPY src/       → Layer 3 (REBUILT ❌)
RUN mvn package → Layer 4 (new)       RUN mvn package → Layer 4 (REBUILT ❌)

Total build: ~2 minutes               Total build: ~10 seconds
```

---

### Spring Boot Maven Plugin: Build Image Without Dockerfile

Spring Boot's Maven plugin can build a Docker image **without** a Dockerfile using **Cloud Native Buildpacks (CNB)**:

```powershell
# Build Docker image directly from Maven — no Dockerfile needed!
mvn spring-boot:build-image

# Build with a custom image name
mvn spring-boot:build-image -Dspring-boot.build-image.imageName=my-app:v1
```

**pom.xml configuration** (already present in your project):
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

**How it works:**
```
mvn spring-boot:build-image
        │
        ▼
Spring Boot Maven Plugin
        │
        ▼
Uses Cloud Native Buildpacks (Paketo)
        │
        ▼
Automatically detects Java app
        │
        ▼
Creates optimized, layered Docker image
        │
        ▼
Image available: docker.io/library/docker-helloworld-demo:0.0.1-SNAPSHOT
```

| Feature | Dockerfile | Maven Plugin (`build-image`) |
|---------|-----------|------------------------------|
| Need to write Dockerfile? | ✅ Yes | ❌ No |
| Layered by default? | ❌ Manual setup | ✅ Automatic |
| Security patches for base image? | ❌ Manual | ✅ Auto (Paketo updates) |
| Customization? | Full control | Limited (via config) |
| Docker daemon required? | ✅ Yes | ✅ Yes |

---

### Creating Docker Image Using IntelliJ IDEA

IntelliJ IDEA has built-in Docker support:

**Method 1: Run Configuration**
1. Go to **Run → Edit Configurations → + → Docker → Dockerfile**
2. Set:
   - **Dockerfile:** Select your `Dockerfile` path
   - **Image tag:** `docker-helloworld-demo:v1`
   - **Container name:** `helloworld-container`
   - **Bind ports:** `8080:8080`
3. Click **Run** ▶️

**Method 2: Gutter Icon**
1. Open your `Dockerfile` in IntelliJ
2. Click the green **▶️ Run** icon in the gutter (left margin)
3. IntelliJ builds the image and optionally runs the container

**Method 3: Services Tool Window**
1. Go to **View → Tool Windows → Services**
2. Click **+ → Docker Connection**
3. Connect to your Docker daemon
4. Right-click on **Images → Pull/Build**
5. Right-click on an image → **Create Container**

**Prerequisites:**
- Docker Desktop must be running
- IntelliJ **Ultimate** edition (Docker support is not available in Community edition)
- Or install the **Docker plugin** from JetBrains Marketplace

---

### Standard Workflow: Build → Check → Run → Verify

```powershell
# ==================== STEP 1: BUILD THE JAR ====================
cd C:\workspace\Intellij-workspace\springboot\docker-helloworld-demo
mvn clean package -DskipTests

# ==================== STEP 2: BUILD DOCKER IMAGE ====================
# Using simple Dockerfile (pre-built JAR)
docker build -t docker-helloworld-demo:v1 -f dockerFileVersion1/Dockerfile .

# OR using multi-stage Dockerfile (builds JAR inside Docker)
docker build -t docker-helloworld-demo:v2 .

# OR using Maven plugin (no Dockerfile needed)
mvn spring-boot:build-image

# ==================== STEP 3: CHECK THE IMAGE ====================
docker images
# REPOSITORY                  TAG       IMAGE ID       SIZE
# docker-helloworld-demo      v1        a1b2c3d4e5f6   250MB

# Inspect image details
docker inspect docker-helloworld-demo:v1

# View image layers
docker history docker-helloworld-demo:v1

# ==================== STEP 4: RUN THE CONTAINER ====================
# Run in detached mode with port mapping
docker run -d -p 8080:8080 --name my-hello-app docker-helloworld-demo:v1

# ==================== STEP 5: CHECK RUNNING CONTAINER ====================
docker ps
# CONTAINER ID   IMAGE                        STATUS         PORTS
# f6e5d4c3b2a1   docker-helloworld-demo:v1    Up 2 minutes   0.0.0.0:8080->8080/tcp

# View container logs
docker logs my-hello-app

# ==================== STEP 6: TEST THE APPLICATION ====================
# http://localhost:8080/api/hello
# → "Hello World from Docker Demo Application! V5.0"

# ==================== STEP 7: STOP & CLEANUP ====================
docker stop my-hello-app
docker rm my-hello-app
```

---

### Docker Commands Quick Reference

| What | Command |
|------|---------|
| **Build image** | `docker build -t name:tag .` |
| **Build image (specific Dockerfile)** | `docker build -t name:tag -f path/Dockerfile .` |
| **Build via Maven plugin** | `mvn spring-boot:build-image` |
| **List images** | `docker images` |
| **Remove image** | `docker rmi name:tag` |
| **Run container** | `docker run -d -p HOST:CONTAINER name:tag` |
| **Run with name** | `docker run -d -p 8080:8080 --name myapp name:tag` |
| **List running containers** | `docker ps` |
| **List all containers** | `docker ps -a` |
| **Stop container** | `docker stop <id or name>` |
| **Remove container** | `docker rm <id or name>` |
| **View logs** | `docker logs <id or name>` |
| **Follow logs** | `docker logs -f <id or name>` |
| **Inspect image** | `docker inspect name:tag` |
| **View layers** | `docker history name:tag` |
| **Shell into container** | `docker exec -it <id> /bin/sh` |
| **Cleanup everything** | `docker system prune -a` |

---

### Project Dockerfile Reference

**Simple Dockerfile** (`dockerFileVersion1/Dockerfile`):
```dockerfile
FROM eclipse-temurin:17-jre
COPY target/docker-helloworld-demo-0.0.1-SNAPSHOT.jar /helloworld.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/helloworld.jar"]
```

**Multi-Stage Dockerfile** (`Dockerfile`):
```dockerfile
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY ./pom.xml /app/pom.xml
COPY ./src /app/src
RUN mvn -f /app/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre
VOLUME /tmp
EXPOSE 8080
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

---

## Part 2: Troubleshooting

---

## Issue: Config Server Hangs / Read Timeout on Endpoints

### Symptoms
- `http://localhost:8888/application/default` works ✅
- `http://localhost:8888/currency-exchange/default` hangs indefinitely ❌
- Logs show repeated timeout errors:

```
WARN: Could not locate PropertySource: I/O error on GET request for 
      "http://localhost:8888/currency-exchange/default": Read timed out

INFO: Fetching config from server at : http://localhost:8888
```

---

## Root Cause: `spring.config.import` in Config Files

The `spring.config.import=optional:configserver:http://localhost:8888` property was present in the **config files served BY Config Server**.

**Example of incorrect config file** (`config/currency-exchange.properties`):
```properties
spring.jpa.show-sql=true
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true
spring.application.name=currency-exchange
server.port=8000

# ❌ THIS CAUSES THE ISSUE
spring.config.import=optional:configserver:http://localhost:8888
```

### Why This Happens

When a client requests config from Config Server:

1. Client calls `http://localhost:8888/currency-exchange/default`
2. Config Server reads `currency-exchange.properties` from its backend (native/git)
3. Config Server sees `spring.config.import=optional:configserver:http://localhost:8888`
4. Config Server **tries to fetch config from itself** (http://localhost:8888)
5. This creates an **infinite loop** → timeout → hang

```
┌─────────────────┐        ┌─────────────────┐
│  Config Server  │───────▶│  Config Server  │
│  (port 8888)    │◀───────│  (same server!) │
└─────────────────┘        └─────────────────┘
        │                          │
        └──────── LOOP ────────────┘
                   ↓
              TIMEOUT!
```

### Where This Property Should Be

| Location | Should have `spring.config.import`? |
|----------|-------------------------------------|
| **Client app** (`currency-exchange/src/main/resources/application.properties`) | ✅ YES |
| **Config Server's served files** (`config-server/src/main/resources/config/*.properties`) | ❌ NO |
| **Config Server's own config** (`config-server/src/main/resources/application.properties`) | ❌ NO |

---

## Solution

### Step 1: Remove `spring.config.import` from ALL config files in Config Server

Files to fix:
- `config-server/src/main/resources/config/application.properties`
- `config-server/src/main/resources/config/currency-exchange.properties`
- `config-server/src/main/resources/config/currency-exchange-dev.properties`
- `config-server/src/main/resources/config/currency-conversion-service.properties`
- `config-server/src/main/resources/config/currency-conversion-service-dev.properties`

**Correct content example** (`config/currency-exchange.properties`):
```properties
spring.jpa.show-sql=true
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true
spring.application.name=currency-exchange
server.port=8000
spring.jpa.defer-datasource-initialization=true

# ✅ NO spring.config.import here!
```

### Step 2: Keep `spring.config.import` ONLY in client apps

**Client app** (`currency-exchange/src/main/resources/application.properties`):
```properties
spring.application.name=currency-exchange

# ✅ This is the correct place for spring.config.import
spring.config.import=optional:configserver:http://localhost:8888
```

### Step 3: Disable config import in Config Server itself

**Config Server** (`config-server/src/main/resources/application.properties`):
```properties
spring.application.name=Config-Server
server.port=8888

spring.profiles.active=native
spring.cloud.config.server.native.search-locations=classpath:/config

# ✅ Disable config client behavior on Config Server itself
spring.cloud.config.enabled=false
spring.cloud.config.import-check.enabled=false
```

---

## Verification

After fixing, test these endpoints:

```powershell
# All should return JSON with propertySources immediately (no timeout)
Invoke-RestMethod "http://localhost:8888/application/default"
Invoke-RestMethod "http://localhost:8888/currency-exchange/default"
Invoke-RestMethod "http://localhost:8888/currency-conversion-service/default"
```

Expected response:
```json
{
  "name": "currency-exchange",
  "profiles": ["default"],
  "label": null,
  "propertySources": [
    {
      "name": "classpath:/config/currency-exchange.properties",
      "source": {
        "spring.jpa.show-sql": "true",
        "server.port": "8000"
      }
    }
  ]
}
```

---

## Issue: Eureka Server Port Not Working

### Symptoms

- You set `server.port=8761` in Naming Server's `application.properties`
- But Eureka Server starts on a **different port** (e.g., 8888)
- Error: `Web server failed to start. Port 8888 was already in use`

### Root Cause: `spring.config.import` Overrides Local Properties

When Naming Server has this in `application.properties`:

```properties
spring.application.name=naming-server
server.port=8761

eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false

# ❌ THIS CAUSES THE ISSUE
spring.config.import=optional:configserver:http://localhost:8888
```

**What happens:**

1. Naming Server starts
2. It fetches config from Config Server (`spring.config.import`)
3. Config Server returns shared `application.properties` (which may have different port settings)
4. Remote config **overrides** your local `server.port=8761`
5. Naming Server tries to start on wrong port!

```
┌─────────────────────┐          ┌─────────────────┐
│   Naming Server     │ ──────▶  │  Config Server  │
│   (port 8761)       │          │   (port 8888)   │
└─────────────────────┘          └─────────────────┘
         │                              │
         │   Fetches shared config      │
         │◀─────────────────────────────│
         │
         ▼
   server.port OVERRIDDEN!
   Tries to start on wrong port
```

### Solution: Remove `spring.config.import` from Naming Server

Eureka Naming Server typically **doesn't need external configuration**. Remove the config import:

**Correct `naming-server/src/main/resources/application.properties`:**

```properties
spring.application.name=naming-server
server.port=8761

eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false

# ✅ NO spring.config.import - Naming Server manages its own config
```

### When to Use `spring.config.import`

| Service | Should use `spring.config.import`? | Reason |
|---------|-----------------------------------|--------|
| **Naming Server (Eureka)** | ❌ NO | Infrastructure service, self-contained |
| **Config Server** | ❌ NO | It IS the config server |
| **Currency Exchange** | ✅ YES | Business service, needs external config |
| **Currency Conversion** | ✅ YES | Business service, needs external config |

### Verification

After removing `spring.config.import`, restart Naming Server:

```powershell
cd C:\workspace\Intellij-workspace\springboot\naming-server
mvn spring-boot:run
```

Access Eureka Dashboard:
```
http://localhost:8761
```

You should see the Eureka Dashboard on port 8761 ✅

---

## Issue: Eureka Client Connection Failed / Service Not Registering

### Symptoms

- Application fails to start or shows repeated connection errors
- Logs show errors like:
```
com.netflix.discovery.shared.transport.TransportException: Cannot execute request on any known server
```
or
```
Connection refused: localhost/127.0.0.1:8761
```
- Service doesn't appear in Eureka Dashboard
- Feign client calls fail with "No instances available"

### Root Cause: Wrong or Missing Eureka URL

The `eureka.client.service-url.defaultZone` property is either:
1. **Missing** - Client doesn't know where Eureka server is
2. **Wrong URL** - Points to incorrect host/port
3. **Eureka Server not running** - URL is correct but server is down

**Example configuration:**
```properties
# ✅ Correct property in client application
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
```

### Common Mistakes

| Mistake | Example | Issue |
|---------|---------|-------|
| Missing `/eureka` suffix | `http://localhost:8761` | Registration fails |
| Wrong port | `http://localhost:8762/eureka` | Connection refused |
| Wrong host | `http://eureka-server:8761/eureka` | DNS resolution fails (local dev) |
| Typo in property name | `eureka.client.serviceUrl.defaultZone` | Property ignored |

### Where to Configure This Property

| Service | Location | Should have `eureka.client.service-url.defaultZone`? |
|---------|----------|-----------------------------------------------------|
| **Currency Exchange** | `currency-exchange/src/main/resources/application.properties` | ✅ YES |
| **Currency Conversion** | `currency-conversion-service/src/main/resources/application.properties` | ✅ YES |
| **Naming Server (Eureka)** | `naming-server/src/main/resources/application.properties` | ❌ NO (it IS the server) |
| **Config Server** | `config-server/src/main/resources/application.properties` | ⚠️ Optional (if registering with Eureka) |

### Solution

#### Step 1: Ensure Eureka Server is Running

Start the Naming Server first:
```powershell
cd C:\workspace\Intellij-workspace\springboot\naming-server
mvn spring-boot:run
```

Verify it's running:
```
http://localhost:8761
```

#### Step 2: Configure Client Applications Correctly

**Client app** (`currency-exchange/src/main/resources/application.properties`):
```properties
spring.application.name=currency-exchange

spring.config.import=optional:configserver:http://localhost:8888

# ✅ Eureka client configuration - URL must include /eureka suffix
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
```

#### Step 3: Verify Registration

After starting your client service, check the Eureka Dashboard:
```
http://localhost:8761
```

Your service should appear under "Instances currently registered with Eureka"

### Startup Order

Services should be started in this order:

```
1. Naming Server (Eureka)     → http://localhost:8761
2. Config Server              → http://localhost:8888
3. Currency Exchange          → http://localhost:8000
4. Currency Conversion        → http://localhost:8100
```

```
┌─────────────────┐
│  Naming Server  │  ← Start FIRST
│   (port 8761)   │
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌────────┐  ┌────────────────────┐
│ Config │  │ Currency Exchange  │
│ Server │  │ Currency Conversion│
└────────┘  └────────────────────┘
              Register with Eureka
```

---

## Related Issues

### Issue: Version Mismatch
- Spring Boot 4.0.5 requires Spring Cloud **2024.0.0** or compatible version
- Using incompatible versions may cause errors

### Issue: Config Server as both Server and Client
- If `spring-cloud-config-server` transitively includes config client, exclude it:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### Issue: Missing LoadBalancer Dependency

**Error:**
```
IllegalStateException: No Feign Client for loadBalancing defined. 
Did you forget to include spring-cloud-starter-loadbalancer?
```

**Solution:** Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

### Issue: Git Push Rejected (Non-Fast-Forward)

**Error:**
```
! [rejected]        main -> main (non-fast-forward)
error: failed to push some refs to 'https://github.com/...'
hint: Updates were rejected because the tip of your current branch is behind
```

**Solution Options:**

1. **Pull and Merge (Recommended):**
   ```powershell
   git pull origin main
   git push origin main
   ```

2. **Pull with Rebase (Cleaner History):**
   ```powershell
   git pull --rebase origin main
   git push origin main
   ```

3. **Force Push (Use with Caution!):**
   ```powershell
   git push --force origin main
   ```

---

### Issue: @LoadBalanced RestClient.Builder Conflicts with Eureka Client (Spring Cloud 2025.x)

**Error:**
```
No instances available for localhost
Cannot execute request on any known server
BeanCurrentlyInCreationException: Error creating bean with name 'scopedTarget.eurekaClient':
  Requested bean is currently in creation: Is there an unresolvable circular reference?
```

**Root Cause:**
In Spring Cloud 2025.x (with Spring Boot 4.x), Eureka internally uses `RestClient.Builder` for its HTTP communication. If you define a `@LoadBalanced RestClient.Builder` bean, Eureka picks it up and tries to load-balance its own calls to `http://localhost:8761/eureka/`, treating `localhost` as a service name to resolve via Eureka — causing a circular dependency.

```
@LoadBalanced RestClient.Builder bean
        ↓
Eureka Client picks it up internally
        ↓
Eureka tries to call http://localhost:8761/eureka/
        ↓
Load Balancer intercepts → tries to resolve "localhost" as a service name in Eureka
        ↓
💥 Circular dependency / "No instances available for localhost"
```

**❌ Wrong Configuration (Causes Conflict):**
```java
@Configuration
public class RestClientConfiguration {

    @Bean
    @LoadBalanced
    RestClient.Builder restClientBuilder() {  // Eureka also uses RestClient.Builder internally!
        return RestClient.builder();
    }

    @Bean
    RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
```

**✅ Correct Configuration (Use RestTemplate Instead):**
```java
@Configuration
public class RestClientConfiguration {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {  // Eureka does NOT use RestTemplate → no conflict
        return new RestTemplate();
    }
}
```

**Why RestTemplate Works:**
| Approach | Conflict with Eureka? |
|----------|----------------------|
| `@LoadBalanced RestClient.Builder` | ❌ YES — Eureka uses `RestClient.Builder` internally |
| `@LoadBalanced RestTemplate` | ✅ NO — Eureka does NOT use `RestTemplate` |

---

### How @LoadBalanced Works with Service Names

**What @LoadBalanced Does:**
- Adds a `LoadBalancerInterceptor` to `RestTemplate`
- Intercepts HTTP calls and resolves service names via Eureka
- `http://currency-exchange/...` → Eureka looks up `CURRENCY-EXCHANGE` → resolves to `http://10.96.172.144:8000/...`

**Configuration:**
```java
@Configuration
public class RestClientConfiguration {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

**Controller Usage — With vs Without Load Balancer:**

```java
@RestController
public class CurrencyConversionController {

    @Autowired
    private RestTemplate restTemplate;  // Load-balanced RestTemplate

    // ─────────────────────────────────────────────────────────
    // WITHOUT Load Balancer — Direct call using localhost + port
    // ─────────────────────────────────────────────────────────
    @GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversion(...) {

        // Use plain RestClient (NOT load-balanced) for direct calls
        CurrencyConversion currencyConversion = RestClient.create().get()
                .uri("http://localhost:8000/currency-exchange/from/{from}/to/{to}",
                        uriVariables)
                .retrieve()
                .body(CurrencyConversion.class);
        // ...
    }

    // ─────────────────────────────────────────────────────────
    // WITH Load Balancer — Uses Eureka service name
    // ─────────────────────────────────────────────────────────
    @GetMapping("/currency-conversion/lb/from/{from}/to/{to}/quantity/{quantity}")
    public CurrencyConversion calculateCurrencyConversionWithLB(...) {

        // Use @LoadBalanced RestTemplate — service name resolved via Eureka
        CurrencyConversion currencyConversion = restTemplate.getForObject(
                "http://currency-exchange/currency-exchange/from/{from}/to/{to}",
                CurrencyConversion.class,
                uriVariables);
        // ...
    }
}
```

**URL Format Comparison:**

| Type | URL Format | Example |
|------|------------|---------|
| **Without LB** (direct) | `http://localhost:{port}/{api-path}` | `http://localhost:8000/currency-exchange/from/USD/to/INR` |
| **With LB** (Eureka) | `http://{service-name}/{api-path}` | `http://currency-exchange/currency-exchange/from/USD/to/INR` |
| **Feign Client** | N/A (uses `@FeignClient(name="currency-exchange")`) | Automatic via proxy |

**⚠️ Common Mistake — Missing API Path in Load-Balanced URL:**

```
❌ http://currency-exchange/from/USD/to/INR
                           ↑ missing /currency-exchange path!

✅ http://currency-exchange/currency-exchange/from/USD/to/INR
   ├── service name ──────┘└── API path (@GetMapping) ──────┘
   │   (Eureka lookup)        (actual controller mapping)
```

The URL has `currency-exchange` **twice** because:
1. **First** `currency-exchange` = Eureka **service name** (resolved by load balancer)
2. **Second** `/currency-exchange/from/{from}/to/{to}` = the actual **API path** defined in `@GetMapping`
3. These are two different things that happen to have the same name

---

### How Service Name Mapping Works in API Gateway (GatewayConfig)

**GatewayConfig.java:**
```java
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Route for Currency Exchange Service
                .route("currency-exchange", r -> r
                        .path("/currency-exchange/**")
                        .uri("lb://currency-exchange"))
                // Route for Currency Conversion Service
                .route("currency-conversion-service", r -> r
                        .path("/currency-conversion/**")
                        .uri("lb://currency-conversion-service"))
                .build();
    }
}
```

**How Gateway Routing Works:**

```
Browser Request:
  http://localhost:8765/currency-exchange/from/USD/to/INR
                  │              │
                  │              └── Path: /currency-exchange/from/USD/to/INR
                  └── Gateway port

Gateway matches: /currency-exchange/** → routes to lb://currency-exchange
Eureka resolves: currency-exchange → http://10.96.172.144:8000
Final call:      http://10.96.172.144:8000/currency-exchange/from/USD/to/INR
```

**Gateway URL vs RestTemplate URL:**

| Call Type | URL | Repeats Service Name? |
|-----------|-----|-----------------------|
| **Gateway** (browser → gateway) | `http://localhost:8765/currency-exchange/from/USD/to/INR` | **NO** — Gateway handles routing |
| **RestTemplate** (service → service) | `http://currency-exchange/currency-exchange/from/USD/to/INR` | **YES** — service name + API path |
| **Feign** (service → service) | Automatic via `@FeignClient(name="currency-exchange")` | **NO** — Feign handles it |

**Why Gateway Doesn't Repeat the Name:**
- GatewayConfig routes `/currency-exchange/**` to `lb://currency-exchange`
- The gateway forwards the **full path** `/currency-exchange/from/USD/to/INR` to the target service
- So the user only types the path once

**Why RestTemplate Repeats the Name:**
- RestTemplate URL = `http://{service-name}/{full-api-path}`
- `currency-exchange` is the service name (for Eureka lookup)
- `/currency-exchange/from/{from}/to/{to}` is the full API path (from `@GetMapping`)
- These are two different things that happen to have the same name

---

## Issue: Zipkin Connection Refused (`java.net.ConnectException` from `ZipkinHttpClientSender`)

### Symptoms
- Application logs show repeated `java.net.ConnectException` errors
- Stack trace includes `ZipkinHttpClientSender.postSpans()` and `ClosedChannelException`
- The app itself works, but tracing data fails to send

### Root Cause
Your application has **Micrometer Tracing + Zipkin** dependencies and is trying to send distributed tracing spans to a Zipkin server at the default endpoint `http://localhost:9411/api/v2/spans`, but **Zipkin is not running**.

### Key Property
```properties
# In application.properties of any service that has Zipkin tracing enabled
management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans   # default value
```

### Fix Options

**Option 1: Start Zipkin** (recommended if you want distributed tracing)
```bash
docker run -d -p 9411:9411 openzipkin/zipkin
```
Then open `http://localhost:9411` to view traces.

**Option 2: Disable Zipkin reporting** (if you don't need tracing right now)
Remove these dependencies from `pom.xml`:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<!-- Remove the Zipkin reporter -->
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```
Or simply set sampling to 0:
```properties
management.tracing.sampling.probability=0.0
```

### Note
This error is **non-fatal** — the application continues to work, but you'll see these connection errors periodically in logs as the async reporter tries to flush spans to Zipkin.

---

## Issue: Trailing Slash in URL Causes 404 Not Found

### Symptoms
- `http://localhost:8765/currency-exchange/from/USD/to/INR` works ✅
- `http://localhost:8765/currency-exchange/from/USD/to/INR/` returns 404 ❌ (note the trailing `/`)
- Error message: `404 NOT_FOUND` or `ResponseStatusException: 404 NOT_FOUND`

### Root Cause

**Starting from Spring Boot 3.0+ (Spring Framework 6.0+), trailing slash matching is disabled by default.**

Previously (Spring Boot 2.x), Spring MVC automatically treated these as the same URL:
- `/currency-exchange/from/USD/to/INR` → matched ✅
- `/currency-exchange/from/USD/to/INR/` → also matched ✅ (trailing slash silently ignored)

Now they are treated as **two different URLs** per the URI specification (RFC 3986).

### Why Spring Removed Trailing Slash Matching

| Reason | Explanation |
|--------|-------------|
| **REST semantics** | `/users` and `/users/` are technically different resources per RFC 3986. `/users/` implies a directory/container, `/users` implies a specific resource |
| **Security concerns** | Two URLs mapping to the same handler can bypass path-based security filters, CORS rules, and rate limiters |
| **Consistency** | The old behavior was a hidden "magic" convenience that obscured the actual URL contract |

### How the Request Flows (with trailing slash)

```
Browser: GET /currency-exchange/from/USD/to/INR/
   ↓
API Gateway: path("/currency-exchange/**") → matches ✅ (** is a glob, matches anything)
   ↓
Forwards to: lb://currency-exchange/currency-exchange/from/USD/to/INR/
   ↓
Currency Exchange Controller: @GetMapping("/currency-exchange/from/{from}/to/{to}")
   ↓
Spring MVC: "/currency-exchange/from/USD/to/INR/" ≠ "/currency-exchange/from/{from}/to/{to}"
   ↓
404 Not Found ❌ (strict pattern matching rejects the trailing slash)
```

### Fix Options

**Option 1: Don't use trailing slashes in URLs (Recommended)**
```
✅ http://localhost:8765/currency-exchange/from/USD/to/INR
❌ http://localhost:8765/currency-exchange/from/USD/to/INR/
```

**Option 2: Re-enable trailing slash matching (Not recommended)**

Add a configuration class in the service:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }
}
```

> ⚠️ **Note:** This is deprecated in Spring Framework 6.0+ and may be removed in future versions. The recommended approach is Option 1.

### Key Takeaway
> In Spring Boot 3.0+ / 4.0+, **always call REST APIs without trailing slashes**. The controller mapping `@GetMapping("/path")` will only match `/path`, not `/path/`.

---

## Issue: SLF4J Simple Conflicts with Logback — Breaks Tracing

### Symptoms
- Application crashes on startup with:
```
SLF4J(I): Actual provider is of type [org.slf4j.simple.SimpleServiceProvider]
IllegalStateException: LoggerFactory is not a Logback LoggerContext but Logback is on the classpath.
Either remove Logback or the competing implementation (class org.slf4j.simple.SimpleLoggerFactory
loaded from file:/.m2/repository/org/slf4j/slf4j-simple/2.0.17/slf4j-simple-2.0.17.jar)
```
- Tracing does NOT work (no traceId/spanId in logs, nothing in Zipkin)

### Root Cause
Both `slf4j-simple` and `logback` are on the classpath. SLF4J can only bind to **one** implementation. `slf4j-simple` wins the binding, but Spring Boot expects Logback. This breaks:
1. **Logging** — Spring Boot's log configuration doesn't apply
2. **Tracing** — Micrometer Tracing stores traceId/spanId in Logback's MDC. Without Logback, MDC doesn't work
3. **Zipkin** — If the app crashes, no spans are ever created or reported

### Fix
**Remove** `slf4j-simple` AND `slf4j-api` from `pom.xml`:
```xml
<!-- ❌ DELETE THESE — Spring Boot already includes SLF4J + Logback -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
</dependency>
```

> **Rule:** NEVER add `slf4j-simple` or `slf4j-api` manually to a Spring Boot project. They are already included via `spring-boot-starter-logging` (pulled in by any starter).

---

## Issue: Feign Client Requests Not Showing in Zipkin Traces

### Symptoms
- Currency-conversion-service shows up in Zipkin ✅
- Currency-exchange does **NOT** appear in the **same trace** ❌
- Currency-exchange logs show a **different traceId** than currency-conversion-service
- In Zipkin, the trace shows only one service instead of both linked together

### Root Cause
Feign clients do **NOT** automatically propagate trace context (traceId/spanId) to downstream services. The `feign-micrometer` library is required to bridge Feign with Micrometer's `ObservationRegistry`.

Without `feign-micrometer`:
- Feign makes a plain HTTP call with **no B3 trace headers**
- The downstream service (currency-exchange) has no incoming trace context
- So it creates a **brand new, unrelated trace**
- Zipkin sees two separate traces instead of one linked trace

### Fix
Add this dependency to `currency-conversion-service/pom.xml` (or any service using `@FeignClient`):
```xml
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-micrometer</artifactId>
</dependency>
```

Also ensure these dependencies are present in **both** the calling and called service:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-zipkin</artifactId>
</dependency>
```

### Verification
After fixing, make a request via Feign and check:
1. **Both service logs** should show the **same traceId**
2. **Zipkin UI** should show both services in the same trace timeline

---

## Issue: RestTemplate / RestClient Requests Not Showing in Zipkin Traces

### Symptoms
- The calling service shows up in Zipkin ✅
- The downstream service does **NOT** appear in the same trace ❌
- Using `@LoadBalanced RestTemplate` or `RestClient.create()` to call other services

### Root Cause 1: `new RestTemplate()` — No Tracing Interceptors

If you create `RestTemplate` with `new RestTemplate()`, Spring's tracing interceptor is **NOT** attached:

```java
// ❌ WRONG — creates a plain RestTemplate with no tracing
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

### Fix for RestTemplate
Use `RestTemplateBuilder` instead:
```java
// ✅ CORRECT — RestTemplateBuilder attaches tracing interceptors automatically
@Bean
@LoadBalanced
public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.build();
}
```

### Root Cause 2: `RestClient.create()` — Unmanaged Client

If you create `RestClient` with `RestClient.create()`, it's an unmanaged instance with no tracing:

```java
// ❌ WRONG — raw client, no tracing
CurrencyConversion result = RestClient.create().get()
        .uri("http://localhost:8000/currency-exchange/from/{from}/to/{to}", vars)
        .retrieve()
        .body(CurrencyConversion.class);
```

### Fix for RestClient
Inject the Spring-managed `RestClient.Builder`:
```java
// ✅ CORRECT — Spring auto-configures Builder with tracing
@Autowired
private RestClient.Builder restClientBuilder;

// In your method:
CurrencyConversion result = restClientBuilder.build().get()
        .uri("http://localhost:8000/currency-exchange/from/{from}/to/{to}", vars)
        .retrieve()
        .body(CurrencyConversion.class);
```

### Summary Table

| HTTP Client | ❌ No Tracing | ✅ With Tracing |
|-------------|--------------|----------------|
| **RestTemplate** | `new RestTemplate()` | `restTemplateBuilder.build()` |
| **RestClient** | `RestClient.create()` | `restClientBuilder.build()` (inject `RestClient.Builder`) |
| **Feign** | Without `feign-micrometer` dependency | With `feign-micrometer` dependency |

> **Key Rule:** Never use `new RestTemplate()` or `RestClient.create()` if you want distributed tracing. Always use Spring-managed builders so tracing interceptors are automatically injected.

---

### Complete Working URLs Reference

| Endpoint | Direct URL | Via API Gateway |
|----------|-----------|-----------------|
| Currency Exchange | `http://localhost:8000/currency-exchange/from/USD/to/INR` | `http://localhost:8765/currency-exchange/from/USD/to/INR` |
| Currency Conversion (direct) | `http://localhost:8100/currency-conversion/from/USD/to/INR/quantity/10` | `http://localhost:8765/currency-conversion/from/USD/to/INR/quantity/10` |
| Currency Conversion (LB) | `http://localhost:8100/currency-conversion/lb/from/USD/to/INR/quantity/10` | `http://localhost:8765/currency-conversion/lb/from/USD/to/INR/quantity/10` |
| Currency Conversion (Feign) | `http://localhost:8100/currency-conversion/feign/from/USD/to/INR/quantity/10` | `http://localhost:8765/currency-conversion/feign/from/USD/to/INR/quantity/10` |

---

