package com.github.hronom.spring.boot.quartz.cluster.example.supervisor.controllers;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.github.hronom.spring.boot.quartz.cluster.example.supervisor.services.JobsService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

  private final Log logger = LogFactory.getLog(getClass());

  private final JobsService jobsService;

  @Autowired
  public TestController(JobsService jobsService) {
    this.jobsService = jobsService;
  }

  @RequestMapping(value = "/jobs", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<List<String>> addJobs(
      @RequestParam(defaultValue = "10", required = false) int jobs)
      throws SchedulerException {
    List<String> ids = jobsService.addNewJobs(jobs);
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ids);
  }

  @RequestMapping(value = "/jobs/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<List<String>> deleteJob(@PathVariable("id") String id)
      throws SchedulerException {
    return ResponseEntity
        .status(jobsService.deleteJob(id) ? HttpStatus.OK : HttpStatus.NOT_FOUND)
        .build();
  }

  @RequestMapping(value = "/jobs", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<Map<String, List<HttpStatus>>> deleteJobs(@RequestBody List<String> ids)
      throws SchedulerException {

    logger.info("to be deleted: \n" + ids);
    return ResponseEntity.status(HttpStatus.MULTI_STATUS)
        .body(ids.stream()
            .filter(Objects::nonNull)
            .collect(groupingBy(Function.identity(), mapping(this::tryDeleteJob, toList()))));
  }

  private HttpStatus tryDeleteJob(String id) {
    try {
      return jobsService.deleteJob(id) ? HttpStatus.OK : HttpStatus.NOT_FOUND;
    } catch (SchedulerException e) {
      logger.error(e);
      return HttpStatus.I_AM_A_TEAPOT;
    }
  }

  @RequestMapping(value = "/jobs", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<List<String>> getJobs()
      throws SchedulerException {
    List<String> ids = jobsService.getJobs();
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ids);
  }

  @RequestMapping(value = "/status/jobs", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  public ResponseEntity<List<JobStatus>> getJobsStatuses()
      throws SchedulerException {
    List<JobStatus> ids = jobsService.getJobsStatuses();
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(ids);
  }
}
