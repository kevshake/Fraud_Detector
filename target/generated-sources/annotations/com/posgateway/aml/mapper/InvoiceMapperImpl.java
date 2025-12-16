package com.posgateway.aml.mapper;

import com.posgateway.aml.dto.psp.InvoiceResponse;
import com.posgateway.aml.entity.psp.Invoice;
import com.posgateway.aml.entity.psp.InvoiceLineItem;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-16T17:02:48+0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251118-1623, environment: Java 21.0.9 (Eclipse Adoptium)"
)
@Component
public class InvoiceMapperImpl implements InvoiceMapper {

    @Override
    public InvoiceResponse toResponse(Invoice invoice) {
        if ( invoice == null ) {
            return null;
        }

        InvoiceResponse.InvoiceResponseBuilder invoiceResponse = InvoiceResponse.builder();

        invoiceResponse.amount( invoice.getTotalAmount() );
        invoiceResponse.periodStart( invoice.getBillingPeriodStart() );
        invoiceResponse.periodEnd( invoice.getBillingPeriodEnd() );
        invoiceResponse.invoiceNumber( invoice.getInvoiceNumber() );
        invoiceResponse.status( invoice.getStatus() );
        invoiceResponse.lineItems( invoiceLineItemListToLineItemList( invoice.getLineItems() ) );

        return invoiceResponse.build();
    }

    protected InvoiceResponse.LineItem invoiceLineItemToLineItem(InvoiceLineItem invoiceLineItem) {
        if ( invoiceLineItem == null ) {
            return null;
        }

        InvoiceResponse.LineItem.LineItemBuilder lineItem = InvoiceResponse.LineItem.builder();

        lineItem.serviceType( invoiceLineItem.getServiceType() );
        lineItem.description( invoiceLineItem.getDescription() );
        if ( invoiceLineItem.getQuantity() != null ) {
            lineItem.quantity( invoiceLineItem.getQuantity() );
        }

        return lineItem.build();
    }

    protected List<InvoiceResponse.LineItem> invoiceLineItemListToLineItemList(List<InvoiceLineItem> list) {
        if ( list == null ) {
            return null;
        }

        List<InvoiceResponse.LineItem> list1 = new ArrayList<InvoiceResponse.LineItem>( list.size() );
        for ( InvoiceLineItem invoiceLineItem : list ) {
            list1.add( invoiceLineItemToLineItem( invoiceLineItem ) );
        }

        return list1;
    }
}
