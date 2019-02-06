package org.jahia.modules.graphql.provider.dxm.sdl.extension;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;

import java.util.List;

public interface FinderMixinInterface {
    enum FinderMixin {
        MF_PERSONALIZATION("mfFinderMixin");

        String mixinName;

        FinderMixin(String mfFinderMixin) {
            mixinName = mfFinderMixin;
        }

        public String getMixinName() {
            return mixinName;
        }
    }
    FinderMixinInterface getInstance();
    List<GraphQLArgument> getArguments();
    GqlJcrNode resolveNode(GqlJcrNode gqlJcrNode, DataFetchingEnvironment environment);
}
