package com.solera.interview.model.interview;

import com.solera.interview.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "interviews")
public class InterviewEntity extends BaseEntity {

    @Column(name = "title", nullable = false, length = 150)
    private String title;
}
