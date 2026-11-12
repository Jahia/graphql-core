package org.jahia.modules.graphql.provider.dxm;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import graphql.annotations.annotationTypes.GraphQLTypeResolver;
import graphql.annotations.processor.retrievers.GraphQLObjectInfoRetriever;
import graphql.annotations.processor.retrievers.fieldBuilders.field.FieldNameBuilder;
import graphql.annotations.processor.retrievers.fieldBuilders.method.MethodNameBuilder;
import graphql.annotations.processor.searchAlgorithms.SearchAlgorithm;
import graphql.annotations.processor.util.ObjectUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Refuses an extensions provider when one of its type extensions adds a field that the extended type, or an earlier
 * provider, already declares: graphql-java-annotations fails the whole schema on such a duplicate field.
 * The field names and the type relations follow OutputObjectBuilder and InterfaceBuilder of graphql-java-annotations.
 */
class ExtensionFieldCollisionGuard {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionFieldCollisionGuard.class);

    private final GraphQLObjectInfoRetriever objectInfoRetriever;
    private final SearchAlgorithm methodSearchAlgorithm;
    private final SearchAlgorithm fieldSearchAlgorithm;
    private final Map<Class<?>, Set<String>> declaredFieldNames = new HashMap<>();

    ExtensionFieldCollisionGuard(GraphQLObjectInfoRetriever objectInfoRetriever, SearchAlgorithm methodSearchAlgorithm,
                                 SearchAlgorithm fieldSearchAlgorithm) {
        this.objectInfoRetriever = objectInfoRetriever;
        this.methodSearchAlgorithm = methodSearchAlgorithm;
        this.fieldSearchAlgorithm = fieldSearchAlgorithm;
    }

    /**
     * @param providers the providers by priority: on a duplicate field, the earlier provider keeps the field
     * @return the providers to register, in the same order
     */
    List<DXGraphQLExtensionsProvider> acceptedProviders(List<DXGraphQLExtensionsProvider> providers) {
        Map<Class<?>, Map<String, String>> fieldOwners = new HashMap<>();
        Set<Class<?>> registeredExtensions = new HashSet<>();
        List<DXGraphQLExtensionsProvider> accepted = new ArrayList<>();
        for (DXGraphQLExtensionsProvider provider : providers) {
            String providerName = describe(provider);
            Map<Class<?>, Map<String, String>> owners = copyOf(fieldOwners);
            try {
                Set<Class<?>> extensions = newTypeExtensions(provider, registeredExtensions);
                String collision = claimFields(providerName, extensions, owners);
                if (collision == null) {
                    fieldOwners = owners;
                    registeredExtensions.addAll(extensions);
                    accepted.add(provider);
                } else {
                    logger.error("The GraphQL extensions of {} are not registered: {}", providerName, collision);
                }
            } catch (RuntimeException | LinkageError e) {
                logger.warn("Unable to check the GraphQL extensions of {} for a duplicate field", providerName, e);
                accepted.add(provider);
            }
        }
        return accepted;
    }

    // Two providers of one bundle can return the same class, which graphql-java-annotations registers once
    private static Set<Class<?>> newTypeExtensions(DXGraphQLExtensionsProvider provider, Set<Class<?>> registeredExtensions) {
        Set<Class<?>> extensions = new LinkedHashSet<>();
        for (Class<?> extension : provider.getExtensions()) {
            if (extension.isAnnotationPresent(GraphQLTypeExtension.class) && !registeredExtensions.contains(extension)) {
                extensions.add(extension);
            }
        }
        return extensions;
    }

    /**
     * @return the reason of the first duplicate field, or null when every field of the extensions is new
     */
    private String claimFields(String providerName, Collection<Class<?>> extensions, Map<Class<?>, Map<String, String>> owners) {
        for (Class<?> extension : extensions) {
            Class<?> type = extension.getAnnotation(GraphQLTypeExtension.class).value();
            for (String field : objectFieldNames(extension)) {
                String owner = ownerOf(type, field, owners);
                if (owner != null) {
                    return String.format("its class %s adds the field %s.%s, which %s already declares",
                            extension.getName(), objectInfoRetriever.getTypeName(type), field, owner);
                }
                owners.computeIfAbsent(type, t -> new HashMap<>()).put(field, providerName);
            }
        }
        return null;
    }

    // An object type also gets the extension fields of each interface it implements directly
    private String ownerOf(Class<?> type, String field, Map<Class<?>, Map<String, String>> owners) {
        String owner = ownerOnType(type, field, owners);
        if (owner == null) {
            owner = isInterfaceType(type) ? ownerOnImplementingTypes(type, field, owners) : ownerOnInterfaces(type, field, owners);
        }
        return owner;
    }

    private String ownerOnImplementingTypes(Class<?> interfaceType, String field, Map<Class<?>, Map<String, String>> owners) {
        return owners.keySet().stream()
                .filter(objectType -> interfaceTypes(objectType).contains(interfaceType))
                .map(objectType -> ownerOnType(objectType, field, owners))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static String ownerOnInterfaces(Class<?> objectType, String field, Map<Class<?>, Map<String, String>> owners) {
        return interfaceTypes(objectType).stream()
                .map(interfaceType -> owners.getOrDefault(interfaceType, Collections.emptyMap()).get(field))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String ownerOnType(Class<?> type, String field, Map<Class<?>, Map<String, String>> owners) {
        if (declaredFieldNames(type).contains(field)) {
            return "the type " + objectInfoRetriever.getTypeName(type);
        }
        return owners.getOrDefault(type, Collections.emptyMap()).get(field);
    }

    private Set<String> declaredFieldNames(Class<?> type) {
        return declaredFieldNames.computeIfAbsent(type,
                t -> isInterfaceType(t) ? interfaceFieldNames(t) : new HashSet<>(objectFieldNames(t)));
    }

    private Set<String> interfaceFieldNames(Class<?> type) {
        Set<String> names = new HashSet<>();
        for (Method method : objectInfoRetriever.getOrderedMethods(type)) {
            if (!Modifier.isStatic(method.getModifiers()) && method.getAnnotation(GraphQLField.class) != null) {
                names.add(new MethodNameBuilder(method).alwaysPrettify(true).build());
            }
        }
        return names;
    }

    private List<String> objectFieldNames(Class<?> type) {
        List<String> names = new ArrayList<>();
        for (Method method : objectInfoRetriever.getOrderedMethods(type)) {
            if (!method.isBridge() && !method.isSynthetic() && methodSearchAlgorithm.isFound(method)) {
                names.add(new MethodNameBuilder(method).alwaysPrettify(true).build());
            }
        }
        for (Field field : ObjectUtil.getAllFields(type).values()) {
            if (!Modifier.isStatic(field.getModifiers()) && fieldSearchAlgorithm.isFound(field)) {
                names.add(new FieldNameBuilder(field).alwaysPrettify(true).build());
            }
        }
        return names;
    }

    private static boolean isInterfaceType(Class<?> type) {
        return type.isAnnotationPresent(GraphQLTypeResolver.class);
    }

    private static List<Class<?>> interfaceTypes(Class<?> type) {
        return Arrays.stream(type.getInterfaces()).filter(ExtensionFieldCollisionGuard::isInterfaceType).collect(Collectors.toList());
    }

    private static Map<Class<?>, Map<String, String>> copyOf(Map<Class<?>, Map<String, String>> fieldOwners) {
        Map<Class<?>, Map<String, String>> copy = new HashMap<>();
        fieldOwners.forEach((type, owners) -> copy.put(type, new HashMap<>(owners)));
        return copy;
    }

    private static String describe(DXGraphQLExtensionsProvider provider) {
        Bundle bundle = FrameworkUtil.getBundle(provider.getClass());
        String name = provider.getClass().getName();
        return bundle == null ? name : String.format("%s (bundle %s %s)", name, bundle.getSymbolicName(), bundle.getVersion());
    }
}
