window.jahia.i18n.loadNamespaces('graphql-dxm-provider');

window.jahia.uiExtender.registry.add('adminRoute', 'graphql-playground', {
    targets: ['developerTools:20'],
    requiredPermission: 'developerToolsAccess',
    icon: window.jahia.moonstone.toIconComponent('GraphQl'),
    label: 'graphql-dxm-provider:graphql',
    isSelectable: true,
    iframeUrl: window.contextJsParameters.contextPath + '/modules/graphql-dxm-provider/tools/developer/graphql-playground.jsp'
});

window.jahia.uiExtender.registry.add('adminRoute', 'sdl-report-tool', {
    targets: ['developerTools:30'],
    requiredPermission: 'developerToolsAccess',
    icon: window.jahia.moonstone.toIconComponent('SdLreport'),
    label: 'graphql-dxm-provider:sdlReport',
    isSelectable: true,
    iframeUrl: window.contextJsParameters.contextPath + '/modules/graphql-dxm-provider/tools/developer/sdlreporttool.jsp'
});
