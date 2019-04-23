package com.powsybl.cgmes.conversion.validation;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */

public class FlowData {

    public enum BranchEndType {
        LINE_ONE, LINE_TWO, XFMR2_ONE, XFMR2_TWO, XFMR3_ONE, XFMR3_TWO, XFMR3_THREE
    }

    public FlowData(String id, BranchEndType endType, double pIidm, double qIidm, double pCgmes, double qCgmes) {
        this.id = id;
        this.endType = endType;
        this.pIidm = pIidm;
        this.qIidm = qIidm;
        this.pCgmes = pCgmes;
        this.qCgmes = qCgmes;
        this.calculated = true;
    }

    public FlowData(String id, BranchEndType endType) {
        this.id = id;
        this.endType = endType;
        this.pIidm = 0.0;
        this.qIidm = 0.0;
        this.pCgmes = 0.0;
        this.qCgmes = 0.0;
        this.calculated = false;
    }

    public String code() {
        StringBuilder code = new StringBuilder();
        switch (endType) {
            case LINE_ONE:
                code.append("LINE-1.");
                break;
            case LINE_TWO:
                code.append("LINE-2.");
                break;
            case XFMR2_ONE:
                code.append("XFMR2-1.");
                break;
            case XFMR2_TWO:
                code.append("XFMR2-2.");
                break;
            case XFMR3_ONE:
                code.append("XFMR3-1.");
                break;
            case XFMR3_TWO:
                code.append("XFMR3-2.");
                break;
            case XFMR3_THREE:
                code.append("XFMR3-3.");
                break;
        }

        code.append(id);

        return code.toString();
    }

    final String        id;
    final BranchEndType endType;
    final double        pIidm;
    final double        qIidm;
    final double        pCgmes;
    final double        qCgmes;
    final boolean       calculated;

}
