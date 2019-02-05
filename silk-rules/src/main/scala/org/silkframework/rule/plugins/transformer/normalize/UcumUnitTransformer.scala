package org.silkframework.rule.plugins.transformer.normalize

import java.nio.charset.Charset
import java.text.ParsePosition
import java.util
import java.util.{Locale, ResourceBundle}

import javax.measure
import javax.measure.Quantity
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.rule.plugins.transformer.normalize.UcumUnitTransformer.OwnFormat
import org.silkframework.runtime.plugin.{MultilineStringParameter, Param, Plugin}
import systems.uom.ucum.format.UCUMFormat
import systems.uom.ucum.format.UCUMFormat.Variant
import tec.uom.se.format.{AbstractUnitFormat, SymbolMap}
import tec.uom.se.unit.Units

import scala.util.{Failure, Success, Try}

@Plugin(
  id = "UcumUnitNormalizer",
  label = "Ucum Unit Normalizer",
  categories = Array("Normalize"),
  description =
    """Normalizes quantities (decimal number + unit symbol) into a new quantity of the desired target unit (or the base unit) of the given dimension.
By default all SI units including all known prefixes are supported. Custom (or lesser known) units can be added via the configuration parameter.

Example:
TODO
"""
)
case class UcumUnitTransformer(
    @Param("Character set: 'ASCII' or 'Unicode'. Default is Unicode.")
    charSet: String = "Unicode",
    @Param(
      """While all SI units and prefixes are supported by default, custom or obsolete units have to be added via this configuration.
        |NOTE: when constructing formulae depending on other units defined in the configuration, make sure to order them dependently.
        |Example:
        |CSV columns:    quantity, unit name , symbol, equals formula (using already registered base units), all additional columns: alternative symbols
        |                   Area , Are       , are   , 100 m²
        |                  Energy, Calory    , cal   , 4.1868 J   , Cal
        | ElectricCurrent, Coulomb per second, C/s   , 1 C / 1 s
      """)
    configuration: MultilineStringParameter = ""
  ) extends SimpleTransformer {
  
  implicit val format: OwnFormat = new OwnFormat

  implicit val javaCharSet : Charset = if(charSet == "ASCII") Charset.forName("US-ASCII") else Charset.defaultCharset()

  // first we initialize the formatter
  UcumUnitTransformer.initFormat

  // then we read the config and append the formatter
  configuration.lines.foreach(line =>{
    if(line.trim.toLowerCase.startsWith("quantity") || ! line.contains(",")){}  // we ignore possible header or empty lines
    else{
      val cells = line.split(",").map(_.trim)
      UcumUnitTransformer.applyConfigurationItem(cells)
    }
  })

  override def evaluate(value: String): String = {
    format.parse(value.trim).getSymbol
  }
}

object UcumUnitTransformer{

  private def addSymbol(symbol: String, unit: javax.measure.Unit[_<: Quantity[_]])
                       (implicit format: OwnFormat, charSet: Charset): Unit ={
    if(new String(charSet.encode(symbol).array()).trim != symbol){
      throw new IllegalArgumentException("The given symbol contains characters outside of the used Charset: " + symbol)
    }
    if(format.getSymbols.getUnit(symbol) != null){
      val zw = format.parse(symbol, new ParsePosition(0))
      assert(zw.equals(unit), throw new IllegalArgumentException("The symbol " + symbol + " was already assigned to unit " + zw.toString))
      return
    }
    format.label(unit, symbol)
  }

  /* add additional known units not in the defaults */
  def initFormat(implicit format: OwnFormat, charSet: Charset): Unit ={
    if(charSet == Charset.defaultCharset()) {
      addSymbol("m²", Units.SQUARE_METRE)
      addSymbol("\u33A1", Units.SQUARE_METRE)
      addSymbol("m³", Units.CUBIC_METRE)
      addSymbol("m/s²", Units.METRE_PER_SQUARE_SECOND)
    }
    else {  // else its ASCII
      addSymbol("m2", Units.SQUARE_METRE)
      addSymbol("m/s2", Units.METRE_PER_SQUARE_SECOND)
    }

    addSymbol("m/s", Units.METRE_PER_SECOND)
  }

  def applyConfigurationItem(configItem: Array[String])(implicit format: OwnFormat, charSet: Charset): Unit ={
    if(configItem.length > 0){
      assert(configItem.length >= 4,
        throw new IllegalArgumentException("The configuration of an UcumUnitTransformer contains an invalid line (not enough cells)."))
      //assert(UcumUnitTransformer.knownQuantities.contains(configItem.head),
      //  throw new IllegalArgumentException("The configuration of an UcumUnitTransformer contains an invalid line (first cell not a valid quantity)."))
      assert(configItem(1).trim.nonEmpty,
        throw new IllegalArgumentException("The configuration of an UcumUnitTransformer contains an invalid line (second cell is empty)."))
      assert(configItem(2).trim.nonEmpty && ! configItem(2).trim.contains(" "),
        throw new IllegalArgumentException("The configuration of an UcumUnitTransformer contains an invalid line (third cell is empty or contains whitespace)."))

      // parse unit by formula
      val unit = Try[javax.measure.Unit[_<: Quantity[_]]]{format.parse(configItem(3), new ParsePosition(0))} match{
        case Success(u) => u
        case Failure(f) => f match {
          case iae: IllegalArgumentException => throw new IllegalArgumentException("The configuration of an UcumUnitTransformer contains an invalid formula.", iae)
          case u => throw new RuntimeException("Unknown error while parsing UcumUnitTransformer configuration.", u)
        }
      }
      //val annotatedUnit = new AnnotatedUnit[_<: Quantity[_]](unit.asInstanceOf[AbstractUnit[_<: Quantity[_]]], configItem(1))
      // add symbol
      addSymbol(configItem(2), unit)
      // add aliases
      configItem.drop(4).foreach(alias => format.getSymbols.alias(unit, alias))
    }
  }

  class OwnFormat extends AbstractUnitFormat{

    private val symbolMap = SymbolMap.of(ResourceBundle.getBundle("systems.uom.ucum.format.UCUMFormat_CS", new ResourceBundle.Control() {
      override def getCandidateLocales(baseName: String, locale: Locale): util.List[Locale] = {
        if (baseName == null)
          throw new NullPointerException
        if (locale == new Locale("", "CS"))
          return util.Arrays.asList(locale, Locale.ROOT)

        super.getCandidateLocales(baseName, locale)
      }
    }))

    private val ucumFormat = UCUMFormat.getInstance(Variant.CASE_SENSITIVE, symbolMap)

    override def parse(csq: CharSequence, cursor: ParsePosition): measure.Unit[_ <: Quantity[_ <: Quantity[_]]] = ucumFormat.parse(csq, cursor)

    override def parse(csq: CharSequence): measure.Unit[_ <: Quantity[_ <: Quantity[_]]] = ucumFormat.parse(csq)

    override def isLocaleSensitive: Boolean = true

    override def getSymbols: SymbolMap = symbolMap

    override def label(unit: measure.Unit[_], label: String): Unit = symbolMap.label(unit.asInstanceOf[measure.Unit[_ <: Quantity[_ <: Quantity[_]]]], label)

    override def parse(csq: CharSequence, index: Int): measure.Unit[_ <: Quantity[_ <: Quantity[_]]] = ucumFormat.parse(csq, new ParsePosition(index))

    override def format(unit: measure.Unit[_], appendable: Appendable): Appendable = ucumFormat.format(unit.asInstanceOf[measure.Unit[_ <: Quantity[_ <: Quantity[_]]]], appendable)
  }
}
