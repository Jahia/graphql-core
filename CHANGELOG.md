# graphql-core Changelog

## 3.7.0

### New Features

* Bump bn.js from 4.12.0 to 4.12.3 (#613)

* Bump markdown-it from 14.1.0 to 14.1.1 (#612)

* Filter out jmix:hiddenNode types when getting children/descendants (#609)

* `renderedContent` now falls back to the default view (instead of cm) when rendering in page context with no view specified. (#617)

* Secured the qs library by enforcing version 6.15.2 or higher to address known vulnerabilities.

### Bug Fixes

* Revert role permission checks and logging added to grantRole/revertRole mutation APIs in #582, #575 (#591)

  * Checks are now done at the JCR level as part of the story https://github.com/Jahia/jahia-private/issues/4730

* Fix: Clean up cached permissions before rebuilding the schema on graphql provider registration (#605)
