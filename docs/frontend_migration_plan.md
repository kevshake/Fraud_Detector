# Frontend Migration Plan

## Assumptions
- Backend is a **REST** Spring Boot application (Spring Web). 
- We will use **React** with **TypeScript** (strongly recommended for non‑trivial code).
- All existing API contracts will remain unchanged.
- Development will run on Windows, Node.js LTS is installed.

## High‑level Prep Checklist
1. **Decide how React and Spring will work together** (Option A – separate dev servers, Option B – bundle into Spring).  
2. **Confirm and document the backend API** (endpoints, request/response shapes, auth).  
3. **Set up the React project** with Vite + TypeScript.  
4. **Configure networking and base URL** (proxy for dev, environment‑based base URL).  
5. **Plan folder & feature structure** (domain‑based modules).  
6. **Choose UI library & data‑fetching library** (e.g., MUI + TanStack Query).  
7. **Set up auth / CORS** (session cookies or JWT, Spring CORS config).  
8. **Configure build & deployment pipeline** (dev server, production build, optional bundling into Spring).  
9. **Create migration task document** to track all UI element migrations.

---
## 1. Decide How React and Spring Will Work Together
### Option A – Separate Deployments (recommended for fast iteration)
- **Backend** runs on `http://localhost:2637` (or `http://localhost:8080` after you change the port).
- **React dev server** runs on `http://localhost:5173` (Vite default).
- Vite proxy forwards `/api/v1/**` to the Spring backend.
- In production you can host the static build on any web server (Nginx, CloudFront, Vercel, Netlify) and keep the Spring service separate.

### Option B – Bundle React Inside Spring Boot (single JAR)
- Build React (`npm run build`) → `frontend/dist`.
- Copy the `dist` folder into `src/main/resources/static` during Maven build.
- Spring Boot serves the UI from the same JAR.
- More complex CI pipeline, but results in a single artifact.

**Recommendation:** Use **Option A** for development and initial production. You can later switch to Option B if you need a single‑artifact deployment.

---
## 2. Confirm and Document Backend API
Create a markdown file `docs/api_spec.md` (or use Swagger/OpenAPI if available) containing:
- **Base URL** (dev: `http://localhost:2637`, prod: `https://api.yourdomain.com`).
- **Endpoints** (method, path, query params, request body, response schema, status codes). Example:
  ```markdown
  ### GET /api/v1/cases
  - Query: `status` (optional)
  - Response: `[{id, reference, status, priority, ...}]`
  ```
- Authentication flow (login endpoint, cookie vs JWT).
- Any custom headers (e.g., `X-Requested-With`).

If you have Swagger, generate TypeScript types with `openapi-generator-cli`.

---
## 3. Set Up the React Project (Vite + TypeScript)
```bash
cd d:\PROJECTS\POS_GATEWAY\APP\AML_FRAUD_DETECTOR
mkdir frontend
cd frontend
npm create vite@latest . -- --template react-ts   # creates a Vite React + TS project
npm install
```
Verify:
```bash
npm run dev   # opens http://localhost:5173
```
Commit the new `frontend` folder.

---
## 4. Configure Networking and Base URL
### a) API Base URL constant
Create `src/config/api.ts`:
```ts
export const API_BASE_URL = import.meta.env.MODE === "production"
  ? "https://api.yourdomain.com"
  : "http://localhost:2637"; // matches Spring dev port
```
### b) Vite dev proxy (so `/api/v1/**` goes to Spring)
Edit `vite.config.ts`:
```ts
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api/v1": {
        target: "http://localhost:2637",
        changeOrigin: true,
        secu
      },
    },
  },
});
```
In production you will use the full `API_BASE_URL` in API calls.

