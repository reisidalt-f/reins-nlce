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

package br.com.dizeno.reins.reasoning;

/**
 * ReasoningPhaseDescriptor is part of the API interactions with LLM endpoints, configuring connections, and logging payloads in the reins architecture.
 * Acts as a component managing inference phase descriptor.
 */
public class ReasoningPhaseDescriptor {
    /**
     * State is part of the API interactions with LLM endpoints, configuring connections, and logging payloads in the reins architecture.
     * Acts as a component managing state.
     */
    public enum State {
        PENDING,
        ACTIVE,
        COMPLETED,
        FAILED
    }

    private final String name;
    private final int ordinal;
    private final State state;

    /**
     * Constructs a new instance of {@link ReasoningPhaseDescriptor}.
     *
     * @param name the name
     * @param ordinal the ordinal
     * @param state the state
     */
    public ReasoningPhaseDescriptor(String name, int ordinal, State state) {
        this.name = name;
        this.ordinal = ordinal;
        this.state = state == null ? State.PENDING : state;
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
     * Gets the ordinal.
     *
     * @return the numeric value
     */
    public int getOrdinal() {
        return ordinal;
    }

    /**
     * Gets the state.
     *
     * @return the resulting state
     */
    public State getState() {
        return state;
    }

    /**
     * With State.
     *
     * @param nextState the next state
     * @return the resolved or constructed object
     */
    public ReasoningPhaseDescriptor withState(State nextState) {
        return new ReasoningPhaseDescriptor(name, ordinal, nextState);
    }
}