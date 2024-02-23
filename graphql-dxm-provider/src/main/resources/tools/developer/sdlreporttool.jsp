<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="graphql" uri="http://www.jahia.org/graphql-dxm-provider/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>

<html>
<head>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/modules/graphql-dxm-provider/css/sdlreporttool.css"
          type="text/css"/>
</head>
<c:set var="sdlDefinitionsStatus" value="${graphql:sdlDefinitionsStatus()}"/>
<c:set var="sdlSchemaStatus" value="${graphql:sdlSchemaStatus()}"/>
<body>
<h2>SDL Definitions</h2>
<div class="status-container">
    <div class="status-header">
        <div class="status-header-item" style="width:15%">
            Definition Type
        </div>
        <div class="status-header-item" style="width:65%">
            Message
        </div>
        <div class="status-header-item" style="width:10%">
            Status
        </div>
    </div>
    <c:forEach items="${sdlDefinitionsStatus}" var="entry">
        <div class="status-row" data-sdl-type="${entry.key}">
            <div class="status-item" data-sdl-type-name="${entry.key}" style="width:15%">
                <c:out value="${entry.key}"/>
            </div>
            <div class="status-item" data-sdl-type-message="${entry.value}" style="width:65%">
                <c:out value="${entry.value}"/>
            </div>
            <div class="status-item" data-sdl-type-status="${entry.value.status == 'OK' ? "success" : "error"}"
                 style="width:10%;text-align: center">
                <div class="status-icon ${entry.value.status == 'OK' ? "success" : "error"}"></div>
            </div>
        </div>
    </c:forEach>
</div>
<h2>SDL Schema</h2>
<div class="status-container">
    <div class="status-header">
        <div class="status-header-item" style="width:15%">
            Module name
        </div>
        <div class="status-header-item" style="width:65%">
            Message
        </div>
        <div class="status-header-item" style="width:10%">
            Status
        </div>
    </div>
    <c:forEach items="${sdlSchemaStatus}" var="entry">
        <div class="status-row" data-sdl-module="${entry.key}">
            <div class="status-item" data-sdl-module-name="${entry.key}" style="width:15%">
                <c:out value="${entry.key}"/>
            </div>
            <div class="status-item" data-sdl-module-messages style="width:65%">
                <c:forEach items="${entry.value}" var="item" varStatus="status">
                    <div data-sdl-module-message="${status.index}"><c:out value="${item.error}"/></div>
                </c:forEach>
            </div>
            <div class="status-item" data-sdl-module-status="${empty entry.value ? "success" : "error"}"
                 style="width:10%;text-align: center; ">
                <div class="status-icon ${empty entry.value ? "success" : "error"}"></div>
            </div>
        </div>
    </c:forEach>
</div>
</body>
</html>
