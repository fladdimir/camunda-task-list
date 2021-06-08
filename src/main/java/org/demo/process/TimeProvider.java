package org.demo.process;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
public class TimeProvider {

    public LocalDate today() {
        return LocalDate.now();
    }

    public Instant now() {
        return Instant.now();
    }

}