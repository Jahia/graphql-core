package org.jahia.modules.graphql.provider.dxm.extensions;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.graphql.provider.dxm.extensions.node.GqlBundleState;
import org.jahia.osgi.BundleUtils;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created at Nov 2018$
 *
 * @author chooliyip
 **/

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@GraphQLDescription("a GraphQL extension for management bundle service")
public class DXGraphQLBundleServiceExtensions {

    private static final Logger logger = LoggerFactory.getLogger(DXGraphQLBundleServiceExtensions.class);

    private static final String[] bundleSymbols = {"graphql-dxm-provider"};

    @GraphQLField
    public static List<GqlBundleState> restartDXBundle(){
        logger.debug("restart the bundles ", bundleSymbols);

        List<GqlBundleState> states = new ArrayList<>();

        for(String symbol : bundleSymbols){
            Bundle bundle = BundleUtils.getBundleBySymbolicName(symbol, null);
            GqlBundleState state = new GqlBundleState(bundle.getSymbolicName());
            state.setVersion(bundle.getVersion().toString());
            try{
                if(Bundle.ACTIVE == bundle.getState()){
                    bundle.stop();
                }

                if(Bundle.RESOLVED == bundle.getState()){
                    bundle.start();
                }

                if(Bundle.ACTIVE == bundle.getState()){
                    state.setState(GqlBundleState.STATE_RESTART);
                }
            }catch (Exception e){
                e.printStackTrace();
                logger.error("failed to restart bundle ", symbol, " due to ", e.getMessage());
                state.setState(GqlBundleState.STATE_ERROR);
            }finally {
                states.add(state);
            }

        }

        return states;
    }

}
