package com.github.hronom.spring.boot.quartz.cluster.example.supervisor.services;

import com.github.hronom.spring.boot.quartz.cluster.example.common.service.TestService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class TestServiceImpl implements TestService {
    private final Log logger = LogFactory.getLog(getClass());

    private final Random random = new Random();

    public void run(String id) throws Exception {
        logger.info("Running job on supervisor, job id " + id);
        if (random.nextInt(100) == 1) {
            throw new Exception("Randomly generated test exception on supervisor");
        }
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        } catch (InterruptedException e) {
            logger.error("Error", e);
        }
        logger.info("Completed job on supervisor, job id " + id);
    }
}
