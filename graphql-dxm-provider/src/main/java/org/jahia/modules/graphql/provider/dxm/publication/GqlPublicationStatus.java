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

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.services.content.PublicationInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Possible publication statuses of the JCR node.
 */
@GraphQLName("PublicationStatus")
@GraphQLDescription("Possible publication statuses of the JCR node")
public enum GqlPublicationStatus {

    @GraphQLDescription("Content is published and up to date")
    PUBLISHED(PublicationInfo.PUBLISHED),
    @GraphQLDescription("Content has been modified since last publication")
    MODIFIED(PublicationInfo.MODIFIED),
    @GraphQLDescription("Content has never been published")
    NOT_PUBLISHED(PublicationInfo.NOT_PUBLISHED),
    @GraphQLDescription("Content was unpublished")
    UNPUBLISHED(PublicationInfo.UNPUBLISHED),
    @GraphQLDescription("Mandatory language is unpublishable")
    MANDATORY_LANGUAGE_UNPUBLISHABLE(PublicationInfo.MANDATORY_LANGUAGE_UNPUBLISHABLE),
    @GraphQLDescription("Content has been modified in live workspace")
    LIVE_MODIFIED(PublicationInfo.LIVE_MODIFIED),
    @GraphQLDescription("Content exists only in live workspace")
    LIVE_ONLY(PublicationInfo.LIVE_ONLY),
    @GraphQLDescription("Content has publication conflicts")
    CONFLICT(PublicationInfo.CONFLICT),
    @GraphQLDescription("Mandatory language is valid")
    MANDATORY_LANGUAGE_VALID(PublicationInfo.MANDATORY_LANGUAGE_VALID),
    @GraphQLDescription("Content has been deleted")
    DELETED(PublicationInfo.DELETED),
    @GraphQLDescription("Content is marked for deletion")
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
