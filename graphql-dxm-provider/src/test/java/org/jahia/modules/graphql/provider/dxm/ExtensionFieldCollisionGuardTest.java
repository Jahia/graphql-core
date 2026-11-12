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
import org.jahia.modules.graphql.provider.dxm.node.GqlJcrNode;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class ExtensionFieldCollisionGuardTest {

    private final GraphQLObjectInfoRetriever objectInfoRetriever = new GraphQLObjectInfoRetriever();
    private final ExtensionFieldCollisionGuard guard = guard(emptyList());

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
    public void refusedProviderReleasesTheFieldsItClaimedBeforeTheCollision() {
        DXGraphQLExtensionsProvider first = provider(PingExtension.class);
        DXGraphQLExtensionsProvider refused = provider(EchoExtension.class, OtherPingExtension.class);
        DXGraphQLExtensionsProvider third = provider(OtherEchoExtension.class);

        assertEquals(asList(first, third), guard.acceptedProviders(asList(first, refused, third)));
    }

    @Test
    public void providerWhoseExtensionsAddTheSameFieldTwiceIsRefused() {
        DXGraphQLExtensionsProvider provider = provider(PingExtension.class, OtherPingExtension.class);

        assertEquals(emptyList(), guard.acceptedProviders(singletonList(provider)));
    }

    @Test
    public void javaFieldOfAnExtensionCollidesLikeAMethod() {
        DXGraphQLExtensionsProvider first = provider(PingExtension.class);
        DXGraphQLExtensionsProvider second = provider(PingFieldExtension.class);

        assertEquals(singletonList(first), guard.acceptedProviders(asList(first, second)));
    }

    @Test
    public void extensionThatAddsAFieldOfTheExtendedTypeIsRefused() {
        DXGraphQLExtensionsProvider provider = provider(NodeImplNameExtension.class);

        assertEquals(emptyList(), guard.acceptedProviders(singletonList(provider)));
    }

    @Test
    public void extensionThatAddsAFieldOfTheExtendedInterfaceIsRefused() {
        DXGraphQLExtensionsProvider provider = provider(NodeNameExtension.class);

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
    public void extensionOfASubInterfaceDoesNotCollideWithAnExtensionOfItsParent() {
        DXGraphQLExtensionsProvider first = provider(SubNodeTagExtension.class);
        DXGraphQLExtensionsProvider second = provider(NodeTagExtension.class);

        assertEquals(asList(first, second), guard.acceptedProviders(asList(first, second)));
    }

    @Test
    public void extensionsOfTwoInterfacesOfOneKnownTypeCollide() {
        DXGraphQLExtensionsProvider first = provider(NodeTagExtension.class);
        DXGraphQLExtensionsProvider second = provider(OtherInterfaceTagExtension.class);

        assertEquals(singletonList(first), guard(singletonList(StubDualNode.class)).acceptedProviders(asList(first, second)));
    }

    @Test
    public void extensionOfAnInterfaceCollidesWithAFieldOfAKnownImplementingType() {
        DXGraphQLExtensionsProvider provider = provider(NodeAgeExtension.class);

        assertEquals(emptyList(), guard(singletonList(StubAgedNode.class)).acceptedProviders(singletonList(provider)));
    }

    @Test
    public void extensionOfAnInterfaceCollidesWithAFieldOfAnEarlierSpecializedType() {
        DXGraphQLExtensionsProvider first = specializedTypesProvider(StubSite.class);
        DXGraphQLExtensionsProvider second = provider(SiteKeyExtension.class);

        assertEquals(singletonList(first), guard.acceptedProviders(asList(first, second)));
    }

    @Test
    public void specializedTypeCollidesWithAnEarlierExtensionOfItsInterface() {
        DXGraphQLExtensionsProvider first = provider(SiteKeyExtension.class);
        DXGraphQLExtensionsProvider second = specializedTypesProvider(StubSite.class);

        assertEquals(singletonList(first), guard.acceptedProviders(asList(first, second)));
    }

    @Test
    public void sameExtensionClassFromTwoProvidersIsNotACollision() {
        DXGraphQLExtensionsProvider first = provider(PingExtension.class);
        DXGraphQLExtensionsProvider second = provider(PingExtension.class, EchoExtension.class);

        assertEquals(asList(first, second), guard.acceptedProviders(asList(first, second)));
    }

    @Test
    public void providerThatCannotBeCheckedIsRefused() {
        DXGraphQLExtensionsProvider failing = new DXGraphQLExtensionsProvider() {
            @Override
            public Collection<Class<?>> getExtensions() {
                throw new IllegalStateException("cannot list the extensions");
            }
        };
        DXGraphQLExtensionsProvider next = provider(PingExtension.class);

        assertEquals(singletonList(next), guard.acceptedProviders(asList(failing, next)));
    }

    @Test
    public void prioritizedPutsTheCoreFirstThenOrdersByBundleNameThenByClassName() {
        DXGraphQLExtensionsProvider core = provider();
        DXGraphQLExtensionsProvider zebraInBundleB = new ZebraProvider();
        DXGraphQLExtensionsProvider alphaInBundleB = new AlphaProvider();
        DXGraphQLExtensionsProvider inBundleA = new ZebraProvider();
        DXGraphQLExtensionsProvider withoutBundle = new AlphaProvider();
        Map<DXGraphQLExtensionsProvider, String> bundleNames = new HashMap<>();
        bundleNames.put(zebraInBundleB, "b");
        bundleNames.put(alphaInBundleB, "b");
        bundleNames.put(inBundleA, "a");

        List<DXGraphQLExtensionsProvider> prioritized = ExtensionFieldCollisionGuard.prioritized(core,
                asList(withoutBundle, zebraInBundleB, inBundleA, alphaInBundleB), bundleNames::get);

        assertEquals(asList(core, inBundleA, alphaInBundleB, zebraInBundleB, withoutBundle), prioritized);
    }

    private ExtensionFieldCollisionGuard guard(Collection<Class<?>> coreObjectTypes) {
        return new ExtensionFieldCollisionGuard(objectInfoRetriever, new BreadthFirstSearch(objectInfoRetriever),
                new ParentalSearch(objectInfoRetriever), coreObjectTypes);
    }

    private static DXGraphQLExtensionsProvider provider(Class<?>... extensions) {
        return new DXGraphQLExtensionsProvider() {
            @Override
            public Collection<Class<?>> getExtensions() {
                return asList(extensions);
            }

            @Override
            public Collection<Class<? extends GqlJcrNode>> getSpecializedTypes() {
                return emptyList();
            }
        };
    }

    private static DXGraphQLExtensionsProvider specializedTypesProvider(Class<? extends GqlJcrNode> specializedType) {
        return new DXGraphQLExtensionsProvider() {
            @Override
            public Collection<Class<?>> getExtensions() {
                return emptyList();
            }

            @Override
            public Collection<Class<? extends GqlJcrNode>> getSpecializedTypes() {
                return singletonList(specializedType);
            }
        };
    }

    public static class AlphaProvider implements DXGraphQLExtensionsProvider {
    }

    public static class ZebraProvider implements DXGraphQLExtensionsProvider {
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
    public static class PingFieldExtension {
        @GraphQLField
        public String ping = "second";
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

    public static class StubTypeResolver implements TypeResolver {
        @Override
        public GraphQLObjectType getType(TypeResolutionEnvironment env) {
            return null;
        }
    }

    @GraphQLTypeResolver(StubTypeResolver.class)
    public interface StubNode {
        @GraphQLField
        String getName();
    }

    @GraphQLTypeResolver(StubTypeResolver.class)
    public interface StubSubNode extends StubNode {
    }

    @GraphQLTypeResolver(StubTypeResolver.class)
    public interface StubOtherInterface {
    }

    public static class StubNodeImpl implements StubNode {
        @Override
        public String getName() {
            return "node";
        }
    }

    public static class StubDualNode implements StubNode, StubOtherInterface {
        @Override
        public String getName() {
            return "dual";
        }
    }

    public static class StubAgedNode implements StubNode {
        @Override
        public String getName() {
            return "aged";
        }

        @GraphQLField
        public int getAge() {
            return 1;
        }
    }

    @GraphQLName("StubSite")
    public abstract static class StubSite implements GqlJcrNode {
        @GraphQLField
        public String getSiteKey() {
            return "site";
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
    public static class NodeNameExtension {
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

    @GraphQLTypeExtension(StubSubNode.class)
    public static class SubNodeTagExtension {
        @GraphQLField
        public String getTag() {
            return "sub";
        }
    }

    @GraphQLTypeExtension(StubOtherInterface.class)
    public static class OtherInterfaceTagExtension {
        @GraphQLField
        public String getTag() {
            return "other";
        }
    }

    @GraphQLTypeExtension(StubNode.class)
    public static class NodeAgeExtension {
        @GraphQLField
        public int getAge() {
            return 2;
        }
    }

    @GraphQLTypeExtension(GqlJcrNode.class)
    public static class SiteKeyExtension {
        @GraphQLField
        public String getSiteKey() {
            return "extension";
        }
    }
}
