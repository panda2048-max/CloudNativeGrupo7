package com.example.Timetogame.security;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Los App roles asignados en Microsoft Entra ID (Azure AD) viajan en un
 * claim "roles" plano en el access token (a diferencia de Keycloak, que los
 * anida en "realm_access.roles"). Este converter los mapea a authorities
 * con prefijo ROLE_ para poder seguir usando hasRole("ADMIN").
 */
public class AzureAdRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            return List.of();
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toUnmodifiableList());
    }
}
