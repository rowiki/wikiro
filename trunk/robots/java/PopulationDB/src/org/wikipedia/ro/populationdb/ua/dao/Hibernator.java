package org.wikipedia.ro.populationdb.ua.dao;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.wikipedia.ro.populationdb.ua.model.Commune;
import org.wikipedia.ro.populationdb.ua.model.Raion;
import org.wikipedia.ro.populationdb.ua.model.Region;
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

    public Region getRegionByTransliteratedName(final String needle) {
        final Session ses = sessionFactory.getCurrentSession();
        final Criteria crit = ses.createCriteria(Region.class).add(Restrictions.eq("transliteratedName", needle));
        final List rez = crit.list();
        if (0 < rez.size()) {
            return (Region) rez.get(0);
        }
        return null;
    }

    public Commune getCommuneByRomanianName(final String needle) {
        final Session ses = sessionFactory.getCurrentSession();
        final Criteria crit = ses.createCriteria(Commune.class).add(Restrictions.eq("romanianName", needle));
        final List rez = crit.list();
        if (0 < rez.size()) {
            return (Commune) rez.get(0);
        }
        return null;
    }

    public Commune getCommuneByTransliteratedName(final String needle) {
        final Session ses = sessionFactory.getCurrentSession();
        final Criteria crit = ses.createCriteria(Commune.class).add(Restrictions.eq("transliteratedName", needle));
        final List rez = crit.list();
        if (0 < rez.size()) {
            return (Commune) rez.get(0);
        }
        return null;
    }

    public Commune getCommuneByTransliteratedNameAndRaion(final String needle, final Raion raion) {
        final Session ses = sessionFactory.getCurrentSession();
        final Criteria crit = ses.createCriteria(Commune.class).add(Restrictions.eq("transliteratedName", needle))
            .add(Restrictions.eq("raion", raion));
        final List rez = crit.list();
        if (0 < rez.size()) {
            return (Commune) rez.get(0);
        }
        return null;
    }

    public Commune getCommuneByTransliteratedNameAndRaion(final String needle, final Region region) {
        final Session ses = sessionFactory.getCurrentSession();
        final Criteria crit = ses.createCriteria(Commune.class).add(Restrictions.eq("transliteratedName", needle))
            .add(Restrictions.eq("region", region));
        final List rez = crit.list();
        if (0 < rez.size()) {
            return (Commune) rez.get(0);
        }
        return null;
    }

    public Raion getRaionByTransliteratedNameAndRegion(final String needle, final Region region) {
        final Session ses = sessionFactory.getCurrentSession();
        final Criteria crit = ses.createCriteria(Raion.class).add(Restrictions.eq("transliteratedName", needle))
            .add(Restrictions.eqOrIsNull("region", region));
        final List rez = crit.list();
        if (0 < rez.size()) {
            return (Raion) rez.get(0);
        }
        return null;
    }

}
