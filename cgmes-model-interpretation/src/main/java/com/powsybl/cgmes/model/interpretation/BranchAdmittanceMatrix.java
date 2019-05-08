/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
class BranchAdmittanceMatrix {

    public void calculateAdmittance(double r, double x, double a1, double angleDegrees1, Complex ysh1, double a2,
            double angleDegrees2, Complex ysh2) {
        double angle1 = Math.toRadians(angleDegrees1);
        double angle2 = Math.toRadians(angleDegrees2);
        Complex aA1 = new Complex(a1 * Math.cos(angle1), a1 * Math.sin(angle1));
        Complex aA2 = new Complex(a2 * Math.cos(angle2), a2 * Math.sin(angle2));

        Complex z = new Complex(r, x);
        y11 = z.reciprocal().add(ysh1).divide(aA1.conjugate().multiply(aA1));
        y12 = z.reciprocal().negate().divide(aA1.conjugate().multiply(aA2));
        y21 = z.reciprocal().negate().divide(aA2.conjugate().multiply(aA1));
        y22 = z.reciprocal().add(ysh2).divide(aA2.conjugate().multiply(aA2));
    }

    Complex y11;
    Complex y12;
    Complex y21;
    Complex y22;
}
