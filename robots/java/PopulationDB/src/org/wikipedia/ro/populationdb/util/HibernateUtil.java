package org.wikipedia.ro.populationdb.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static final Map<String, SessionFactory> sessionFactories = new HashMap<String, SessionFactory>();

    private static SessionFactory buildSessionFactory(final File f) {
        try {
            // Create the SessionFactory from hibernate.cfg.xml
            if (null == f) {
                return new Configuration().configure().buildSessionFactory();
            } else {
                return new Configuration().configure(f).buildSessionFactory();
            }
        } catch (final Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory(final File f) {
        final String key = null == f ? null : f.getAbsolutePath();
        final SessionFactory sf = ObjectUtils.defaultIfNull(sessionFactories.get(key), buildSessionFactory(f));
        sessionFactories.put(key, sf);
        return sf;
    }
}
