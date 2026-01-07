package com.posgateway.aml.aspect;

import com.posgateway.aml.service.PrometheusMetricsService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Aspect to automatically collect metrics from service methods
 */
@Aspect
@Component
public class MetricsAspect {

    private static final Logger logger = LoggerFactory.getLogger(MetricsAspect.class);

    private final PrometheusMetricsService metricsService;

    @Autowired
    public MetricsAspect(PrometheusMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Around("execution(* com.posgateway.aml.service.TransactionIngestionService.*(..))")
    public Object measureTransactionProcessing(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordTransactionProcessingTime(duration);
            metricsService.incrementTransactionProcessed(null, "processed");
            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordTransactionProcessingTime(duration);
            throw e;
        }
    }

    @Around("execution(* com.posgateway.aml.service.AmlService.*(..))")
    public Object measureAmlAssessment(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordAmlAssessmentTime(duration);
            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordAmlAssessmentTime(duration);
            throw e;
        }
    }

    @Around("execution(* com.posgateway.aml.service.FraudDetectionService.*(..))")
    public Object measureFraudAssessment(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordFraudAssessmentTime(duration);
            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordFraudAssessmentTime(duration);
            throw e;
        }
    }

    @Around("execution(* com.posgateway.aml.service.*ScoringService.*(..))")
    public Object measureModelScoring(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordModelScoringTime(duration);
            metricsService.incrementModelScoring(true);
            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordModelScoringTime(duration);
            metricsService.incrementModelScoring(false);
            throw e;
        }
    }
}

