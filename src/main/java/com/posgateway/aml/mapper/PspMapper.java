package com.posgateway.aml.mapper;

import com.posgateway.aml.dto.psp.PspResponse;
import com.posgateway.aml.dto.psp.PspUserResponse;
import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.entity.psp.PspUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PspMapper {

    @Mapping(target = "id", source = "pspId")
    PspResponse toResponse(Psp psp);

    @Mapping(target = "id", source = "userId")
    PspUserResponse toResponse(PspUser user);
}
