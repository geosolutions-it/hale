package net.opengis.gml.v_3_1_1;

import java.math.BigInteger;

public interface SRSReferenceGroup extends SRSInformationGroup {

	public String getSrsName();

	public void setSrsName(String value);

	public BigInteger getSrsDimension();

	public void setSrsDimension(BigInteger value);

}
