package com.demo.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@MappedSuperclass
@Getter
@Setter
public class BaseModel {

    @CreationTimestamp // JPA automatically assigns value when the row is created
    @Column(name = "created_time", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdTime;

    @UpdateTimestamp // JPA automatically assigns value when the row is updated
    @Column(name = "updated_time", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime updatedTime;
}
