import  gql  from "graphql-tag"

describe('Test GraphQL Field Sorter', () => {
    before('setup list with two children', () => {
        cy.apollo({
            mutation: gql`mutation {
                jcr(workspace: EDIT) {
                    addNode(parentPathOrId: "/", name: "testList", primaryNodeType: "jnt:contentList") {
                        child1:addChild(name: "ZHello", primaryNodeType: "jnt:contentList") {
                            uuid
                        }
                        child2:addChild(name: "abonjour", primaryNodeType: "jnt:contentList") {
                            uuid
                        }
                    }
                }
            }`
        })
        
    })

    it('Should sort display name ASC with case', () => {
        cy.apollo({
            queryFile: 'sortingList.graphql',
            variables: {
                sortType: 'ASC',
                ignoreCase: true,
            }
        })
        .should(result => {
            const sortedList1 = result?.data?.jcr?.nodesByCriteria?.nodes
            expect(sortedList1).to.have.length(2)
            expect(sortedList1[0]).to.have.property('displayName',"abonjour")
            expect(sortedList1[1]).to.have.property('displayName',"ZHello")
        })

    })
        
    it('Should sort display name ASC ignoring case', () => {
        cy.apollo({
            queryFile: 'sortingList.graphql',
            variables: {
                sortType: 'ASC',
                ignoreCase: false,
            }
        })
        .should(result => {
            const sortedList1 = result?.data?.jcr?.nodesByCriteria?.nodes
            expect(sortedList1).to.have.length(2)
            expect(sortedList1[0]).to.have.property('displayName',"ZHello")
            expect(sortedList1[1]).to.have.property('displayName',"abonjour")
        })

    })
        
    it('Should sort display name DESC with case', () => {    
        cy.apollo({
            queryFile: 'sortingList.graphql',
            variables: {
                sortType: 'DESC',
                ignoreCase: true,
            }
        })
        .should(result => {
            const sortedList1 = result?.data?.jcr?.nodesByCriteria?.nodes
            expect(sortedList1).to.have.length(2)
            expect(sortedList1[0]).to.have.property('displayName',"ZHello")
            expect(sortedList1[1]).to.have.property('displayName',"abonjour")
        })

    })

    it('Should sort display name DESC ignoring case', () => {
        cy.apollo({
            queryFile: 'sortingList.graphql',
            variables: {
                sortType: 'DESC',
                ignoreCase: false,
            }
        })
        .should(result => {
            const sortedList1 = result?.data?.jcr?.nodesByCriteria?.nodes
            expect(sortedList1).to.have.length(2)
            expect(sortedList1[0]).to.have.property('displayName',"abonjour")
            expect(sortedList1[1]).to.have.property('displayName',"ZHello")
        })

    })

    after('Delete list', function () {
        
        cy.apollo({
            mutationFile: 'jcr/deleteNode.graphql',
            variables: {
                pathOrId: '/testList',
            }
        });
    })
})