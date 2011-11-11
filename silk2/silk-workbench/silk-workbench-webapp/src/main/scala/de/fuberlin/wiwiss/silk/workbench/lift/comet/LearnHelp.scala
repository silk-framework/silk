package de.fuberlin.wiwiss.silk.workbench.lift.comet

class LearnHelp extends LinksHelp {

  override def overview = {
    <span>
      Learns linkage rules.
      { howToRateLinks }
    </span>
  }

  override def actions = {
    <div>
      Start the learning by pressing the <em>Start</em> button.
      When you are happy with the results and press the <em>Done</em> button.
    </div>
  }
}