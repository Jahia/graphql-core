package org.jahia.modules.graphql.provider.dxm.service.vanity;

import graphql.annotations.annotationTypes.*;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.graphql.provider.dxm.GqlConstraintViolationException;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrMutationSupport;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNodeMutation;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.seo.VanityUrl;
import org.jahia.services.seo.jcr.NonUniqueUrlMappingException;
import org.jahia.services.seo.jcr.VanityUrlService;

import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.Map;

@GraphQLTypeExtension(GqlJcrNodeMutation.class)
@GraphQLName("VanityUrlJCRNodeMutationExtensions")
public class VanityUrlJCRNodeMutationExtensions  extends GqlJcrMutationSupport {

    private JCRNodeWrapper targetNode;
    private VanityUrlService vanityUrlService;


    /**
     * Initializes an instance of this class.
     *
     * @param node the corresponding GraphQL node
     */
    public VanityUrlJCRNodeMutationExtensions(GqlJcrNodeMutation node) {
        this.vanityUrlService = BundleUtils.getOsgiService(VanityUrlService.class, null);
        this.targetNode = node.getNode().getNode();
    }


    /**
     * Add the vanity URL.
     *
     * @param active Desired value of the active flag or null to keep existing value
     * @param defaultMapping Desired value of the default flag or null to keep existing value
     * @param language Desired vanity URL language or null to keep existing value
     * @param url Desired URL value or null to keep existing value
     * @return Always true
     * @throws GqlConstraintViolationException In case the desired values violate a vanity URL uniqueness constraint
     */
    @GraphQLField
    @GraphQLDescription("Add vanity URL")
    @GraphQLName("addVanityUrl")
    public boolean addVanityUrl(@GraphQLName("active") @GraphQLDescription("Desired value of the active flag or null to keep existing value") Boolean active,
                          @GraphQLName("defaultMapping") @GraphQLNonNull @GraphQLDescription("Desired value of the default flag or null to keep existing value") Boolean defaultMapping,
                          @GraphQLName("language") @GraphQLNonNull @GraphQLDescription("Desired vanity URL language or null to keep existing value") String language,
                          @GraphQLName("url") @GraphQLNonNull @GraphQLDescription("Desired URL value or null to keep existing value") String url
    ) throws GqlConstraintViolationException {
        try {
            VanityUrl vanityUrl = new VanityUrl();;

            if (active != null) {
                vanityUrl.setActive(active);
            }
            if (defaultMapping != null) {
                vanityUrl.setDefaultMapping(defaultMapping);
            }
            if (language != null) {
                vanityUrl.setLanguage(language);
            }
            if (url != null) {
                vanityUrl.setUrl(url);
            }
            vanityUrlService.saveVanityUrlMapping(targetNode, vanityUrl, false);
            return true;
        } catch (NonUniqueUrlMappingException e) {
            Map<String,Object> extensions = new HashMap<>();
            extensions.put("type",e.getClass().getName());
            extensions.put("existingNodePath",e.getExistingNodePath());
            extensions.put("urlMapping",e.getUrlMapping());
            extensions.put("workspace",e.getWorkspace());
            extensions.put("nodePath",e.getNodePath());
            throw new GqlConstraintViolationException(e, extensions);
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
    }
}
