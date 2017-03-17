package org.test.ignite.feeder;

import io.vertx.core.json.JsonObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author ctranxuan
 */
public final class Configuration {

    private final int cacheMin;
    private final int cacheMax;
    private final long frequency;
    private final long pause;
    private final long duration;

    public Configuration(final JsonObject aConfig) {
        checkNotNull(aConfig);

        String cacheRange;
        cacheRange = aConfig.getString("cache.range", "[0-10]");

        Pattern pattern;
        pattern = Pattern.compile("^\\[(\\d+)-(\\d+)\\]$");

        Matcher matcher;
        matcher = pattern.matcher(cacheRange);

        if (!matcher.matches()) {
            throw new RuntimeException("failed to extract cache range from cache.range=" + cacheRange);

        }

        cacheMin = Integer.valueOf(matcher.group(1));
        cacheMax = Integer.valueOf(matcher.group(2));

        checkCacheRange(cacheMin, cacheMax);

        frequency = aConfig.getLong("cache.feed.frequency", 1000L);
        checkArgument(frequency > 0);

        pause = aConfig.getLong("simulation.starting.pause", 1000L);
        checkArgument(pause >= 0);

        duration = aConfig.getLong("simulation.duration", 60000L);
        checkArgument(duration > 0);
    }

    public int getCacheMin() {
        return cacheMin;
    }

    public int getCacheMax() {
        return cacheMax;
    }

    public long getFrequency() {
        return frequency;
    }

    public long getPause() {
        return pause;
    }

    public long getDuration() {
        return duration;
    }

    private void checkCacheRange(final int aCacheMin, final int aCacheMax) {
        checkArgument(aCacheMin >= 0);
        checkArgument(aCacheMax > 0);
        checkArgument(aCacheMax > aCacheMin);
    }
}
