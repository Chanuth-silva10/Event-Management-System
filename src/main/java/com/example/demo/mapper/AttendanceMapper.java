package com.example.demo.mapper;

import com.example.demo.domain.dto.response.AttendanceResponse;
import com.example.demo.domain.entity.Attendance;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {UserMapper.class, EventMapper.class})
public interface AttendanceMapper {

    AttendanceResponse toAttendanceResponse(Attendance attendance);
}
