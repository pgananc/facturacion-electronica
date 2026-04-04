package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.service.ICreditNoteService;
import com.wallparisoft.ebillbilling.service.IGuideService;
import com.wallparisoft.ebillbilling.service.IInvoiceService;
import com.wallparisoft.ebillbilling.service.TaskService;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {

  @Autowired private IInvoiceService invoiceService;
  @Autowired private ICreditNoteService creditNoteService;
  @Autowired private IGuideService guideService;

  @Override
  public void executeTask() {
    log.info("Tarea programada ejecutada en: " + LocalDateTime.now());
    invoiceService.executeSendInvoiceToSri();
    creditNoteService.executeSendCreditNoteToSri();
    guideService.executeSendGuideToSri();
  }
}
