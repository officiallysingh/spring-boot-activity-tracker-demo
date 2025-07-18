package com.optastack.adapter.repository;

import com.ksoot.problem.core.Problems;
import com.optastack.common.mongo.AuditEvent;
import com.optastack.common.mongo.AuditMetaData;
import com.optastack.domain.SampleErrorTypes;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class MongoAuditHistoryRepository {

  private final MongoOperations mongoOperations;

  private final AuditMetaData auditMetaData;

  public Page<AuditEvent> getAuditHistory(
      final String collectionName,
      final AuditEvent.Type type,
      final List<Long> revisions,
      final String actor,
      final OffsetDateTime fromDateTime,
      final OffsetDateTime tillDateTime,
      final Pageable pageRequest) {
    if (!this.auditMetaData.isPresent(collectionName)) {
      throw Problems.newInstance(SampleErrorTypes.AUDIT_COLLECTION_NOT_FOUND)
          .detailArgs(collectionName)
          .throwAble();
    }

    String auditCollectionName = this.auditMetaData.getAuditCollection(collectionName).get();
    final Query query = new Query();
    if (StringUtils.isNotBlank(collectionName)) {
      query.addCriteria(Criteria.where("collection_name").is(collectionName));
    }
    if (Objects.nonNull(type)) {
      query.addCriteria(Criteria.where("type").is(type));
    }
    if (CollectionUtils.isNotEmpty(revisions)) {
      query.addCriteria(Criteria.where("revision").in(revisions));
    }
    if (StringUtils.isNotBlank(actor)) {
      query.addCriteria(Criteria.where("actor").is(actor));
    }
    if (Objects.nonNull(fromDateTime) && Objects.nonNull(tillDateTime)) {
      query.addCriteria(
          Criteria.where("datetime")
              .gte(fromDateTime)
              .andOperator(Criteria.where("datetime").lte(tillDateTime)));
    } else if (Objects.nonNull(fromDateTime)) {
      query.addCriteria(Criteria.where("datetime").gte(fromDateTime));
    } else if (Objects.nonNull(tillDateTime)) {
      query.addCriteria(Criteria.where("datetime").lte(tillDateTime));
    }
    final long totalRecords = this.mongoOperations.count(query, auditCollectionName);
    if (totalRecords == 0) {
      return Page.empty();
    } else {
      final Pageable pageable =
          totalRecords <= pageRequest.getPageSize()
              ? PageRequest.of(0, pageRequest.getPageSize(), pageRequest.getSort())
              : pageRequest;
      final List<AuditEvent> feeMovementRecords =
          this.mongoOperations.find(query.with(pageable), AuditEvent.class, auditCollectionName);
      return new PageImpl<>(feeMovementRecords, pageable, totalRecords);
    }
  }
}
