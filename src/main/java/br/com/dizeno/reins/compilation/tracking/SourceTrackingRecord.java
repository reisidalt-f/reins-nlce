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

package br.com.dizeno.reins.compilation.tracking;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SourceTrackingRecord is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a data carrier representation of its prefix information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceTrackingRecord {
    private String sourcePath;
    private String sourceCategory;
    private String sourceHash;
    private Long sourceModificationTime;
    private List<String> blockFingerprints = new ArrayList<>();
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Map<String, FileTrackingDetails> markdownReferences = new LinkedHashMap<>();
    @JsonAlias("contextFingerprint")
    private String inferenceFingerprint;
    private String model;
    private String outputPolicy;
    private String lastCompiledAt;
    private String lastStatus;
    private String resolvedTargetRoot;

    private Map<String, FileTrackingDetails> compiledFiles = new LinkedHashMap<>();
    private Map<String, FileTrackingDetails> inspectedFiles = new LinkedHashMap<>();

    /**
     * Gets the source path.
     *
     * @return the string result
     */
    public String getSourcePath() {
        return sourcePath;
    }

    /**
     * Sets the source path.
     *
     * @param sourcePath the path of the source file
     */
    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    /**
     * Gets the resolved target root.
     *
     * @return the string result
     */
    public String getResolvedTargetRoot() {
        return resolvedTargetRoot;
    }

    /**
     * Sets the resolved target root.
     *
     * @param resolvedTargetRoot the resolved target root
     */
    public void setResolvedTargetRoot(String resolvedTargetRoot) {
        this.resolvedTargetRoot = resolvedTargetRoot;
    }

    /**
     * Gets the source category.
     *
     * @return the string result
     */
    public String getSourceCategory() {
        return sourceCategory;
    }

    /**
     * Sets the source category.
     *
     * @param sourceCategory the category of the source file (e.g. main or test)
     */
    public void setSourceCategory(String sourceCategory) {
        this.sourceCategory = sourceCategory;
    }

    /**
     * Gets the source hash.
     *
     * @return the string result
     */
    public String getSourceHash() {
        return sourceHash;
    }

    /**
     * Sets the source hash.
     *
     * @param sourceHash the source hash
     */
    public void setSourceHash(String sourceHash) {
        this.sourceHash = sourceHash;
    }

    /**
     * Gets the source modification time.
     *
     * @return the numeric value
     */
    public Long getSourceModificationTime() {
        return sourceModificationTime;
    }

    /**
     * Sets the source modification time.
     *
     * @param sourceModificationTime the source modification time
     */
    public void setSourceModificationTime(Long sourceModificationTime) {
        this.sourceModificationTime = sourceModificationTime;
    }

    /**
     * Gets the block fingerprints.
     *
     * @return the string result
     */
    public List<String> getBlockFingerprints() {
        return blockFingerprints;
    }

    /**
     * Sets the block fingerprints.
     *
     * @param blockFingerprints the block fingerprints
     */
    public void setBlockFingerprints(List<String> blockFingerprints) {
        this.blockFingerprints = blockFingerprints == null ? new ArrayList<>() : new ArrayList<>(blockFingerprints);
    }

    /**
     * Gets the markdown references.
     *
     * @return the string result
     */
    public Map<String, FileTrackingDetails> getMarkdownReferences() {
        return markdownReferences;
    }

    /**
     * Sets the markdown references.
     *
     * @param markdownReferences the markdown references
     */
    @JsonSetter("markdownReferences")
    public void setMarkdownReferences(Object markdownReferences) {
        Map<String, FileTrackingDetails> converted = new LinkedHashMap<>();
        if (markdownReferences instanceof Map<?, ?> mapReferences) {
            for (Map.Entry<?, ?> entry : mapReferences.entrySet()) {
                String path = String.valueOf(entry.getKey());
                Object rawDetails = entry.getValue();
                if (rawDetails instanceof FileTrackingDetails details) {
                    converted.put(path, details);
                } else if (rawDetails instanceof Map<?, ?> rawMap) {
                    String tracking = rawMap.get("tracking") == null ? path : String.valueOf(rawMap.get("tracking"));
                    String category = rawMap.get("category") == null ? null : String.valueOf(rawMap.get("category"));
                    Long modificationTime = null;
                    if (rawMap.get("modificationTime") instanceof Number number) {
                        modificationTime = number.longValue();
                    }
                    converted.put(path, new FileTrackingDetails(tracking, category, modificationTime));
                } else {
                    converted.put(path, new FileTrackingDetails(path, sourceCategory, null));
                }
            }
        } else if (markdownReferences instanceof List<?> pathList) {
            for (Object item : pathList) {
                String path = String.valueOf(item);
                converted.put(path, new FileTrackingDetails(path, sourceCategory, null));
            }
        }
        this.markdownReferences = converted;
    }

    /**
     * Gets the compiled files.
     *
     * @return the string result
     */
    public Map<String, FileTrackingDetails> getCompiledFiles() {
        return compiledFiles;
    }

    /**
     * Sets the compiled files.
     *
     * @param compiledFiles the compiled files
     */
    public void setCompiledFiles(Map<String, FileTrackingDetails> compiledFiles) {
        this.compiledFiles = compiledFiles == null ? new LinkedHashMap<>() : new LinkedHashMap<>(compiledFiles);
    }

    /**
     * Gets the inspected files.
     *
     * @return the string result
     */
    public Map<String, FileTrackingDetails> getInspectedFiles() {
        return inspectedFiles;
    }

    /**
     * Sets the inspected files.
     *
     * @param inspectedFiles the inspected files
     */
    public void setInspectedFiles(Map<String, FileTrackingDetails> inspectedFiles) {
        this.inspectedFiles = inspectedFiles == null ? new LinkedHashMap<>() : new LinkedHashMap<>(inspectedFiles);
    }

    /**
     * Gets the inference fingerprint.
     *
     * @return the string result
     */
    public String getInferenceFingerprint() {
        return inferenceFingerprint;
    }

    /**
     * Sets the inference fingerprint.
     *
     * @param inferenceFingerprint the inference fingerprint
     */
    public void setInferenceFingerprint(String inferenceFingerprint) {
        this.inferenceFingerprint = inferenceFingerprint;
    }

    /**
     * Gets the model.
     *
     * @return the string result
     */
    public String getModel() {
        return model;
    }

    /**
     * Sets the model.
     *
     * @param model the model name string
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Gets the output policy.
     *
     * @return the string result
     */
    public String getOutputPolicy() {
        return outputPolicy;
    }

    /**
     * Sets the output policy.
     *
     * @param outputPolicy the output policy
     */
    public void setOutputPolicy(String outputPolicy) {
        this.outputPolicy = outputPolicy;
    }

    /**
     * Gets the last compiled at.
     *
     * @return the string result
     */
    public String getLastCompiledAt() {
        return lastCompiledAt;
    }

    /**
     * Sets the last compiled at.
     *
     * @param lastCompiledAt the last compiled at
     */
    public void setLastCompiledAt(String lastCompiledAt) {
        this.lastCompiledAt = lastCompiledAt;
    }

    /**
     * Gets the last status.
     *
     * @return the string result
     */
    public String getLastStatus() {
        return lastStatus;
    }

    /**
     * Sets the last status.
     *
     * @param lastStatus the last status
     */
    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    @JsonProperty("notes")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<ReasoningNote> notes = new ArrayList<>();

    /**
     * Gets the notes.
     *
     * @return the collection of elements
     */
    public List<ReasoningNote> getNotes() {
        return notes;
    }

    /**
     * Sets the notes.
     *
     * @param notes the notes
     */
    public void setNotes(List<ReasoningNote> notes) {
        this.notes = notes == null ? new ArrayList<>() : new ArrayList<>(notes);
    }
}
