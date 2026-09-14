---
name: spring-boot-conventions
description: Project skeleton + hexagonal architecture conventions for new Spring Boot backend services (Java 25, Boot 4, Lombok, MapStruct, ArchUnit, springdoc). Use when setting up a new Spring Boot service or its module structure. Extracted from wasilah/backend.
license: MIT
---

# Spring Boot Backend Conventions

Konventionen für neue Spring-Boot-Backend-Services: Versions-Matrix,
hexagonale Struktur, Code-Regeln, ArchUnit. Extrahiert aus
`wasilah/backend` (funktionierendes Referenzprojekt) und verallgemeinert.

## When to use

- Ein neuer Spring-Boot-Service wird aufgesetzt (Projekt-Skeleton, `pom.xml`, Package-Struktur).
- Ein bestehender Service soll auf die gemeinsamen Konventionen gebracht werden.
- Braucht der Service **Persistenz** (Datenbank/State)? → Zusätzlich den Skill
  `spring-boot-persistence` installieren (`apm install`). Dieser Skill deckt nur
  das zustandslose Grundgerüst ab.

Nicht für: Auth-Integration mit Keycloak (→ `spring-boot-keycloak-integration`,
Wasilah-spezifisch), Infra/Betrieb (→ `postgres-ops`, `argocd-gitops-deploy`).

## Versions-Matrix (geprüft 2026-09-14)

| Komponente | Version | Hinweis |
|---|---|---|
| Java | 25 | `maven.compiler.release=25` |
| Spring Boot | 4.1.1 | neueste stabile; Parent-POM `spring-boot-starter-parent` |
| Lombok | 1.18.38 | `scope=provided` + im `annotationProcessorPaths` |
| MapStruct | 1.6.3 | `mapstruct` + `mapstruct-processor` |
| lombok-mapstruct-binding | 0.2.0 | **Pflicht** im `annotationProcessorPaths`, sonst Processor-Konflikt |
| springdoc-openapi | 3.1.1 | `springdoc-openapi-starter-webmvc-ui` — **nicht** 2.x (das ist für Boot 3) |
| ArchUnit | 1.4.1 | `archunit-junit5`, `scope=test` |
| JUnit | 6.0.1 | `junit-jupiter`, `scope=test` |
| Starter | — | `spring-boot-starter-webmvc` (Boot-4-Name, nicht `starter-web`) |

Vor jeder Neuinstallation gegen offizielle Quellen prüfen: `spring.io`,
`springdoc.org`, Maven Central, `mapstruct.org` (siehe „Versionen aktuell halten").

## Hexagonale Struktur (PFLICHT)

```
src/main/java/<group>/<artifact>/
├── domain/            → immutable Records, reine Business-Logik, KEINE Spring-Deps
├── port/in/           → Use-Case Interfaces (driven by web adapter)
├── port/out/          → Repository Interfaces (driving persistence adapter)
├── application/       → Use-Case Implementierungen (@Service, @Transactional)
└── adapter/web/       → REST Controller, DTOs, MapStruct Mapper
```

`adapter/persistence/` kommt nur dazu, wenn der Service Persistenz braucht
(→ `spring-boot-persistence`).

## Code-Regeln

```java
// ✅ Domain-Modelle = immutable Records
public record Organization(OrganizationId id, String name, PortalType portalType) {
    public static Organization create(...) { }              // Factory
    public Organization activate() { return new Organization(...); }  // Transition
}

// ✅ Lombok auf Services (PFLICHT)
@Service @Slf4j @RequiredArgsConstructor
class OrganizationService {
    private final OrganizationRepository repo;  // Constructor Injection via Lombok
    // log.info(...) funktioniert via @Slf4j
}

// ❌ NIEMALS manuell:
private static final Logger log = LoggerFactory.getLogger(X.class);
public String getName() { return name; }
```

- Domain-Records: keine Spring-Annotationen, keine Lombok-Getter nötig (Records haben sie nativ).
- Mapper: MapStruct-Interfaces, DTOs in `adapter/web/dto/`.
- JPA-Entities (falls Persistenz): NUR Strings statt Enums, Konvertierung im Mapper
  (Details → `spring-boot-persistence`).

## ArchUnit-Regel-Vorlage

```java
@AnalyzeClasses(packages = "com.example.app")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_has_no_spring_deps =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "..adapter..");

    @ArchTest
    static final ArchRule application_depends_only_on_ports =
        classes().that().resideInAPackage("..application..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain..", "..port..", "..application..", "java..", "org.springframework..");

    @ArchTest
    static final ArchRule web_adapter_depends_on_ports_and_application =
        classes().that().resideInAPackage("..adapter.web..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain..", "..port..", "..application..", "..adapter.web..", "java..", "org.springframework..", "org.mapstruct..");
}
```

## Versionen aktuell halten

Bei jeder Neuinstallation (oder vor einem Versions-Bump in einem Projekt):

1. Gegen offizielle Quellen prüfen: `spring.io/projects/spring-boot` (Boot-Version),
   `springdoc.org` (springdoc-Version), Maven Central (Lombok, MapStruct, ArchUnit, JUnit),
   `mapstruct.org` (MapStruct + Binding).
2. Abweichungen in diesem Skill korrigieren und `version` in `apm.yaml` per SemVer
   hochzählen: Patch = Versions-Bump ohne Strukturänderung, Minor = neue Regel ergänzt,
   Major = Architektur-Musterwechsel.
3. Beide Skills (`spring-boot-conventions` + `spring-boot-persistence`) synchron halten —
   sie teilen sich Java-/Lombok-/MapStruct-Versionen.