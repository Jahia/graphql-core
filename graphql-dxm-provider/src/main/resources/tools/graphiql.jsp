<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt_rt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<c:set var="graphQLURL" value=""/>

<!-- The GraphiQL version used here is: 1.0.6-->
<!-- More details here: https://github.com/graphql/graphiql/tree/main/packages/graphiql -->
<html>
<head>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/modules/graphql-dxm-provider/css/graphiql.min.106.css" type="text/css" />
    <script src="${pageContext.request.contextPath}/modules/graphql-dxm-provider/javascript/vendor/react.production.min.js"></script>
    <script src="${pageContext.request.contextPath}/modules/graphql-dxm-provider/javascript/vendor/react-dom.production.min.js"></script>
    <script src="${pageContext.request.contextPath}/modules/graphql-dxm-provider/javascript/graphiql.min.106.js" ></script>   
    
    <style>
        #graphiql {
            height: 100vh;
        }
    </style>
</head>

<body>
    <script>
          window.graphQLURL = '${pageContext.request.contextPath}' + '/modules/graphql';
    </script>
    <div id="graphiql">Loading...</div>
    <script defer src="${pageContext.request.contextPath}/modules/graphql-dxm-provider/javascript/renderExample.js" type="application/javascript"></script>
  </body>
</body>
</html>
