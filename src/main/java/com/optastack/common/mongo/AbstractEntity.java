package com.optastack.common.mongo;

import com.optastack.common.Identifiable;
import com.optastack.common.Versionable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
public abstract class AbstractEntity implements Identifiable<String>, Versionable<Long> {

  @Id protected String id;

  @Version protected Long version;

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public Long getVersion() {
    return this.version;
  }

  public static String newMongoId() {
    return new ObjectId().toHexString();
  }
}
