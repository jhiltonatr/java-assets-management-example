---
name: manage-library
description: Use when updating, bumping, or removing a library that is already managed in the Maven standards BOMs. Triggers: "bump version", "update library", "upgrade", "downgrade", "remove/delete dependency", "change version property", "per-JDK override". Locates the version property and its per-flavor overrides, applies the JDK-compatibility rule, and regenerates the BOM docs.
---

# Manage a managed library

Maintain version properties and per-JDK overrides for a library that already exists in the BOM
hierarchy (added via `enroll-library`).

## Steps

1. **Locate the property + all references.**
   - Definition: `<dependencyManagement>` entry and property in `java-company-standards/pom.xml`
     (template = top of the hierarchy).
   - Overrides: grep the property name across `java-company-standards-jdk*/pom.xml` and
     `java-springboot-standards-jdk*/pom.xml`. It can be redefined per variant.
2. **Update the template version.** Bump/downgrade the property value in
   `java-company-standards/pom.xml` (one place, all flavors inherit it). Keep
   `logback-classic` and `logback-core` paired at the same version.
3. **Fix per-JDK overrides.**
   - Verify each override's version is the newest that works on THAT JDK (same
     compatibility check as `enroll-library`: docs + class-file major version 61=17, 65=21,
     69=25; bytecode above a flavor's JDK breaks it).
   - If the new template version now works for a flavor, DELETE its redundant override so the
     parent handles the version (no limitation → no override).
   - If a flavor still needs a different version, keep/update its override in the matching
     leaf: Spring Boot consumers → `java-springboot-standards-jdkXX/pom.xml`; plain Java →
     `java-company-standards-jdkXX/pom.xml`. Never put it in the sibling `-jdkXX` company BOM
     for Spring Boot consumers (invisible to them).
4. **Removals.** Delete the property, its `<dependency>` under `<dependencyManagement>` in the
   template, and ALL per-JDK overrides. Grep first to confirm nothing else references the
   property.
5. **Precedence sanity check.** Direct `<dependencyManagement>` entries win over imported BOMs
   (`spring-boot-dependencies`). If a bump lowers a version vs. the Spring Boot platform because
   a newer one is CVE-affected, note the reason in the change summary.
6. **Regenerate docs and verify.** `mvn generate-resources` + `mvn install`. Docs are rendered
   from the effective model — never edit them by hand.

## Rules

- The rule "a version override only takes effect in a POM in the consumer's parent chain" decides
  where overrides live. Sibling `java-company-standards-jdkXX` overrides are invisible to Spring
  Boot consumers.
- Never rename a property without updating every reference/override.
- Keep per-flavor overrides minimal: only the variants that genuinely differ.