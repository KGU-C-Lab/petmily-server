package com.clab.securecoding.mapper;

import com.clab.securecoding.type.dto.UserRequestDto;
import com.clab.securecoding.type.dto.UserResponseDto;
import com.clab.securecoding.type.entity.User;
import com.clab.securecoding.type.etc.OAuthProvider;
import com.clab.securecoding.type.etc.Role;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User mapDtoToEntity(UserRequestDto userRequestDto) {
        return User.builder()
                .id(userRequestDto.getId())
                .password(userRequestDto.getPassword())
                .nickname(userRequestDto.getNickname())
                .email(userRequestDto.getEmail())
                .address(userRequestDto.getAddress())
                .contact(userRequestDto.getContact())
                .type(userRequestDto.getType())
                .role(Role.USER)
                .provider(OAuthProvider.LOCAL)
                .build();
    }

    public UserResponseDto mapEntityToDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .address(user.getAddress())
                .contact(user.getContact())
                .type(user.getType())
                .role(user.getRole())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .build();
    }

}
