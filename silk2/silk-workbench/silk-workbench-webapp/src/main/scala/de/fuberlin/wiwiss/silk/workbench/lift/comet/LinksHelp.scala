package de.fuberlin.wiwiss.silk.workbench.lift.comet

trait LinksHelp extends Help {

  protected def howToRateLinks = {
    <div>
      Based on its correctness, each link can be associated to one of the following 3 categories:
      <br/>
      <img src="./static/img/confirm.png"></img>
      Confirms the link as correct. Confirmed links are part of the positive reference link set.
      <br/>
      <img src="./static/img/undecided.png"></img>
      Link whose correctness is undecided i.e. which is not contained in the reference link sets.
      <br/>
      <img src="./static/img/decline.png"></img>
      Confirms the link as incorrect. Incorrect links are part of the negative reference link set.
    </div>
  }

  protected def howToAddReferenceLinks = {
    <div>
      You can add reference links the following ways:
      <ul>
        <li>Import existing reference links</li>
        <li>Using the <em>Learn</em> Tab</li>
        <li>Using the <em>Generate Links</em> Tab</li>
      </ul>
    </div>
  }
}