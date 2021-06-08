package org.demo.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Route;

import org.demo.process.AppProcessInstanceRepository;
import org.demo.process.ProcessService;

@Route
public class MainView extends VerticalLayout {

    private final transient ProcessService service;
    private final transient AppProcessInstanceRepository repository;

    public MainView(ProcessService service, AppProcessInstanceRepository repository) {
        this.service = service;
        this.repository = repository;
        initComponents();
    }

    private void initComponents() {
        this.removeAll();
        var tasks = service.getCurrentProcessTasks();

        add(new ProgressBar(0, service.getNumberOfTasksOverall(), service.getNumberOfCompletedTasks()));

        add("Current tasks this great morning:");

        if (tasks.isEmpty()) {
            var button = new Button("Done - start next day...", e -> {
                repository.findByIsActive(true).forEach(p -> {
                    p.setActive(false);
                    repository.save(p);
                });
                initComponents();
            });
            add(button);
            return;
        }
        tasks.forEach(t -> add(new Button("Complete '" + t + "'", e -> onClick(t))));
    }

    private void onClick(String t) {
        service.completeTask(t);
        initComponents();
    }
}
