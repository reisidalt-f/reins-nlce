package br.com.dizeno.reins.compilation.pipeline;

import br.com.dizeno.reins.compilation.CompilationOutput;
import br.com.dizeno.reins.compilation.CompilationSummary;
import br.com.dizeno.reins.compilation.CycleWorkSetEntry;
import br.com.dizeno.reins.compilation.ResultPrinter;
import br.com.dizeno.reins.compilation.SourceProcessingStatus;
import br.com.dizeno.reins.compilation.tracking.CompilationTrackingStore;
import br.com.dizeno.reins.compilation.tracking.SourceFingerprintService;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingManager;
import br.com.dizeno.reins.compilation.tracking.SourceTrackingRecord;
import br.com.dizeno.reins.reasoning.ReasoningResult;
import br.com.dizeno.reins.run.config.ReinsConfig;
import br.com.dizeno.reins.run.config.settings.TargetSettings;
import br.com.dizeno.reins.source.graph.MarkdownDependencyGraph;
import br.com.dizeno.reins.source.graph.MarkdownSourceNode;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class OutcomeBranchNoChangeTest {

    @TempDir
    Path projectRoot;

    @Test
    void testNoopOutcome_ProducesNoChangeStatus() throws Exception {
        Path sourceFile = projectRoot.resolve("src/main/nl/Test.md");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "# Test source\n");

        SourceCompilationContext ctx = createContext("main:Test.md", sourceFile, false);

        ReasoningResult result = new ReasoningResult();
        result.setFinalIntent("finish_success");
        result.setWrittenPaths(List.of());
        ctx.setReasoningResult(result);

        OutcomeBranchPhase phase = new OutcomeBranchPhase();
        phase.execute(ctx, new PhaseChain(List.of()));

        assertEquals("no-change", ctx.getOutput().getStatus());
        assertEquals("no-change", ctx.getOutput().getMessage());
        assertEquals(1, ctx.getSummary().getProcessed());
        assertEquals(1, ctx.getSummary().getNoChange());
        assertEquals(0, ctx.getSummary().getCompiled());
        assertEquals(0, ctx.getSummary().getSkipped());
    }

    @Test
    void testNoopOutcome_PriorFailedRecord_UpdatesStatusToNoChange() throws Exception {
        Path sourceFile = projectRoot.resolve("src/main/nl/Test.md");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "# Test source\n");

        SourceCompilationContext ctx = createContext("main:Test.md", sourceFile, false);

        SourceTrackingRecord prior = new SourceTrackingRecord();
        prior.setSourcePath("main:Test.md");
        prior.setSourceCategory("main");
        prior.setSourceHash("oldhash");
        prior.setLastStatus("failed");
        ctx.getTrackingStore().save(projectRoot, "main:Test.md", prior);
        ctx.setPriorRecord(prior);

        ReasoningResult result = new ReasoningResult();
        result.setFinalIntent("finish_success");
        result.setWrittenPaths(List.of());
        ctx.setReasoningResult(result);

        OutcomeBranchPhase phase = new OutcomeBranchPhase();
        phase.execute(ctx, new PhaseChain(List.of()));

        assertEquals("no-change", ctx.getOutput().getStatus());
        assertEquals(1, ctx.getSummary().getNoChange());

        SourceTrackingRecord record = ctx.getTrackingStore().load(projectRoot, "main:Test.md").orElse(null);
        assertNotNull(record);
        assertEquals("no-change", record.getLastStatus());
    }

    @Test
    void testSuccessOutcome_NoOutputsChanged_ProducesNoChangeStatus() throws Exception {
        Path sourceFile = projectRoot.resolve("src/main/nl/Test.md");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "# Test source\n");

        Path outputFile = projectRoot.resolve("src/main/java/com/example/Compiled.java");
        Files.createDirectories(outputFile.getParent());
        Files.writeString(outputFile, "class Compiled {}\n");

        long pastMtime = System.currentTimeMillis() - 50000;
        Files.setLastModifiedTime(outputFile, FileTime.fromMillis(pastMtime));

        SourceCompilationContext ctx = createContext("main:Test.md", sourceFile, false);

        // Pre-populate tracking record with exact prior mtime
        SourceTrackingRecord prior = new SourceTrackingRecord();
        prior.setSourcePath("main:Test.md");
        prior.setSourceCategory("main");
        prior.setSourceHash("hash");
        prior.setResolvedTargetRoot("src/main/java");
        prior.setCompiledFiles(Map.of("target:com/example/Compiled.java",
                new br.com.dizeno.reins.compilation.tracking.FileTrackingDetails("main:Test.md", "main", pastMtime)));
        prior.setLastStatus("success");
        ctx.getTrackingStore().save(projectRoot, "main:Test.md", prior);

        ReasoningResult result = new ReasoningResult();
        result.setFinalIntent("finish_success");
        result.setWrittenPaths(List.of(outputFile.toString()));
        ctx.setReasoningResult(result);

        OutcomeBranchPhase phase = new OutcomeBranchPhase();
        phase.execute(ctx, new PhaseChain(List.of()));

        assertEquals("no-change", ctx.getOutput().getStatus());
        assertEquals("no-change", ctx.getOutput().getMessage());
        assertEquals(1, ctx.getSummary().getProcessed());
        assertEquals(1, ctx.getSummary().getNoChange());
        assertEquals(0, ctx.getSummary().getCompiled());

        SourceTrackingRecord record = ctx.getTrackingStore().load(projectRoot, "main:Test.md").orElse(null);
        assertNotNull(record);
        assertEquals("no-change", record.getLastStatus());
    }

    private SourceCompilationContext createContext(String canonicalPath, Path sourceFile, boolean validateOnly) {
        SourceCompilationContext ctx = new SourceCompilationContext();
        ctx.setCanonicalSourcePath(canonicalPath);
        ctx.setRelativeSourcePath("src/main/nl/Test.md");
        ctx.setSourceCategory("main");
        ctx.setResolvedTargetRoot("src/main/java");
        ctx.setProjectRoot(projectRoot);
        ctx.setSourceHash("hash123");
        ctx.setValidateOnly(validateOnly);
        ctx.setNode(new MarkdownSourceNode("src/main/nl/Test.md", sourceFile, System.currentTimeMillis(), List.of()));

        ReinsConfig config = new ReinsConfig();
        TargetSettings target = new TargetSettings();
        target.setTargetBase("main", "src/main/java");
        config.setTarget(target);
        ctx.setConfig(config);

        ctx.setLog(mock(Log.class));
        ctx.setSummary(new CompilationSummary());
        ctx.setWorkSetEntry(new CycleWorkSetEntry(sourceFile.toFile(), "src/main/nl/Test.md",
                validateOnly ? SourceProcessingStatus.VALIDATE : SourceProcessingStatus.COMPILE));

        CompilationOutput output = new CompilationOutput();
        ctx.setOutput(output);

        CompilationTrackingStore store = new CompilationTrackingStore();
        ctx.setTrackingStore(store);
        ctx.setSourceTrackingManager(new SourceTrackingManager());
        ctx.setFingerprintService(new SourceFingerprintService());
        ctx.setResultPrinter(mock(ResultPrinter.class));
        ctx.setGraph(new MarkdownDependencyGraph(
                Map.of("src/main/nl/Test.md", ctx.getNode()),
                Map.of("src/main/nl/Test.md", List.of()),
                Map.of("src/main/nl/Test.md", List.of()),
                List.of("src/main/nl/Test.md")
        ));
        ctx.setWorkSetByPath(Map.of());
        ctx.setOrderIndex(Map.of());
        return ctx;
    }
}
