package com.demo.api.task;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherMonitorTaskTest {

    @Test
    void canInstantiateTask() {
        WeatherMonitorTask task = new WeatherMonitorTask();

        assertThat(task).isNotNull();
    }
}
