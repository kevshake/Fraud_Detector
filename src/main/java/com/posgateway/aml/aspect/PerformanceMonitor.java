package com.posgateway.aml.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
@Slf4j
public class PerformanceMonitor {

    @Around("execution(* com.posgateway.aml.service.aml.AmlScreeningOrchestrator.screenMerchant(..)) || " +
            "execution(* com.posgateway.aml.service.RiskAssessmentService.assessTransactionRisk(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        final StopWatch stopWatch = new StopWatch();

        stopWatch.start();
        try {
            return joinPoint.proceed();
        } finally {
            stopWatch.stop();
            log.info("Execution time for {}.{} :: {} ms", className, methodName, stopWatch.getTotalTimeMillis());
        }
    }
}
