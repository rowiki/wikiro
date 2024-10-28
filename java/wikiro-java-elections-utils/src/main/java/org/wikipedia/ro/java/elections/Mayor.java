package org.wikipedia.ro.java.elections;

import org.bson.types.ObjectId;

public class Mayor
{
    private ObjectId id;
    private String uat;
    private String county;
    private int siruta;
    private String firstName;
    private String lastName;
    private String partyQId;
    private String partyName;

    public ObjectId getId()
    {
        return id;
    }

    public void setId(ObjectId id)
    {
        this.id = id;
    }

    public String getUat()
    {
        return uat;
    }

    public void setUat(String uat)
    {
        this.uat = uat;
    }

    public String getCounty()
    {
        return county;
    }

    public void setCounty(String county)
    {
        this.county = county;
    }

    public int getSiruta()
    {
        return siruta;
    }

    public void setSiruta(int siruta)
    {
        this.siruta = siruta;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    public String getPartyQId()
    {
        return partyQId;
    }

    public void setPartyQId(String partyQId)
    {
        this.partyQId = partyQId;
    }

    public String getPartyName()
    {
        return partyName;
    }

    public void setPartyName(String partyName)
    {
        this.partyName = partyName;
    }
}
