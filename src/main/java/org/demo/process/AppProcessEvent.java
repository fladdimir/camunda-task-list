package org.demo.process;

import java.time.Instant;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Data
@Setter(value = AccessLevel.NONE)
@NoArgsConstructor
public class AppProcessEvent {

    private static final String START = "_START";
    private static final String END = "_END";

    @Id
    @GeneratedValue
    private Long id;

    @Setter(value = AccessLevel.NONE)
    private String name;

    private Instant timestamp;

    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false)
    private AppProcessInstance processInstance;

    AppProcessEvent(String name, boolean isStart, Instant timestamp, AppProcessInstance processInstance) {
        this.name = name.replace(" ", "") + (isStart ? START : END);
        this.timestamp = timestamp;
        this.processInstance = processInstance;
    }
}
