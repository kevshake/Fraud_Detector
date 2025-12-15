package com.posgateway.aml.service.analytics;

import com.posgateway.aml.entity.merchant.BeneficialOwner;
import com.posgateway.aml.entity.merchant.Merchant;
import com.posgateway.aml.repository.BeneficialOwnerRepository;
import com.posgateway.aml.repository.MerchantRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorporateStructureService {

    private final BeneficialOwnerRepository beneficialOwnerRepository;
    private final MerchantRepository merchantRepository;

    @Transactional(readOnly = true)
    public CorporateGraph buildCorporateGraph(Long merchantId) {
        log.info("Building Corporate Graph for Merchant ID: {}", merchantId);

        Merchant rootMerchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));

        CorporateGraph graph = new CorporateGraph();
        graph.setRootMerchantName(rootMerchant.getLegalName());
        graph.setRootMerchantId(merchantId);

        // 1. Get Owners of Root
        List<BeneficialOwner> rootOwners = beneficialOwnerRepository.findByMerchant_MerchantId(merchantId);

        for (BeneficialOwner owner : rootOwners) {
            GraphNode ownerNode = GraphNode.builder()
                    .id("OWNER-" + owner.getOwnerId())
                    .label(owner.getFullName())
                    .type("UBO")
                    .details("Passport: " + (owner.getPassportNumber() != null
                            ? "***" + owner.getPassportNumber().substring(owner.getPassportNumber().length() - 4)
                            : "N/A"))
                    .build();
            graph.getNodes().add(ownerNode);
            graph.getEdges().add(new GraphEdge("OWNS", ownerNode.getId(), "MERCHANT-" + merchantId));

            // 2. Find Related Companies (Merchants shared by this Owner)
            // Query by Passport/National ID
            List<BeneficialOwner> otherAppearances = new ArrayList<>();
            if (owner.getPassportNumber() != null) {
                otherAppearances.addAll(beneficialOwnerRepository.findByPassportNumber(owner.getPassportNumber()));
            } else if (owner.getNationalId() != null) {
                otherAppearances.addAll(beneficialOwnerRepository.findByNationalId(owner.getNationalId()));
            }

            for (BeneficialOwner other : otherAppearances) {
                if (!other.getMerchant().getMerchantId().equals(merchantId)) {
                    Merchant related = other.getMerchant();

                    GraphNode relatedMerchantNode = GraphNode.builder()
                            .id("MERCHANT-" + related.getMerchantId())
                            .label(related.getLegalName())
                            .type("MERCHANT")
                            .status(related.getStatus())
                            .build();

                    // Add node if not exists (using Set or logic)
                    if (graph.getNodes().stream().noneMatch(n -> n.getId().equals(relatedMerchantNode.getId()))) {
                        graph.getNodes().add(relatedMerchantNode);
                    }

                    graph.getEdges().add(new GraphEdge("OWNS", ownerNode.getId(), relatedMerchantNode.getId()));
                }
            }
        }

        // Add Root Node last to ensure it exists
        graph.getNodes().add(GraphNode.builder()
                .id("MERCHANT-" + merchantId)
                .label(rootMerchant.getLegalName())
                .type("MERCHANT")
                .status(rootMerchant.getStatus())
                .isRoot(true)
                .build());

        return graph;
    }

    @Data
    public static class CorporateGraph {
        private Long rootMerchantId;
        private String rootMerchantName;
        private List<GraphNode> nodes = new ArrayList<>();
        private List<GraphEdge> edges = new ArrayList<>();
    }

    @Data
    @Builder
    public static class GraphNode {
        private String id;
        private String label;
        private String type; // MERCHANT, UBO
        private String details;
        private String status;
        private boolean isRoot;
    }

    @Data
    @AllArgsConstructor
    public static class GraphEdge {
        private String relationship;
        private String source;
        private String target;
    }
}
