package com.hokeka.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Java interpreter of the Rule IR bundle — the same semantics as the Rust {@code rule-core} crate.
 *
 * <p>On the edge host it is the <b>fallback</b> evaluator: the container runs on it when the native
 * Rust core is not present, so the service is always functional; when {@code libedge_engine.so} is
 * loaded, {@link EdgeEngine} routes evaluation through the native kernel for speed. Pure and
 * I/O-free — feature values are supplied by the caller.
 */
public final class EdgeRuleInterpreter {

    public enum Action {
        ALLOW(0), ALERT(1), HOLD(2), BLOCK(3);
        final int severity;
        Action(int s) { this.severity = s; }
        Action max(Action other) { return other.severity > this.severity ? other : this; }
    }

    public record Decision(Action action, int score, List<Long> triggeredRuleIds, List<String> reasons) {}

    private final ObjectMapper mapper = new ObjectMapper();
    private volatile JsonNode bundle;

    public long loadBundle(byte[] bundleJson) {
        try {
            JsonNode parsed = mapper.readTree(bundleJson);
            this.bundle = parsed;
            return parsed.path("version").asLong(-1);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid rule bundle IR", e);
        }
    }

    public boolean hasBundle() {
        return bundle != null;
    }

    public Decision evaluate(Map<String, Object> features) {
        JsonNode b = this.bundle;
        if (b == null) {
            return new Decision(Action.HOLD, 0, List.of(),
                    List.of("edge engine has no active rule bundle (fail-closed)"));
        }
        Action action = Action.ALLOW;
        int score = 0;
        List<Long> triggered = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        for (JsonNode rule : b.path("rules")) {
            if (evalCondition(rule.get("condition"), features)) {
                action = action.max(Action.valueOf(rule.path("action").asText("ALLOW")));
                score += rule.path("score").asInt(0);
                triggered.add(rule.path("id").asLong());
                String desc = rule.path("description").asText(null);
                reasons.add(desc != null && !desc.isEmpty() ? desc : rule.path("name").asText(""));
            }
        }
        return new Decision(action, score, triggered, reasons);
    }

    private boolean evalCondition(JsonNode cond, Map<String, Object> f) {
        if (cond == null || cond.isNull()) {
            return false;
        }
        switch (cond.path("type").asText()) {
            case "all":
                for (JsonNode c : cond.path("all")) {
                    if (!evalCondition(c, f)) {
                        return false;
                    }
                }
                return true;
            case "any":
                for (JsonNode c : cond.path("any")) {
                    if (evalCondition(c, f)) {
                        return true;
                    }
                }
                return false;
            case "not":
                return !evalCondition(cond.get("not"), f);
            case "cmp":
                return evalCmp(cond, f);
            default:
                return false;
        }
    }

    private boolean evalCmp(JsonNode cond, Map<String, Object> f) {
        String field = cond.path("field").asText();
        if (!f.containsKey(field) || f.get(field) == null) {
            return false;
        }
        Object actual = f.get(field);
        JsonNode expected = cond.get("value");
        switch (cond.path("op").asText()) {
            case "EQ": return valuesEqual(actual, expected);
            case "NE": return !valuesEqual(actual, expected);
            case "GT": case "GTE": case "LT": case "LTE": {
                Double a = asNum(actual);
                Double e = expected.isNumber() ? expected.asDouble() : parseNum(expected.asText());
                if (a == null || e == null) {
                    return false;
                }
                return switch (cond.path("op").asText()) {
                    case "GT" -> a > e;
                    case "GTE" -> a >= e;
                    case "LT" -> a < e;
                    default -> a <= e;
                };
            }
            case "IN": {
                String a = valueToString(actual);
                for (String item : expected.asText().split(",")) {
                    if (item.trim().equals(a)) {
                        return true;
                    }
                }
                return false;
            }
            case "CONTAINS":
                return valueToString(actual).contains(expected.asText());
            default:
                return false;
        }
    }

    private boolean valuesEqual(Object actual, JsonNode expected) {
        Double a = asNum(actual);
        Double e = expected.isNumber() ? expected.asDouble()
                : (expected.isBoolean() ? (expected.asBoolean() ? 1.0 : 0.0) : parseNum(expected.asText()));
        if (a != null && e != null) {
            return Math.abs(a - e) < 1e-9;
        }
        return valueToString(actual).equals(expected.isBoolean()
                ? String.valueOf(expected.asBoolean()) : expected.asText());
    }

    private static Double asNum(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof Boolean b) return b ? 1.0 : 0.0;
        if (v instanceof String s) return parseNum(s);
        return null;
    }

    private static Double parseNum(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }

    private static String valueToString(Object v) {
        if (v instanceof Boolean b) return b.toString();
        if (v instanceof Number n) {
            double d = n.doubleValue();
            return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
        }
        return String.valueOf(v);
    }
}
