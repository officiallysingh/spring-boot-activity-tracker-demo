package com.optastack.common.config;

import static org.springframework.data.mongodb.core.query.SerializationUtils.serializeToJsonSafely;

import com.ksoot.activity.model.AuthorProvider;
import com.optastack.common.mongo.AuditEvent;
import com.optastack.common.mongo.AuditMetaData;
import com.optastack.common.mongo.Auditable;
import com.optastack.common.mongo.MongoAuditProperties;
import com.optastack.common.util.ClassUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

@Configuration
@EnableConfigurationProperties(MongoAuditProperties.class)
@ConditionalOnProperty(
    prefix = "application.mongodb.auditing",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@Slf4j
@RequiredArgsConstructor
public class MongoAuditListener implements InitializingBean {

  private final MongoAuditProperties mongoAuditProperties;

  private final MongoOperations mongoOperations;

  private final AuditMetaData auditMetaData;

  private final AuthorProvider authorProvider;

  @EventListener(condition = "@auditMetaData.isPresent(#event.getCollectionName())")
  public void onAfterSave(final AfterSaveEvent<?> event) {
    if (log.isDebugEnabled()) {
      log.debug(
          String.format(
              "onAfterSave: %s, %s",
              event.getSource(), serializeToJsonSafely(event.getDocument())));
    }
    this.createAuditEntryOnAfterSave(event, 0);
  }

  private AuditEvent createAuditEntryOnAfterSave(final AfterSaveEvent<?> event, final int attempt) {
    if (this.validateTransaction()) {
      String auditCollectionName =
          this.auditMetaData.getAuditCollection(event.getCollectionName()).get();
      try {
        Query query = new Query(Criteria.where("collection_name").is(event.getCollectionName()));
        long revision = this.mongoOperations.count(query, auditCollectionName) + 1;
        final AuditEvent auditEvent =
            AuditEvent.ofSaveEvent(
                event,
                revision,
                this.authorProvider.getAuthor(),
                this.auditMetaData.getVersionProperty(event.getCollectionName()));
        return this.mongoOperations.insert(auditEvent, auditCollectionName);
      } catch (final DuplicateKeyException exception) {
        if (attempt > 2) { // Max three attempts
          throw new IllegalStateException(
              "Non recoverable Race condition in MongoDB Auditing, "
                  + "while getting next revision number for collection: '"
                  + auditCollectionName
                  + "'");
        }
        return createAuditEntryOnAfterSave(event, attempt + 1);
      }
    } else {
      throw new IllegalStateException(
          "No active transaction while MongoDB Auditing. Try updating collection: '"
              + event.getCollectionName()
              + "' in a Transaction");
    }
  }

  @EventListener(condition = "@auditMetaData.isPresent(#event.getCollectionName())")
  public void onAfterDelete(final AfterDeleteEvent<?> event) {
    if (log.isDebugEnabled()) {
      log.debug(
          String.format(
              "onAfterDelete: %s, %s",
              event.getSource(), serializeToJsonSafely(event.getDocument())));
    }
    this.createAuditEntryOnAfterDelete(event, 0);
  }

  private AuditEvent createAuditEntryOnAfterDelete(
      final AfterDeleteEvent<?> event, final int attempt) {
    if (this.validateTransaction()) {
      String auditCollectionName =
          this.auditMetaData.getAuditCollection(event.getCollectionName()).get();
      try {
        Query query = new Query(Criteria.where("collection_name").is(event.getCollectionName()));
        long revision = this.mongoOperations.count(query, auditCollectionName) + 1;
        final AuditEvent auditEvent =
            AuditEvent.ofDeleteEvent(event, revision, this.authorProvider.getAuthor());
        return this.mongoOperations.insert(auditEvent, auditCollectionName);
      } catch (final DuplicateKeyException exception) {
        if (attempt > 2) { // Max three attempts
          throw new IllegalStateException(
              "Non recoverable Race condition in MongoDB Auditing, "
                  + "while getting next revision number for collection: '"
                  + auditCollectionName
                  + "'");
        }
        return createAuditEntryOnAfterDelete(event, attempt + 1);
      }
    } else {
      throw new IllegalStateException(
          "No active transaction while MongoDB Auditing. Try updating collection: '"
              + event.getCollectionName()
              + "' in a Transaction");
    }
  }

  private boolean validateTransaction() {
    return this.mongoAuditProperties.getAuditing().isWithoutTransaction()
        || TransactionSynchronizationManager.isActualTransactionActive();
  }

  //  private String getAuditUserName() {
  //    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
  //        .map(Authentication::getName)
  //        .orElse(CommonConstants.SYSTEM_USER);
  //  }
  //  private String getAuditUserName() {
  //    return CommonConstants.SYSTEM_USER;
  //  }

  @Override
  public void afterPropertiesSet() {
    Assert.state(this.mongoOperations != null, "A MongoOperations implementation is required.");
    if (StringUtils.isBlank(this.mongoAuditProperties.getAuditing().getPrefix())
        && StringUtils.isBlank(this.mongoAuditProperties.getAuditing().getSuffix())) {
      throw new IllegalArgumentException(
          "At-least one of 'mongodb.auditing.prefix' or 'mongodb.auditing.suffix' properties must not be null or empty");
    }
    MappingContext<?, ?> mappingContext = this.mongoOperations.getConverter().getMappingContext();
    mappingContext.getPersistentEntities().stream()
        .forEach(
            entity ->
                this.getAuditableAnnotation((BasicMongoPersistentEntity) entity)
                    .ifPresent(
                        auditable -> {
                          String collectionName =
                              ((BasicMongoPersistentEntity) entity).getCollection();
                          String versionProperty =
                              entity.hasVersionProperty()
                                  ? ((BasicMongoPersistentEntity) entity)
                                      .getVersionProperty()
                                      .getName()
                                  : null;

                          String auditCollectionName;
                          if (StringUtils.isNotBlank(auditable.name())) {
                            auditCollectionName = auditable.name();
                          } else {
                            auditCollectionName =
                                this.mongoAuditProperties.getAuditing().getPrefix()
                                    + collectionName
                                    + this.mongoAuditProperties.getAuditing().getSuffix();
                          }
                          this.createAuditCollectionIfDoesNotExist(auditCollectionName);

                          this.auditMetaData.put(
                              collectionName,
                              AuditMetaData.Metadata.of(auditCollectionName, versionProperty));
                        }));
  }

  private Optional<Auditable> getAuditableAnnotation(final BasicMongoPersistentEntity<?> entity) {
    // Ideally following line should work, but not working, so getting the annotation from Class
    //    return Optional.ofNullable(AnnotationUtils.findAnnotation(entity.getType(),
    // Auditable.class));

    final String className = entity.getType().getName();
    try {
      final Class<?> clazz = Class.forName(className);
      final List<Class<?>> hierarchy = ClassUtils.getHierarchy(clazz);

      final List<Auditable> auditableAnnotations =
          hierarchy.stream()
              .map(c -> c.getAnnotation(Auditable.class))
              .filter(Objects::nonNull)
              .toList();

      if (CollectionUtils.isNotEmpty(auditableAnnotations)) {
        if (auditableAnnotations.stream().map(Auditable::name).distinct().toList().size() == 1) {
          return Optional.of(auditableAnnotations.get(0));
        } else {
          throw new IllegalStateException(
              "Illegal @Auditable in class hierarchy. Multiple @Auditable annotations found with different 'name'");
        }
      } else {
        return Optional.empty();
      }
    } catch (final ClassNotFoundException e) {
      return Optional.empty();
    }
  }

  // Create Audit collection and indexes if it does not exist
  private void createAuditCollectionIfDoesNotExist(final String auditCollectionName) {
    if (!this.mongoOperations.collectionExists(auditCollectionName)) {
      log.info("Created Audit collection: " + auditCollectionName);
      this.mongoOperations.createCollection(auditCollectionName);

      Index indexDatetime = new Index().named("idx_datetime").on("datetime", Sort.Direction.ASC);
      this.mongoOperations.indexOps(auditCollectionName).ensureIndex(indexDatetime);

      Index indexActor = new Index().named("idx_actor").on("actor", Sort.Direction.ASC);
      this.mongoOperations.indexOps(auditCollectionName).ensureIndex(indexActor);

      Index indexUnqRevision =
          new Index()
              .named("idx_unq_revision")
              .on("revision", Sort.Direction.ASC)
              .on("collection_name", Sort.Direction.ASC)
              .unique();
      this.mongoOperations.indexOps(auditCollectionName).ensureIndex(indexUnqRevision);
    }
  }
}
