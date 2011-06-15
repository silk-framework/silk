<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
   <!-- <xsl:import href="de/fuberlin/wiwiss/silk/linkspec/identity.xsl"/>-->

     <xsl:template match="node()|@*">
        <!-- Copy the current node -->
        <xsl:copy>
          <!-- Including any attributes it has and any child nodes -->
          <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
      </xsl:template>

    <!-- add new Attribute to the Compare-Element-->
    <xsl:template match="Compare">
        <xsl:choose>
            <xsl:when test="@metric = 'levenshtein' or @metric = 'wgs84' or @metric= 'num'">
                <xsl:copy>
                    <xsl:copy-of select="@*"/>
                        <xsl:attribute name="threshold">
                            <xsl:variable name="oldTreshold" select="ancestor::Interlink/Filter/@threshold"/>
                            <xsl:variable name="div" select="1 - $oldTreshold"/>
                            <xsl:if test="Param/@name = 'maxDistance' or Param/@name = 'threshold'">
                                <xsl:variable name="valueNode" select="Param[@name = 'threshold' or @name = 'maxDistance']"/>
                                <xsl:value-of select="round($valueNode/@value * $div)"/>
                            </xsl:if>
                        </xsl:attribute>
                    <xsl:copy-of select="node()"/>
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:apply-templates select="node()|@*"/>
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Remove the Filter-element-->
    <xsl:template match="Filter">
        <xsl:copy>
            <xsl:copy-of select="@limit"/>
        </xsl:copy>
    </xsl:template>

    <!-- Remove Param-node with old attributes [name = 'threshold' or name = 'maxDistance']
    <xsl:template match="Compare[child::Param]">
         <xsl:apply-templates/>
    </xsl:template>-->

</xsl:stylesheet>