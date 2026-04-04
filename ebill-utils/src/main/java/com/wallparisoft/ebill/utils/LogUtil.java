package com.wallparisoft.ebill.utils;

import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.ebill.utils.log.EventType;
import com.wallparisoft.ebill.utils.log.Level;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class LogUtil {
  private LogUtil() {}

  public static void setLogs(
      EventLog.EventLogBuilder traceElement, EventType request, Level level) {
    EventLog eventLog = traceElement.eventType(request.name()).level(level.name()).build();
    log.debug("Event log: {}", eventLog);
  }
}
