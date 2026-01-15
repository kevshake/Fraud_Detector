package com.posgateway.aml.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.posgateway.aml.entity.User;
import com.posgateway.aml.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

@Aspect
@Component
public class AuditLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditLoggingAspect.class);

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AuditLoggingAspect(AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    // Pointcut for all RestControllers, excluding AuditLogController to avoid loops
    // and excluding GET requests to focus on "actions" (modifications)
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) " +
              "&& !within(com.posgateway.aml.controller.AuditLogController) " +
              "&& @annotation(org.springframework.web.bind.annotation.RequestMapping) " +
              "|| @annotation(org.springframework.web.bind.annotation.PostMapping) " +
              "|| @annotation(org.springframework.web.bind.annotation.PutMapping) " +
              "|| @annotation(org.springframework.web.bind.annotation.PatchMapping) " +
              "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void apiAction() {}

    @Around("apiAction()")
    public Object logApiAction(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        
        // Skip GET requests unless explicitly wanted (commented out for now to reduce noise)
         if ("GET".equalsIgnoreCase(request.getMethod())) {
             return joinPoint.proceed();
         }

        String action = request.getMethod() + " " + request.getRequestURI();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String entityType = className.replace("Controller", "").toUpperCase();
        
        // Attempt to find entity ID from args (heuristic)
        String entityId = "N/A";
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long) {
            entityId = args[0].toString();
        }

        User user = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            user = (User) authentication.getPrincipal();
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            
             // Log Success
            auditLogService.logAction(
                    user,
                    "API_ACTION",
                    entityType,
                    entityId,
                    null, // Before state (hard to capture generic)
                    null, // After state
                    request.getRemoteAddr(),
                    "Action: " + action + " | Method: " + methodName
            );

            return result;
        } catch (Throwable e) {
            // Log Failure
            auditLogService.logAction(
                    user,
                    "API_ERROR",
                    entityType,
                    entityId,
                    null,
                    null,
                    request.getRemoteAddr(),
                    "Failed Action: " + action + " | Error: " + e.getMessage()
            );
            throw e;
        }
    }
}
