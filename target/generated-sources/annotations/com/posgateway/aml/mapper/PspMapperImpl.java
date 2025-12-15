package com.posgateway.aml.mapper;

import com.posgateway.aml.dto.psp.PspResponse;
import com.posgateway.aml.dto.psp.PspUserResponse;
import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.entity.psp.PspUser;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-15T17:42:28+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251118-1623, environment: Java 21.0.9 (Eclipse Adoptium)"
)
@Component
public class PspMapperImpl implements PspMapper {

    @Override
    public PspResponse toResponse(Psp psp) {
        if ( psp == null ) {
            return null;
        }

        PspResponse pspResponse = new PspResponse();

        pspResponse.setId( psp.getPspId() );
        pspResponse.setPspCode( psp.getPspCode() );
        pspResponse.setLegalName( psp.getLegalName() );
        pspResponse.setStatus( psp.getStatus() );
        pspResponse.setBillingPlan( psp.getBillingPlan() );

        return pspResponse;
    }

    @Override
    public PspUserResponse toResponse(PspUser user) {
        if ( user == null ) {
            return null;
        }

        PspUserResponse pspUserResponse = new PspUserResponse();

        pspUserResponse.setId( user.getUserId() );
        pspUserResponse.setEmail( user.getEmail() );
        pspUserResponse.setFullName( user.getFullName() );
        pspUserResponse.setRole( user.getRole() );
        pspUserResponse.setStatus( user.getStatus() );

        return pspUserResponse;
    }
}
