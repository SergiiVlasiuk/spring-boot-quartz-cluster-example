package com.github.hronom.spring.boot.quartz.cluster.example.supervisor.services;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import com.github.hronom.spring.boot.quartz.cluster.example.common.job.TestJob1;
import com.github.hronom.spring.boot.quartz.cluster.example.supervisor.controllers.JobStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.utils.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

@Service
public class JobsService {

  private final Log logger = LogFactory.getLog(getClass());

  private final String groupName = "normal-group";

  private final Scheduler scheduler;

  @Autowired
  public JobsService(SchedulerFactoryBean schedulerFactory) {
    this.scheduler = schedulerFactory.getScheduler();
  }

  public List<String> addNewJobs(int jobs) throws SchedulerException {
    LinkedList<String> list = new LinkedList<>();
    for (int i = 0; i < jobs; i++) {
      list.add(addNewJob());
    }
    return list.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
  }

  public String addNewJob() throws SchedulerException {
    String id = UUID.randomUUID().toString();

    JobDetail job =
        newJob(TestJob1.class)
            .withIdentity(id, groupName)
            // http://www.quartz-scheduler.org/documentation/quartz-2.2.x/configuration/ConfigJDBCJobStoreClustering.html
            // https://stackoverflow.com/a/19270566/285571
            .requestRecovery()
            .build();
    Trigger trigger =
        newTrigger()
            .withIdentity(id + "-trigger", groupName)
//            .startNow()
            .startAt(Date.from(
                LocalDateTime.now().atZone(ZoneId.systemDefault()).plusSeconds(3).toInstant()))
            .withSchedule(
                simpleSchedule()
//                    .withIntervalInSeconds(10)
                    .withMisfireHandlingInstructionFireNow()
            )
            .build();

    scheduler.scheduleJob(job, trigger);
    logger.info("register job: " + job);

    return id;
  }

  public boolean deleteJob(String id) throws SchedulerException {
    JobKey jobKey = new JobKey(id, groupName);
    return scheduler.deleteJob(jobKey);
  }

  public List<String> getJobs() throws SchedulerException {
    return scheduler
        .getJobKeys(GroupMatcher.jobGroupEquals(groupName))
        .stream()
        .map(Key::getName)
        .sorted(Comparator.naturalOrder())
        .collect(Collectors.toList());
  }

  /**
   * Check realization was inspired by https://stackoverflow.com/a/31479434/285571
   */
  public List<JobStatus> getJobsStatuses() throws SchedulerException {
    LinkedList<JobStatus> list = new LinkedList<>();
    for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
      JobDetail jobDetail = scheduler.getJobDetail(jobKey);
      List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobDetail.getKey());
      for (Trigger trigger : triggers) {
        Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
        if (Trigger.TriggerState.COMPLETE.equals(triggerState)) {
          list.add(new JobStatus(jobKey.getName(), true, trigger.getNextFireTime()));
        } else {
          list.add(new JobStatus(jobKey.getName(), false, trigger.getNextFireTime()));
        }
      }
    }
    list.sort(Comparator.comparing(o -> o.id));
    return list;
  }
}
