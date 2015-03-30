package prng.collector;

import prng.Config;

/**
 * Use the amount of time between calls as a source of entropy
 * 
 * @author Simon Greatrix
 *
 */
public class SchedulingEntropy extends EntropyCollector {
    /** Last time this was called */
    private long lastTime_ = System.nanoTime();
    public SchedulingEntropy(Config config) {
        super(config,50);
    }

    @Override
    protected int getDelay() {
        return 50;
    }


    @Override
    protected boolean initialise() {
        return true;
    }


    @Override
    public void run() {
        long now = System.nanoTime();
        long diff = now - lastTime_;
        lastTime_ = now;
        setEvent((short) diff);
    }
}