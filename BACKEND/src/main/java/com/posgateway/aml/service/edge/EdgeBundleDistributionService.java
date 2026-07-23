package com.posgateway.aml.service.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.posgateway.aml.config.edge.EdgeControlPlaneKeys;
import com.posgateway.aml.edge.EdgeBundleService;
import com.posgateway.aml.edge.crypto.HokekaSecureEnvelope;
import com.posgateway.aml.entity.edge.EdgeNode;
import com.posgateway.aml.entity.rules.RuleDefinition;
import com.posgateway.aml.repository.rules.RuleDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

/**
 * Builds and HSE-1 seals the rule bundle for one authorised edge node.
 *
 * <p>Flow (contract §5, platform doc §5): the PSP's enabled rules are compiled to the edge Rule IR,
 * versioned, serialised, then sealed with {@link com.posgateway.aml.edge.crypto.HokekaSecureEnvelope}
 * against <b>that edge's pinned X25519 public key</b> and signed with the control plane's Ed25519
 * key, under the fixed context {@value #BUNDLE_CONTEXT}. Because the context is bound into both the
 * HKDF info and the AEAD AAD, a bundle envelope can never be opened as a metrics envelope, and
 * because the sealing key is the edge's own pinned key, no other edge (and nothing in between, e.g.
 * a PSP-controlled TLS-terminating proxy) can read it.
 */
@Service
public class EdgeBundleDistributionService {

    private static final Logger log = LoggerFactory.getLogger(EdgeBundleDistributionService.class);

    /** Contract §3 — rule bundle distribution context bytes. */
    public static final String BUNDLE_CONTEXT = "hokeka.rules.bundle";

    private final RuleDefinitionRepository ruleDefinitionRepository;
    private final EdgeRuleCompiler compiler;
    private final EdgeBundleService bundleService;
    private final EdgeControlPlaneKeys controlPlaneKeys;
    private final HokekaSecureEnvelope envelope;
    private final ObjectMapper mapper = new ObjectMapper();

    public EdgeBundleDistributionService(RuleDefinitionRepository ruleDefinitionRepository,
                                         EdgeRuleCompiler compiler,
                                         EdgeBundleService bundleService,
                                         EdgeControlPlaneKeys controlPlaneKeys,
                                         HokekaSecureEnvelope envelope) {
        this.ruleDefinitionRepository = ruleDefinitionRepository;
        this.compiler = compiler;
        this.bundleService = bundleService;
        this.controlPlaneKeys = controlPlaneKeys;
        this.envelope = envelope;
    }

    /** @param version the bundle version, also used as the HTTP ETag for {@code If-None-Match}. */
    public record SealedBundle(long version, byte[] sealed, int ruleCount, List<String> skippedRules) {}

    /** The version an edge would be served right now, without doing the (costlier) sealing work. */
    @Transactional(readOnly = true)
    public long currentVersion(Long pspId) {
        return compiler.compile(rulesFor(pspId)).version();
    }

    /**
     * Compile + seal the current bundle for {@code node}. The node must already have been
     * authorised by {@link EdgeEnrollmentService#findAuthorized(String)} — this method assumes the
     * keys are pinned and does not re-check the lifecycle state.
     *
     * <p>The rule IR is wrapped in the contract §4 <b>replay envelope</b> before sealing, because
     * the channel is symmetric: the edge's {@code SealedEnvelopeCodec.openPayloadBytes} opens the
     * HSE-1 envelope, validates {@code nonce}/{@code issuedAt}/{@code edgeId} against its own
     * replay guard, and only then hands the inner {@code payload} to the interpreter. Sealing the
     * bare IR would be rejected by every edge as "replay envelope has no payload".
     */
    @Transactional(readOnly = true)
    public SealedBundle sealFor(EdgeNode node) {
        EdgeRuleCompiler.CompilationResult compiled = compiler.compile(rulesFor(node.getPspId()));

        byte[] plaintext = wrapInReplayEnvelope(
                bundleService.buildBundleJson(compiled.version(), node.getPspId(), compiled.rules()),
                node.getEdgeId());

        byte[] edgeX25519Pub = Base64.getDecoder().decode(node.getEdgeX25519PublicKey());
        byte[] sealed = envelope.seal(plaintext, edgeX25519Pub, controlPlaneKeys.signingPrivateKey(),
                BUNDLE_CONTEXT.getBytes(StandardCharsets.UTF_8));

        log.debug("Sealed edge bundle v{} for edge {} (psp {}): {} rules, {} skipped",
                compiled.version(), node.getEdgeId(), node.getPspId(), compiled.rules().size(),
                compiled.skipped().size());
        return new SealedBundle(compiled.version(), sealed, compiled.rules().size(), compiled.skipped());
    }

    /** {@code {"nonce":…,"issuedAt":…,"edgeId":…,"payload":<rule IR>}} — contract §4. */
    private byte[] wrapInReplayEnvelope(byte[] payloadJson, String edgeId) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("nonce", EdgeReplayGuard.newNonce());
            root.put("issuedAt", Instant.now().toString());
            root.put("edgeId", edgeId);
            root.set("payload", mapper.readTree(payloadJson));
            return mapper.writeValueAsBytes(root);
        } catch (Exception e) {
            throw new IllegalStateException("failed to build the bundle replay envelope", e);
        }
    }

    /**
     * A PSP's own enabled rules; when it has none yet (no per-PSP copies provisioned), the enabled
     * global system defaults, so a freshly onboarded edge still evaluates something meaningful.
     */
    private List<RuleDefinition> rulesFor(Long pspId) {
        List<RuleDefinition> own = ruleDefinitionRepository
                .findByEnabledTrueAndPspIdOrderByPriorityDesc(pspId);
        if (!own.isEmpty()) {
            return own;
        }
        return ruleDefinitionRepository.findByEnabledTrueAndPspIdIsNullOrderByPriorityDesc();
    }
}
