package com.github.hronom.spring.boot.quartz.cluster.example.supervisor.controllers;

import java.util.Date;

public class JobStatus {
    public final String id;
    public final boolean running;
    public final Date date;

    public JobStatus(String id, boolean running) {
        this(id, running, null);
    }
    public JobStatus(String id, boolean running, Date date) {
        this.id = id;
        this.running = running;
        this.date = date;
    }
}
