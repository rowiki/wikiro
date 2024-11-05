package org.wikipedia.ro.java.oldcountries.data;

import org.wikibase.data.Claim;

import lombok.Data;

@Data
public class Operation
{
    private OperationType type;
    private String claimId;
    private Claim newClaim;
    private Claim oldClaim;
}
