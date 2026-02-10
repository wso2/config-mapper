/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.config.mapper.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Model object for parse values within phases.
 */
public class Context {


    private final Map<String, Object> templateData;
    private final Map<String, String> resolvedSystemProperties;
    private final Map<String, String> resolvedEnvironmentVariables;
    private final Map<String, String> secrets;

    public Context() {

        this.templateData = new HashMap<>();
        this.resolvedEnvironmentVariables = new HashMap<>();
        this.resolvedSystemProperties = new HashMap<>();
        this.secrets = new HashMap<>();
    }



    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EI_EXPOSE_REP")
    public Map<String, Object> getTemplateData() {

        return templateData;
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EI_EXPOSE_REP")
    public Map<String, String> getResolvedSystemProperties() {

        return resolvedSystemProperties;
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EI_EXPOSE_REP")
    public Map<String, String> getResolvedEnvironmentVariables() {

        return resolvedEnvironmentVariables;
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings("EI_EXPOSE_REP")
    public Map<String, String> getSecrets() {

        return secrets;
    }
}
