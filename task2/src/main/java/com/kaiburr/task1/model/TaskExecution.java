package com.kaiburr.task1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecution {
    private Instant startTime;
    private Instant endTime;
    private String output;
}
