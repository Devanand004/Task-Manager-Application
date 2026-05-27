package com.example.demo.task.mapper;

import com.example.demo.task.dto.TaskRequestDTO;
import com.example.demo.task.dto.TaskResponseDTO;
import com.example.demo.task.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = TaskMapperUtil.class)
public interface TaskMapper {

    TaskResponseDTO toResponseDTO(Task task);

    @Mapping(target = "status", source = "status", qualifiedByName = "stringToTaskStatus")
    @Mapping(target = "priority", source = "priority", qualifiedByName = "stringToTaskPriority")
    Task toEntity(TaskRequestDTO requestDTO);

    @Mapping(target = "status", source = "status", qualifiedByName = "stringToTaskStatus")
    @Mapping(target = "priority", source = "priority", qualifiedByName = "stringToTaskPriority")
    void updateEntityFromDto(TaskRequestDTO requestDTO, @MappingTarget Task task);
}
