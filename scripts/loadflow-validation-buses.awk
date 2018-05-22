#
# CGMES scripts
# Copyright (C) 2017 - 2018 RTE (http://rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
# @author Luma Zamarreño <zamarrenolm at aia.es>
#

/BUSES validation error.* P / {
	vl = $9;
	print "DEBUG " vl;
	substation[vl] = $8;
	p[vl] = $11 + $12;
}
/BUSES validation error.* Q / {
	vl = $9;
	print "DEBUG " vl;
	substation[vl] = $8;
	q[vl] = $11 + $12;
}
END {
	print "Substation" "\t" "Voltage level" "\t" "Mismatch P" "\t" "Mismatch Q"
	for (vl in substation) {
		print substation[vl] "\t" vl "\t" p[vl] "\t" q[vl]
	}
}
