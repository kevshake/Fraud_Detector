# System Architecture Summary

## Quick Reference

### Project Layout
```
AML_FRAUD_DETECTOR/
├── BACKEND/     → Spring Boot API (Java 17, Maven, Port 2637)
├── FRONTEND/    → React SPA (TypeScript, Vite, Port 5173)
└── docs/        → All documentation
```

### Technology Stack

**Backend:**
- Java 17 + Spring Boot 3.2.0
- PostgreSQL + Aerospike + Redis
- Maven build
- REST API at `/api/v1`

**Frontend:**
- React 18 + TypeScript 5.2
- Vite 5.0 (build tool)
- Material-UI 5.14 (components)
- TanStack Query 5.14 (data fetching)

### Communication
- Frontend calls backend via REST API
- Vite proxy: `/api/v1/**` → `http://localhost:2637`
- Independent deployment
- No shared code

### Running the System

**Backend:**
```bash
cd BACKEND
mvn spring-boot:run
```

**Frontend:**
```bash
cd FRONTEND
npm install
npm run dev
```

### Key Documents
- `docs/01-Technical-Architecture.md` - Complete architecture
- `docs/ARCHITECTURE_OVERVIEW.md` - Detailed overview
- `docs/FINAL_STRUCTURE.md` - Project structure
- `FRONTEND/README.md` - Frontend guide

---

*For complete details, see ARCHITECTURE_OVERVIEW.md*
