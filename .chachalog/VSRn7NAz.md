---
# Allowed version bumps: patch, minor, major
graphql-core: minor
---

**Deprecation:** the time-zone-less date surface of JCR properties is deprecated. On `JCRProperty`, `notZonedDateValue` and `notZonedDateValues` are marked `@deprecated`; on the property mutations (`setValue`, `addValue`, etc.), the `NOT_ZONED_DATE` value of the `option` argument is deprecated. All of them keep working exactly as before; nothing is removed.

Prefer the zoned variants: read a date with `value` / `values`, which return the JCR ISO-8601 form carrying the time zone offset, and write one by passing `type: DATE` with an offset-bearing ISO-8601 value and no `option`.

The deprecated surface formats and parses in the _server's_ default time zone and drops the offset, so the same string denotes a different instant depending on where it is read or written. This ambiguity is the reason for deprecation.
