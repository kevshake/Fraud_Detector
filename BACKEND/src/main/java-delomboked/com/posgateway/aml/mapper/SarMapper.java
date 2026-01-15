package com.posgateway.aml.mapper;

import com.posgateway.aml.dto.compliance.SarResponse;
import com.posgateway.aml.entity.compliance.SuspiciousActivityReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SarMapper {

    @Mapping(target = "reportId", source = "id")
    @Mapping(target = "caseId", source = "complianceCase.id")
    @Mapping(target = "filingDate", source = "filedAt")
    @Mapping(target = "createdBy", source = "createdBy.username")
    // Status is enum to string, handled automatically
    // Narrative, CreatedAt mapped automatically by name
    SarResponse toResponse(SuspiciousActivityReport sar);
}
