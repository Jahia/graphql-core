<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="modulePath" value="${pageContext.request.contextPath}/modules/graphql-dxm-provider"/>
<c:set var="graphQlEndpoint" value="${pageContext.request.contextPath}/modules/graphql"/>

<!doctype html>
<html>
<head>
    <meta charset="UTF-8"/>
    <title>GraphQL Playground</title>
    <link rel="stylesheet" href="${modulePath}/css/graphql-playground-react.css" type="text/css"/>
</head>
<body>
<div id="graphql-playground">
    <p class="loading">Loading <span class="title">GraphQL Playground</span></p>
</div>
<script src="${modulePath}/javascript/graphql-playground-react-1.7.27.js"></script>
<script>
    window.addEventListener('load', function () {
        GraphQLPlayground.init(document.getElementById('graphql-playground'), {
            endpoint: '${graphQlEndpoint}',
            settings: {
                'request.credentials': 'same-origin'
            },
            shareEnabled: false
        })
    })
        </script>
    </body>
</html>
