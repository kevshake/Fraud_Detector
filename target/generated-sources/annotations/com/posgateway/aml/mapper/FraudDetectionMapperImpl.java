package com.posgateway.aml.mapper;

import com.posgateway.aml.dto.FraudDetectionResponseDTO;
import com.posgateway.aml.service.AsyncFraudDetectionOrchestrator;
import com.posgateway.aml.service.FraudDetectionOrchestrator;
import com.posgateway.aml.service.HighConcurrencyFraudOrchestrator;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-07T12:54:25+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260101-2150, environment: Java 21.0.9 (Eclipse Adoptium)"
)
@Component
public class FraudDetectionMapperImpl implements FraudDetectionMapper {

    @Override
    public FraudDetectionResponseDTO toResponse(FraudDetectionOrchestrator.FraudDetectionResult result) {
        if ( result == null ) {
            return null;
        }

        FraudDetectionResponseDTO fraudDetectionResponseDTO = new FraudDetectionResponseDTO();

        fraudDetectionResponseDTO.setTxnId( result.getTxnId() );
        fraudDetectionResponseDTO.setAction( result.getAction() );
        List<String> list = result.getReasons();
        if ( list != null ) {
            fraudDetectionResponseDTO.setReasons( new ArrayList<String>( list ) );
        }
        fraudDetectionResponseDTO.setLatencyMs( result.getLatencyMs() );

        return fraudDetectionResponseDTO;
    }

    @Override
    public FraudDetectionResponseDTO toResponse(HighConcurrencyFraudOrchestrator.FraudDetectionResult result) {
        if ( result == null ) {
            return null;
        }

        FraudDetectionResponseDTO fraudDetectionResponseDTO = new FraudDetectionResponseDTO();

        fraudDetectionResponseDTO.setTxnId( result.getTxnId() );
        fraudDetectionResponseDTO.setAction( result.getAction() );
        List<String> list = result.getReasons();
        if ( list != null ) {
            fraudDetectionResponseDTO.setReasons( new ArrayList<String>( list ) );
        }
        fraudDetectionResponseDTO.setLatencyMs( result.getLatencyMs() );

        return fraudDetectionResponseDTO;
    }

    @Override
    public FraudDetectionResponseDTO toResponse(AsyncFraudDetectionOrchestrator.FraudDetectionResult result) {
        if ( result == null ) {
            return null;
        }

        FraudDetectionResponseDTO fraudDetectionResponseDTO = new FraudDetectionResponseDTO();

        fraudDetectionResponseDTO.setTxnId( result.getTxnId() );
        fraudDetectionResponseDTO.setAction( result.getAction() );
        List<String> list = result.getReasons();
        if ( list != null ) {
            fraudDetectionResponseDTO.setReasons( new ArrayList<String>( list ) );
        }
        fraudDetectionResponseDTO.setLatencyMs( result.getLatencyMs() );

        return fraudDetectionResponseDTO;
    }
}
