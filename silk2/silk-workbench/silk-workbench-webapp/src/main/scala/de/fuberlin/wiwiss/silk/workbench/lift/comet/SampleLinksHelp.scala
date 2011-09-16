package de.fuberlin.wiwiss.silk.workbench.lift.comet

class SampleLinksHelp extends LinksHelp {

  override def renderOverview = {
    <span>
      Samples links.
      { howToRateLinks }
    </span>
  }
}