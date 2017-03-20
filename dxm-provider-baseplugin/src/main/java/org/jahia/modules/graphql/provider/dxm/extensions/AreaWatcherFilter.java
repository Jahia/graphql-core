package org.jahia.modules.graphql.provider.dxm.extensions;

import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.jahia.services.render.filter.RenderFilter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.util.Map;

@Component(service = RenderFilter.class)
public class AreaWatcherFilter extends AbstractFilter {


    @Activate
    public void activate() {
        setPriority(-50);
        setApplyOnConfigurations(Resource.CONFIGURATION_WRAPPEDCONTENT);
        addCondition(new ExecutionCondition() {
            @Override
            public boolean matches(RenderContext renderContext, Resource resource) {
                return renderContext.getRequest().getAttribute("graphQLStructureResult") != null;
            }
        });
    }

    @Override
    public String execute(String previousOut, RenderContext renderContext, Resource resource, RenderChain chain) throws Exception {
        Map<Resource, String> m = (Map) renderContext.getRequest().getAttribute("graphQLStructureResult");
        m.put(resource, previousOut);
        return previousOut;
    }

}
