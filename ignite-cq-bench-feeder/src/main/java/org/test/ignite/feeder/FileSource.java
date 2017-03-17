package org.test.ignite.feeder;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author ctranxuan
 */
public final class FileSource {
    private Iterator<String> cycle;

    public FileSource(String aFileName) {
        checkNotNull(aFileName);

        try {
            URL resource;
            resource = getClass().getClassLoader().getResource(aFileName);

            // quite concise to write, not the best way to handle lines but...
            String text;
            text = Resources.toString(resource, StandardCharsets.UTF_8);

            cycle = Iterables.cycle(Splitter.on("\n").split(text)).iterator();

        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public String next() {
        return cycle.next();
    }

}
