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

package br.com.dizeno.reins.run.cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ReinsCliTest {

    @Test
    public void testEmptyArgumentsReturnsFailure() {
        int code = ReinsCli.runCli(new String[]{});
        assertEquals(1, code, "Empty arguments should print usage and return exit code 1");
    }

    @Test
    public void testInvalidSubcommandReturnsFailure() {
        int code = ReinsCli.runCli(new String[]{"invalid-cmd"});
        assertEquals(1, code, "Invalid subcommands should return exit code 1");
    }

    @Test
    public void testMissingMandatoryParamsReturnsFailure() {
        
        int code = ReinsCli.runCli(new String[]{"compile", "--provider", "gemini"});
        assertEquals(1, code, "Missing API key in gemini provider should fail validation with exit code 1");
    }
}
