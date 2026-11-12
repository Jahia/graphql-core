package org.jahia.modules.graphql.provider.dxm;

import graphql.TypeResolutionEnvironment;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.annotations.annotationTypes.GraphQLTypeResolver;
import graphql.annotations.processor.retrievers.GraphQLObjectInfoRetriever;
import graphql.annotations.processor.searchAlgorithms.BreadthFirstSearch;
import graphql.annotations.processor.searchAlgorithms.ParentalSearch;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import org.junit.Test;

import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class ExtensionFieldCollisionGuardTest {

    private final GraphQLObjectInfoRetriever objectInfoRetriever = new GraphQLObjectInfoRetriever();
    private final ExtensionFieldCollisionGuard guard = new ExtensionFieldCollisionGuard(objectInfoRetriever,
            new BreadthFirstSearch(objectInfoRetriever), new ParentalSearch(objectInfoRetriever));

    @Test
    public void providersWithDistinctFieldsAreAllAccepted() {
        DXGraphQLExtensionsProvider first = provider(PingExtension.class);
        DXGraphQLExtensionsProvider second = provider(EchoExtension.class);

        assertEquals(asList(first, second), guard.acceptedProviders(asList(first, second)));
    }

    @Test
    public void laterProviderThatAddsTheSameFieldIsRefusedWithAllItsExtensions() {
        DXGraphQLExtensionsProvider first = provider(PingExtension.class);
        DXGraphQLExtensionsProvider second = provider(OtherPingExtension.class, EchoExtension.class);

        assertEquals(singletonList(first), guard.acceptedProviders(asList(first, second)));
    }

    @Test
    public void refusedProviderLeavesItsOtherFieldsFree() {
        DXGraphQLExtensionsProvider first = provider(PingExtension.class);
        DXGraphQLExtensionsProvider refused = provider(OtherPingExtension.class, EchoExtension.class);
        DXGraphQLExtensionsProvider third = provider(OtherEchoExtension.class);

        assertEquals(asList(first, third), guard.acceptedProviders(asList(first, refused, third)));
    }

    @Test
    public void providerWhoseExtensionsAddTheSameFieldTwiceIsRefused() {
        DXGraphQLExtensionsProvider provider = provider(PingExtension.class, OtherPingExtension.class);

        assertEquals(emptyList(), guard.acceptedProviders(singletonList(provider)));
    }

    @Test
    public void extensionThatAddsAFieldOfTheExtendedTypeIsRefused() {
        DXGraphQLExtensionsProvider provider = provider(NodeImplNameExtension.class);

        assertEquals(emptyList(), guard.acceptedProviders(singletonList(provider)));
    }

    @Test
    public void extensionOfATypeCollidesWithAnExtensionOfItsInterface() {
        DXGraphQLExtensionsProvider first = provider(NodeTagExtension.class);
        DXGraphQLExtensionsProvider second = provider(NodeImplTagExtension.class);

        assertEquals(singletonList(first), guard.acceptedProviders(asList(first, second)));
    }

    @Test
    public void extensionOfAnInterfaceCollidesWithAnExtensionOfAnImplementingType() {
        DXGraphQLExtensionsProvider first = provider(NodeImplTagExtension.class);
        DXGraphQLExtensionsProvider second = provider(NodeTagExtension.class);

        assertEquals(singletonList(first), guard.acceptedProviders(asList(first, second)));
    }

    @Test
    public void sameExtensionClassFromTwoProvidersIsNotACollision() {
        DXGraphQLExtensionsProvider first = provider(PingExtension.class);
        DXGraphQLExtensionsProvider second = provider(PingExtension.class, EchoExtension.class);

        assertEquals(asList(first, second), guard.acceptedProviders(asList(first, second)));
    }

    @Test
    public void providerThatCannotBeCheckedIsKept() {
        DXGraphQLExtensionsProvider failing = new DXGraphQLExtensionsProvider() {
            @Override
            public Collection<Class<?>> getExtensions() {
                throw new IllegalStateException("cannot list the extensions");
            }
        };

        assertEquals(singletonList(failing), guard.acceptedProviders(singletonList(failing)));
    }

    private static DXGraphQLExtensionsProvider provider(Class<?>... extensions) {
        return new DXGraphQLExtensionsProvider() {
            @Override
            public Collection<Class<?>> getExtensions() {
                return asList(extensions);
            }
        };
    }

    @GraphQLName("StubQuery")
    public static class StubQuery {
    }

    @GraphQLTypeExtension(StubQuery.class)
    public static class PingExtension {
        @GraphQLField
        public static String ping() {
            return "first";
        }
    }

    @GraphQLTypeExtension(StubQuery.class)
    public static class OtherPingExtension {
        @GraphQLField
        @GraphQLName("ping")
        public static String otherPing() {
            return "second";
        }
    }

    @GraphQLTypeExtension(StubQuery.class)
    public static class EchoExtension {
        @GraphQLField
        public static String echo() {
            return "first";
        }
    }

    @GraphQLTypeExtension(StubQuery.class)
    public static class OtherEchoExtension {
        @GraphQLField
        public static String getEcho() {
            return "second";
        }
    }

    @GraphQLTypeResolver(StubNodeTypeResolver.class)
    public interface StubNode {
        @GraphQLField
        String getName();
    }

    public static class StubNodeImpl implements StubNode {
        @Override
        public String getName() {
            return "node";
        }
    }

    public static class StubNodeTypeResolver implements TypeResolver {
        @Override
        public GraphQLObjectType getType(TypeResolutionEnvironment env) {
            return null;
        }
    }

    @GraphQLTypeExtension(StubNodeImpl.class)
    public static class NodeImplNameExtension {
        @GraphQLField
        public String getName() {
            return "extension";
        }
    }

    @GraphQLTypeExtension(StubNode.class)
    public static class NodeTagExtension {
        @GraphQLField
        public String getTag() {
            return "first";
        }
    }

    @GraphQLTypeExtension(StubNodeImpl.class)
    public static class NodeImplTagExtension {
        @GraphQLField
        public String getTag() {
            return "second";
        }
    }
}
