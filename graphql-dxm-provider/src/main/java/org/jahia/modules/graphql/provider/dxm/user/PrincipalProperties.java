package org.jahia.modules.graphql.provider.dxm.user;

import org.jahia.services.content.decorator.JCRUserNode;

import java.util.Collections;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * The principal property names the schema withholds from the generic {@code property(name:)} accessor.
 *
 * <p>A principal type answers that accessor from the account object's own property map, so the rule is
 * stated here rather than left to whichever session resolved the account.
 */
final class PrincipalProperties {

    /**
     * {@code j:password} holds the account's salted password digest. Every value the schema publishes
     * about a principal is a profile or membership value, and no typed field returns this one.
     */
    private static final Set<String> WITHHELD = Collections.singleton(JCRUserNode.J_PASSWORD);

    private PrincipalProperties() {
    }

    /**
     * Answers the named property of a principal, for the names the schema publishes.
     *
     * @param reader the principal's own property accessor
     * @param name   the requested property name
     * @return the value the reader holds under {@code name}, or {@code null} for a withheld name — the
     *         same answer a name the principal does not carry gets
     */
    static String read(UnaryOperator<String> reader, String name) {
        return WITHHELD.contains(name) ? null : reader.apply(name);
    }
}
