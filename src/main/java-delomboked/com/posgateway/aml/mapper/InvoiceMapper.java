package com.posgateway.aml.mapper;

import com.posgateway.aml.dto.psp.InvoiceResponse;
import com.posgateway.aml.entity.psp.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "amount", source = "totalAmount")
    @Mapping(target = "periodStart", source = "billingPeriodStart")
    @Mapping(target = "periodEnd", source = "billingPeriodEnd")
    InvoiceResponse toResponse(Invoice invoice);

    // Inner class mapping handling
    InvoiceResponse.LineItem toLineItemResponse(Invoice.LineItem item);
}
