package com.optastack.adapter.controller;

import static com.optastack.common.CommonConstants.DEFAULT_PAGE_SIZE;
import static com.optastack.common.util.rest.ApiConstants.INTERNAL_SERVER_ERROR_EXAMPLE_RESPONSE;

import com.ksoot.activity.model.util.PaginatedResource;
import com.ksoot.activity.model.util.PaginatedResourceAssembler;
import com.optastack.common.mongo.AuditEvent;
import com.optastack.domain.service.MongoAuditHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/audit-history")
@Tag(name = "Mongo Audit History", description = "query APIs")
@RequiredArgsConstructor
class MongoAuditHistoryController {

  private final MongoAuditHistoryService auditHistoryService;

  @GetMapping
  @Operation(operationId = "get-audit-history", summary = "Gets a page of Audit History")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description =
                "Audit History page returned successfully. Returns an empty page if no records found"),
        @ApiResponse(
            responseCode = "500",
            description = "Internal Server error",
            content = @Content(examples = @ExampleObject(INTERNAL_SERVER_ERROR_EXAMPLE_RESPONSE)))
      })
  public PaginatedResource<AuditEvent> getProducts(
      @Parameter(
              description = "Source MongoDB Collection name. E.g. <b>employees</b>",
              required = true)
          @RequestParam
          final String collectionName,
      @Parameter(description = "Audit Event type.") @RequestParam(required = false)
          final AuditEvent.Type type,
      @Parameter(description = "Audit Revisions.") @RequestParam(required = false)
          final List<Long> revisions,
      @Parameter(description = "Audit Username. E.g. <b>SYSTEM</b>") @RequestParam(required = false)
          final String actor,
      @Parameter(description = "From Datetime, Inclusive. E.g. <b>2023-12-20T13:57:13+05:30</b>")
          @RequestParam(required = false)
          final OffsetDateTime fromDateTime,
      @Parameter(description = "Till Datetime, Inclusive. E.g. <b>2023-12-22T13:57:13+05:30</b>")
          @RequestParam(required = false)
          final OffsetDateTime tillDateTime,
      @ParameterObject @PageableDefault(size = DEFAULT_PAGE_SIZE) final Pageable pageRequest) {
    final Page<AuditEvent> feePage =
        this.auditHistoryService.getAuditHistory(
            collectionName, type, revisions, actor, fromDateTime, tillDateTime, pageRequest);
    return PaginatedResourceAssembler.assemble(feePage);
  }
}
