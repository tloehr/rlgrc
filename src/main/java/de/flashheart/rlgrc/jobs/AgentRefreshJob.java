package de.flashheart.rlgrc.jobs;

import de.flashheart.rlgrc.FrameMain;
import lombok.extern.log4j.Log4j2;
import org.quartz.*;

@Log4j2
@DisallowConcurrentExecution
public class AgentRefreshJob implements InterruptableJob {
    public static final String name = "networkingmonitor";


    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        try {
            log.trace(jobExecutionContext.getJobDetail().getKey() + " executed");
            FrameMain frameMain = (FrameMain) jobExecutionContext.getScheduler().getContext().get("rlgrc");
            frameMain.refreshAgents();
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
