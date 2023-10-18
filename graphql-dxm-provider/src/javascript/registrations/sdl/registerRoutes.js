import {registry} from '@jahia/ui-extender';

export const registerRoutes = function () {
    registry.add('adminRoute', 'sdl-report-tool', {
        targets: ['developerTools:30'],
        requiredPermission: 'developerToolsAccess',
        icon: window.jahia.moonstone.toIconComponent('SdLreport'),
        label: 'graphql-dxm-provider:sdlReport',
        isSelectable: true,
        iframeUrl: window.contextJsParameters.contextPath + '/modules/graphql-dxm-provider/tools/developer/sdlreporttool.jsp'
    });
};
