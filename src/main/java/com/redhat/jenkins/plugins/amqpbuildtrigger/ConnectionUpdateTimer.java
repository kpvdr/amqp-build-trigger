package com.redhat.jenkins.plugins.amqpbuildtrigger;

import hudson.Extension;
import hudson.model.AperiodicWork;

@Extension
public class ConnectionUpdateTimer extends AperiodicWork {

    private static final long DEFAULT_RECCURENCE_TIME = 60000; // ms, ie 60 sec
    private static final long INITIAL_DELAY_TIME = 15000; // ms, ie 15 sec

    private volatile boolean stopRequested;
    private long reccurencePeriod;
    
    public ConnectionUpdateTimer() {
        this(DEFAULT_RECCURENCE_TIME, false);
    }

    public ConnectionUpdateTimer(long reccurencePeriod, boolean stopRequested) {
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
		return new ConnectionUpdateTimer(reccurencePeriod, stopRequested);
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
