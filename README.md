# Maven Standards (maven-assets)

Company-wide Maven standards as BOMs ("Bill of Materials"). Every target JDK level is its own
Maven project, so each artifact ships with a stable, concrete `artifactId` (no profiles, no
dynamic coordinates):

- `java-company-standards` — shared plain-Java settings template (defaults to JDK 17)
- `java-company-standards-jdk17` / `java-company-standards-jdk21` — plain-Java BOMs, one per JDK
- `java-springboot-standards` — shared Spring Boot settings template
- `java-springboot-standards-jdk17` / `java-springboot-standards-jdk21` — Spring Boot BOMs built on
  the matching company variant
- `sample-springboot-app` — standalone demo app consuming `java-springboot-standards-jdk21` from
  the local repository (it is intentionally NOT part of the aggregator reactor)

## Layout

```
maven-assets/
├── pom.xml                                        maven-standards (aggregator)
├── java-company-standards/
│   ├── pom.xml                                    java-company-standards (template)
│   ├── java-company-standards-jdk17/pom.xml
│   └── java-company-standards-jdk21/pom.xml
├── java-springboot-standards/
│   ├── pom.xml                                    java-springboot-standards (template)
│   ├── java-springboot-standards-jdk17/pom.xml
│   └── java-springboot-standards-jdk21/pom.xml
└── sample-springboot-app/
    ├── pom.xml
    └── src/...
```

## How it works

The templates hold all shared configuration once: managed dependency versions, build plugin
versions and configuration (compiler `--release`, enforcer, jacoco, spotless, ...), corporate
repositories, and distribution targets.

Each `-jdkXX` variant is a thin POM that extends the template and only pins the settings that
differ per JDK flavor (`java.version`, `maven.compiler.release`, `spring-boot.version`, ...).

The parent chain for a Spring Boot consumer is:

```
app
└── java-springboot-standards-jdkXX
    └── java-springboot-standards (template)
        └── java-company-standards (template)
            └── maven-standards (aggregator)
```

## Build and install

A single build publishes every variant to the local Maven repository:

```
mvn install        # in the project root
```

Consumers that use `<relativePath/>` (like the sample app) resolve the standards parent from the
local cache, so they must be (re)installed whenever the standards change:

```
mvn install -U     # refresh after editing the standards
cd sample-springboot-app
mvn clean spring-boot:run
```

## Using the standards in your application

As a parent (recommended): all standards (JDK level, plugin config, repositories) are inherited
in one step.

```xml
<parent>
    <groupId>com.example.company</groupId>
    <artifactId>java-springboot-standards-jdk21</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath/>    <!-- resolve from the local repository, not from disk -->
</parent>
```

Switching JDK flavor is just changing the parent `artifactId` to
`java-springboot-standards-jdk17` (or `java-company-standards-jdkXX` for plain-Java projects).

Alternatively the BOM can be used as `dependencyManagement` with scope `import`. Note that an
import only conveys version management — `java.version`, plugin configuration, repositories and
`distributionManagement` are NOT imported; consumers must declare those themselves.

## Managing versions per JDK flavor

Each version knob lives in a specific POM. **The rule: a version override only takes effect in a
POM that sits in the consumer's parent chain.**

| Setting | Where to change it |
|---|---|
| Target JDK (`java.version`, `maven.compiler.release`) | The `-jdkXX` leaf you are publishing |
| Spring Boot version (`spring-boot.version`) | `java-springboot-standards-jdkXX/pom.xml` |
| Springdoc version (`springdoc.version`) | `java-springboot-standards-jdkXX/pom.xml` |
| Library versions that must differ per flavor (e.g. `logback.version`) | `java-springboot-standards-jdkXX/pom.xml` |
| Library versions shared by all flavors (JUnit, Jackson, ...) | `java-company-standards/pom.xml` (template) |

Example — JDK 21 currently pairs Spring Boot 3.5.3 with Logback 1.5.16:

```xml
<!-- java-springboot-standards-jdk21/pom.xml -->
<properties>
    <java.version>21</java.version>
    <maven.compiler.release>21</maven.compiler.release>
    <spring-boot.version>3.5.3</spring-boot.version>
    <logback.version>1.5.16</logback.version>
</properties>
```

### Why the override must live in the Spring Boot variant

`java-company-standards-jdk21` is a *sibling* BOM of the Spring Boot chain, not an ancestor.
Overrides placed there are invisible to Spring Boot consumers (the app inherits the *template*
`java-company-standards`, which defaults `logback.version` to 1.4.14). Put flavor-specific
overrides in the POM the consumer actually inherits — the `-jdkXX` Spring Boot leaf.

### Version skew and dependencyManagement precedence

A direct `<dependencyManagement>` entry in the standards always wins over versions imported via
the Spring Boot `spring-boot-dependencies` BOM. Because of that:

- `logback-classic` and `logback-core` are both pinned at the same `${logback.version}` so the
  pair cannot drift. Mismatched versions break at runtime:
  - classic > core: `NoClassDefFoundError: ch/qos/logback/core/util/StatusPrinter2`
  - classic < core: `PatternLayout ... getDefaultConverterSupplierMap()`
- Spring Boot 3.2.x uses the Logback 1.4.x line, Spring Boot 3.5.x the 1.5.x line. Keep the
  flavor's `logback.version` on the same line as its `spring-boot.version`.

## Adding a new JDK flavor

1. Copy `java-company-standards-jdk21` to `java-company-standards-jdk25` (project and folder),
   update `artifactId` and the two JDK properties.
2. Copy `java-springboot-standards-jdk21` to `java-springboot-standards-jdk25`, update
   `artifactId`, JDK properties, and any version that needs a newer line.
3. Register both new modules in the aggregator `pom.xml` (`<modules>`).

Build once with `mvn install`; both old and new flavor artifacts coexist in the local repository
and can be selected per application by parent `artifactId`.