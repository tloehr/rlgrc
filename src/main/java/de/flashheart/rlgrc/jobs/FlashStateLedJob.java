package de.flashheart.rlgrc.jobs;

import de.flashheart.rlgrc.ui.FrameMain;
import de.flashheart.rlgrc.ui.PnlActiveGame;
import lombok.extern.log4j.Log4j2;
import org.quartz.*;

@Log4j2
@DisallowConcurrentExecution
public class FlashStateLedJob implements InterruptableJob {
    public static final String name = "flashstateledjob";


    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        try {
            log.trace(jobExecutionContext.getJobDetail().getKey() + " executed");
            PnlActiveGame pnl = (PnlActiveGame) jobExecutionContext.getScheduler().getContext().get("rlgrc");
            pnl.flash_state_led();
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
