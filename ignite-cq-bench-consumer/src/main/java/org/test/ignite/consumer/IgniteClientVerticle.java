package org.test.ignite.consumer;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;

import javax.cache.Cache;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author ctranxuan
 */
public class IgniteClientVerticle extends AbstractVerticle {
     private static final Logger SIMULATION_LOGGER = LoggerFactory.getLogger("simulation");
    private static final Logger LOGGER = LoggerFactory.getLogger(IgniteClientVerticle.class);

    private static final String INDEX = "index";
    private static final String TIMESTAMP = "timestamp";

    private static final String REPORT_OK = "OK";
    private static final String REPORT_KO = "KO";

    private final Ignite ignite;
    private final List<QueryCursor<Cache.Entry<String, JsonObject>>> cursors;

    private String scenarioName;

    public IgniteClientVerticle() {
        super();
        ignite = Ignition.start("ignite-cq.xml");
        cursors = Lists.newArrayList();
    }

    @Override
    public void start() {
        Configuration configuration;
        configuration = new Configuration(config());

        LOGGER.info("Consumer start");

        writeSimulationHeader(configuration);
        readCaches(configuration);

        vertx.setTimer(configuration.getDuration(), l -> {
            try {
                stop();
                System.exit(0);

            } catch (Exception e) {
                LOGGER.error("failed to stop properly", e);

            }
        });
    }

    private void readCaches(final Configuration aConfiguration) {
        checkNotNull(aConfiguration);
        IntStream.range(aConfiguration.getCacheMin(), aConfiguration.getCacheMax())
                 .forEach(i -> {
                        String cacheName;
                         cacheName = "cache-" + i;

                         readCache(aConfiguration, cacheName);
                         LOGGER.info("starting to read cache #" + i);
                 });
    }

    private void readCache(final Configuration aConfiguration, final String aCacheName) {
        checkNotNull(aConfiguration);
        checkNotNull(aCacheName);

        IgniteCache<String, JsonObject> cache;
        cache = ignite.getOrCreateCache(aCacheName);

        ContinuousQuery<String, JsonObject> query;
        query = new ContinuousQuery<>();

        query.setLocalListener(new CacheEntryUpdatedListener<String, JsonObject>() {
            private long lastIndex;

            @Override
            public void onUpdated(
                    final Iterable<CacheEntryEvent<? extends String, ? extends JsonObject>> events) throws CacheEntryListenerException {
                LOGGER.debug("CQ update events received for " + aCacheName + ", size=" + Iterables.size(events) + " - " + StreamSupport.stream(events.spliterator(), false)
                                                             .map(Cache.Entry::getKey)
                                                             .collect(Collectors.joining(", ")));
                events.forEach(event -> {
                    LOGGER.debug("cache=" + aCacheName + ", key=" + event.getKey() + ", val=" + event.getValue());
                    LOGGER.debug("cache=" + aCacheName + ", key=" + event.getKey() + ", time=" + (System.currentTimeMillis() - event.getValue().getLong(TIMESTAMP)));

                    Long currentIndex;
                    currentIndex = event.getValue().getLong(INDEX);

                    if (currentIndex == lastIndex + 1) {
                        writeSuccessLog(aCacheName, aConfiguration.getScenarioName(), event, System.currentTimeMillis());

                    } else if (currentIndex < lastIndex) {
                        LOGGER.error("received current index=" + currentIndex + " less than lastIndex=" + lastIndex);
                        writeErrorLog(aCacheName, aConfiguration.getScenarioName(), event, System.currentTimeMillis(), "received current index=" + currentIndex + " less than lastIndex=" + lastIndex);

                    } else {
                        long count;
                        count = currentIndex - lastIndex - 1;

                        for (int i = 0; i < count; i++) {
                            LOGGER.error("misses index " + (lastIndex + i) + " (currentIndex was " + currentIndex + ")");
                            writeErrorLog(aCacheName, aConfiguration.getScenarioName(), event, System.currentTimeMillis(), "misses index " + (lastIndex + i));

                        }
                    }

                    lastIndex = currentIndex;
                });

            }
        });

        query.setRemoteFilterFactory(() -> event -> true);

        QueryCursor<Cache.Entry<String, JsonObject>> cursor;
        cursor = cache.query(query);

        cursors.add(cursor);

        // Iterate through existing data.
        for (Cache.Entry<String, JsonObject> e : cursor) {
            System.out.println("Queried existing entry [key=" + e.getKey() + ", val=" + e.getValue() + ']');

        }
    }

    private void writeSuccessLog(final String aUserId, String aScenarioName, final CacheEntryEvent<? extends String, ? extends JsonObject> aEvent, final long aEnd) {
        JsonObject json;
        json = aEvent.getValue();

        Long start;
        start = json.getLong(TIMESTAMP);

        SIMULATION_LOGGER.info("REQUEST\t{}\t{}\t\tread\t{}\t{}\t{}\t{}\tOK\t ", aScenarioName, aUserId, start, start, aEnd, aEnd);
    }

    private void writeErrorLog(final String aUserId, String aScenarioName, final CacheEntryEvent<? extends String, ? extends JsonObject> aEvent,
                               final long aEnd, final String aErrorMsg) {

        SIMULATION_LOGGER.info("REQUEST\t{}\t{}\t\tread\t{}\t{}\t{}\t{}\tKO\t{}", aScenarioName, aUserId, aEnd, aEnd, aEnd, aEnd, aErrorMsg == null ? "" : aErrorMsg);
    }

    private void writeSimulationHeader(final Configuration aConfiguration) {
        checkNotNull(aConfiguration);

        LocalDateTime date;
        date = LocalDateTime.now();

        DateTimeFormatter formatter;
        formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        int cacheNber;
        cacheNber = aConfiguration.getCacheMax() - aConfiguration.getCacheMin();

        SIMULATION_LOGGER.info("RUN\t{}\tignitecachewith{}caches\t ", date.format(formatter), cacheNber);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        vertx.executeBlocking(future -> {
            cursors.forEach(QueryCursor::close);
            future.complete();
        }, res -> System.out.println("IgniteClientVerticle.stop"));
    }
}
