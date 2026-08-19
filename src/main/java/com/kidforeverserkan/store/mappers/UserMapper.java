package com.kidforeverserkan.store.mappers;

import com.kidforeverserkan.store.controllers.dtos.UserDto;
import com.kidforeverserkan.store.dtos.RegisterUserRequest;
import com.kidforeverserkan.store.dtos.UpdateUserRequest;
import com.kidforeverserkan.store.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
    User toEntity(RegisterUserRequest request);
    void update(UpdateUserRequest request, @MappingTarget User user);
}
