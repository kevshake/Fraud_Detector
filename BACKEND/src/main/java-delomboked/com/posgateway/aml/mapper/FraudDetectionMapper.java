package com.posgateway.aml.mapper;

import com.posgateway.aml.dto.FraudDetectionResponseDTO;
import com.posgateway.aml.service.HighConcurrencyFraudOrchestrator;
import com.posgateway.aml.service.AsyncFraudDetectionOrchestrator;
import com.posgateway.aml.service.FraudDetectionOrchestrator.FraudDetectionResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FraudDetectionMapper {

    FraudDetectionResponseDTO toResponse(FraudDetectionResult result);

    FraudDetectionResponseDTO toResponse(HighConcurrencyFraudOrchestrator.FraudDetectionResult result);

    FraudDetectionResponseDTO toResponse(AsyncFraudDetectionOrchestrator.FraudDetectionResult result);
}
