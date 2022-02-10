package de.flashheart.rlgrc.jobs;

import de.flashheart.rlgrc.FrameMain;
import lombok.extern.log4j.Log4j2;
import org.quartz.*;

@Log4j2
@DisallowConcurrentExecution
public class ServerRefreshJob implements InterruptableJob {
    public static final String name = "refreshserverjob";


    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        try {
            log.trace(jobExecutionContext.getJobDetail().getKey() + " executed");
            FrameMain frameMain = (FrameMain) jobExecutionContext.getScheduler().getContext().get("rlgrc");
            frameMain.refreshServer();
        } catch (SchedulerException e) {
            log.fatal(e);
            System.exit(0);
        }
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        log.info("job '{}' interrupted", name);
        // nothing to do here
    }

}
