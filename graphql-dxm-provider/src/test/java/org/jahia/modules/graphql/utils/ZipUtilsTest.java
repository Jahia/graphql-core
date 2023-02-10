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
package org.jahia.modules.graphql.utils;

import org.jahia.modules.graphql.provider.dxm.util.ZipUtils;
import org.junit.Test;
import org.testng.Assert;

import java.io.IOException;

public class ZipUtilsTest {

    @Test
    public void testMimeType() throws IOException {
        Assert.assertEquals(ZipUtils.getMimeType("test.css", null), "text/css");
        Assert.assertEquals(ZipUtils.getMimeType("test", getClass().getResourceAsStream("/mime-samples/text")), "text/plain");
        Assert.assertEquals(ZipUtils.getMimeType("test", getClass().getResourceAsStream("/mime-samples/text-csv")), "text/plain");
        Assert.assertEquals(ZipUtils.getMimeType("excel.xlsx", null), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        Assert.assertEquals(ZipUtils.getMimeType("excel-xlsx", getClass().getResourceAsStream("/mime-samples/excel.xlsx")), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        Assert.assertEquals(ZipUtils.getMimeType("excel-xlsx", getClass().getResourceAsStream("/mime-samples/excel")), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }
}
