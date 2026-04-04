package com.wallparisoft.ebill.utils.log;

import lombok.Builder;
import lombok.ToString;

@Builder
@ToString
public class EventLog {

  private String service;
  private String method;
  private String event;
  private String eventType;
  private String level;
  private Object information;
  private Long elapsedTime;
  private String sessionId;
  private String identification;
  private String code;
  private String message;
}
