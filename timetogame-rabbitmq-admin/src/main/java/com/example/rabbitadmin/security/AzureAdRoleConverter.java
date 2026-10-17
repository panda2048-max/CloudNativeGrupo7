package com.example.rabbitadmin.security;

import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Mapea el claim plano "roles" de Microsoft Entra ID (App roles) a
 * authorities con prefijo ROLE_, para poder usar hasRole("ADMIN").
 */
public class AzureAdRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
