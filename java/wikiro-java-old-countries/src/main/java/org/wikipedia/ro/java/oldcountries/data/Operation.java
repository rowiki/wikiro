package org.wikipedia.ro.java.oldcountries.data;

import org.wikibase.data.Claim;
import org.wikibase.data.Property;
import org.wikibase.data.WikibaseData;

import lombok.Data;

@Data
public class Operation
{
    private OperationType type;
    private String claimId;
    private Claim newClaim;
    private Claim oldClaim;
    private String qualifierId;
    private Property qualifierProperty;
    private WikibaseData qualifierData;
}
