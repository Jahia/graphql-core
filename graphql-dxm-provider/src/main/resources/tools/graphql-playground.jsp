<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="modulePath" value="${pageContext.request.contextPath}/modules/graphql-dxm-provider"/>
<c:set var="graphQlEndpoint" value="${pageContext.request.contextPath}/modules/graphql"/>

<!doctype html>
<html>
    <head>
        <meta charset="UTF-8"/>
        <title>GraphQL Playground</title>
        <link rel="stylesheet" href="${modulePath}/css/graphql-playground.css" type="text/css"/>
    </head>
    <body>
        <div id="graphql-playground">
            <p class="loading">Loading <span class="title">GraphQL Playground</span></p>
        </div>
        <script src="${modulePath}/javascript/graphql-playground-1.7.23.js"></script>
        <script>
            const customTheme = {
                // property: '',
                // comment: '',
                // punctuation: '',
                //keyword: '',
                // def: '',
                // qualifier: '',
                // attribute: '',
                // number: '',
                // string: '',
                // builtin: '',
                // string2: '',
                // variable: '',
                // meta: '',
                // atom: '',
                // ws: '',
                // selection: '',
                // cursorColor: '',
                // editorBackground: '',
                // resultBackground: '',
                // leftDrawerBackground: '',
                // rightDrawerBackground: ''
            };

            window.addEventListener('load', function() {
                GraphQLPlayground.init(document.getElementById('graphql-playground'), {
                    endpoint: '${graphQlEndpoint}',
                    settings: {
                        'request.credentials': 'same-origin'
                    },
                    shareEnabled: true,
                    //workspaceName: 'Jahia',
                    //codeTheme: customTheme
                })
            })
        </script>
    </body>
</html>
