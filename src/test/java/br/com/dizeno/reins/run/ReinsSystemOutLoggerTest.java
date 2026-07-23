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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ReinsSystemOutLoggerTest {

    @Test
    public void testLoggerOutput() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        try {
            System.setOut(new PrintStream(outStream));
            System.setErr(new PrintStream(errStream));
            
            ReinsSystemOutLogger logger = new ReinsSystemOutLogger(true, true);
            
            assertTrue(logger.isDebugEnabled());
            logger.debug("Debug msg");
            logger.info("Info msg");
            logger.warn("Warn msg");
            logger.error("Error msg");
            
            String outVal = outStream.toString();
            String errVal = errStream.toString();
            
            assertTrue(outVal.contains("[DEBUG] Debug msg"), "Expected debug prefix: " + outVal);
            assertTrue(outVal.contains("[INFO] Info msg"), "Expected info prefix: " + outVal);
            assertTrue(errVal.contains("[WARN] Warn msg"), "Expected warn prefix: " + errVal);
            assertTrue(errVal.contains("[ERROR] Error msg"), "Expected error prefix: " + errVal);
            
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
}
