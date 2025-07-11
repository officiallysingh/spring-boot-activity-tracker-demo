package com.optastack.adapter.repository;

import com.optastack.domain.model.Employee;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmployeeRepository extends MongoRepository<Employee, String> {

  Optional<Employee> findByCode(final String code);

  boolean existsByCode(final String code);
}
