package org.demo.process;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Data
@NoArgsConstructor
public class AppProcessInstance {

    @Id
    @GeneratedValue
    private Long id;

    @Column(updatable = false, nullable = false)
    private LocalDate createdDate;

    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private String engineInstanceId;

    private int numTasksCompleted = 0;

    @Column(nullable = false, updatable = false)
    private Integer numTasksOverall;

    @Setter(value = AccessLevel.NONE)
    @EqualsAndHashCode.Exclude
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "processInstance")
    private Set<AppProcessEvent> events = new HashSet<>();

    public void addEvent(String name, boolean isStart, Instant timestamp) {
        events.add(new AppProcessEvent(name, isStart, timestamp, this));
    }
}
