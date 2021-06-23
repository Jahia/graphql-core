/* eslint-disable @typescript-eslint/no-explicit-any */
import gql from 'graphql-tag'
import { apollo } from '../../../support/apollo'
import { DocumentNode } from 'graphql'

describe('Test admin user endpoint', () => {
    let GQL_USER: DocumentNode

    before('load graphql file', function () {
        GQL_USER = require(`graphql-tag/loader!../../../fixtures/admin/user.graphql`)
    })

    it('gets a user', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: GQL_USER,
                variables: { userName: 'jay' },
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.name).to.equal('jay')
            expect(response.data.admin.userAdmin.user.username).to.equal('jay')
            expect(response.data.admin.userAdmin.user.firstname).to.equal('Jay')
            expect(response.data.admin.userAdmin.user.lastname).to.equal('Hawking')
            expect(response.data.admin.userAdmin.user.organization).not.to.be.undefined
            expect(response.data.admin.userAdmin.user.language).to.equal('en')
            expect(response.data.admin.userAdmin.user.locked).to.equal(false)
            expect(response.data.admin.userAdmin.user.email).not.to.be.undefined
        })
    })

    it('gets a non existing user', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: gql`
                    {
                        admin {
                            userAdmin {
                                user(userName: "noob") {
                                    name
                                }
                            }
                        }
                    }
                `,
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user).to.be.null
        })
    })

    it('gets a user name', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: gql`
                    {
                        admin {
                            userAdmin {
                                user(userName: "bill") {
                                    name
                                    displayName
                                }
                            }
                        }
                    }
                `,
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.name).to.equal('bill')
            expect(response.data.admin.userAdmin.user.displayName).to.equal('Bill Galileo')
        })
    })

    it('tests membership', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: gql`
                    {
                        admin {
                            userAdmin {
                                user(userName: "bill") {
                                    yes: memberOf(group: "site-administrators", site: "digitall")
                                    no: memberOf(group: "site-administrators", site: "systemsite")
                                }
                            }
                        }
                    }
                `,
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.yes).to.equal(true)
            expect(response.data.admin.userAdmin.user.no).to.equal(false)
        })
    })

    it('tests membership list', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: gql`
                    {
                        admin {
                            userAdmin {
                                user(userName: "bill") {
                                    groupMembership {
                                        pageInfo {
                                            totalCount
                                        }
                                        nodes {
                                            name
                                        }
                                    }
                                }
                            }
                        }
                    }
                `,
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.groupMembership.pageInfo.totalCount).to.be.greaterThan(3)
            expect(response.data.admin.userAdmin.user.groupMembership.nodes.map((n) => n.name)).to.contains(
                'site-administrators',
            )
        })
    })

    it('tests membership list for a site', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: gql`
                    {
                        admin {
                            userAdmin {
                                user(userName: "bill") {
                                    groupMembership(site: "digitall") {
                                        pageInfo {
                                            totalCount
                                        }
                                    }
                                }
                            }
                        }
                    }
                `,
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.groupMembership.pageInfo.totalCount).to.equal(3)
        })
    })

    it('tests membership list with filter', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: gql`
                    {
                        admin {
                            userAdmin {
                                user(userName: "bill") {
                                    groupMembership(
                                        fieldFilter: { filters: { fieldName: "site.name", value: "digitall" } }
                                    ) {
                                        pageInfo {
                                            totalCount
                                        }
                                    }
                                }
                            }
                        }
                    }
                `,
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.groupMembership.pageInfo.totalCount).to.equal(3)
        })
    })

    it('tests members list', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: gql`
                    {
                        admin {
                            userAdmin {
                                group(groupName: "site-administrators", site: "digitall") {
                                    members {
                                        nodes {
                                            name
                                        }
                                    }
                                }
                            }
                        }
                    }
                `,
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.group.members.nodes.map((n) => n.name)).to.contains('bill')
        })
    })
})
