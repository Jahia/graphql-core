import gql from 'graphql-tag';

/**
 * A finder argument is a value, not a fragment of the query: whatever characters it holds —
 * apostrophes included — it is compared as the text it contains.
 *
 * Each case expecting an empty result queries the plain value first and requires the marker back,
 * so an empty result means the argument was handled rather than the fixture being absent.
 *
 * jcr:title is i18n, so the field carries its own language argument: the finder's language sets the
 * query session's locale, it does not re-resolve the node the property is then read from.
 */
describe('SDL finder argument handling', () => {
    const PLAIN = 'sdlLit plain marker';
    const QUOTED = 'sdlLit O\'Brien marker';
    const OUT_OF_RANGE = 'sdlLit out of range marker';
    const IN_RANGE_BOUND = '2020-01-01T00:00:00.000+00:00';

    const byTitle = (args: string) => gql`
        query {
            myNewsByTitle(${args}) {
                title(language: "en")
            }
        }
    `;

    const byDate = (args: string) => gql`
        query {
            myNewsByDate(${args}) {
                title(language: "en")
            }
        }
    `;

    /**
     * Returns the titles the finder selected. An argument the query engine cannot make sense of
     * is reported as an error and yields no data — that is a valid outcome here, since the point
     * of every negative case below is that the node set was NOT widened.
     */
    const titlesOf = (response, field: string): string[] =>
        (response?.data?.[field] || []).map(node => node.title);

    const queryTitles = (args: string) =>
        cy.apollo({errorPolicy: 'all', query: byTitle(args)}).then(response => titlesOf(response, 'myNewsByTitle'));

    const queryDates = (args: string) =>
        cy.apollo({errorPolicy: 'all', query: byDate(args)}).then(response => titlesOf(response, 'myNewsByDate'));

    before('create the test content', () => {
        cy.executeGroovy('groovy/prepareSDLFinderLiteralTest.groovy', {});
    });

    after('clean up the test content', () => {
        cy.apollo({
            errorPolicy: 'all',
            mutation: gql`
                mutation {
                    jcr {
                        mutateNodes(pathsOrIds: ["/sites/systemsite/contents/sdlFinderLiteralTest"]) {
                            delete
                        }
                    }
                }
            `
        });
        cy.apollo({
            errorPolicy: 'all',
            mutation: gql`
                mutation {
                    jcr {
                        mutateNodes(pathsOrIds: ["/sites/systemsite"]) {
                            publish
                        }
                    }
                }
            `
        });
    });

    it('matches a title containing an apostrophe', () => {
        queryTitles('equals: "sdlLit O\'Brien marker"').should('deep.equal', [QUOTED]);
    });

    it('keeps an equals query to its literal match when the value carries quote characters', () => {
        // Anchor: the plain value does select the node
        queryTitles(`equals: "${PLAIN}"`).should('deep.equal', [PLAIN]);

        // No title equals the quote-carrying string, so nothing may come back — least of all the
        // three markers a widened node set would sweep in
        queryTitles('equals: "sdlLit plain marker\' OR [jcr:title] LIKE \'%"').then(titles => {
            expect(titles).to.not.include(PLAIN);
            expect(titles).to.not.include(QUOTED);
            expect(titles).to.not.include(OUT_OF_RANGE);
        });
    });

    it('keeps a contains query to its literal match when the value carries quote characters', () => {
        // Anchor: the plain terms do select the node
        queryTitles('contains: "plain marker"').should('include', PLAIN);

        queryTitles('contains: "marker\') and n.[jcr:title] like \'%\' and contains(n.[jcr:title],\'marker"').should(
            'not.include',
            PLAIN
        );
    });

    it('selects only the nodes inside the requested date range', () => {
        queryDates(`after: "${IN_RANGE_BOUND}"`).then(titles => {
            expect(titles).to.include(PLAIN);
            expect(titles).to.include(QUOTED);
            expect(titles).to.not.include(OUT_OF_RANGE);
        });
    });

    it('keeps a date range to its bound when the bound carries quote characters', () => {
        // Anchor: the plain bound does select the in-range nodes and excludes the 1999 one
        queryDates(`after: "${IN_RANGE_BOUND}"`).then(titles => {
            expect(titles).to.include(PLAIN);
            expect(titles).to.not.include(OUT_OF_RANGE);
        });

        // The node dated 1999 sits outside the bound: it may only appear if the bound stopped
        // being a bound
        queryDates(`after: "${IN_RANGE_BOUND}' AS DATE) OR [jcr:title] IS NOT NULL OR ('"`).should(
            'not.include',
            OUT_OF_RANGE
        );
    });
});
