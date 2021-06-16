/* eslint-disable @typescript-eslint/no-explicit-any */
import gql from 'graphql-tag'
import { apollo } from '../../../support/apollo'

describe('Test admin user endpoint', () => {
    it('gets a user', () => {
        cy.apolloQuery(
            apollo(Cypress.config().baseUrl, { username: 'root', password: Cypress.env('SUPER_USER_PASSWORD') }),
            {
                query: gql`
                    {
                        admin {
                            userAdmin {
                                user(userName: "root") {
                                    name
                                }
                            }
                        }
                    }
                `,
            },
        ).should((response: any) => {
            expect(response.data.admin.userAdmin).to.exist
            expect(response.data.admin.userAdmin.user.name).to.equal('root')
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
