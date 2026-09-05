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

package br.com.dizeno.reins.run.config;

import br.com.dizeno.reins.run.config.settings.ContextSettings;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinsConfigPlainAttachmentExtensionsTest {

    @Test
    void wellKnownExtensions_areRecognizedByDefault() {
        ContextSettings settings = new ContextSettings();

        assertTrue(settings.isPlainAttachment("readme.md"));
        assertTrue(settings.isPlainAttachment("Main.java"));
        assertTrue(settings.isPlainAttachment("script.py"));
        assertTrue(settings.isPlainAttachment("config.json"));
        assertTrue(settings.isPlainAttachment("data.yaml"));
        assertTrue(settings.isPlainAttachment("pom.xml"));
        assertTrue(settings.isPlainAttachment("styles.css"));
        assertTrue(settings.isPlainAttachment("script.sh"));
        assertTrue(settings.isPlainAttachment("lib.rs"));
        assertTrue(settings.isPlainAttachment("Cargo.toml"));
    }

    @Test
    void customExtensions_canBeConfiguredAndRecognized() {
        ContextSettings settings = new ContextSettings();
        settings.setPlainAttachmentExtensions(List.of("gcode", "customext", ".xyz"));

        assertTrue(settings.isPlainAttachment("model.gcode"));
        assertTrue(settings.isPlainAttachment("data.customext"));
        assertTrue(settings.isPlainAttachment("file.xyz"));

        // Well known should still work
        assertTrue(settings.isPlainAttachment("App.java"));
    }

    @Test
    void binaryExtensions_areNotPlainAttachments() {
        ContextSettings settings = new ContextSettings();

        assertFalse(settings.isPlainAttachment("image.png"));
        assertFalse(settings.isPlainAttachment("photo.jpg"));
        assertFalse(settings.isPlainAttachment("document.pdf"));
        assertFalse(settings.isPlainAttachment("archive.zip"));
        assertFalse(settings.isPlainAttachment("binary.exe"));
    }

    @Test
    void configLoader_bindsPlainAttachmentExtensionsFromProperties() {
        Properties props = new Properties();
        props.setProperty("reins.context.plainAttachmentExtensions", "gcode,customext,xyz");

        ReinsConfig config = ReinsConfigLoader.load(null, props, null, new File("."));

        assertTrue(config.getContext().isPlainAttachment("model.gcode"));
        assertTrue(config.getContext().isPlainAttachment("data.customext"));
        assertTrue(config.getContext().isPlainAttachment("file.xyz"));
        assertTrue(config.getContext().isPlainAttachment("Main.java"));
    }
}
