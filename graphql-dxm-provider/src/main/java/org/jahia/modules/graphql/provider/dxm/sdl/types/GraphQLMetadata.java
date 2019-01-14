package org.jahia.modules.graphql.provider.dxm.sdl.types;

/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

/**
 * Created at 14 Jan$
 *
 * @author chooliyip
 **/
public class GraphQLMetadata extends GraphQLScalarType {

    private static final String DEFAULT_NAME = "Metadata";

    public GraphQLMetadata() {
        this(DEFAULT_NAME);
    }

    public GraphQLMetadata(final String name) {
        super(name, "Metadata type", new Coercing<Metadata, String>() {
            private Metadata convertImpl(Object input) {
                if (input instanceof String) {
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<Metadata>>() {}.getType();
                    return gson.fromJson((String)input, type);
                }
                return null;
            }

            @Override
            public String serialize(Object input) {
                Gson gson = new Gson();
                return gson.toJson(input);
            }

            @Override
            public Metadata parseValue(Object input) {
                Metadata result = convertImpl(input);
                if (result == null) {
                    throw new CoercingParseValueException("Invalid value '" + input + "' for Date");
                }
                return result;
            }

            @Override
            public Metadata parseLiteral(Object input) {
                if (!(input instanceof StringValue)) return null;
                String value = ((StringValue) input).getValue();
                Metadata result = convertImpl(value);
                return result;
            }
        });
    }

}

class Metadata {

    private Date created;
    private String createdBy;
    private Date modified;
    private String modifiedBy;
    private Date published;
    private String publishedBy;

    public Metadata(Date created, String createdBy, Date modified, String modifiedBy, Date published, String publishedBy){
        this.created = created;
        this.createdBy = createdBy;
        this.modified = modified;
        this.modifiedBy = modifiedBy;
        this.published = published;
        this.publishedBy = publishedBy;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Date getPublished() {
        return published;
    }

    public void setPublished(Date published) {
        this.published = published;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(String publishedBy) {
        this.publishedBy = publishedBy;
    }



}
