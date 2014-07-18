package org.wikipedia.ro.populationdb.ua.dao;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wikipedia.ro.populationdb.util.HibernateUtil;

public class Hibernator {
    private SessionFactory sessionFactory;

    public Hibernator() {
        final URL url = this.getClass().getResource(".");
        File f;
        try {
            f = new File(new File(url.toURI()).getParentFile(), "hibernate.cfg.xml");
            sessionFactory = HibernateUtil.getSessionFactory(f);
        } catch (final URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    public void close() throws IOException {
        if (null != sessionFactory) {
            sessionFactory.close();
        }
    }
}
