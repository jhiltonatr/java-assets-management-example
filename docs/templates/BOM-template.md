# Global Company BOM Reference Manual

Welcome to the internal engineering catalog. This central **Bill of Materials (BOM)** governs all software builds across our application ecosystem, serving as the immutable architectural truth for stable and approved libraries. 

Every platform tier, developer workbench, and downstream microservice inherits these standard baselines. By centralizing versioning, we isolate our runtime configurations from ad-hoc dependencies, preventing version-drift and establishing an automated foundation for **JFrog Xray** security auditing at our primary registry endpoints.

---

## 🛠️ Global Build Environment Baseline

The fundamental environment constraints enforced inside our core builds. Every conforming compilation node must establish compatibility boundaries around these runtime profiles.

| Ecosystem Primitive | Version Profile | Operational Boundary / Constraints |
| :--- | :--- | :--- |
| **Java Platform Platform (JDK)** | `21.0.6-LTS` | Target compilation baseline (`source/target = 21`). Explicitly optimized for virtual threads. |
| **Apache Maven Framework** | `3.9.9` | Enforced execution minimum. Build loops drop dependencies on standard wrappers `mvnw` below this tier. |
| **Enterprise Node Architecture** | `22.14.0-LTS` | Global container standard for microservice frontends and full-stack runtime bundles. |

---

## 📦 Core Ecosystem Dependencies

The canonical manifest of foundational software modules authorized for production environments. Sub-modules inherit these declarations explicitly by declaring the master BOM under their `<dependencyManagement>` tree block.

### 🍃 Enterprise Services Tier (Spring Ecosystem)
* **`org.springframework.boot:spring-boot-dependencies`** → `3.4.3`  
  *Core foundation for microservice profiles, cloud routing, configuration management, and declarative data adapters.*
* **`org.springframework.cloud:spring-cloud-dependencies`** → `2024.0.0`  
  *Service registry synchronization frameworks, internal cluster load balancing topologies, and discovery clients.*
* **`org.springframework.security:spring-security-core`** → `6.4.2`  
  *Mandatory system authorization adapter. Preconfigured with secure OIDC protocol profiles for enterprise SSO interfaces.*

### 🛠️ Common Utility Primitives
* **`org.projectlombok:lombok`** → `1.18.36`  
  *Compile-time metadata transformation engine. Explicitly matched to work natively with the target JDK JVM profile.*
* **`org.apache.commons:commons-lang3`** → `3.17.0`  
  *Standard core extensions for character matrix mutations, system runtime inspections, and fallback mathematical utilities.*
* **`com.google.guava:guava`** → `33.4.0-jre`  
  *Advanced graph algorithms, memory cache models, and immutable collections wrapper pipelines.*

### 📑 Persistence & Data Exchange Planes
* **`org.postgresql:postgresql`** → `42.7.5`  
  *Primary storage driver engine layer. Configured explicitly with optimized client connection pool timeouts.*
* **`com.fasterxml.jackson.core:jackson-databind`** → `2.18.2`  
  *Enterprise standard parsing infrastructure layer. Patched to safely block abstract polymorphism injection exploits.*

### 🧪 Validation & Regression Frameworks
* **`org.junit.jupiter:junit-jupiter-engine`** → `5.11.4`  
  *Standard compilation assertion block. Integrated into standard CI regression suites.*
* **`org.mockito:mockito-core`** → `5.15.2`  
  *Declarative mock generator layer for boundary condition and remote pipeline structural isolation test environments.*

---

## 🔌 Approved Maven Plugin Suite

Standard plugin specifications configured globally. Direct application lifecycles rely on these fixed versions within `<pluginManagement>` to enforce predictable artifact compilation routines.

### 🏭 Compilation & Packaging Pipeline
* **`org.apache.maven.plugins:maven-compiler-plugin`** → `3.13.0`  
  *Direct Java compiler abstraction handler. Leverages multi-core compilation passes on local work targets.*
* **`org.apache.maven.plugins:maven-surefire-plugin`** → `3.5.2`  
  *Asynchronous isolated regression verification loop runner. Generates structural test metrics output standard.*
* **`org.apache.maven.plugins:maven-failsafe-plugin`** → `3.5.2`  
  *Integration environment verification harness. Runs post-packaging verification operations on test images.*
* **`org.apache.maven.plugins:maven-jar-plugin`** → `3.4.2`  
  *Final bundle compression engine layer. Explicitly injects standardized system manifest attributes.*

### 🏗️ Code Quality & Structural Analysis
* **`org.jacoco:jacoco-maven-plugin`** → `0.8.12`  
  *Code path analysis toolkit. Enforces standard minimum target unit test coverage branches during CI build pipelines.*
* **`org.apache.maven.plugins:maven-checkstyle-plugin`** → `3.6.0`  
  *Static analysis framework enforcing universal code formatting conventions.*
* **`com.diffplug.spotless:spotless-maven-plugin`** → `2.44.3`  
  *Automated target refactoring formatter engine ensuring standard indentation patterns before codebase commits.*

---

## 🚀 How to Consume This BOM

Developers must prevent local version overrides. Include this unified BOM definition in your application's root `pom.xml` configuration layout.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.company.architecture</groupId>
            <artifactId>corporate-global-bom</artifactId>
            <version>${project.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

When importing dependencies within child microservices, **omit the `<version>` element entirely** to inherit the approved global version automatically:

```xml
<dependencies>
    <!-- Approved version inherited transparently from Enterprise BOM -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```