<%@ page import="org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig"%>
<%@ page import="org.jahia.osgi.BundleUtils"%>

<%
    DXGraphQLConfig gqlConfig = BundleUtils.getOsgiService(DXGraphQLConfig.class, null);
%>

contextJsParameters.config.graphqlDxmProvider = {
    isIntrospectionEnabled: <%=gqlConfig.isIntrospectionEnabled()%>
};
