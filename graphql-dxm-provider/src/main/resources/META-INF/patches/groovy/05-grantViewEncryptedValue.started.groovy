import org.jahia.services.content.JCRCallback
import org.jahia.services.content.JCRPropertyWrapper
import org.jahia.services.content.JCRSessionWrapper
import org.jahia.services.content.JCRTemplate

import javax.jcr.RepositoryException

/**
 * Grants the viewEncryptedValue permission, which this module declares, to the roles that administer the
 * platform, so that those roles read the decrypted value of an encrypted property.
 *
 * Runs on bundle start (the ".started" suffix) so it reaches EXISTING installs too: the permission arrives
 * with this module, and no role carries a permission until something grants it.
 *
 * The grant is a list of role paths, and each one is handled the same way. A path this installation does not
 * hold is skipped, because an operator who removed a role chose to. system-administrator holds its
 * permissions for the repository root in the root-access child, which is why that path is named as well.
 *
 * Idempotent: a role that already carries the permission is left untouched.
 */
def grantViewEncryptedValue() {
    def permission = "viewEncryptedValue"
    def rolePaths = [
            "/roles/server-administrator",
            "/roles/system-administrator",
            "/roles/system-administrator/root-access"
    ]

    def addValue = (JCRPropertyWrapper property, String value) -> {
        if (property.getValues().find { it.getString() == value }) {
            return false
        }
        property.addValue(value)
        return true
    }

    JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Integer>() {
        Integer doInJCR(JCRSessionWrapper session) throws RepositoryException {
            def granted = []
            rolePaths.each { path ->
                if (!session.nodeExists(path)) {
                    log.info("No ${path} on this installation, so the ${permission} permission is not granted there.")
                    return
                }
                def role = session.getNode(path)
                if (role.hasProperty("j:permissionNames")) {
                    if (addValue(role.getProperty("j:permissionNames"), permission)) {
                        granted.add(path)
                    }
                } else {
                    role.setProperty("j:permissionNames", [permission] as String[])
                    granted.add(path)
                }
            }
            if (granted.isEmpty()) {
                log.info("The ${permission} permission is already granted where this script grants it.")
                return null
            }
            session.save()
            log.info("Granted the ${permission} permission on ${granted}.")
            return null
        }
    })
}

grantViewEncryptedValue()
