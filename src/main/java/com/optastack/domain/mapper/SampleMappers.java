package com.optastack.domain.mapper;

import com.optastack.domain.model.Employee;
import com.optastack.domain.model.dto.EmployeeVM;
import java.util.List;
import java.util.function.Function;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Sort;

@Mapper
public interface SampleMappers {

  SampleMappers INSTANCE = Mappers.getMapper(SampleMappers.class);

  Sort SORT_BY_NAME = Sort.by(Sort.Order.asc("name").ignoreCase());

  Function<List<Employee>, List<EmployeeVM>> EMPLOYEE_AUDIT_PAGE_TRANSFORMER =
      revisions -> revisions.stream().map(SampleMappers.INSTANCE::toEmployeeVM).toList();

  EmployeeVM toEmployeeVM(final Employee employee);
}
