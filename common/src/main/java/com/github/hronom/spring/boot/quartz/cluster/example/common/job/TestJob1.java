package com.github.hronom.spring.boot.quartz.cluster.example.common.job;

import static java.util.Optional.ofNullable;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import com.github.hronom.spring.boot.quartz.cluster.example.common.service.TestService;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@DisallowConcurrentExecution
public class TestJob1 implements Job, Serializable {

  public static final String GET_TIMES_TRIGGERED = "getTimesTriggered";
  private final Log logger = LogFactory.getLog(getClass());

  @Autowired
  private TestService testService;
  @Autowired
  private Scheduler scheduler;

  @Value("${testJob1.maxRetry:5}")
  private int MAX_RETRY;
  /*
  TODO
  the map is not cluster solution.
  that was used to speed up experiment (interested in behavior of rescheduling job to avoid of deadlocks)
   */
  public static Map<String, Integer> map = new HashMap<>();

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    try {
      JobDetail jobDetail = jobExecutionContext.getJobDetail();
      String id = jobDetail.getKey().getName();
      testService.run(id);
      // next row provides rescheduling jobs configured with simple triggers
      reschedule(jobExecutionContext);
    } catch (Exception e) {
      logger.error(e);
      throw new JobExecutionException(e);
    }
  }

  private void reschedule(JobExecutionContext jobExecutionContext) {
    String job = jobExecutionContext.getJobDetail().getKey().getName();
    SimpleTriggerImpl trigger = getTrigger(jobExecutionContext);
    Integer triggeredCount = Optional.ofNullable(map.get(job))
        .filter(Integer.class::isInstance)
        .map(Integer.class::cast)
        .map(i -> ++i)
        .orElse(1);
    logger.info("job " + job + " was fired + " + triggeredCount + " times");
    if (triggeredCount < MAX_RETRY) {
      map.put(job, triggeredCount);
      try {
        TriggerKey triggerKey = trigger.getKey();
        logger.info("rescheduling trigger " + triggerKey + " " + trigger.getTimesTriggered());
        scheduler.rescheduleJob(triggerKey, newTrigger()
            .withIdentity(triggerKey.getName(), trigger.getJobGroup().toString())
            .startAt(Date.from(
                LocalDateTime.now().atZone(ZoneId.systemDefault()).plusSeconds(3).toInstant()))
            .withSchedule(
                simpleSchedule()
                    .withMisfireHandlingInstructionFireNow()
            )
            .build());

      } catch (SchedulerException e) {
        logger.error(e);
      }
    }
  }

  protected SimpleTriggerImpl getTrigger(JobExecutionContext firedJobExecutionContext) {
    return ofNullable(firedJobExecutionContext)
        .map(JobExecutionContext::getTrigger)
        .filter(SimpleTriggerImpl.class::isInstance)
        .map(SimpleTriggerImpl.class::cast)
        .orElse(null);
  }
}
