package com.clab.securecoding.auth.util;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

@RequiredArgsConstructor
public class AuthUtil {

//    public static User getAuthenticationInfo() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null || authentication.getName() == null) {
//            throw new RuntimeException("No authentication information.");
//        }
//        return (User) authentication.getPrincipal();
//    }

    public static Long getAuthenticationInfoUserId()
    {
//        return getAuthenticationInfo().getUsername();
        return 1L;
    }

}