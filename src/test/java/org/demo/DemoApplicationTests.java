package org.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneOffset;

import javax.transaction.Transactional;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.demo.process.AppProcessInstanceRepository;
import org.demo.process.ProcessService;
import org.demo.process.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

// 'docker-compose up' before running the tests (or use test-containers)
@SpringBootTest
class DemoApplicationTests {

	@Autowired
	private AppProcessInstanceRepository repository;

	@Mock
	private TimeProvider timeProvider;

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;

	private ProcessService service;

	@BeforeEach
	void beforeEach() {
		MockitoAnnotations.openMocks(this);
		repository.deleteAll();
		service = new ProcessService(repository, timeProvider, runtimeService, taskService);
	}

	@Test
	@Transactional
	@Rollback(false)
	void testDailyCreation() {
		var day1 = LocalDate.ofEpochDay(0);
		var day2 = LocalDate.ofEpochDay(1);

		// day 1
		when(timeProvider.today()).thenReturn(day1);
		var tasks1 = service.getCurrentProcessTasks(); // creates new entity
		assertThat(tasks1).isNotEmpty();
		assertThat(repository.findAll()).hasSize(1);
		assertThat(repository.findAll().get(0).getCreatedDate()).isEqualTo(day1);

		// still day 1
		tasks1 = service.getCurrentProcessTasks(); // entity already created
		assertThat(tasks1).isNotEmpty();
		assertThat(repository.findAll()).hasSize(1);
		assertThat(repository.findAll().get(0).getCreatedDate()).isEqualTo(day1);

		// complete all tasks
		when(timeProvider.now()).thenReturn(day1.atTime(1, 0).toInstant(ZoneOffset.UTC));
		do {
			String completedTask = tasks1.stream().findAny().orElseThrow();
			service.completeTask(completedTask);
			tasks1 = service.getCurrentProcessTasks();
			assertThat(tasks1).doesNotContain(completedTask);
		} while (!tasks1.isEmpty());
		var instanceEntity = repository.findAll().get(0);
		assertThat(instanceEntity.getNumTasksCompleted()).isEqualTo(instanceEntity.getNumTasksOverall());

		// day 2
		when(timeProvider.today()).thenReturn(day2);
		var tasks2 = service.getCurrentProcessTasks(); // creates new entity and outdates old one
		assertThat(tasks2).isNotEmpty();
		assertThat(repository.findAll()).hasSize(2);
		assertThat(repository.findByIsActive(true)).hasSize(1);
		assertThat(repository.findByIsActive(true).get(0).getCreatedDate()).isEqualTo(day2);
	}

}
