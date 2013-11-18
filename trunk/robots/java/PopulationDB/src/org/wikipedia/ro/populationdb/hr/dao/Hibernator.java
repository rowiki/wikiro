package org.wikipedia.ro.populationdb.hr.dao;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wikipedia.ro.populationdb.hr.model.Commune;
import org.wikipedia.ro.populationdb.hr.model.County;
import org.wikipedia.ro.populationdb.hr.model.Nationality;
import org.wikipedia.ro.populationdb.hr.model.Religion;
import org.wikipedia.ro.populationdb.util.HibernateUtil;

public class Hibernator implements Closeable {

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

    public County getCountyByName(final String countyName) {
        County ret = null;
        final Session ses = sessionFactory.getCurrentSession();
        final Query findCounty = ses.createQuery("from County county where county.name=:countyName");
        findCounty.setParameter("countyName", countyName);
        final List<County> res = findCounty.list();
        if (0 == res.size()) {
            ret = new County();
            ret.setName(countyName);
            ses.saveOrUpdate(ret);
        } else {
            ret = res.get(0);
        }
        return ret;
    }

    public Nationality getNationalityByName(final String natName) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query findNat = ses.createQuery("from Nationality nat where nat.name=:natName");
        findNat.setParameter("natName", natName);
        final List<Nationality> res = findNat.list();
        Nationality ret = null;
        if (0 == res.size()) {
            ret = new Nationality();
            ret.setName(natName);
            ses.saveOrUpdate(ret);
        } else {
            ret = res.get(0);
        }
        return ret;
    }

    public Religion getReligionByName(final String relName) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query findNat = ses.createQuery("from Religion rel where rel.name=:relName");
        findNat.setParameter("relName", relName);
        final List<Religion> res = findNat.list();
        Religion ret = null;
        if (0 == res.size()) {
            ret = new Religion();
            ret.setName(relName);
            ses.saveOrUpdate(ret);
        } else {
            ret = res.get(0);
        }
        return ret;
    }

    public Commune getCommuneByName(final String communeName, final County county, final int town) {
        final Session ses = sessionFactory.getCurrentSession();
        final Query findSettlement = ses.createQuery("from Commune s where s.name=:communeName and s.county=:county");
        findSettlement.setParameter("communeName", communeName);
        findSettlement.setParameter("county", county);
        final List<Commune> res = findSettlement.list();
        Commune ret = null;
        if (0 == res.size()) {
            ret = new Commune();
            ret.setName(communeName);
            ret.setCounty(county);
            ret.setTown(town);
            ses.saveOrUpdate(ret);
        } else {
            ret = res.get(0);
        }
        return ret;
    }

    public List<County> getAllCounties() {
        final Session ses = sessionFactory.getCurrentSession();
        final Criteria countyCrit = ses.createCriteria(County.class).addOrder(Order.asc("name"));
        return countyCrit.list();
    }

    public void close() throws IOException {
        if (null != sessionFactory) {
            sessionFactory.close();
        }
    }

    public void saveCommune(final Commune commune) {
        final Session ses = sessionFactory.getCurrentSession();
        ses.saveOrUpdate(commune);
    }

    public Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    public List<Commune> getCommunesByCounty(final County county) {
        final Session ses = sessionFactory.getCurrentSession();
        final Criteria comCrit = ses.createCriteria(Commune.class).add(Restrictions.eq("county", county))
            .addOrder(Order.asc("name"));
        return comCrit.list();
    }

    public long countCommunesWithName(String name) {
        final Session ses = sessionFactory.getCurrentSession();
        Query comCrit = ses.createQuery("select count(com) from Commune com where com.name=:name");
        comCrit.setParameter("name", name);
        return (Long) comCrit.uniqueResult();
    }

    public List<Commune> getCommunesWithName(String name) {
        final Session ses = sessionFactory.getCurrentSession();
        Query comCrit = ses
            .createQuery("select com from Commune com left join com.county as cty where com.name=:name order by com.town,cty.name");
        comCrit.setParameter("name", name);
        return comCrit.list();
    }
}
