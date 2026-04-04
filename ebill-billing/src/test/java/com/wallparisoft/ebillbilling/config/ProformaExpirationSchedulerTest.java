package com.wallparisoft.ebillbilling.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wallparisoft.ebillbilling.entity.Proforma;
import com.wallparisoft.ebillbilling.event.CronConfigUpdatedEvent;
import com.wallparisoft.ebillbilling.repository.IProformaRepo;
import com.wallparisoft.ebillbilling.service.ParamsService;
import java.time.LocalDate;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProformaExpirationSchedulerTest {

  private static final String PROFORMA_EXPIRE_CRON_EXPRESSION_PARAM =
      "PROFORMA_EXPIRE_CRON_EXPRESSION";
  private static final String PROFORMA_EXPIRE_CRON_ENABLED_PARAM = "PROFORMA_EXPIRE_CRON_ENABLED";

  @Mock private IProformaRepo proformaRepo;
  @Mock private ThreadPoolTaskScheduler taskScheduler;
  @Mock private ParamsService paramsService;
  @Mock private ScheduledFuture<?> scheduledFuture;

  private ProformaExpirationScheduler scheduler;

  @BeforeEach
  void setUp() {
    when(paramsService.getParamValueOrDefault(eq(PROFORMA_EXPIRE_CRON_EXPRESSION_PARAM), any()))
        .thenReturn("0 0 0 * * *");
    when(paramsService.getParamValueOrDefault(eq(PROFORMA_EXPIRE_CRON_ENABLED_PARAM), any()))
        .thenReturn("true");
    when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
        .thenAnswer(inv -> scheduledFuture);

    scheduler = new ProformaExpirationScheduler(proformaRepo, taskScheduler, paramsService);
  }

  @Test
  void init_loadsConfigFromParamsAndSchedulesTask() {
    scheduler.init();

    verify(paramsService).getParamValueOrDefault(eq(PROFORMA_EXPIRE_CRON_EXPRESSION_PARAM), any());
    verify(paramsService).getParamValueOrDefault(eq(PROFORMA_EXPIRE_CRON_ENABLED_PARAM), any());
    verify(taskScheduler).schedule(any(Runnable.class), any(CronTrigger.class));
  }

  @Test
  void executeTask_invokesMarkExpiredProformas() {
    when(proformaRepo.markExpiredProformas(any(LocalDate.class), eq(Proforma.Status.EXPIRED)))
        .thenReturn(3);

    ReflectionTestUtils.invokeMethod(scheduler, "executeTask");

    ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
    verify(proformaRepo).markExpiredProformas(dateCaptor.capture(), eq(Proforma.Status.EXPIRED));
    assertEquals(LocalDate.now(), dateCaptor.getValue());
  }

  @Test
  void rescheduleTask_whenEnabled_schedulesTask() {
    scheduler.init();
    scheduler.rescheduleTask(true, "0 22 23 * * *");

    verify(scheduledFuture).cancel(false);
    verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(CronTrigger.class));
  }

  @Test
  void rescheduleTask_whenDisabled_cancelsAndDoesNotReschedule() {
    scheduler.init();
    scheduler.rescheduleTask(false, "0 0 0 * * *");

    verify(scheduledFuture).cancel(false);
    verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(CronTrigger.class));
  }

  @Test
  void handleCronConfigUpdated_forProformaParams_callsRescheduleTask() {
    scheduler.init();
    scheduler.handleCronConfigUpdated(
        new CronConfigUpdatedEvent(this, PROFORMA_EXPIRE_CRON_EXPRESSION_PARAM));

    verify(paramsService, times(4)).getParamValueOrDefault(any(), any());
    verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(CronTrigger.class));
  }

  @Test
  void handleCronConfigUpdated_forOtherParam_doesNotReschedule() {
    scheduler.init();
    scheduler.handleCronConfigUpdated(new CronConfigUpdatedEvent(this, "OTHER_PARAM"));

    verify(paramsService, times(2)).getParamValueOrDefault(any(), any());
    verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(CronTrigger.class));
  }

  @Test
  void destroy_cancelsScheduledFuture() throws Exception {
    scheduler.init();
    scheduler.destroy();

    verify(scheduledFuture).cancel(false);
  }
}
