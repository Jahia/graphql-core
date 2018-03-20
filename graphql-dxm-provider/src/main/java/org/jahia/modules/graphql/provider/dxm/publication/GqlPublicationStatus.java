/*
 *  ==========================================================================================
 *  =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 *  ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 *      Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *      THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *      1/GPL OR 2/JSEL
 *
 *      1/ GPL
 *      ==================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      This program is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *      2/ JSEL - Commercial and Supported Versions of the program
 *      ===================================================================================
 *
 *      IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *      Alternatively, commercial and supported versions of the program - also known as
 *      Enterprise Distributions - must be used in accordance with the terms and conditions
 *      contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *      If you are unsure which license is appropriate for your use,
 *      please contact the sales department at sales@jahia.com.
 *
 */

package org.jahia.modules.graphql.provider.dxm.publication;

import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.services.content.PublicationInfo;

import java.util.HashMap;
import java.util.Map;

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
            PUBLICATION_STATUS_BY_STATUS_VALUE.put(publicationStatus.getStatusValue(), publicationStatus);
        }
    }

    private int statusValue;

    private GqlPublicationStatus(int statusValue) {
        this.statusValue = statusValue;
    }

    public static GqlPublicationStatus fromStatusValue(int statusValue) {
        if (PUBLICATION_STATUS_BY_STATUS_VALUE.containsKey(statusValue)) {
            return PUBLICATION_STATUS_BY_STATUS_VALUE.get(statusValue);
        }
        throw new IllegalArgumentException("Unknown publication status value: " + statusValue);
    }

    public int getStatusValue() {
        return statusValue;
    }
}
