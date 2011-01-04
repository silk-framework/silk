package de.fuberlin.wiwiss.silk.linkspec.condition

case class Blocking(blocks : Int = 10, overlap : Double = 0.0)
{
    require(blocks > 0, "blocks > 0")
    require(overlap >= 0.0, "overlap >= 0.0")
    require(overlap < 0.5, "overlap < 0.5")
}