---
## 5. Plan Folder & Feature Structure
```
frontend/
├─ public/                # static assets (favicon, index.html placeholder)
├─ src/
│   ├─ app/
│   │   ├─ App.tsx
│   │   └─ routes.tsx
│   ├─ features/
│   │   ├─ cases/
│   │   │   ├─ components/
│   │   │   │   ├─ CasesTable.tsx
│   │   │   │   ├─ CaseDetail.tsx
│   │   │   │   └─ ActionButtons.tsx
│   │   │   ├─ api/
│   │   │   │   └─ casesApi.ts
│   │   │   └─ hooks/
│   │   │       └─ useCases.ts
│   │   ├─ alerts/ …
│   │   └─ … (other domains)
│   ├─ shared/
│   │   ├─ ui/          # reusable UI components (Button, Modal, Table)
│   │   └─ lib/         # utility functions, formatters
│   ├─ config/
│   │   └─ api.ts
│   └─ index.tsx
├─ vite.config.ts
├─ tsconfig.json
└─ package.json
```
This domain‑based layout scales as you add more AML features.

---
## 6. Choose UI Library & Data‑Fetching Library
### UI Library (pick one)
- **Material‑UI (MUI)** – robust, widely used, excellent table/grid components.
- **Ant Design** – rich component set, good for enterprise.
- **Mantine** – modern, lightweight, good TypeScript support.
- **Chakra UI** – simple, composable.
Add the library:
```bash
npm install @mui/material @emotion/react @emotion/styled   # example for MUI
```
### Data‑Fetching / Server State
- **TanStack Query (React Query)** – handles caching, refetching, loading/error states.
```bash
npm install @tanstack/react-query
```
Create a generic API client (`src/lib/apiClient.ts`) that uses `fetch` and integrates with React Query.
Optionally replace with **Axios** later if you need interceptors.

---
## 7. Set Up Auth / CORS
### Backend (Spring)
Add a CORS configuration bean (restrict to dev origin during development):
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173") // dev origin
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                .allowCredentials(true);
    }
}
```
For production replace the origin with your static host URL.
### Frontend
- If the backend uses **session cookies**, ensure `fetch` includes `credentials: "include"` (already in the generic client).
- If the backend uses **JWT**, store the token securely (httpOnly cookie preferred, otherwise in memory or localStorage) and add an `Authorization: Bearer <token>` header in the API client.

---
## 8. Configure Build & Deployment
### Development
```bash
npm run dev   # Vite dev server with proxy
```
### Production Build
```bash
npm run build   # outputs to frontend/dist
```
#### Deployment Options
1. **Static host** (Nginx, CloudFront, Vercel, Netlify) – serve `dist` folder. Backend runs separately.
2. **Bundle into Spring** (optional later) – add a Maven `frontend-maven-plugin` step to run `npm install && npm run build` and copy `dist` into `src/main/resources/static`.
   ```xml
   <plugin>
       <groupId>com.github.eirslett</groupId>
       <artifactId>frontend-maven-plugin</artifactId>
       <version>1.12.0</version>
       <executions>
           <execution>
               <id>npm install</id>
               <goals><goal>npm</goal></goals>
               <configuration><arguments>install</arguments></configuration>
           </execution>
           <execution>
               <id>npm run build</id>
               <goals><goal>npm</goal></goals>
               <configuration><arguments>run build</arguments></configuration>
           </execution>
           <execution>
               <id>copy to static</id>
               <phase>process-resources</phase>
               <goals><goal>copy-resources</goal></goals>
               <configuration>
                   <outputDirectory>${project.build.outputDirectory}/static</outputDirectory>
                   <resources>
                       <resource>
                           <directory>frontend/dist</directory>
                       </resource>
                   </resources>
               </configuration>
           </execution>
       </executions>
   </plugin>
   ```
### CI/CD (high level)
- **GitHub Actions** (or Azure Pipelines) step to `npm ci && npm run build`.
- Maven build runs afterwards, packaging the JAR (or Docker image) with the built UI if you choose Option B.

---
## 9. Migration Task Document
Create `docs/migration_tasks.md` (see separate file) that lists every UI component to be migrated from the legacy static HTML/JS to React components, and tracks progress.

---
## 10. Next Steps
1. Commit the new `frontend` folder and updated `frontend_migration_plan.md`.
2. Add `docs/api_spec.md` and `docs/migration_tasks.md`.
3. Run the backend (`./run_temp.ps1`) and start the React dev server (`npm run dev`).
4. Verify the dashboard loads, API calls succeed, and CORS works.
5. Iterate on UI components, gradually replacing the old static pages.

---
*This plan incorporates the detailed checklist you provided and aligns with the goal of a lightweight, maintainable front‑end*
