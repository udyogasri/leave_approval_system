package com.app.leaveapprovalsystem.mapper;

import com.app.leaveapprovalsystem.dto.LeaveResponseDTO;
import com.app.leaveapprovalsystem.entity.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeaveMapper {

    @Mapping(target = "userId",       source = "user.id")
    @Mapping(target = "employeeCode", source = "user.employeeCode")
    @Mapping(target = "employeeName", expression = "java(leave.getUser().getFirstName() + \" \" + leave.getUser().getLastName())")
    @Mapping(target = "department",   source = "user.department")
    LeaveResponseDTO toResponseDTO(LeaveRequest leave);
}
