<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sql" uri="http://java.sun.com/jsp/jstl/sql" %>
<%@ page import="org.jahia.modules.graphql.provider.dxm.config.DXGraphQLConfig"%>
<%@ page import="org.jahia.osgi.BundleUtils"%>

<%
    DXGraphQLConfig gqlConfig = BundleUtils.getOsgiService(DXGraphQLConfig.class, null);
    pageContext.setAttribute("enableSandbox", gqlConfig.isIntrospectionEnabled());
%>
<script type="module"
        src="<c:url value='/modules/graphql-dxm-provider/javascript/tools/toolsembed.graphqldxm.bundle.js'/>"></script>
<body>
<div id="embedded-sandbox">
    <c:if test="${not enableSandbox}">
        <div role="alert">
            GraphQL playground is not available due to introspection being currently disabled. To enable introspection for production, set
            the property <code>graphql.introspection.enabled</code> to <code>true</code> in your
            <code>org.jahia.modules.graphql.provider-default.cfg</code> configuration file.
        </div>
    </c:if>
</div>
</body>

<script>
    console.log('enable graphql sandbox: ${enableSandbox}');
    <c:if test="${enableSandbox}">
        document.addEventListener("DOMContentLoaded",() => {
            console.log('dom loaded');
            GraphqlPlayground.EmbeddedSandbox('embedded-sandbox');
        });
    </c:if>
</script>
