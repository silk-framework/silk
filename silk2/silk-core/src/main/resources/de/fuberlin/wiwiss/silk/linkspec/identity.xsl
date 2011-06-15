<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"><xsl:output method="xml"/>

    <xsl:template match="node()|@*">
        <!-- Copy the current node -->
        <xsl:copy>
          <!-- Including any attributes it has and any child nodes -->
          <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
      </xsl:template>

</xsl:stylesheet>