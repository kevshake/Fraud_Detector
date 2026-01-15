# Migration Tasks Checklist

## Phase 1: Project Restructuring ⏳

### Folder Creation
- [ ] Create `FRONEND` folder at project root
- [ ] Create `FRONEND/frontend` subfolder
- [ ] Create `BACKEND` folder at project root
- [ ] Create `docs` folder at project root

### Backend Code Migration
- [ ] Move `src/main/java/**` to `BACKEND/src/main/java/`
- [ ] Move `src/main/resources/**` to `BACKEND/resources/`
- [ ] Verify all Java packages are intact after move
- [ ] Update `pom.xml` to reference `BACKEND/src/main/java` as source directory
- [ ] Update `pom.xml` to reference `BACKEND/resources` as resources directory

### Documentation Migration
- [ ] Move all `.md` files to `docs/` folder
- [ ] Move `frontend_migration_plan.md` to `docs/`
- [ ] Create `docs/UI_SDLc.md`
- [ ] Create `docs/Backend_SDLc.md`
- [ ] Create `docs/api_spec.md` (document all REST endpoints)

### Build Configuration Updates
- [ ] Update `run_temp.ps1` to reference new JAR location (if needed)
- [ ] Test Maven build: `mvn clean package -DskipTests`
- [ ] Verify JAR is created in correct location
- [ ] Test backend startup with `run_temp.ps1`

---

## Phase 2: React Project Setup ⏳

### Initial Setup
- [ ] Navigate to `FRONEND/frontend`
- [ ] Run `npm create vite@latest . -- --template react-ts`
- [ ] Run `npm install`
- [ ] Verify dev server starts: `npm run dev`
- [ ] Access `http://localhost:5173` to confirm

### Configuration
- [ ] Create `src/config/api.ts` with API_BASE_URL
- [ ] Update `vite.config.ts` with proxy configuration
- [ ] Install UI library: `npm install @mui/material @emotion/react @emotion/styled`
- [ ] Install TanStack Query: `npm install @tanstack/react-query`
- [ ] Install React Router: `npm install react-router-dom`

### Project Structure
- [ ] Create `src/app/` folder
- [ ] Create `src/features/` folder
- [ ] Create `src/features/cases/` folder
- [ ] Create `src/features/alerts/` folder
- [ ] Create `src/shared/ui/` folder
- [ ] Create `src/shared/lib/` folder
- [ ] Create `src/lib/apiClient.ts`

---

## Phase 3: Backend CORS & API Documentation ⏳

### CORS Configuration
- [ ] Create `BACKEND/src/main/java/com/posgateway/aml/config/WebConfig.java`
- [ ] Add CORS mapping for `http://localhost:5173`
- [ ] Test CORS with browser dev tools
- [ ] Update CORS for production origin (when ready)

### API Documentation
- [ ] Document all `/api/v1/compliance/cases` endpoints
- [ ] Document all `/api/v1/alerts` endpoints
- [ ] Document all `/api/v1/merchants` endpoints
- [ ] Document authentication/authorization flow
- [ ] Document request/response schemas
- [ ] Add example requests and responses

---

## Phase 4: UI Component Migration ⏳

### Core Components
- [ ] Create `App.tsx` with routing setup
- [ ] Create `src/features/cases/components/CasesTable.tsx`
- [ ] Create `src/features/cases/components/CaseDetail.tsx`
- [ ] Create `src/features/cases/components/ActionButtons.tsx`
- [ ] Create `src/features/cases/api/casesApi.ts`
- [ ] Create `src/features/cases/hooks/useCases.ts`

### API Integration
- [ ] Implement `fetchCases()` in casesApi.ts
- [ ] Implement `fetchCase(id)` in casesApi.ts
- [ ] Implement `resolveCase(id)` in casesApi.ts
- [ ] Implement `escalateCase(id, reason)` in casesApi.ts
- [ ] Test all API calls with React Query

### Dashboard Migration
- [ ] Migrate dashboard layout to React
- [ ] Migrate case statistics display
- [ ] Migrate case filters
- [ ] Migrate case table with pagination
- [ ] Test all dashboard functionality

---

## Phase 5: Legacy Code Removal ⏳

### Static Files to Delete
- [ ] Delete `src/main/resources/static/js/dashboard.js`
- [ ] Delete `src/main/resources/static/css/` (all CSS files)
- [ ] Delete `src/main/resources/static/index.html`
- [ ] Delete `src/main/resources/static/login.html`
- [ ] Delete `src/main/resources/static/logout-success.html`
- [ ] Delete `src/main/resources/static/password-reset.html`
- [ ] Delete entire `src/main/resources/static/` folder

### Code Cleanup
- [ ] Search for references to `static/` in Java code: `grep -R "static/" BACKEND/src`
- [ ] Remove any `@Value("classpath:/static/...")` references
- [ ] Update `application.properties` to set `spring.resources.static-locations=`
- [ ] Remove any controllers serving static content

### Verification
- [ ] Run backend: `./run_temp.ps1`
- [ ] Verify backend starts without errors
- [ ] Verify no static files are served by Spring Boot
- [ ] Run frontend: `cd FRONEND/frontend && npm run dev`
- [ ] Verify UI loads at `http://localhost:5173`
- [ ] Test all API calls from React app

---

## Phase 6: Testing & Deployment ⏳

### Backend Testing
- [ ] Run unit tests: `mvn test`
- [ ] Run integration tests
- [ ] Verify all tests pass
- [ ] Check code coverage

### Frontend Testing
- [ ] Set up Jest and React Testing Library
- [ ] Write unit tests for components
- [ ] Write integration tests for API calls
- [ ] Run tests: `npm test`

### Build & Deployment
- [ ] Build React app: `npm run build`
- [ ] Verify build output in `FRONEND/frontend/dist`
- [ ] Test production build locally
- [ ] Configure CI/CD pipeline (if needed)
- [ ] Deploy backend to staging
- [ ] Deploy frontend to staging
- [ ] Smoke test all features in staging

---

## Phase 7: Documentation & Finalization ⏳

### Documentation Updates
- [ ] Update main README.md with new structure
- [ ] Document how to start backend
- [ ] Document how to start frontend
- [ ] Document build process
- [ ] Document deployment process
- [ ] Complete `UI_SDLc.md`
- [ ] Complete `Backend_SDLc.md`

### Final Checks
- [ ] Review all code changes
- [ ] Ensure no hardcoded URLs or credentials
- [ ] Verify environment variables are documented
- [ ] Check all dependencies are listed
- [ ] Verify `.gitignore` is updated
- [ ] Create release notes

### Git & Version Control
- [ ] Commit all changes with clear messages
- [ ] Tag release version
- [ ] Push to remote repository
- [ ] Create pull request (if using PR workflow)
- [ ] Get code review approval

---

## Current Status Summary

### ✅ Completed Today (2026-01-12)
- Fixed database schema (`V103__fix_case_queues_schema.sql`)
- Implemented real API integration in `dashboard.js`
- Created comprehensive frontend migration plan
- Application is running successfully

### ⏳ In Progress
- Frontend migration planning (documentation complete)

### ❌ Not Started
- Project restructuring (FRONEND/BACKEND folders)
- React project initialization
- UI component migration
- Legacy code removal

---

## Notes
- This checklist should be updated as tasks are completed
- Each phase can be worked on incrementally
- Some tasks can be done in parallel (e.g., documentation while coding)
- Testing should be continuous, not just at the end
- Keep the backend running and stable while migrating the frontend

---

*Last updated: 2026-01-12T10:56:06+03:00*
