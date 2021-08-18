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
 *     Copyright (C) 2002-2021 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm;

import java.util.List;

/**
 * Exception for propagating multiple DataFetchingException errors
 */
public class AggregateDataFetchingException extends DataFetchingException {

    private List<DataFetchingException> errors;

    public AggregateDataFetchingException(List<DataFetchingException> errors) {
        super(AggregateDataFetchingException.class.getName());
        this.errors = errors;
    }

    public List<DataFetchingException> getErrors() {
        return errors;
    }
}
