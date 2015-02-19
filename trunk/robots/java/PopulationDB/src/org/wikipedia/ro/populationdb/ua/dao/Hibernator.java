package org.wikipedia.ro.populationdb.ua.dao;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;

import static org.hibernate.criterion.Order.*;
import static org.hibernate.criterion.Restrictions.*;

import org.wikipedia.ro.populationdb.ua.model.Commune;
import org.wikipedia.ro.populationdb.ua.model.Language;
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
        final Session ses = sessionFactory.getCurrentSession();
        return ses;
    }

    public void close() throws IOException {
        if (null != sessionFactory) {
            sessionFactory.close();
        }
    }

    public Region getRegionByTransliteratedName(final String needle) {
        final Session ses = sessionFactory.getCurrentSession();
        final Criteria crit = ses.createCriteria(Region.class).add(eq("transliteratedName", needle));
        final List rez = crit.list();
        if (0 < rez.size()) {
            return (Region) rez.get(0);
        }
        return null;
    }

    public Commune getCommuneByRomanianName(final String needle) {
        final Session ses = sessionFactory.getCurrentSession();
        final Criteria crit = ses.createCriteria(Commune.class).add(eq("romanianName", needle));
        final List rez = crit.list();
        if (0 < rez.size()) {
            return (Commune) rez.get(0);
        }
        return null;
    }

    public Commune getCommuneByTransliteratedName(final String needle) {
        final Session ses = sessionFactory.getCurrentSession();
        final Criteria crit = ses.createCriteria(Commune.class).add(eq("transliteratedName", needle));
        final List rez = crit.list();
        if (0 < rez.size()) {
            return (Commune) rez.get(0);
        }
        return null;
    }

    public Commune getCommuneByTransliteratedNameAndRaion(final String needle, final Raion raion) {
        final Session ses = sessionFactory.getCurrentSession();
        final Criteria crit = ses.createCriteria(Commune.class).add(eq("transliteratedName", needle))
            .add(eq("raion", raion));
        final List rez = crit.list();
        if (0 < rez.size()) {
            return (Commune) rez.get(0);
        }
        return null;
    }

    public Commune getCommuneByTransliteratedNameAndRegion(final String needle, final Region region) {
        final Session ses = sessionFactory.getCurrentSession();
        /*
         * final Criteria crit = ses.createCriteria(Commune.class).add(eq("transliteratedName", needle)) .add(or(eq("region",
         * region), and(isNotNull("raion"), eq("raion.region", region))));
         */
        Query q = ses
            .createQuery("select c from Commune c left join c.raion as raion where c.transliteratedName=:transl and (c.region=:reg or (raion is not null and raion.region=:reg))");
        q.setParameter("transl", needle);
        q.setParameter("reg", region);
        final List rez = q.list();
        if (0 < rez.size()) {
            return (Commune) rez.get(0);
        }
        return null;
    }

    public Raion getRaionByTransliteratedNameAndRegion(final String needle, final Region region) {
        final Session ses = sessionFactory.getCurrentSession();
        final Criteria crit = ses.createCriteria(Raion.class).add(eq("transliteratedName", needle))
            .add(eqOrIsNull("region", region)).add(eq("miskrada", Boolean.FALSE));
        final List rez = crit.list();
        if (0 < rez.size()) {
            return (Raion) rez.get(0);
        }
        return null;
    }

    public Language getLanguageByName(final String languageName) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query findNat = ses.createQuery("from Language nat where nat.name=:natName");
        findNat.setParameter("natName", languageName);
        final List<Language> res = findNat.list();
        Language ret = null;
        if (0 == res.size()) {
            ret = new Language();
            ret.setName(languageName);
            ses.saveOrUpdate(ret);
        } else {
            ret = res.get(0);
        }
        return ret;
    }

    public List<Region> getAllRegions() {
        final Session ses = sessionFactory.getCurrentSession();
        final Criteria crit = ses.createCriteria(Region.class).addOrder(asc("name"));
        final List list = crit.list();
        return list;
    }

    public List<Commune> getRegionalCitiesForRegion(final Region eachReg) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query q = ses
            .createQuery("from Commune com where (com.raion is null and com.region=:region) or (com.raion is not null and com.raion.miskrada=:true and com.raion.region=:region)");
        q.setParameter("region", eachReg);
        q.setParameter("true", Boolean.TRUE);
        return q.list();
    }

    public List<Raion> getRaionsForRegion(final Region eachReg) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query q = ses.createQuery("from Raion r where r.miskrada is not true and r.region=:region");
        q.setParameter("region", eachReg);
        return q.list();
    }

    public List<Raion> getRaionsByRomanianOrTransliteratedName(final String roName) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query q = ses.createQuery("from Raion r where r.transliteratedName=:translName or r.romanianName=:translName");
        q.setParameter("translName", roName);
        return q.list();
    }

    public int countRaionsByRomanianOrTransliteratedName(final String transliteratedName) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query q = ses
            .createQuery("select count(r) from Raion r where r.transliteratedName=:translName or r.romanianName=:translName");
        q.setParameter("translName", transliteratedName);
        final Long uniqueResult = (Long) q.uniqueResult();
        return uniqueResult.intValue();
    }

    public int countCommunesByRomanianOrTransliteratedName(final String transliteratedName) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query q = ses
            .createQuery("select count(c) from Commune c where c.transliteratedName=:translName or c.romanianName=:translName");
        q.setParameter("translName", transliteratedName);
        final Long uniqueResult = (Long) q.uniqueResult();
        return uniqueResult.intValue();
    }

    public int countCommunesInRegionByRomanianOrTransliteratedName(final String transliteratedName, final Region region) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query q = ses
            .createQuery("select count(c) from Commune c where (c.transliteratedName=:translName or c.romanianName=:translName) and ((c.raion is not null and c.raion.region=:region) or c.region=:region)");
        q.setParameter("translName", transliteratedName);
        q.setParameter("region", region);
        final Long uniqueResult = (Long) q.uniqueResult();
        return uniqueResult.intValue();
    }

    public int countSettlementsInRaionByRomanianOrTransliteratedName(final String transliteratedName, final Raion raion) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query q = ses
            .createQuery("select count(s) from Settlement s left join s.commune as c where (s.transliteratedName=:translName or s.romanianName=:translName) and (c.raion is not null and c.raion=:raion)");
        q.setParameter("translName", transliteratedName);
        q.setParameter("raion", raion);
        final Long uniqueResult = (Long) q.uniqueResult();
        return uniqueResult.intValue();
    }

    public int countSettlementsByRomanianOrTransliteratedName(final String transliteratedName) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query q = ses
            .createQuery("select count(s) from Settlement s where (s.transliteratedName=:translName or s.romanianName=:translName)");
        q.setParameter("translName", transliteratedName);
        final Long uniqueResult = (Long) q.uniqueResult();
        return uniqueResult.intValue();
    }

    public int countSettlementsInRegionByRomanianOrTransliteratedName(final String transliteratedName, final Region region) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query q = ses
            .createQuery("select count(s) from Settlement s left join s.commune as c where (s.transliteratedName=:translName or s.romanianName=:translName) and ((c.raion is not null and c.raion.region=:region) or c.region=:region)");
        q.setParameter("translName", transliteratedName);
        q.setParameter("region", region);
        final Long uniqueResult = (Long) q.uniqueResult();
        return uniqueResult.intValue();
    }

    public List<Raion> findOuterRaionsForCity(Commune city) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query q = ses.createQuery("select raion from Raion raion left join raion.capital as c where c=:city");
        q.setParameter("city", city);
        return q.list();
    }

    public void saveRegion(final Region region) {
        final Session ses = sessionFactory.getCurrentSession();
        ses.saveOrUpdate(region);
        ses.getTransaction().commit();
    }

    public Commune getUnassignedCommuneByTransliteratedName(String cityTranslName) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query q = ses
            .createQuery("from Commune c where c.raion is null and c.region is null and c.transliteratedName=:name");
        q.setParameter("name", cityTranslName);
        List<Commune> communes = q.list();
        if (null == communes || 0 == communes.size()) {
            return null;
        }
        return communes.get(0);
    }

    public Raion getMiskradaByTransliteratedNameAndRegion(String miskradaTranslName, Region reg) {
        final Session ses = sessionFactory.getCurrentSession();
        Query q = null;
        if (null == reg) {
            q = ses.createQuery("from Raion r where r.miskrada=:true and r.region is null and r.transliteratedName=:name");
        } else {
            q = ses.createQuery("from Raion r where r.miskrada=:true and r.region=:reg and r.transliteratedName=:name");
        }
        q.setParameter("name", miskradaTranslName);
        q.setParameter("true", Boolean.TRUE);
        if (null != reg) {
            q.setParameter("reg", reg);
        }
        List<Raion> miskradas = q.list();
        if (null == miskradas || 0 == miskradas.size()) {
            return null;
        }
        return miskradas.get(0);
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

}
