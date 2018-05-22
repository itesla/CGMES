#
# CGMES scripts
# Copyright (C) 2017 - 2018 RTE (http://rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
# @author Luma Zamarreño <zamarrenolm at aia.es>
#

# Use:
# cat *.xml | awk -f ${APP_HOME}/scripts/check-cn-tn-bijection.awk
# The script reads EQ, TP files and checks relationship between connectivity and topological nodes

# Current element is a Terminal
/cim:Terminal.rdf.ID/ {
	terminal = id($0);
}
/cim:Terminal.rdf.about/ {
	terminal = about($0);
}

# Properties of a Terminal
/cim:Terminal.ConnectivityNode.rdf.resource/ {
	terminal_connectivityNode[terminal] = resource($0);
}
/cim:Terminal.TopologicalNode.rdf.resource/ {
	terminal_topologicalNode[terminal] = resource($0);
}

# Current element is a TopologicalNode 
/cim:TopologicalNode.rdf.ID/ {
	topologicalNodes[id($0)] = 1;
}

# Current element is a ConnectivityNode
/cim:ConnectivityNode.rdf.ID/ {
	connectivityNode = id($0);
}
/cim:ConnectivityNode.rdf.about/ {
	connectivityNode = about($0);
}
/cim.ConnectivityNode.TopologicalNode.rdf.resource/ {
	topologicalNode = resource($0);
	connectivityNode_topologicalNode[connectivityNode] = topologicalNode;
	topologicalNode_numConnectivityNodes[topologicalNode]++;
	# print "DEBUG found tp " topologicalNode " cn " connectivityNode ", num = " topologicalNode_numConnectivityNodes[topologicalNode];
}

END {
	# Check if all TopologicalNodes have exactly one connectivityNode
	bijection = 1;
	for (t in topologicalNodes) {
		n = topologicalNode_numConnectivityNodes[t];
		if (n != 1) {
			print "TP " t " has " n " CNs"
			bijection = 0;
		}
	}
	if (bijection) {
		print "Mapping between ConnectivityNode and TopologicalNode is a bijection"
	}
}

function id(s) {
	sub(/.*:ID=\"[#]*/, "", s);
	sub(/\".*/, "", s);
	return s;
}
function about(s) {
	sub(/.*:about=\"[#]*/, "", s);
	sub(/\".*/, "", s);
	return s;
}
function resource(s) {
	sub(/.*:resource=\"[#]*/, "", s);
	sub(/\".*/, "", s);
	return s;
}