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
package org.jahia.modules.graphql.provider.dxm.publication;

import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.services.content.PublicationInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Possible publication statuses of the JCR node.
 */
@GraphQLName("PublicationStatus")
public enum GqlPublicationStatus {

    PUBLISHED(PublicationInfo.PUBLISHED),
    MODIFIED(PublicationInfo.MODIFIED),
    NOT_PUBLISHED(PublicationInfo.NOT_PUBLISHED),
    UNPUBLISHED(PublicationInfo.UNPUBLISHED),
    MANDATORY_LANGUAGE_UNPUBLISHABLE(PublicationInfo.MANDATORY_LANGUAGE_UNPUBLISHABLE),
    LIVE_MODIFIED(PublicationInfo.LIVE_MODIFIED),
    LIVE_ONLY(PublicationInfo.LIVE_ONLY),
    CONFLICT(PublicationInfo.CONFLICT),
    MANDATORY_LANGUAGE_VALID(PublicationInfo.MANDATORY_LANGUAGE_VALID),
    DELETED(PublicationInfo.DELETED),
    MARKED_FOR_DELETION(PublicationInfo.MARKED_FOR_DELETION);

    private static final Map<Integer, GqlPublicationStatus> PUBLICATION_STATUS_BY_STATUS_VALUE = new HashMap<Integer,GqlPublicationStatus>();
    static {
        for (GqlPublicationStatus publicationStatus : GqlPublicationStatus.values()) {
            PUBLICATION_STATUS_BY_STATUS_VALUE.put(publicationStatus.statusValue, publicationStatus);
        }
    }

    private int statusValue;

    private GqlPublicationStatus(int statusValue) {
        this.statusValue = statusValue;
    }

    /**
     * Convert an integer publication status value (see PublicationInfo status constants) into corresponding enum value
     *
     * @param statusValue Integer publication status value
     * @return Enum publication status value corresponding to the integer one passed
     * @throws IllegalArgumentException In case wrong integer status value is passed
     */
    public static GqlPublicationStatus fromStatusValue(int statusValue) throws IllegalArgumentException {
        if (PUBLICATION_STATUS_BY_STATUS_VALUE.containsKey(statusValue)) {
            return PUBLICATION_STATUS_BY_STATUS_VALUE.get(statusValue);
        }
        throw new IllegalArgumentException("Unknown publication status value: " + statusValue);
    }
}
