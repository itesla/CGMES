package com.powsybl.triplestore;

/*
 * #%L
 * Triple stores for CGMES models
 * %%
 * Copyright (C) 2017 - 2018 RTE (http://rte-france.com)
 * %%
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * #L%
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.commons.datasource.DataSource;
import com.powsybl.commons.datasource.ReadOnlyDataSource;

/**
 * @author Luma Zamarreño <zamarrenolm at aia.es>
 */
public abstract class AbstractPowsyblTripleStore {
    public AbstractPowsyblTripleStore() {
        queryPrefixes = new ArrayList<>();
        addQueryPrefix("prefix rdf: <" + RDF_NAMESPACE + ">");
    }

    public void addQueryPrefix(String prefix) {
        queryPrefixes.add(prefix);
        cacheQueryPrefixes();
    }

    public void deserialize(ReadOnlyDataSource dataSource) {
        String base = baseName(dataSource);
        for (String filename : filenames(dataSource)) {
            LOG.info("deserializing [{}]", filename);
            try (InputStream is = dataSource.newInputStream(filename)) {
                deserialize(is, filename, base);
            } catch (IOException e) {
                String msg = String.format("Deserializing file [%s]", filename);
                LOG.warn(msg);
                throw new TripleStoreException(msg, e);
            }
        }
    }

    private String baseName(ReadOnlyDataSource dataSource) {
        // Build an absolute IRI from the data source base name
        String ds = dataSource.getBaseName().toLowerCase();
        if (ds.isEmpty()) {
            ds = "default";
        }
        return "http://" + ds;
    }

    public abstract void deserialize(InputStream is, String filename, String base);

    public abstract void serialize(DataSource ds);

    public abstract void dump(PrintStream out);

    public abstract void clear(String context);

    public void dump(Consumer<String> liner) {
        dump(new PrintStream(new LinesOutputStream(liner)));
    }

    public abstract PropertyBags query(String query);

    public abstract void add(String graph, String type, PropertyBags objects);

    // fileFromContext and contextFromFile should be at this level ...
    // But some triple stores use named graphs and other use implementation specific Resources
    protected String namespaceForContexts() {
        return NAMESPACE_FOR_CONTEXTS;
    }

    protected OutputStream outputStream(DataSource ds, String cname) {
        try {
            boolean append = false;
            return ds.newOutputStream(fileNameFromContextName(cname), append);
        } catch (IOException x) {
            throw new TripleStoreException(
                    String.format("New output stream %s in data source %s", cname, ds), x);
        }
    }

    private String fileNameFromContextName(String contextName) {
        // Remove the namespace prefix for contexts
        String fname = contextName.replaceFirst(namespaceForContexts(), "");
        // filename could contain a path, take only the last component of the path
        fname = fname.replaceAll("^.*/", "");
        return fname;
    }

    protected String adjustedQuery(String q) {
        String q1 = cachedQueryPrefixes + q;
        if (LOG.isDebugEnabled()) {
            LOG.debug("prepared query [{}{}]", System.lineSeparator(), q1);
        }
        return q1;
    }

    protected void cacheQueryPrefixes() {
        cachedQueryPrefixes = queryPrefixes.stream().collect(Collectors.joining(" "));
    }

    static class LinesOutputStream extends OutputStream {
        LinesOutputStream(Consumer<String> liner) {
            this.liner = liner;
        }

        @Override
        public void write(int b) throws IOException {
            byte[] bytes = new byte[1];
            bytes[0] = (byte) (b & 0xff);
            line = line + new String(bytes);
            if (line.endsWith(System.lineSeparator())) {
                line = line.substring(0, line.length() - 1);
                flush();
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            // We don't implement an optimal function,
            // we only call the function byte to byte.
            if ((off | len | (b.length - (len + off)) | (off + len)) < 0) {
                throw new IndexOutOfBoundsException();
            }

            for (int i = 0; i < len; i++) {
                write(b[off + i]);
            }
        }

        @Override
        public void flush() {
            liner.accept(line);
            line = "";
        }

        private final Consumer<String> liner;
        private String                 line = "";
    }

    // FIXME This code is duplicated in CgmesModel
    public static String[] filenames(ReadOnlyDataSource dataSource) {
        String[] filenames;
        try {
            filenames = dataSource.listFileNames("^.*\\.xml$");
        } catch (IOException x) {
            throw new TripleStoreException(
                    String.format("Listing filenames in data source %s", dataSource), x);
        }
        if (filenames == null || filenames.length == 0) {
            throw new TripleStoreException(
                    String.format("Data source %s does not contain filenames", dataSource));
        }
        return filenames;
    }

    private List<String>        queryPrefixes;
    private String              cachedQueryPrefixes;

    private static final String RDF_NAMESPACE          = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String NAMESPACE_FOR_CONTEXTS = "files:";
    private static final Logger LOG                    = LoggerFactory
            .getLogger(AbstractPowsyblTripleStore.class);
}
