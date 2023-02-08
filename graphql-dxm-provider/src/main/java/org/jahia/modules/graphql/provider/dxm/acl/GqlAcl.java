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
 *     Copyright (C) 2002-2023 Jahia Solutions Group. All rights reserved.
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
package org.jahia.modules.graphql.provider.dxm.acl;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import org.jahia.modules.graphql.provider.dxm.acl.service.JahiaAclEntry;
import org.jahia.modules.graphql.provider.dxm.acl.service.JahiaAclService;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeImpl;
import org.jahia.modules.graphql.provider.dxm.osgi.annotations.GraphQLOsgiService;
import org.jahia.modules.graphql.provider.dxm.user.PrincipalInput;
import org.jahia.services.content.JCRNodeWrapper;

import javax.inject.Inject;
import javax.jcr.RepositoryException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@GraphQLName("GqlAcl")
@GraphQLDescription("ACL properties and list of access control entry")
public class GqlAcl {

    @Inject
    @GraphQLOsgiService
    private JahiaAclService aclService;

    private final JCRNodeWrapper parentNode;

    public GqlAcl(JCRNodeWrapper parent) {
        this.parentNode = parent;
    }

    @GraphQLField
    @GraphQLDescription("Get parent node for this ACL")
    public GqlJcrNode getParentNode() {
        return new GqlJcrNodeImpl(parentNode);
    }

    @GraphQLField
    @GraphQLDescription("Get inheritance break attribute for this node")
    public boolean getInheritanceBreak() throws RepositoryException {
        return parentNode.getAclInheritanceBreak();
    }

    @GraphQLField
    @GraphQLDescription("Get list of access control entries for this ACL")
    public List<GqlAclEntry> getAclEntries(
            @GraphQLName("principalFilter") @GraphQLDescription("Fetch ACL entry only for this principal") PrincipalInput principalInput,
            @GraphQLName("inclInherited") @GraphQLDescription("The languages to check") Boolean inclInherited) {

        Stream<JahiaAclEntry> s = (principalInput == null) ?
                aclService.getAclEntries(parentNode).stream() :
                aclService.getAclEntries(parentNode, principalInput.getPrincipalKey()).stream();
        if (Boolean.FALSE.equals(inclInherited)) {
            s = s.filter(ace -> !ace.isInherited());
        }
        return s.map(GqlAclEntry::new).collect(Collectors.toList());
    }


}
