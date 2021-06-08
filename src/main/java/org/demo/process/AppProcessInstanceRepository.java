package org.demo.process;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppProcessInstanceRepository extends JpaRepository<AppProcessInstance, Long> {

    List<AppProcessInstance> findByIsActive(boolean isActive);
}
