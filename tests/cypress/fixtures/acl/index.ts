export function grantUserRole(pathOrId, roleName, userName) {
    return grantRoles({pathOrId, roles: [roleName], pType: 'USER', pName: userName});
}

function grantRoles(apiParams) {
    return cy
        .apollo({
            mutationFile: 'acl/grantRoles.graphql',
            variables: apiParams
        })
        .should(resp => {
            expect(resp?.data?.jcr?.mutateNode?.grantRoles, 'Grant role request OK').to.be.true;
        });
}

export function revokeUserRole(pathOrId, roleName, userName) {
    return revokeRoles({pathOrId, roles: [roleName], pType: 'USER', pName: userName});
}

function revokeRoles(apiParams) {
    return cy
        .apollo({
            mutationFile: 'acl/revokeRoles.graphql',
            variables: apiParams
        })
        .should(resp => {
            expect(resp?.data?.jcr?.mutateNode?.revokeRoles, 'Revoke role request OK').to.be.true;
        });
}

export function getAclEntries(path, principalFilter = null, inclInherited = true) {
    return cy.apollo({
        queryFile: 'acl/getAclEntries.graphql',
        variables: {path, principalFilter, inclInherited}
    });
}
