<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
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
<html lang="${fn:substring(renderContext.request.locale,0,2)}">
<head>
    <meta charset="UTF-8">
    <jcr:nodeProperty node="${renderContext.mainResource.node}" name="jcr:description" inherited="true"
                      var="description"/>
    <jcr:nodeProperty node="${renderContext.mainResource.node}" name="jcr:createdBy" inherited="true" var="author"/>
    <c:set var="keywords" value="${jcr:getKeywords(renderContext.mainResource.node, true)}"/>
    <c:if test="${!empty description}">
        <meta name="description" content="${fn:escapeXml(description.string)}"/>
    </c:if>
    <c:if test="${!empty author}">
        <meta name="author" content="${fn:escapeXml(author.string)}"/>
    </c:if>
    <c:if test="${!empty keywords}">
        <meta name="keywords" content="${fn:escapeXml(keywords)}"/>
    </c:if>
    <title>${fn:escapeXml(renderContext.mainResource.node.displayableName)}</title>

</head>

<body>

<div class=" clearfix">
    <template:area path="pagecontent"/>
</div>

<c:if test="${renderContext.editMode}">
    <template:addResources type="css" resources="edit.css"/>
</c:if>

<template:theme/>

</body>
</html>
