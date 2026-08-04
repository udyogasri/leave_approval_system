package com.app.leaveapprovalsystem.mapper;

import com.app.leaveapprovalsystem.dto.UserResponseDTO;
import com.app.leaveapprovalsystem.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role",        expression = "java(user.getRole().getName().name())")
    @Mapping(target = "managerId",   expression = "java(user.getManager() != null ? user.getManager().getId() : null)")
    @Mapping(target = "managerName", expression = "java(user.getManager() != null ? user.getManager().getFirstName() + \" \" + user.getManager().getLastName() : null)")
    UserResponseDTO toResponse(User user);
}
