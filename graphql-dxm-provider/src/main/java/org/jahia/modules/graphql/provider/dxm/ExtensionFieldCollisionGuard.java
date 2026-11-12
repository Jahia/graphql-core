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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Refuses an extensions provider when one of its type extensions adds a field that a type already has.
 * graphql-java-annotations fails the whole schema on such a duplicate field.
 * The field names and the type relations follow OutputObjectBuilder and InterfaceBuilder of graphql-java-annotations.
 */
class ExtensionFieldCollisionGuard {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionFieldCollisionGuard.class);

    private final GraphQLObjectInfoRetriever objectInfoRetriever;
    private final SearchAlgorithm methodSearchAlgorithm;
    private final SearchAlgorithm fieldSearchAlgorithm;
    private final Collection<Class<?>> coreObjectTypes;
    private final Map<Class<?>, Set<String>> declaredFieldNames = new HashMap<>();
    private final Map<Class<?>, Map<String, String>> fieldOwners = new HashMap<>();
    private final Set<Class<?>> objectTypes = new LinkedHashSet<>();

    /**
     * @param coreObjectTypes the object types that implement an interface type and that no provider declares as a specialized type
     */
    ExtensionFieldCollisionGuard(GraphQLObjectInfoRetriever objectInfoRetriever, SearchAlgorithm methodSearchAlgorithm,
                                 SearchAlgorithm fieldSearchAlgorithm, Collection<Class<?>> coreObjectTypes) {
        this.objectInfoRetriever = objectInfoRetriever;
        this.methodSearchAlgorithm = methodSearchAlgorithm;
        this.fieldSearchAlgorithm = fieldSearchAlgorithm;
        this.coreObjectTypes = coreObjectTypes;
    }

    /**
     * Orders the providers by priority: the core provider first, then by bundle symbolic name, then by class name.
     * A provider whose bundle is unknown comes last.
     */
    static List<DXGraphQLExtensionsProvider> prioritized(DXGraphQLExtensionsProvider core, Collection<DXGraphQLExtensionsProvider> providers,
                                                         Function<DXGraphQLExtensionsProvider, String> bundleName) {
        Map<DXGraphQLExtensionsProvider, String> bundleNames = new HashMap<>();
        providers.forEach(provider -> bundleNames.put(provider, bundleName.apply(provider)));
        Comparator<DXGraphQLExtensionsProvider> byBundle = Comparator.comparing(bundleNames::get, Comparator.nullsLast(Comparator.<String>naturalOrder()));
        List<DXGraphQLExtensionsProvider> prioritized = new ArrayList<>();
        prioritized.add(core);
        providers.stream()
                .filter(provider -> provider != core)
                .sorted(byBundle.thenComparing(provider -> provider.getClass().getName()))
                .forEach(prioritized::add);
        return prioritized;
    }

    static String bundleSymbolicName(DXGraphQLExtensionsProvider provider) {
        Bundle bundle = FrameworkUtil.getBundle(provider.getClass());
        return bundle == null ? null : bundle.getSymbolicName();
    }

    /**
     * @param providers the providers by priority: on a duplicate field, the earlier provider keeps the field
     * @return the providers to register, in the same order
     */
    List<DXGraphQLExtensionsProvider> acceptedProviders(List<DXGraphQLExtensionsProvider> providers) {
        fieldOwners.clear();
        objectTypes.clear();
        objectTypes.addAll(coreObjectTypes);
        Set<Class<?>> registeredExtensions = new HashSet<>();
        List<DXGraphQLExtensionsProvider> accepted = new ArrayList<>();
        for (DXGraphQLExtensionsProvider provider : providers) {
            String providerName = describe(provider);
            List<Map.Entry<Class<?>, String>> claims = new ArrayList<>();
            try {
                Set<Class<?>> extensions = newTypeExtensions(provider, registeredExtensions);
                List<Class<?>> specializedTypes = new ArrayList<>(provider.getSpecializedTypes());
                String collision = check(providerName, extensions, specializedTypes, claims);
                if (collision == null) {
                    registeredExtensions.addAll(extensions);
                    objectTypes.addAll(specializedTypes);
                    accepted.add(provider);
                } else {
                    release(claims);
                    logger.error("The GraphQL extensions of {} are not registered: {}", providerName, collision);
                }
            } catch (RuntimeException | LinkageError e) {
                release(claims);
                logger.error("The GraphQL extensions of {} are not registered: they cannot be checked for a duplicate field", providerName, e);
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
     * @return the reason of the first duplicate field, or null when every field of the provider is new
     */
    private String check(String providerName, Collection<Class<?>> extensions, Collection<Class<?>> specializedTypes,
                         List<Map.Entry<Class<?>, String>> claims) {
        String collision = claimFields(providerName, extensions, claims);
        for (Class<?> specializedType : specializedTypes) {
            if (collision == null && !objectTypes.contains(specializedType)) {
                collision = duplicateField(specializedType);
            }
        }
        return collision;
    }

    private String claimFields(String providerName, Collection<Class<?>> extensions, List<Map.Entry<Class<?>, String>> claims) {
        for (Class<?> extension : extensions) {
            Class<?> type = extension.getAnnotation(GraphQLTypeExtension.class).value();
            for (String field : objectFieldNames(extension)) {
                String owner = ownerOf(type, field);
                if (owner != null) {
                    return String.format("its class %s adds the field %s.%s, which %s already declares",
                            extension.getName(), objectInfoRetriever.getTypeName(type), field, owner);
                }
                fieldOwners.computeIfAbsent(type, t -> new HashMap<>()).put(field, providerName);
                claims.add(new AbstractMap.SimpleEntry<>(type, field));
            }
        }
        return null;
    }

    private void release(List<Map.Entry<Class<?>, String>> claims) {
        claims.forEach(claim -> fieldOwners.get(claim.getKey()).remove(claim.getValue()));
    }

    // A specialized type gets its own fields, then the fields added to each interface it implements directly and to itself
    private String duplicateField(Class<?> specializedType) {
        Map<String, String> owners = new HashMap<>();
        String typeName = objectInfoRetriever.getTypeName(specializedType);
        declaredFieldNames(specializedType).forEach(field -> owners.put(field, "the type " + typeName));
        List<Class<?>> sources = new ArrayList<>(interfaceTypes(specializedType));
        sources.add(specializedType);
        for (Class<?> source : sources) {
            for (Map.Entry<String, String> claim : fieldOwners.getOrDefault(source, Collections.emptyMap()).entrySet()) {
                String owner = owners.putIfAbsent(claim.getKey(), claim.getValue());
                if (owner != null) {
                    return String.format("its specialized type %s gets the field %s.%s from %s and from %s",
                            specializedType.getName(), typeName, claim.getKey(), owner, claim.getValue());
                }
            }
        }
        return null;
    }

    private String ownerOf(Class<?> type, String field) {
        if (!isInterfaceType(type)) {
            return ownerOnObjectType(type, field);
        }
        String owner = ownerOnType(type, field);
        return owner != null ? owner : ownerOnImplementingTypes(type, field);
    }

    // An object type also gets the fields added to each interface it implements directly
    private String ownerOnObjectType(Class<?> objectType, String field) {
        String owner = ownerOnType(objectType, field);
        return owner != null ? owner : ownerOnInterfaces(objectType, field);
    }

    private String ownerOnImplementingTypes(Class<?> interfaceType, String field) {
        return knownObjectTypes().stream()
                .filter(objectType -> interfaceTypes(objectType).contains(interfaceType))
                .map(objectType -> ownerOnObjectType(objectType, field))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String ownerOnInterfaces(Class<?> objectType, String field) {
        return interfaceTypes(objectType).stream()
                .map(interfaceType -> fieldOwners.getOrDefault(interfaceType, Collections.emptyMap()).get(field))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String ownerOnType(Class<?> type, String field) {
        if (declaredFieldNames(type).contains(field)) {
            return "the type " + objectInfoRetriever.getTypeName(type);
        }
        return fieldOwners.getOrDefault(type, Collections.emptyMap()).get(field);
    }

    private Set<Class<?>> knownObjectTypes() {
        Set<Class<?>> types = new LinkedHashSet<>(objectTypes);
        fieldOwners.keySet().stream().filter(type -> !isInterfaceType(type)).forEach(types::add);
        return types;
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

    private static String describe(DXGraphQLExtensionsProvider provider) {
        Bundle bundle = FrameworkUtil.getBundle(provider.getClass());
        String name = provider.getClass().getName();
        return bundle == null ? name : String.format("%s (bundle %s %s)", name, bundle.getSymbolicName(), bundle.getVersion());
    }
}
