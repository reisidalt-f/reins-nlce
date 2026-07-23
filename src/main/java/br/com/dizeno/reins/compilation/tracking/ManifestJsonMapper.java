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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

 
/**
 * ManifestJsonMapper is part of the persistent tracking of file status, change detection, file cleanup, and recompilation deciders in the reins architecture.
 * Acts as a helper utility for mapping its prefix data formats.
 */
public class ManifestJsonMapper {

    private static final ObjectMapper MAPPER = createObjectMapper();

     
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

     
    /**
     * Gets the instance.
     *
     * @return the map of key-value associations
     */
    public static ObjectMapper getInstance() {
        return MAPPER;
    }

     
    /**
     * To Json.
     *
     * @param obj the obj
     * @return the string result
     */
    public static String toJson(Object obj) throws Exception {
        return MAPPER.writeValueAsString(obj);
    }

     
    /**
     * From Json.
     *
     * @param json the json
     * @param clazz the clazz
     * @return the resolved or constructed object
     */
    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return MAPPER.readValue(json, clazz);
    }
}
