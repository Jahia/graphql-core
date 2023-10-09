export function validateNode(
    node,
    expectedName: string,
    expectedUuid?: string,
    expectedPath?: string,
    expectedParentNodePath?: string
) {
    expect(node).to.have.property('name', expectedName);
    if (expectedUuid) {
        expect(node).to.have.property('uuid', expectedUuid);
    }

    if (expectedPath) {
        expect(node).to.have.property('path', expectedPath);
    }

    if (expectedParentNodePath) {
        expect(node).to.have.property('parent.path', expectedParentNodePath);
    }
}
