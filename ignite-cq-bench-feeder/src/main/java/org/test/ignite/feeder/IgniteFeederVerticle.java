package org.test.ignite.feeder;

import com.google.common.collect.Lists;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author ctranxuan
 */
public class IgniteFeederVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(IgniteFeederVerticle.class);

    private static final String DATA = "data";
    private static final String INDEX = "index";
    private static final String TIMESTAMP = "timestamp";


    private final Ignite ignite;
    private final List<Long> feeders;

    public IgniteFeederVerticle() {
        super();
        ignite = Ignition.start("ignite-cq.xml");
        feeders = Lists.newArrayList();
    }

    @Override
    public void start() {
        FileSource source;
        source = new FileSource("curls.txt");

        Configuration configuration;
        configuration = new Configuration(config());

        feedCaches(source, configuration);
        LOGGER.info("Feeder start");

        vertx.setTimer(configuration.getDuration(), l -> {
            try {
                stop();
                System.exit(0);

            } catch (Exception e) {
                LOGGER.error("failed to stop properly", e);

            }
        });
    }

    private void feedCaches(final FileSource aSource, final Configuration aConfiguration) {
        checkNotNull(aSource);
        checkNotNull(aConfiguration);

        IntStream.range(aConfiguration.getCacheMin(), aConfiguration.getCacheMax())
                 .forEach(i -> {
                     String cacheName;
                     cacheName = "cache-" + i;

                     vertx.executeBlocking(future -> {
                         try {
                             Thread.sleep(aConfiguration.getPause());

                         } catch (InterruptedException e) {
                             e.printStackTrace();

                         }
                         feedCache(cacheName, aSource, aConfiguration.getFrequency());
                         future.complete();
                     }, res -> LOGGER.info("starting to feed cache #" + i));

                 })
                 ;
    }

    private void feedCache(final String aCacheName, final FileSource aSource, final long aFrequency) {
        checkNotNull(aCacheName);
        checkNotNull(aSource);
        checkArgument(aFrequency > 0);

        IgniteCache<String, JsonObject> cache;
        cache = ignite.getOrCreateCache(aCacheName);

        AtomicLong counter;
        counter = new AtomicLong();

        feeders.add(vertx.setPeriodic(aFrequency, f -> {
            JsonObject json;
            json = new JsonObject();

            long index;
            index = counter.incrementAndGet();

            json.put(INDEX, index);
            json.put(TIMESTAMP, System.currentTimeMillis());
            json.put(DATA, new JsonArray(aSource.next()));

            String key;
            key = Long.toString(index);

            cache.put(key, json);
            LOGGER.debug("feed cache=" + aCacheName + " with key="  + key + ", value= " + json);
        }));
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        // caches are not closed but the bench should stop at that time
        vertx.executeBlocking(future -> {
                                feeders.forEach(id -> vertx.cancelTimer(id));
                                future.complete();
                             },
                             res -> LOGGER.info("IgniteFeederVerticle.stop"));
    }
}
