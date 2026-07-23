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

import freemarker.template.Template;

 
/**
 * ScriptDescriptor is part of the dynamic script evaluation using Freemarker templates and context injection in the reins architecture.
 * Acts as a component managing script descriptor.
 */
public class ScriptDescriptor {

    private final String name;
    private final PhaseType phaseType;
    private final ScriptSource resolvedFrom;
    private final String resolvedLocation;
    private final Template template;

    /**
     * Constructs a new instance of {@link ScriptDescriptor}.
     *
     * @param name the name
     * @param phaseType the phase type
     * @param resolvedFrom the resolved from
     * @param resolvedLocation the resolved location
     * @param template the template
     */
    public ScriptDescriptor(String name, PhaseType phaseType, ScriptSource resolvedFrom,
                            String resolvedLocation, Template template) {
        this.name = name;
        this.phaseType = phaseType;
        this.resolvedFrom = resolvedFrom;
        this.resolvedLocation = resolvedLocation;
        this.template = template;
    }

     
    /**
     * Gets the name.
     *
     * @return the string result
     */
    public String getName() {
        return name;
    }

     
    /**
     * Gets the phase type.
     *
     * @return the collection of elements
     */
    public PhaseType getPhaseType() {
        return phaseType;
    }

     
    /**
     * Gets the resolved from.
     *
     * @return the resolved or constructed object
     */
    public ScriptSource getResolvedFrom() {
        return resolvedFrom;
    }

     
    /**
     * Gets the resolved location.
     *
     * @return the string result
     */
    public String getResolvedLocation() {
        return resolvedLocation;
    }

     
    /**
     * Gets the template.
     *
     * @return the resolved or constructed object
     */
    public Template getTemplate() {
        return template;
    }
}
