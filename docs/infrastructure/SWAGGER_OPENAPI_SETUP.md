# Swagger/OpenAPI Documentation Setup

## Overview

This project now includes **SpringDoc OpenAPI** (Swagger) for automatic API documentation generation. This provides an interactive API documentation interface that allows developers to explore and test all available endpoints.

## Access Points

Once the application is running, you can access the API documentation at:

- **Swagger UI**: `http://localhost:2637/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:2637/api-docs`
- **OpenAPI YAML**: `http://localhost:2637/api-docs.yaml`

## Configuration

### Dependencies

The following dependency has been added to `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### Configuration Files

1. **OpenApiConfig.java** (`src/main/java/com/posgateway/aml/config/OpenApiConfig.java`)
   - Configures API metadata (title, description, version)
   - Sets up security scheme (JWT Bearer token)
   - Defines server URLs (local and production)

2. **application.properties**
   - Configures Swagger UI behavior
   - Sets paths and scanning packages
   - Enables/disables Swagger UI via `SWAGGER_UI_ENABLED` environment variable

## Features

### Interactive API Testing
- Test endpoints directly from the browser
- View request/response examples
- See all available parameters and their types
- Test authentication with JWT tokens

### Automatic Documentation
- All endpoints are automatically discovered
- Request/response schemas are generated from Java classes
- Parameter descriptions from JavaDoc and annotations

### Security Integration
- JWT Bearer token authentication configured
- Use the "Authorize" button in Swagger UI to add your token
- Token obtained from `/api/v1/auth/login` endpoint

## Adding Annotations to Controllers

To enhance the documentation, you can add OpenAPI annotations to your controllers:

### Example:

```java
@RestController
@RequestMapping("/cases")
@Tag(name = "Case Management", description = "APIs for managing compliance cases")
public class CaseManagementController {

    @Operation(
            summary = "Get case timeline",
            description = "Retrieves a chronological timeline of all events related to a specific case"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Timeline retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Case not found")
    })
    @GetMapping("/{caseId}/timeline")
    public ResponseEntity<CaseTimelineDTO> getCaseTimeline(
            @Parameter(description = "Case ID", required = true, example = "1")
            @PathVariable Long caseId) {
        // Implementation
    }
}
```

### Available Annotations

- `@Tag` - Group endpoints by tag/category
- `@Operation` - Describe individual endpoints
- `@ApiResponse` - Document response codes and descriptions
- `@Parameter` - Describe parameters with examples
- `@Schema` - Describe request/response models

## Environment Variables

You can control Swagger UI visibility using:

```bash
# Disable Swagger UI in production
export SWAGGER_UI_ENABLED=false
```

## Production Considerations

**Security Note**: It's recommended to disable Swagger UI in production environments:

1. Set `SWAGGER_UI_ENABLED=false` in production
2. Or use Spring profiles to exclude Swagger in production:
   ```properties
   springdoc.swagger-ui.enabled=false
   ```

## Customization

### Changing API Info

Edit `OpenApiConfig.java` to update:
- API title and description
- Contact information
- License information
- Server URLs

### Adding More Servers

Add additional server configurations in `OpenApiConfig.java`:

```java
.servers(List.of(
    new Server().url("http://localhost:2637/api/v1").description("Local"),
    new Server().url("https://staging.api.com/api/v1").description("Staging"),
    new Server().url("https://api.com/api/v1").description("Production")
))
```

## Benefits

1. **Developer Experience**: Easy API exploration and testing
2. **Documentation**: Always up-to-date with code changes
3. **Integration**: Frontend developers can see all available endpoints
4. **Testing**: Quick endpoint testing without Postman/curl
5. **Standards**: OpenAPI 3.0 standard for API documentation

## Next Steps

1. Add `@Operation` and `@ApiResponse` annotations to more controllers
2. Add `@Schema` annotations to DTOs for better model documentation
3. Consider adding examples for complex request/response objects
4. Document error responses consistently across all endpoints

## References

- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Swagger UI](https://swagger.io/tools/swagger-ui/)

