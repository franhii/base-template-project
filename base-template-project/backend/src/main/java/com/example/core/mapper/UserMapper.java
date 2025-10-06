package com.example.core.mapper;

import com.example.core.dto.RegisterRequest;
import com.example.core.dto.UserDTO;
import com.example.core.model.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", ignore = true)
    User fromRegisterRequest(RegisterRequest req);
}