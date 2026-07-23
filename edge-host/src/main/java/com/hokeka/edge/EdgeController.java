package com.hokeka.edge;

import com.hokeka.edge.channel.EdgeMetricsAggregator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Local transaction API exposed to the PSP's own API nodes over TLS 1.3. Requests are served on
 * virtual threads (see {@code spring.threads.virtual.enabled}). No transaction data leaves the edge;
 * only the aggregate counters recorded here are ever shipped upward.
 */
@RestController
@RequestMapping("/edge")
public class EdgeController {

    private final EdgeEngine engine;
    private final EdgeMetricsAggregator metrics;

    public EdgeController(EdgeEngine engine, EdgeMetricsAggregator metrics) {
        this.engine = engine;
        this.metrics = metrics;
    }

    /**
     * Engine + authorization status (also surfaced through /actuator/health). {@code evaluator}
     * reports which engine actually serves traffic, so a silent fall back from the native kernel to
     * the Java interpreter is visible at a glance.
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("evaluator", engine.activeEvaluator());
        status.put("nativeCore", engine.nativeAvailable() ? "loaded" : "absent");
        status.put("nativeDegraded", engine.nativeDegraded());
        status.put("nativeDegradedReason", engine.nativeDegradedReason());
        status.put("standbyBundleReady", engine.standbyReady());
        status.put("ruleBundleVersion", engine.activeVersion());
        status.put("ruleBundleHash", engine.activeBundleHash());
        status.put("authorization", engine.authorizationState());
        status.put("authorizationReason", engine.authorizationReason());
        return status;
    }

    /** Evaluate a transaction's features → decision. HOLD until the node is authorized. */
    @PostMapping("/evaluate")
    public ResponseEntity<EdgeRuleInterpreter.Decision> evaluate(@RequestBody Map<String, Object> features) {
        long startNanos = System.nanoTime();
        EdgeRuleInterpreter.Decision decision = engine.evaluate(features);
        metrics.record(decision, (System.nanoTime() - startNanos) / 1000.0);
        return ResponseEntity.ok(decision);
    }
}
