package com.powsybl.cgmes.conversion.validation;

public class ElementFlowValidation {

    public ElementFlowValidation(String cgmesId) {
        super();
        this.cgmesId = cgmesId;
    }

    public double getP() {
        return p;
    }

    public void setP(double p) {
        this.p = p;
    }

    public double getQ() {
        return q;
    }

    public void setQ(double q) {
        this.q = q;
    }

    public boolean isApproximateValue() {
        return isApproximateValue;
    }

    public void setApproximateValue(boolean isApproximateValue) {
        this.isApproximateValue = isApproximateValue;
    }

    private String  cgmesId;
    private double  p;
    private double  q;
    private boolean isApproximateValue;
}
