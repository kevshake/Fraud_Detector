package com.posgateway.aml.controller.compliance;

import com.posgateway.aml.entity.merchant.Merchant;
import com.posgateway.aml.model.ScreeningResult;
import com.posgateway.aml.service.aml.SumsubAmlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/screening")
public class BatchScreeningController {

    private final SumsubAmlService sumsubAmlService;

    @PostMapping("/batch")
    public CompletableFuture<ResponseEntity<List<ScreeningResult>>> screenBatch(@RequestBody List<Merchant> merchants) {
        return sumsubAmlService.screenBatch(merchants)
                .thenApply(ResponseEntity::ok);
    }
}
