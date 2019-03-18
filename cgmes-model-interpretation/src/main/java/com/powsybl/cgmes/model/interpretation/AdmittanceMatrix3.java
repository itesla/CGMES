/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

/**
 * @author José Antonio Marqués <marquesja at aia.es>, Marcos de Miguel <demiguelm at aia.es>
 */
class AdmittanceMatrix3 {

    public AdmittanceMatrix3() {
        super();
        end1 = new AdmittanceMatrix();
        end2 = new AdmittanceMatrix();
        end3 = new AdmittanceMatrix();
    }

    AdmittanceMatrix end1;
    AdmittanceMatrix end2;
    AdmittanceMatrix end3;
}
