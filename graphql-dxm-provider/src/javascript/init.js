import registrations from './registrations';
import {registry} from '@jahia/ui-extender';
import i18next from 'i18next';

export default function () {
    registry.add('callback', 'graphql-dxm-provider', {
        targets: ['jahiaApp-init:50'],
        callback: () => {
            i18next.loadNamespaces('graphql-dxm-provider');
            registrations();
            console.log('%c GraphQL Jahia Provider routes have been registered', 'color: #3c8cba');
        }
    });
}
