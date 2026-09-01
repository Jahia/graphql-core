import gql from 'graphql-tag';

/*
 * MutateNodesByQuery's `limit` argument as a page size, not a ceiling (graphql-core#663).
 *
 * #645 introduced graphql.mutation.batch.limit and folded the caller's `limit` into it as a single effective
 * limit, so a `limit` narrower than the query's match count was refused instead of applied as a page. #663
 * restores paging by comparing the two instead of merging them.
 *
 * The cases below marked "pages ..." are the ones that flip: they fail on graphql-dxm-provider 3.9.0 (the bug)
 * and pass once 3.9.1 (unreleased at the time of writing) is deployed. The cases marked "sanity" assert the
 * guard itself still refuses an oversized request either way, so a passing run isn't just a weakened guard.
 */
describe('mutateNodesByQuery limit pages instead of refusing (graphql-core#663)', () => {
    const parentPath = '/sites/systemsite/contents/mutateNodesByQueryLimitTest';
    const subLists = ['subList1', 'subList2', 'subList3'].map(name => `${parentPath}/${name}`);
    const subListsQuery = `select * from [jnt:contentList] where isdescendantnode('${parentPath}') order by localname()`;

    const configPid = 'org.jahia.modules.graphql.provider';
    const waitUntilOptions = {
        interval: 250,
        timeout: 5000,
        errorMsg: 'Failed to verify graphql.mutation.batch.limit update'
    };

    const setMutationBatchLimit = (limit: number) => {
        cy.runProvisioningScript({
            script: [
                {
                    editConfiguration: `${configPid}-default`,
                    properties: {'graphql.mutation.batch.limit': String(limit)}
                }
            ]
        });
        cy.waitUntil(
            () =>
                cy
                    .task('sshCommand', [`config:list "(service.factoryPid=${configPid})"`])
                    .then((response: string) => response.indexOf(`graphql.mutation.batch.limit = ${limit}`) !== -1),
            waitUntilOptions
        );
    };

    before('create three orderable nodes to page over', () => {
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr(workspace: EDIT) {
                        addNode(
                            parentPathOrId: "/sites/systemsite/contents"
                            name: "mutateNodesByQueryLimitTest"
                            primaryNodeType: "jnt:contentFolder"
                        ) {
                            addChildrenBatch(
                                nodes: [
                                    {name: "subList1", primaryNodeType: "jnt:contentList"}
                                    {name: "subList2", primaryNodeType: "jnt:contentList"}
                                    {name: "subList3", primaryNodeType: "jnt:contentList"}
                                ]
                            ) {
                                uuid
                            }
                        }
                    }
                }
            `
        });
    });

    after('remove the test content and restore the shipped default', () => {
        cy.apollo({
            mutation: gql`
                mutation {
                    jcr {
                        mutateNodes(pathsOrIds: ["${parentPath}"]) {
                            delete
                        }
                    }
                }
            `
        });
        setMutationBatchLimit(5000);
    });

    // Reads jcr:title off all three sub-lists in one request, aliased so a single response covers every path.
    const readTitles = () =>
        cy
            .apollo({
                query: gql`
            query {
                jcr {
                    ${subLists.map((path, i) => `n${i}: nodeByPath(path: "${path}") {
                        property(name: "jcr:title", language: "en") { value }}`).join('\n')}
                }
            }
        `
            })
            .then((response: any) => subLists.map((_, i) => response.data.jcr[`n${i}`]?.property?.value ?? null));

    // Every case writes its own marker as jcr:title, so which sub-lists carry it says which ones were mutated -
    // independent of what an earlier case in the matrix left behind.
    const assertTitled = (marker: string, titledPaths: string[]) =>
        readTitles().then(values => {
            subLists.forEach((path, i) => {
                const shouldBeTitled = titledPaths.includes(path);
                expect(
                    String(values[i]) === marker,
                    `${path} ${shouldBeTitled ? 'should' : 'should not'} carry "${marker}", got ${JSON.stringify(values[i])}`
                ).to.equal(shouldBeTitled);
            });
        });

    const mutateByQuery = (marker: string, extraArguments: string) =>
        cy.apollo({
            mutation: gql`
            mutation {
                jcr {
                    mutateNodesByQuery(query: "${subListsQuery}", queryLanguage: SQL2${extraArguments}) {
                        mutateProperty(name: "jcr:title") {
                            setValue(language: "en", value: "${marker}")
                        }
                    }
                }
            }
        `,
            errorPolicy: 'all'
        });

    type Case = {
        name: string;
        bound: number;
        extraArguments: string;
        expectPaged: boolean;
        titledOnSuccess?: string[];
    };

    const cases: Case[] = [
        {
            name: 'pages to a limit narrower than the match count, within the bound',
            bound: 2,
            extraArguments: ',limit:1',
            expectPaged: true,
            titledOnSuccess: [subLists[0]]
        },
        {
            name: 'pages with limit and offset within the bound',
            bound: 2,
            extraArguments: ',limit:1,offset:1',
            expectPaged: true,
            titledOnSuccess: [subLists[1]]
        },
        {
            name: 'pages with a limit equal to the bound',
            bound: 2,
            extraArguments: ',limit:2',
            expectPaged: true,
            titledOnSuccess: [subLists[0], subLists[1]]
        },
        {
            name: 'still pages when the bound is disabled',
            bound: 0,
            extraArguments: ',limit:1',
            expectPaged: true,
            titledOnSuccess: [subLists[0]]
        },
        {
            name: 'sanity: still refuses a limit wider than the bound',
            bound: 1,
            extraArguments: ',limit:3',
            expectPaged: false
        },
        {
            name: 'sanity: still refuses with no limit when the match count exceeds the bound',
            bound: 2,
            extraArguments: '',
            expectPaged: false
        }
    ];

    cases.forEach(({name, bound, extraArguments, expectPaged, titledOnSuccess}) => {
        it(name, () => {
            setMutationBatchLimit(bound);
            const marker = name.replace(/[^a-z0-9]+/gi, '-');
            mutateByQuery(marker, extraArguments).then((response: any) => {
                if (expectPaged) {
                    expect(response?.errors ?? [], JSON.stringify(response?.errors)).to.have.length(0);
                } else {
                    expect(response?.errors ?? [], 'expected the request to be refused').to.have.length.greaterThan(0);
                }

                return assertTitled(marker, expectPaged ? titledOnSuccess : []);
            });
        });
    });
});
