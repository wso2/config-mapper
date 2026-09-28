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

package org.wso2.config.mapper;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ValueInferrerTest {

    private static final String INFER_JSON = "infer.json";
    private static final String CYCLIC_INFER_JSON = "infer-cyclic-extends.json";

    @Test(dataProvider = "contextProvider")
    public void testParse(Map<String, Object> context, String key, Object expectedValue) throws ConfigParserException {

        String inferConfiguration =
                FileUtils.getFile("src", "test", "resources", INFER_JSON).getAbsolutePath();

        Map<String, Object> inferredValues = ValueInferrer.infer(context, inferConfiguration);
        Object actualValue = inferredValues.get(key);
        Assert.assertEquals(actualValue, expectedValue, "Incorrect inferred value for " + key);
    }

    @DataProvider(name = "contextProvider")
    public Object[][] inferringDataSet() {

        Map<String, Object> jdbcContext = new HashMap<>();
        Map<String, Object> readOnlyLdapContext = new HashMap<>();
        Map<String, Object> invalidContext = new HashMap<>();
        jdbcContext.put("user_store.type", "jdbc");
        readOnlyLdapContext.put("user_store.type", "read_only_ldap");
        invalidContext.put("user_store.type", "invalid_value");
        Map<String, Object> is710Context = new HashMap<>();
        Map<String, Object> is720Context = new HashMap<>();
        Map<String, Object> is730Context = new HashMap<>();
        is710Context.put("preserve_previous_product_behaviour.version", "IS_7.1.0");
        is720Context.put("preserve_previous_product_behaviour.version", "IS_7.2.0");
        is730Context.put("preserve_previous_product_behaviour.version", "IS_7.3.0");
        Map<String, Object> variableContext = new HashMap<>();
        variableContext.put("datasource.apim.type", "mysql");
        variableContext.put("datasource.abc.type", "mysql");
        variableContext.put("datasource.abc.name", "hellodb");
        variableContext.put("datasource.cde.type", "oracle");
        variableContext.put("datasource.carbon.type", "mysql");
        return new Object[][]{
                {jdbcContext, "user_store.class", "org.wso2.carbon.user.core.jdbc.JDBCUserStoreManager"},
                {jdbcContext, "user_store.properties.ReadOnly", false},
                {jdbcContext, "user_store.properties.dummyArray", Arrays.asList("foo", "bar")},
                {readOnlyLdapContext, "user_store.class", "org.wso2.carbon.user.core.ldap" +
                        ".ReadOnlyLDAPUserStoreManager"},
                {readOnlyLdapContext, "user_store.properties.LDAPConnectionTimeout", 5000},
                {invalidContext, "user_store.class", null},
                {variableContext, "datasource.apim.driver", "com.mysql.jdbc.Driver"},
                {variableContext, "datasource.abc.driver", "com.mysql.jdbc.Driver"},
                {variableContext, "datasource.abc.url", "jdbc:mysql://localhost:3306/$ref{datasource.abc.name}"},
                {variableContext, "datasource.cde.driver", null},
                {variableContext, "datasource.carbon.driver", "com.oracle.Driver"},
                {jdbcContext, "tenant_mgt.tenant_manager.config_builder", "org.wso2.carbon.user.core.config" +
                        ".multitenancy.SimpleRealmConfigBuilder"},
                // A value declared only in the newest block reaches every block that extends it, transitively.
                {is730Context, "saml.validate_assertion_consumer_url_for_signed_requests", false},
                {is720Context, "saml.validate_assertion_consumer_url_for_signed_requests", false},
                {is710Context, "saml.validate_assertion_consumer_url_for_signed_requests", false},
                // An explicit null declines an inherited entry without giving up the rest of the chain.
                {is710Context, "ai_services.http_client_use_system_properties", null},
                // What a block declares itself overrides what it inherits.
                {is710Context, "webappscommon.inherit_app_level_custom_layout", true},
                {is720Context, "webappscommon.inherit_app_level_custom_layout", false},
                // Inheritance only flows from the extended block, never back into it.
                {is730Context, "ai_services.http_client_use_system_properties", null},
                {is720Context, "oauth.return_sp_id_to_apps", null}
        };
    }

    @Test(expectedExceptions = ConfigParserException.class,
            expectedExceptionsMessageRegExp = "Cyclic .*IS_7\\.3\\.0 -> IS_7\\.2\\.0 -> IS_7\\.3\\.0")
    public void testCyclicExtends() throws ConfigParserException {

        String inferConfiguration =
                FileUtils.getFile("src", "test", "resources", CYCLIC_INFER_JSON).getAbsolutePath();
        Map<String, Object> context = new HashMap<>();
        context.put("preserve_previous_product_behaviour.version", "IS_7.3.0");
        ValueInferrer.infer(context, inferConfiguration);
    }
}
