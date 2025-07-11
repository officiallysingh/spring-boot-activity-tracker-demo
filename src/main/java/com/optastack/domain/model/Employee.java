package com.optastack.domain.model;

import com.optastack.common.mongo.AbstractEntity;
import com.optastack.common.mongo.Auditable;
import com.optastack.common.util.RegularExpressions;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.*;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
// @Accessors(chain = true, fluent = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
@Auditable
@Document(collection = "employees")
@TypeAlias("employee")
public class Employee extends AbstractEntity {

  @NotEmpty
  @Size(min = 5, max = 10)
  @Pattern(regexp = RegularExpressions.REGEX_EMPLOYEE_CODE)
  @Indexed(unique = true)
  private String code;

  @NotEmpty
  @Size(min = 3, max = 50)
  @Pattern(regexp = RegularExpressions.REGEX_ALPHABETS_AND_SPACES)
  @Setter
  @Indexed
  private String name;

  @NotNull @Past @Setter private LocalDate dob;
}
