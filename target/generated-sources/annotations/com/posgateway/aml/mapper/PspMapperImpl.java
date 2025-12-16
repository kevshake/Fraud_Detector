package com.posgateway.aml.mapper;

import com.posgateway.aml.dto.psp.PspResponse;
import com.posgateway.aml.dto.psp.PspUserResponse;
import com.posgateway.aml.entity.Role;
import com.posgateway.aml.entity.User;
import com.posgateway.aml.entity.psp.Psp;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-16T17:02:47+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251118-1623, environment: Java 21.0.9 (Eclipse Adoptium)"
)
@Component
public class PspMapperImpl implements PspMapper {

    @Override
    public PspResponse toResponse(Psp psp) {
        if ( psp == null ) {
            return null;
        }

        PspResponse.PspResponseBuilder pspResponse = PspResponse.builder();

        pspResponse.id( psp.getPspId() );
        pspResponse.pspCode( psp.getPspCode() );
        pspResponse.legalName( psp.getLegalName() );
        pspResponse.status( psp.getStatus() );
        pspResponse.billingPlan( psp.getBillingPlan() );

        return pspResponse.build();
    }

    @Override
    public PspUserResponse toResponse(User user) {
        if ( user == null ) {
            return null;
        }

        PspUserResponse.PspUserResponseBuilder pspUserResponse = PspUserResponse.builder();

        pspUserResponse.id( user.getId() );
        pspUserResponse.role( userRoleName( user ) );
        pspUserResponse.email( user.getEmail() );

        pspUserResponse.fullName( user.getFullName() );
        pspUserResponse.status( user.isEnabled() ? "ACTIVE" : "INACTIVE" );

        return pspUserResponse.build();
    }

    private String userRoleName(User user) {
        if ( user == null ) {
            return null;
        }
        Role role = user.getRole();
        if ( role == null ) {
            return null;
        }
        String name = role.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }
}
