package com.solera.interview.repository;

import com.solera.interview.model.interview.InterviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<InterviewEntity, Long> {
}
