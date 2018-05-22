#
# CGMES scripts
# Copyright (C) 2017 - 2018 RTE (http://rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
# @author Luma Zamarreño <zamarrenolm at aia.es>
#

BEGIN {
	print "case" "\t" "id" "\t" "q" "\t" "minQ" "\t" "maxQ" "\t" "v" "\t" "targetV"; 
	}
	
/testLoadFlow/ {
	scase = $6;
}
/TORS validation error/ {
	id = $8;
	sub(":", "", id);
	q = $13;
	sub("Q=", "", q);
	qmin = $14;
	sub("minQ=", "", qmin);
	qmax = $15;
	sub("maxQ=", "", qmax);
	v = $17;
	sub("V=", "", v);
	targetv = $18;
	sub("targetV=", "", targetv);

	print scase "\t" id "\t" q "\t" qmin "\t" qmax "\t" v "\t" targetv; 
}
