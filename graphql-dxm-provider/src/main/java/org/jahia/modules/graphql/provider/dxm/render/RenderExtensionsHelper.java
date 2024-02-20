/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2024 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.graphql.provider.dxm.render;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A Simple helper for render extensions
 *
 * @author Jerome Blanchard
 */
public class RenderExtensionsHelper {

    private static final List<Pattern> CLEAN_PATTERNS = new ArrayList<>();
    static  {
        CLEAN_PATTERNS.add(Pattern.compile("<!-- jahia:temp [^>]*-->"));
    }

    public static String clean(String input)  {
        String output = input;
        if (StringUtils.isNotEmpty(output)) {
            for (Pattern pattern: CLEAN_PATTERNS) {
                output = pattern.matcher(output).replaceAll("");
            }
        }
        return output;
    }

}
