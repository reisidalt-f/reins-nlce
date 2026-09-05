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

package br.com.dizeno.reins.run;

import br.com.dizeno.reins.run.config.ReinsConfig;
import br.com.dizeno.reins.run.config.ReinsConfigLoader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class SourceTagLoggerTest {

    @Test
    public void testSourceTagLoggerFormatting() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        try {
            System.setOut(new PrintStream(outStream));
            System.setErr(new PrintStream(errStream));

            ReinsSystemOutLogger baseLogger = new ReinsSystemOutLogger(true, true);
            SourceTagLogger tagLogger = new SourceTagLogger(baseLogger, "Customer.md");

            assertEquals("[Customer.md] ", tagLogger.getPrefix());
            assertEquals(baseLogger, tagLogger.getDelegate());
            assertTrue(tagLogger.isDebugEnabled());
            assertTrue(tagLogger.isInfoEnabled());
            assertTrue(tagLogger.isWarnEnabled());
            assertTrue(tagLogger.isErrorEnabled());

            tagLogger.debug("Debug message");
            tagLogger.info("Info message");
            tagLogger.warn("Warn message");
            tagLogger.error("Error message");

            String outVal = outStream.toString();
            String errVal = errStream.toString();

            assertTrue(outVal.contains("[DEBUG] [Customer.md] Debug message"), "Actual out: " + outVal);
            assertTrue(outVal.contains("[INFO] [Customer.md] Info message"), "Actual out: " + outVal);
            assertTrue(errVal.contains("[WARN] [Customer.md] Warn message"), "Actual err: " + errVal);
            assertTrue(errVal.contains("[ERROR] [Customer.md] Error message"), "Actual err: " + errVal);

        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Test
    public void testConfigLoaderSourceTagBinding() {
        Properties sysProps = new Properties();
        sysProps.setProperty("reins.logging.sourceTag", "true");

        ReinsConfig config = ReinsConfigLoader.load(new String[0], sysProps, null, new File("."));
        assertNotNull(config.getLogging());
        assertTrue(config.getLogging().isSourceTag());
    }
}
