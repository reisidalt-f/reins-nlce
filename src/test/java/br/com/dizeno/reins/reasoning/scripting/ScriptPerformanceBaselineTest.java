/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * MPL-2.0-ADDENDUM.md
 * -------------------
 * This project includes additional terms and clarifications that apply
 * to this file. See MPL-2.0-ADDENDUM.md for details.
 */

package br.com.dizeno.reins.reasoning.scripting;

import br.com.dizeno.reins.reasoning.ToolOperationsReference;
import br.com.dizeno.reins.reasoning.tooling.file.FilePolicy;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptPerformanceBaselineTest {

    @Test
    void registryInitializationAndUserPromptScriptPerformanceStaysWithinBaseline() throws Exception {
        long registryMedianNanos = measureRegistryInitMedianNanos(40, 5);
        double registryMedianMs = nanosToMillis(registryMedianNanos);

        ReasoningScriptContext context = buildLargeContext();
        ScriptEvaluator evaluator = new ScriptEvaluator(ScriptRegistry.build(new ScriptResolver()), null);

        
        for (int i = 0; i < 30; i++) {
            evaluator.evaluate("max-turn-grace-prompt.ftl", context);
        }

        int iterations = 40;
        long scriptTotalNanos = 0L;

        for (int i = 0; i < iterations; i++) {
            long s0 = System.nanoTime();
            evaluator.evaluate("max-turn-grace-prompt.ftl", context);
            long s1 = System.nanoTime();
            scriptTotalNanos += (s1 - s0);
        }

        double scriptAvgNanos = ((double) scriptTotalNanos) / iterations;

        String metricLine = "registryMedianMs=" + round2(registryMedianMs)
            + ",maxTurnGraceScriptAvgMs=" + round2(nanosToMillis(scriptAvgNanos));

        System.out.println("[perf-baseline] registryMedianMs=" + round2(registryMedianMs)
                + " maxTurnGraceScriptAvgMs=" + round2(nanosToMillis(scriptAvgNanos)));

        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "script-performance-baseline.txt"),
            metricLine + "\n", StandardCharsets.UTF_8);

        
        assertTrue(registryMedianMs < 150.0,
                "ScriptRegistry median init too high: " + round2(registryMedianMs) + " ms");

        
        assertTrue(nanosToMillis(scriptAvgNanos) < 10.0,
                "Max-turn grace script average render time too high. scriptMs="
            + round2(nanosToMillis(scriptAvgNanos)));
    }

    private long measureRegistryInitMedianNanos(int iterations, int warmups) {
        List<Long> samples = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            long t0 = System.nanoTime();
            ScriptRegistry.build(new ScriptResolver());
            long t1 = System.nanoTime();
            if (i >= warmups) {
                samples.add(t1 - t0);
            }
        }
        Collections.sort(samples);
        return samples.get(samples.size() / 2);
    }

    private ReasoningScriptContext buildLargeContext() {
        StringBuilder refs = new StringBuilder();
        for (int i = 0; i < 50000; i++) {
            refs.append("root-").append(i).append(".md").append('\n');
        }

        StringBuilder msg = new StringBuilder("Implement feature using referenced files and keep behavior stable.\n");
        for (int i = 0; i < 20000; i++) {
            msg.append("Rule-").append(i).append(": preserve output compatibility.\n");
        }

        return ReasoningScriptContext.builder()
            .source(new ReasoningScriptViews.SourceView(
                        "src/main/nl/large-source.md",
                        "/tmp/large-source.md",
                        "# large",
                        "hash-large",
                        "main",
                        List.of()))
                .fileBases(new ReasoningScriptViews.FileBasesView(
                        "/tmp/main",
                        "/tmp/test",
                        "/tmp/target",
                        "/tmp"))
                .inference(new ReasoningScriptViews.InferenceStateView(
                        msg.toString(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()))
                .config(new ReasoningScriptViews.ConfigView(new br.com.dizeno.reins.run.config.ReinsConfig()))
                .cycle(new ReasoningScriptViews.CycleView("perf-cycle", 1, 8, "IN_PROGRESS", null))
                .policy(new ReasoningScriptViews.PolicyView(
                        List.of("main", "test", "target"),
                        List.of("main", "test", "target"),
                        List.of("target"),
                        List.of("target"),
                        List.of("target"),
                        List.of("main", "test", "target"),
                        false,
                        ToolOperationsReference.build(FilePolicy.allPermissive(), false, true, true, false)))
                .referenceTree(refs.toString())
                .build();
    }

    private double nanosToMillis(double nanos) {
        return nanos / 1_000_000.0d;
    }

    private double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }
}
