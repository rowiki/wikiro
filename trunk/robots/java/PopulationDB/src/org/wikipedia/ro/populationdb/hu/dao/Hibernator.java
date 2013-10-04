package org.wikipedia.ro.populationdb.hu.dao;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wikipedia.ro.populationdb.hu.model.County;
import org.wikipedia.ro.populationdb.hu.model.District;
import org.wikipedia.ro.populationdb.hu.model.Nationality;
import org.wikipedia.ro.populationdb.hu.model.Religion;
import org.wikipedia.ro.populationdb.hu.model.Settlement;
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
        ses.beginTransaction();
        final Query findCounty = ses.createQuery("from County county where county.name=:countyName");
        findCounty.setParameter("countyName", countyName);
        final List<County> res = findCounty.list();
        if (0 == res.size()) {
            ret = new County();
            ret.setName(countyName);
            ses.saveOrUpdate(ret);
            ses.getTransaction().commit();
        } else {
            ret = res.get(0);
            ses.getTransaction().rollback();
        }
        return ret;
    }

    public District getDistrictByName(final String districtName, final String countyName) {
        final County county = getCountyByName(countyName);
        final Session ses = sessionFactory.getCurrentSession();
        ses.beginTransaction();
        final Query findDistrict = ses
            .createQuery("from District district where district.name=:districtName and district.county.name=:countyName");
        findDistrict.setParameter("districtName", districtName);
        findDistrict.setParameter("countyName", countyName);
        final List<District> res = findDistrict.list();
        District ret = null;
        if (0 == res.size()) {
            ret = new District();
            ret.setName(districtName);
            ret.setCounty(county);
            ses.saveOrUpdate(ret);
            ses.getTransaction().commit();
        } else {
            ret = res.get(0);
            ses.getTransaction().rollback();
        }
        return ret;
    }

    public Nationality getNationalityByName(final String natName) {
        final Session ses = sessionFactory.getCurrentSession();
        ses.beginTransaction();
        final Query findNat = ses.createQuery("from Nationality nat where nat.name=:natName");
        findNat.setParameter("natName", natName);
        final List<Nationality> res = findNat.list();
        Nationality ret = null;
        if (0 == res.size()) {
            ret = new Nationality();
            ret.setName(natName);
            ses.saveOrUpdate(ret);
            ses.getTransaction().commit();
        } else {
            ret = res.get(0);
            ses.getTransaction().rollback();
        }
        return ret;
    }

    public Religion getReligionByName(final String relName) {
        final Session ses = sessionFactory.getCurrentSession();
        ses.beginTransaction();
        final Query findNat = ses.createQuery("from Religion rel where rel.name=:relName");
        findNat.setParameter("relName", relName);
        final List<Religion> res = findNat.list();
        Religion ret = null;
        if (0 == res.size()) {
            ret = new Religion();
            ret.setName(relName);
            ses.saveOrUpdate(ret);
            ses.getTransaction().commit();
        } else {
            ret = res.get(0);
            ses.getTransaction().rollback();
        }
        return ret;
    }

    public Settlement getSettlementByName(final String settlementName, final District district, final int town) {
        final Session ses = sessionFactory.getCurrentSession();
        ses.beginTransaction();
        final Query findSettlement = ses
            .createQuery("from Settlement s where s.name=:settlementName and s.district=:district");
        findSettlement.setParameter("settlementName", settlementName);
        findSettlement.setParameter("district", district);
        final List<Settlement> res = findSettlement.list();
        Settlement ret = null;
        if (0 == res.size()) {
            ret = new Settlement();
            ret.setName(settlementName);
            ret.setDistrict(district);
            ret.setTown(town);
            ses.saveOrUpdate(ret);
            ses.getTransaction().commit();
        } else {
            ret = res.get(0);
            ses.getTransaction().rollback();
        }
        return ret;
    }

    public void close() throws IOException {
        if (null != sessionFactory) {
            sessionFactory.close();
        }
    }

    public void saveSettlement(final Settlement settlement) {
        final Session ses = sessionFactory.getCurrentSession();
        ses.beginTransaction();
        ses.saveOrUpdate(settlement);
        ses.getTransaction().commit();
    }

    public void storeDistrict(final District district, final County county) {
        final Session ses = sessionFactory.getCurrentSession();
        ses.beginTransaction();
        district.setCounty(county);
        // county.getDistricts().add(district);
        ses.saveOrUpdate(county);
        ses.getTransaction().commit();
    }

    public Session getSession() {
        return sessionFactory.getCurrentSession();
    }
}
