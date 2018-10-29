# GraphQL DXM API

## Generalities on DX GraphQL Schema

### Internationalization

The API gives a global view of the nodes and allows to access multiple languages at the same time. As opposed to JCR/REST API, there’s no concept of locale in a session - a query can ask properties values in english and french at the same time. When a language is required for resolving a specific field like a property, it will be passed as a language argument.

### Authentication and Authorization

GraphQL uses HTTP and relies on standard DX authentication valve to get the current user. 
Based on the user permissions, some graphQL fields may or may not be allowed. 

Security filter

### Relay standard support

When a field returns a potentially long list of items, which may need to be paginated, it can return a Connection object, as defined in [https://facebook.github.io/relay/graphql/connections.htm](https://facebook.github.io/relay/graphql/connections.htm). 
Connection support cursor-based and/or offset-based pagination. Note that in the relay model, items are called “nodes”, which can be confusing with jcr nodes - here “nodes” can be any type of objects.

Any field returning a connection accepts the standard arguments : `first`,`after`,`last`, `before`, `afterOffset`, as described in [https://facebook.github.io/relay/graphql/connections.htm#sec-Arguments](https://facebook.github.io/relay/graphql/connections.htm#sec-Arguments) 

`afterOffset` / `beforeOffset` are added to be able use offset-based pagination and go directly to a specific page. They are used the same way as after / before, but specifying an offset instead of a cursor.
In this document, all these arguments will be referred as : `(...connection arguments...)`

The PageInfo type is common to all connections, and is defined as :
```
# Information about pagination in a connection.
type PageInfo {
 # When paginating forwards, are there more items?
 hasNextPage: Boolean!

 # When paginating backwards, are there more items?
 hasPreviousPage: Boolean!

 # When paginating backwards, the cursor to continue.
 startCursor: String

 # When paginating forwards, the cursor to continue.
 endCursor: String

 # Number of nodes in the current page
 nodesCount: Int

 # The total number of items in the connection
 totalCount: Int
}
```

nodesCount return the number of items in the current page, totalNodesCount return the total number of items in the list (may be null if not available).

All `Connection` and `Edge` types follow the following template :

```
type XxxConnection {
	edges: [XxxEdge]
	pageInfo: PageInfo
	nodes: [Xxx]
}

type XxxEdge {
	node: Xxx
	cursor: String
	offset: Int
}
```

The `nodes` field is an additional non-standard shortcut to `edges { node }`. It directly returns the list of items, which is usually enough even for pagination as you have `startCursor`/`startOffset` in the `PageInfo` object. `offset` in `Edge` return the offset of this particular edge.
Connection types won’t be described everytime in the schema and are assumed to follow this schema, when a field return a `...Connection` type.

### Field filtering and sorting

Some fields returning a Connection or a list of items can take a fieldFilter and/or a fieldSorter parameter. The fieldFilter allows to filter the list of results based on any GraphQL field (or a combination of fields) available on the current type.

The fieldSorter allows to sort by the value of a field.

## Base JCR schema 

### Generic JCR nodes representation

All JCR nodes are represented by a graphQL `JCRNode` interface type. Base properties (`uuid`, `name`, `path`) are available as fields. Parent can be accessed through a dedicated field. 

A base implementation `GenericJCRNode` of the `JCRNode` interface is provided. Other implementation, more specialized can be added by other modules.

Properties can be accessed by the `properties` fields. An optional `names` argument allows to filter the requested properties by name - otherwise, all properties are returned.

Children nodes are accessed with the `nodes` field. They can be filtered by using optional arguments : 
- names : only nodes matching one of the provided names pattern will be returned.
- type : test the node types of the node. Only node with any/all of the specified type will be returned.
- property : test property existence/value. Only nodes having a specific property, or having a specific value for this property, will be returned.

Descendant nodes can be accessed with the `descendants` field. The descendants can be filtered by type or property, as children.

`ancestors` return the list of all ancestors, from root to the direct parent. If `upToPath` is specified, the returned list will start at this node instead of root node.

Children and descendants fields use the Connection pagination model.

### Queries
All JCR queries are done within a JCRQuery object, which defines in which workspace the operations are done. The provider adds the following fields for Query : 

```
extends type Query {
 # JCR Queries
 jcr(
   # The name of the workspace to fetch the node from; either 'edit', 'live', or null to use 'edit' by default
   workspace: Workspace
 ): JCRQuery
}
```

The `JCRQuery` type contains fields to get nodes by id, path, query, ...

### Mutations

Mutations are provided to update or create nodes. Nested mutations can be used to do different operations on nodes and properties, which can then be easily extended. 

At the first level, all JCR operations are done inside a `JCRMutation` object - a `session.save()` is done once all nested mutations have been resolved. 

JCRMutation objects provide fields to do operations at JCR session level : 
- Adds new node with `addNode` field. This returns a `JCRNodeMutation` object, on which subsequent operations can be done on the added node.
- Adds multiple nodes in batch by passing a full JSON structure in input to `addNodesBatch` field. The same thing can be achieved by using `addNode` field and sub fields multiple times, but this one provides the ability to create a node and set its properties, mixin types and optional sub nodes by passing a single JSON object.
- Select existing nodes for edition with `mutateNode`, `mutateNodes`, and `mutateNodesByQuery` fields. These mutations fields returns a `JCRNodeMutation` object (or a list of `JCRNodeMutation`).
- `delete` / `markForDeletion` / `unmarkForDeletion` with `deleteNode`, `markNodeForDeletion` and `unmarkNodeForDeletion` fields
These fields take a pathOrId (parentPathOrId) parameter when needed, which can be used indifferently as an absolute path or a node uuid.

The `JCRNodeMutation` contains operations that can be done on a JCR node. The base API provides fields to edit properties, children, mixin, and also move, delete or rename the node - but it can be extended in other modules to do more complex operations like publication, versioning, locking, or custom operation. 

- Edit properties with `mutateProperty` and `mutateProperties` fields, which return a `JCRPropertyMutation` object (or a list of `JCRPropertyMutation` objects)
- Edit descendants with `mutateDescendant`, `mutateChildren` and `mutateDescendants` fields, which return a `JCRNodeMutation` object on the sub node.
- Add children with `addChild`, similar to the `addNode` at `JCRMutation` level, without specifying the parent
- Add children in batch can also be done with `addChildrenBatch` , like the `addNodesBatch` at `JCRMutation` level.
- Add/remove mixin with `addMixins` / `removeMixins` fields
- Move or rename the node with `move` / `rename` fields
- Delete, mark for deletion or unmark for deletion with `delete` / `markForDeletion` / `unmarkForDeletion` fields
- Reorder children with `reorderChildren` field, based on a list of names. Children not listed in parameter are ignored in the reordering.
- Add / set properties in batch with `setPropertiesBatch`. If properties were already existing, the value is entirely replaced with the one passed in parameter

Note that most operations at `JCRMutation` levels are shortcuts : 
- `addNode` is equivalent to `JCRMutation.mutateNode.addChild`
- `deleteNode` is equivalent to `JCRMutation.mutateNode.delete`
- `undeleteNode` is equivalent to `JCRMutation.mutateNode.undelete`

The JCRPropertyMutation contains operations on properties. Fields are provided to replace value(s), add/remove value(s) to multi-valued properties, or remove the property.

### Node types

Nodetypes definitions have their equivalent in graphQL schema, allowing to query any metadata of a nodetype. The types maps the properties of the ExtendedNodeType / ExtendedItemDefinition / ExtendedNodeDefinition / ExtendedPropertyDefinition classes. Fields are available on node / properties :

```
extend type JCRNode {
	primaryNodeType: JCRNodeType!
	mixinTypes: [JCRNodeType]
	isNodeType(type: NodeTypeCriteria!): boolean
	definition: JCRNodeDefinition
}
```

```
extend type JCRProperty {
	definition: JCRPropertyDefinition
}
```


Properties and child node definitions can be queried by name - if no name is passed as argument, all items are returned. Using * as name will return all unstructured item definitions.

### Query nodes

A predefined set of input object allows to create a comprehensive query, without the need of creating a JCR-SQL2 query. They are used with the `nodesByQuery` field on JCRQuery : 

```
extend type Query {
	nodesByQuery(queryInput: JCRNodesQueryInput, ...connection arguments...): JCRNodeConnection
}
```

#### Examples

Query nodes by property value and base path :

```
nodesByQuery(queryInput:{
    nodeType:”jnt:bigText”, 
    basePaths:[“/sites/digitall/home”], 
    language:”en”,
    constraint:{property:”text”, equals: “test”}
})
```
is equivalent to :
```SELECT * from [jnt:bigText] where isdescendantnode(“/sites/digitall/home”) and [text]=”test”```

Query nodes by node name and direct parent path:
```
nodesByQuery(queryInput:{
    nodeType:”jnt:bigText”, 
    basePaths:[“/sites/digitall/home”], 
    includeDescendants: false
    constraint:{function: NODE_NAME, equals: “test”}
})
```
is equivalent to :
```
SELECT * from [jnt:bigText] where ischildnode(“/sites/digitall/home”) and name()=”test”
```

Query nodes by property value with lower case modifier and multiple base paths :

```
nodesByQuery(queryInput:{
    nodeType:”jnt:bigText”, 
    basePaths:[“/sites/digitall/home/about”, “/sites/digitall/home/news”], 
    constraint:{property:”author”, function: LOWER_CASE, equals: “user”}
})
```

is equivalent to :
```
SELECT * from [jnt:bigText] where (isdescendantnode(“/sites/digitall/home/about”) or isdescendantnode(“/sites/digitall/home/news”)) and lowercase(“author”)=”user”
```

Query nodes by fulltext on a property :
```
nodesByQuery(queryInput:{
    nodeType:”jnt:bigText”, 
    language:”en”,
    constraint:{property:”text”, contains: “test”}
})
```
is equivalent to :
```
SELECT * from [jnt:bigText] where contains(“text”,”test”)
```

Query nodes by fulltext on any property :

```
nodesByQuery(queryInput:{
    nodeType:”jnt:bigText”, 
    language:”en”,
    constraint:{contains: “test”}
})
```

is equivalent to :
```
SELECT * from [jnt:bigText] contains(“*”,”test”)
```

Query nodes with multiple constraints :
```
nodesByQuery(queryInput:{
    nodeType:”jnt:bigText”, 
    language:”en”,
    constraint:{ 
        any: [ 
            {function:NODE_NAME, equals: “test”},
            {property:”text”, contains: “test”}
        ]
    }
})
```
is equivalent to :
```
SELECT * from [jnt:bigText] where (name()=”test” or [text]=”test”)
```

