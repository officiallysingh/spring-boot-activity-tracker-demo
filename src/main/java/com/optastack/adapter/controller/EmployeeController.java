package com.optastack.adapter.controller;

import static com.optastack.common.CommonErrorKeys.EMPTY_UPDATE_REQUEST;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.ksoot.activity.model.Tag;
import com.ksoot.activity.model.TrackActivity;
import com.ksoot.problem.core.Problems;
import com.optastack.common.util.GeneralMessageResolver;
import com.optastack.common.util.rest.response.APIResponse;
import com.optastack.domain.mapper.SampleMappers;
import com.optastack.domain.model.Employee;
import com.optastack.domain.model.dto.EmployeeCreationRQ;
import com.optastack.domain.model.dto.EmployeeUpdationRQ;
import com.optastack.domain.model.dto.EmployeeVM;
import com.optastack.domain.service.EmployeeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class EmployeeController implements EmployeeApi {

  private final EmployeeService employeeService;

  @Override
  public ResponseEntity<Boolean> validateEmployeeCode(final String code) {
    return ResponseEntity.ok(this.employeeService.doesEmployeeExist(code));
  }

  @TrackActivity(
      activity = "CREATE_EMPLOYEE",
      description = "Created new Employee",
      tags = {@Tag("permanent"), @Tag("java")})
  @Override
  public ResponseEntity<APIResponse<?>> createEmployee(final EmployeeCreationRQ request) {
    final String id = this.employeeService.createEmployee(request).getId();
    return ResponseEntity.created(
            linkTo(methodOn(EmployeeController.class).getEmployee(id)).withSelfRel().toUri())
        .body(APIResponse.newInstance().addSuccess(GeneralMessageResolver.RECORD_CREATED));
  }

  @TrackActivity(
      activity = "FIND_EMPLOYEE",
      description = "Get an existing Employee",
      tags = {
        @Tag("adhoc"),
      })
  @Override
  public ResponseEntity<EmployeeVM> getEmployee(final String id) {
    return ResponseEntity.ok(
        SampleMappers.INSTANCE.toEmployeeVM(this.employeeService.getEmployeeById(id)));
  }

  @Override
  public ResponseEntity<APIResponse<?>> updateEmployee(
      final String id, final EmployeeUpdationRQ request) {
    if (request.isEmpty()) {
      throw Problems.newInstance(EMPTY_UPDATE_REQUEST).throwAble(HttpStatus.BAD_REQUEST);
    }
    final Employee employee = this.employeeService.updateEmployee(id, request);
    return ResponseEntity.ok()
        .location(
            linkTo(methodOn(EmployeeController.class).getEmployee(employee.getId()))
                .withSelfRel()
                .toUri())
        .body(APIResponse.newInstance().addSuccess(GeneralMessageResolver.RECORD_UPDATED));
  }

  @Override
  public ResponseEntity<List<EmployeeVM>> getAllEmployees() {
    return ResponseEntity.ok(
        this.employeeService.getAllEmployees().stream()
            .map(SampleMappers.INSTANCE::toEmployeeVM)
            .toList());
  }

  @Override
  public ResponseEntity<APIResponse<?>> deleteEmployee(final String id) {
    this.employeeService.deleteEmployee(id);
    return ResponseEntity.ok(
        APIResponse.newInstance().addSuccess(GeneralMessageResolver.RECORD_DELETED));
  }
}
