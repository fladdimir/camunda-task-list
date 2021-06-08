package org.demo.process;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class ProcessService {

    private static final String PROCESS_KEY = "goodMorning";

    private AppProcessInstanceRepository repository;

    private TimeProvider timeProvider;

    private RuntimeService runtimeService;

    private TaskService taskService;

    /**
     * @return open tasks to be completed.
     */
    @Transactional
    public Collection<String> getCurrentProcessTasks() {
        var todaysInstance = findTodaysOrCreateNewInstance();

        // get all currently active tasks
        var processInstanceId = todaysInstance.getEngineInstanceId();
        var tasks = findTasks(processInstanceId);
        return tasks.stream().map(Task::getName).collect(Collectors.toList());
    }

    /**
     * @return number of completed tasks today
     */
    @Transactional
    public int getNumberOfCompletedTasks() {
        return findTodaysOrCreateNewInstance().getNumTasksCompleted();
    }

    /**
     * @return overall number of tasks today
     */
    @Transactional
    public int getNumberOfTasksOverall() {
        return findTodaysOrCreateNewInstance().getNumTasksOverall();
    }

    /**
     * Complete an open task.
     */
    @Transactional
    public void completeTask(String taskName) {
        var todaysInstance = findTodaysInstance().orElseThrow();
        var tasks = findTasks(todaysInstance.getEngineInstanceId());
        var taskToComplete = tasks.stream().filter(t -> taskName.equals(t.getName())).findAny().orElseThrow();

        logEvents(todaysInstance, taskToComplete);

        taskService.complete(taskToComplete.getId());
        todaysInstance.setNumTasksCompleted(todaysInstance.getNumTasksCompleted() + 1);
        repository.save(todaysInstance);
    }

    private AppProcessInstance findTodaysOrCreateNewInstance() {
        return findTodaysInstance().orElseGet(() -> {
            deactivateOldInstances(repository.findByIsActive(true));
            return createNewProcessInstance();
        });
    }

    private AppProcessInstance createNewProcessInstance() {
        var entity = new AppProcessInstance();
        entity.setCreatedDate(timeProvider.today());
        var pi = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
        entity.setEngineInstanceId(pi.getId());
        entity.setNumTasksOverall(countNumberOfTasksOverall());
        return repository.save(entity);
    }

    private int countNumberOfTasksOverall() { // by running through the process once
        var pi = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
        var c = 0;
        List<Task> tasks;
        do {
            tasks = findTasks(pi.getId());
            c += tasks.size();
            tasks.forEach(t -> taskService.complete(t.getId()));
        } while (!tasks.isEmpty());
        if (c == 0)
            throw new IllegalStateException("No tasks found in the process!");
        return c;
    }

    private void deactivateOldInstances(List<AppProcessInstance> oldInstances) {
        oldInstances.forEach(old -> {
            var processInstanceId = old.getEngineInstanceId();
            var tasks = findTasks(processInstanceId);
            if (!tasks.isEmpty()) { // remaining tasks
                runtimeService.deleteProcessInstance(processInstanceId, null);
            }
            old.setActive(false);
            repository.save(old);
        });
    }

    private Optional<AppProcessInstance> findTodaysInstance() {
        return repository.findByIsActive(true).stream().filter(p -> timeProvider.today().equals(p.getCreatedDate()))
                .findAny();
    }

    private List<Task> findTasks(String processInstanceId) {
        var list = taskService.createTaskQuery().active().processInstanceId(processInstanceId).list();
        list.sort(Comparator.comparing(Task::getCreateTime)); // executionId stays the same on parallel branches
        return list;
    }

    private void logEvents(AppProcessInstance todaysInstance, Task task) {
        todaysInstance.addEvent(task.getName(), true, task.getCreateTime().toInstant());
        todaysInstance.addEvent(task.getName(), false, timeProvider.now());
        repository.save(todaysInstance);
    }
}
