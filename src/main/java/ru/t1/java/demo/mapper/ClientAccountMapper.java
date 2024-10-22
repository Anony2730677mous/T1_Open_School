package ru.t1.java.demo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.t1.java.demo.model.ClientAccount;
import ru.t1.java.demo.model.dto.ClientAccountDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING)
public interface ClientAccountMapper {
    ClientAccount toEntity(ClientAccountDto clientAccountDto);
    ClientAccountDto toDto(ClientAccount clientAccount);
}
