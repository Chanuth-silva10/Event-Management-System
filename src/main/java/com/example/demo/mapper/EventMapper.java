package com.example.demo.mapper;

import com.example.demo.domain.dto.response.EventResponse;
import com.example.demo.domain.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {UserMapper.class})
public interface EventMapper {

    @Mapping(target = "attendeeCount", expression = "java(event.getAttendeeCount())")
    EventResponse toEventResponse(Event event);
}
