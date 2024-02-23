/// <reference types="cypress" />
// ***********************************************************
// This example plugins/index.js can be used to load plugins
//
// You can change the location of this file or turn off loading
// the plugins file with the 'pluginsFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/plugins-guide
// ***********************************************************

// This function is called when a project is opened or re-opened (e.g. due to
// the project's config changing)

/**
 * @type {Cypress.PluginConfig}
 */
const sshCommand = require('./ssh');
module.exports = (on, config) => {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    require('@jahia/cypress/dist/plugins/registerPlugins').registerPlugins(on, config);
    // Custom tasks (Useful to run code in Node from cypress tests)
    on('task', {
        sshCommand(commands) {
            return sshCommand(commands, {
                hostname: process.env.JAHIA_HOST,
                port: process.env.JAHIA_PORT_KARAF,
                username: process.env.JAHIA_USERNAME_TOOLS,
                password: process.env.JAHIA_PASSWORD_TOOLS
            });
        }
    });
    return config;
};
