---
name: spring-boot-keycloak-integration
description: Integrates Spring Boot backends with Keycloak (OAuth2 resource server + admin client). Use when adding auth to a Spring Boot service, managing Keycloak realms/users/roles programmatically, or debugging 401/403 issues against a Keycloak-secured API.
license: MIT
---

# Spring Boot + Keycloak Integration

## Two distinct integration modes — don't confuse them

1. **Resource server** (validates incoming JWTs): `spring-boot-starter-oauth2-resource-server`.
   The service trusts tokens issued by Keycloak, extracts roles/claims, enforces
   `@PreAuthorize`. No admin access needed for this mode.
2. **Admin client** (manages Keycloak itself): `keycloak-admin-client` dependency,
   used to create/update realms, users, roles, client scopes programmatically
   (e.g. demo-user provisioning, org-onboarding flows). Needs a service-account
   client with `manage-users`/`manage-realm` roles in Keycloak, never end-user tokens.

## Resource server setup

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://<keycloak-host>/realms/<realm>
```

Role extraction from Keycloak's `realm_access.roles` claim needs a custom
`JwtAuthenticationConverter` — Spring's default only reads `scope`/`scp`:

```java
JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
converter.setJwtGrantedAuthoritiesConverter(jwt -> {
    var realmAccess = jwt.getClaimAsMap("realm_access");
    var roles = (List<String>) realmAccess.getOrDefault("roles", List.of());
    return roles.stream()
        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
        .collect(Collectors.toList());
});
```

## Admin client setup

```java
Keycloak kc = KeycloakBuilder.builder()
    .serverUrl(keycloakUrl)
    .realm("master")               // auth against master realm...
    .clientId("admin-cli")
    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
    .clientSecret(adminClientSecret)
    .build();
kc.realm("target-realm").users().create(userRep);   // ...but operate on the target realm
```

## Common failure modes

- **401 on every request**: `issuer-uri` mismatch (http vs https, trailing slash,
  or Keycloak behind a reverse proxy reporting a different `iss` than what's
  configured) — check the actual JWT's `iss` claim against `issuer-uri` exactly.
- **403 despite valid token**: role claim not being read (see converter above) —
  Keycloak puts roles in `realm_access.roles` or `resource_access.<client>.roles`,
  not in a flat `scope` string like most other OAuth2 providers.
- **"User has no organization" / tenant-scoping bugs**: check custom protocol
  mappers add the org/tenant claim to the token — this is not automatic, needs
  an explicit mapper on the client.
- **Admin client calls fail silently**: service account role assignment is
  separate from "Service Accounts Enabled" toggle — both must be set, and the
  specific realm-management roles (`manage-users`, `view-realm`, etc.) assigned.

## When to use vs. skip

Use this skill when adding/debugging auth on a Spring Boot service against
Keycloak. Skip for pure API-key or session-based auth — this is OAuth2/OIDC
+ Keycloak specifically.
