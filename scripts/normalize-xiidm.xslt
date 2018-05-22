<?xml version="1.0"?>
<!-- 
CGMES scripts
Copyright (C) 2017 - 2018 RTE (http://rte-france.com)
This Source Code Form is subject to the terms of the Mozilla Public
License, v. 2.0. If a copy of the MPL was not distributed with this
file, You can obtain one at http://mozilla.org/MPL/2.0/.

@author Luma Zamarreño <zamarrenolm at aia.es>

Normalize Powsybl XIIDM files for easy comparison
The stylesheet can be applied from the command line with xsltproc
-->
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:iidm="http://www.itesla_project.eu/schema/iidm/1_0" 
	exclude-result-prefixes="iidm"
	>
  <xsl:output method="xml" indent="yes" />
  <xsl:strip-space elements="*" />

  <xsl:template match="@* | node()">
    <xsl:copy>
      <!-- Sort the attributes by name. -->
      <xsl:for-each select="@*">
        <xsl:sort select="name(.)"/>
        <xsl:copy/>
      </xsl:for-each>
			<!-- Sort the children by id. Put all nodes without id at the end -->
      <xsl:apply-templates select="./*[@id]">
				<xsl:sort select="@id"/>
			</xsl:apply-templates>
      <xsl:apply-templates select="./*[not(@id)]"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="text()|comment()|processing-instruction()">
    <xsl:copy/>
  </xsl:template>
	
</xsl:stylesheet>
