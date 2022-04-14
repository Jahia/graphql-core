/*
 * Copyright (C) 2002-2022 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.graphql.osgi.stub.inject;

import org.jahia.modules.graphql.osgi.stub.service.StubService;
import org.jahia.modules.graphql.osgi.stub.service.StubServiceSubClass;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;

import javax.inject.Inject;

public class InjectorFieldTypedTest implements InjectorTest {

    @Inject
    @GraphQLOsgiService(service = StubServiceSubClass.class)
    private StubService stubService;

    public StubService getStubService() {
        return stubService;
    }
}
