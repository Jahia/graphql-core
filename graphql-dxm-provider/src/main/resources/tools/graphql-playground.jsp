<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<script type="module"
        src="<c:url value='/modules/graphql-dxm-provider/javascript/tools/toolsembed.graphqldxm.bundle.js'/>"></script>
<body>
<div id="embedded-sandbox">
</div>
</body>
<script>
    console.log('starting');
    document.addEventListener("DOMContentLoaded",() => {
        console.log('dom loaded');
        GraphqlPlayground.EmbeddedSandbox('#embedded-sandbox');
    });
</script>
