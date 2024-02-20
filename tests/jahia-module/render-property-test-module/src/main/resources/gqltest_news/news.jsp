<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>


<fmt:formatDate value="${creationDate.time}" pattern="d-MMMM-yyyy" var="date"/>
<c:if test="${currentNode.properties['title'] != null}">
    <div class="head">
        <span class="title">${currentNode.properties['title'].string}</span>
        <span class="subtitle">Created on: ${date} by ${currentNode.properties['author']}</span>
    </div>
</c:if>
<c:forEach items="${currentNode.properties['description']}" var="descr" varStatus="idx">
    <p>
        <span>Description n°${idx.count}: </span>
        <span>${descr}</span>
    </p>
</c:forEach>
<c:if test="${currentNode.properties['author_bio'] != null}">
    <h4>About the author:</h4>
    <p>${currentNode.properties['author_bio']}</p>
</c:if>