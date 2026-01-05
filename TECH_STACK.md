# Technology Stack Documentation

## Backend
*   **Language**: Java 21
*   **Framework**: Spring Boot 3.x
*   **Database**:
    *   **Main**: PostgreSQL (Transaction data, Users)
    *   **High-Speed/Cache**: Aerospike (Sanctions Lists, Feature aggregation)
*   **Object Mapping**:
    *   **Core**: Jackson (JSON serialization/deserialization)
    *   **DTO Mapping**: MapStruct (Performance-oriented, type-safe bean mapping)
*   **Build Tool**: Maven

## Frontend
*   **Core**: HTML5, Vanilla JavaScript (ES6+)
*   **Styling**: Custom CSS3 with CSS Custom Properties (Variables) for theming.
    *   **Design System**: Glassmorphism, Dark Mode default.
*   **Architecture**: Single Page Application (SPA) logic simulated via `ViewRenderer` in `dashboard.js`.
*   **Libraries**:
    *   **Charts**: Chart.js (Visualization of transaction volumes and risk breakdown)
    *   **Icons**: FontAwesome 6 (UI iconography)
    *   **Fonts**: Google Fonts (Open Sans)

## Infrastructure & DevOps
*   **Containerization**: Docker (planned)
*   **Testing**: JUnit 5, Mockito
*   **Browser Testing**: Custom Browser Agent for UI verification.

## Key Design Patterns implemented
*   **Controller-Service-Repository**: Standard layered architecture.
*   **Orchestrator Pattern**: Used in AML screening flows to coordinate multiple checks (Sanctions, Velocity, Rules).
*   **Factory/Strategy Pattern**: Implemented in various service logic components.
*   **Singleton**: AerospikeConnectionService ensures a single efficient connection pool.

## Recent Refactoring (MapStruct Migration)
We migrated from manual DTO conversion methods to **MapStruct** to reduce boilerplate and error susceptibility.
*   **Mappers Created**: `SarMapper`, `FraudDetectionMapper`, `InvoiceMapper`, `PspMapper`.
*   **Configuration**: `mapstruct-processor` configured in `pom.xml`.
