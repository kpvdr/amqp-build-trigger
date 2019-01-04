package org.jenkinsci.plugins.amqpbuildtrigger;

import hudson.Extension;
import hudson.model.AperiodicWork;

@Extension
public class UpdateTimer extends AperiodicWork {
    private static final long DEFAULT_RECCURENCE_TIME = 20000; // ms
    private static final long INITIAL_DELAY_TIME = 15000; // ms
    
    private volatile boolean stopRequested;
    private long reccurencePeriod;
    
    public UpdateTimer() {
        this(DEFAULT_RECCURENCE_TIME, false);
    }
    
    public UpdateTimer(long reccurencePeriod, boolean stopRequested) {
    	this.reccurencePeriod = reccurencePeriod;
    	this.stopRequested = stopRequested;
    }
	
    @Override
	public long getRecurrencePeriod() {
		return reccurencePeriod;
	}
    
    @Override
    public long getInitialDelay() {
        return INITIAL_DELAY_TIME;
    }

	@Override
	public AperiodicWork getNewInstance() {
		return new UpdateTimer(reccurencePeriod, stopRequested);
	}

	@Override
	protected void doAperiodicRun() {
        if (!stopRequested) {
        	ConnectionManager.getInstance().update();
        }
	}

    public void stop() {
        stopRequested = true;
    }
    
    public void start() {
        stopRequested = false;
    }
}
