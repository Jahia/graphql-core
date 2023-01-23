import { defineConfig } from 'cypress'

export default defineConfig({
    chromeWebSecurity: false,
    defaultCommandTimeout: 10000,
    videoUploadOnPasses: false,
    reporter: 'cypress-multi-reporters',
    reporterOptions: {
        configFile: 'reporter-config.json',
    },
    screenshotsFolder: './results/screenshots',
    videosFolder: './results/videos',
    viewportWidth: 1366,
    viewportHeight: 768,
    e2e: {
        // eslint-disable-next-line @typescript-eslint/no-var-requires
        setupNodeEvents(on, config) {
            return require('./cypress/plugins/index.js')(on, config)
        },
        excludeSpecPattern: ['*.ignore.ts', 'admin/users.cy.ts'],
        baseUrl: 'http://localhost:8080',
        experimentalSessionAndOrigin: false,
    },
})
