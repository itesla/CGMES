/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import org.apache.commons.math3.complex.Complex;

/**
 * XXX LUMA Review authors in separate lines
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
// XXX LUMA Review this class should be named BranchAdmittanceMatrix ???
class AdmittanceMatrix {

    // XXX Luma Review Uniform naming. Should we use 11, 12, 21, 22 instead of ff,
    // ft, tf, tt ???
    Complex yff;
    Complex yft;
    Complex ytf;
    Complex ytt;

    // XXX LUMA Review Why we put here a detected branch model ? This is not part of
    // an admittance matrix
    DetectedBranchModel branchModel;
}
