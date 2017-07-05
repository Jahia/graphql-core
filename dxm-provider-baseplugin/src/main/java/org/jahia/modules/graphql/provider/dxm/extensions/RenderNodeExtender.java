//package org.jahia.modules.graphql.provider.dxm.extensions;
//
//import graphql.schema.*;
//import graphql.servlet.GraphQLContext;
//import org.apache.commons.collections.iterators.IteratorEnumeration;
//import org.apache.commons.collections.map.HashedMap;
//import org.jahia.bin.Render;
//import org.jahia.modules.graphql.provider.dxm.builder.DXGraphQLExtender;
//import org.jahia.modules.graphql.provider.dxm.node.DXGraphQLJCRNode;
//import org.jahia.services.content.JCRNodeWrapper;
//import org.jahia.services.content.JCRSessionFactory;
//import org.jahia.services.content.JCRSessionWrapper;
//import org.jahia.services.content.decorator.JCRSiteNode;
//import org.jahia.services.render.RenderContext;
//import org.jahia.services.render.RenderService;
//import org.jahia.services.render.Resource;
//import org.jahia.settings.SettingsBean;
//import org.jahia.utils.LanguageCodeConverters;
//import org.osgi.service.component.annotations.Component;
//import org.osgi.service.component.annotations.Reference;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletRequestWrapper;
//import javax.servlet.http.HttpServletResponse;
//import java.util.*;
//
//import static graphql.Scalars.GraphQLString;
//
//@Component(service = DXGraphQLExtender.class, immediate = true, property = {"graphQLType=node"})
//public class RenderNodeExtender implements DXGraphQLExtender {
//    private RenderService renderService;
//
//    @Reference
//    public void setRenderService(RenderService renderService) {
//        this.renderService = renderService;
//    }
//
//    @Override
//    public List<GraphQLFieldDefinition> getFields() {
//        GraphQLEnumType areaTypeEnumType = GraphQLEnumType.newEnum().name("AreaType")
//                .value("currentPage")
//                .value("template")
//                .value("absolute")
//                .build();
//
//        GraphQLObjectType renderArea = GraphQLObjectType.newObject().name("RenderArea")
//                .field(GraphQLFieldDefinition.newFieldDefinition().name("name")
//                        .type(GraphQLString)
//                        .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition().name("type")
//                        .type(areaTypeEnumType)
//                        .build())
//                .field(GraphQLFieldDefinition.newFieldDefinition().name("content")
//                        .type(GraphQLString))
//                .field(GraphQLFieldDefinition.newFieldDefinition().name("node")
//                        .type(new GraphQLTypeReference("node")))
//                .build();
//
//
//
//        GraphQLObjectType renderedStructure = GraphQLObjectType.newObject().name("RenderedStructure")
//                .field(GraphQLFieldDefinition.newFieldDefinition().name("area")
//                        .type(new GraphQLList(renderArea))
//                        .argument(GraphQLArgument.newArgument().name("name").type(GraphQLString).build())
//                        .argument(GraphQLArgument.newArgument().name("type").type(areaTypeEnumType).build())
//                        .dataFetcher(getAreaDataFetcher()))
//                .build();
//
//        return Arrays.asList(GraphQLFieldDefinition.newFieldDefinition()
//                .name("renderStructure")
//                .argument(GraphQLArgument.newArgument().name("language").type(new GraphQLNonNull(GraphQLString)))
//                .type(renderedStructure)
//                .dataFetcher(getRenderStructureDataFetcher())
//                .build());
//    }
//
//    private DataFetcher getRenderStructureDataFetcher() {
//        return new DataFetcher() {
//            @Override
//            public Object get(DataFetchingEnvironment environment) {
//                String language = environment.getArgument("language");
//                DXGraphQLJCRNode node = (DXGraphQLJCRNode) environment.getSource();
//                Map<Resource, String> map = new HashMap<>();
//                try {
//                    JCRSessionWrapper locSession = JCRSessionFactory.getInstance().getCurrentUserSession(null, LanguageCodeConverters.languageCodeToLocale(language));
//                    JCRNodeWrapper nodeWrapper = locSession.getNode(node.getPath());
//                    HttpServletRequest request = ((GraphQLContext) environment.getContext()).getRequest().get();
//                    final Map<String,Object> attributes = new HashMap();
//                    Enumeration<String> names = request.getAttributeNames();
//                    while (names.hasMoreElements()) {
//                        String name = names.nextElement();
//                        attributes.put(name, request.getAttribute(name));
//
//                    }
//                    request = new HttpServletRequestWrapper(request) {
//                        @Override
//                        public Object getAttribute(String name) {
//                            return attributes.get(name);
//                        }
//
//                        @Override
//                        public Enumeration<String> getAttributeNames() {
//                            return new IteratorEnumeration(attributes.keySet().iterator());
//                        }
//
//                        @Override
//                        public void setAttribute(String name, Object o) {
//                            attributes.put(name,o);
//                        }
//
//                        @Override
//                        public void removeAttribute(String name) {
//                            attributes.remove(name);
//                        }
//                    };
//
//                    HttpServletResponse response = ((GraphQLContext) environment.getContext()).getResponse().get();
//                    request.setAttribute("graphQLStructureResult", map);
//                    Resource resource = new Resource(nodeWrapper, "html", null, Resource.CONFIGURATION_PAGE);
//
//                    RenderContext renderContext = new RenderContext(request, response, JCRSessionFactory.getInstance().getCurrentUser());
//                    renderContext.setMainResource(resource);
//                    renderContext.setServletPath(Render.getRenderServletPath());
//
//                    JCRSiteNode site = nodeWrapper.getResolveSite();
//                    renderContext.setSite(site);
//                    response.setCharacterEncoding(SettingsBean.getInstance().getCharacterEncoding());
//
//                    renderService.render(resource,renderContext);
//                    request.removeAttribute("graphQLStructureResult");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                return new DXArea(node, map);
//            }
//        };
//    }
//
//    private DataFetcher getAreaDataFetcher() {
//        return new DataFetcher() {
//            @Override
//            public Object get(DataFetchingEnvironment environment) {
//                String name = environment.getArgument("name");
//                String type = environment.getArgument("type");
//
//                DXArea dxArea = (DXArea) environment.getSource();
//                List<Map<String,Object>> result = new ArrayList<>();
//
//                for (Map.Entry<Resource, String> entry : dxArea.getResourcesMap().entrySet()) {
//                    String thisType;
//                    if (entry.getKey().getNodePath().startsWith("/modules")) {
//                        thisType = "template";
//                    } else if (entry.getKey().getNodePath().startsWith(dxArea.getNode().getPath())) {
//                        thisType = "currentPage";
//                    } else {
//                        thisType = "absolute";
//                    }
//
//                    if ((name == null || entry.getKey().getNodePath().endsWith("/"+name)) &&
//                            (type == null ||  type.equals(thisType))) {
//                        Map<String, Object> r = new HashMap<>();
//                        r.put("name", entry.getKey().getNode().getName());
//                        r.put("type", thisType);
//                        r.put("content", entry.getValue());
//                        r.put("node", new DXGraphQLJCRNode(entry.getKey().getNode()));
//                        result.add(r);
//                    }
//                }
//
//                return result;
//            }
//        };
//    }
//
//    class DXArea {
//        DXGraphQLJCRNode node;
//        Map<Resource, String> resourcesMap;
//
//        public DXArea(DXGraphQLJCRNode node, Map<Resource, String> map) {
//            this.node = node;
//            this.resourcesMap = map;
//        }
//
//        public DXGraphQLJCRNode getNode() {
//            return node;
//        }
//
//        public Map<Resource, String> getResourcesMap() {
//            return resourcesMap;
//        }
//    }
//}
//
