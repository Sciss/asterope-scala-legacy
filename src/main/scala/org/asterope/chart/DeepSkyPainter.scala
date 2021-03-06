package org.asterope.chart

import java.awt.geom.Line2D
import java.awt.BasicStroke
import edu.umd.cs.piccolo.nodes.PPath
import java.awt.geom.Ellipse2D
import edu.umd.cs.piccolo.PNode
import org.asterope.util._
import java.awt.Shape
import java.awt.geom.Path2D
import org.asterope.data._

case class DeepSkyPainterConfig(
  bigObjectsTransparent : Boolean = true,
  showGlobularCluster   : Boolean = true,
  showOpenCluster       : Boolean = true,
  showGalaxy            : Boolean = true,
  showBrightNebula      : Boolean = true,
  showPlanetaryNebula   : Boolean = true,
  showDarkNebula        : Boolean = true,
  showSupernovaRemnant  : Boolean = true
)

class DeepSkyPainter(dao: DeepSkyDao)
  extends ChartFeature[DeepSkyPainterConfig]
  with ChartPainter[DeepSkyPainterConfig, DeepSky] {

  def paintObject(chart: Chart, config: DeepSkyPainterConfig,
                  ds: DeepSky, addToLayer: Boolean): Option[PNode] = {

    val pos = chart.wcs.project(ds.vector)
    if(pos.isEmpty || ds.sizeMax .isEmpty) return None //TODO handle object with unknown size

    import DeepSkyType._
    val n = ds.deepSkyType match {
      case BRTNB | GX_DN | LMCDN | SMCDN  => paintBrightNebula(chart, config, ds)
      case CL_NB                          => paintOpenCluster (chart, config, ds)
      case DRKNB                          => paintDarkNebula  (chart, config, ds)
      case GALXY | GALCL | G_C_N          => paintGalaxy      (chart, config, ds) //TODO custom painter for galaxy cluster
      case GLOCL | GX_GC | LMCGC | SMCGC  => paintGlobularCluster(chart, config, ds)
      case OPNCL | ASTER | CL_NB | LMCCN | LMCOC | SMCCN | SMCOC | STAR1 | STAR2 | STAR3 | STAR4 | STAR8 =>
                                             paintOpenCluster(chart, config, ds)
      case PLNNB | SNREM | NONEX | QUASR  => paintPlanetaryNebula(chart, config, ds)

      //TODO nonexistent (NONEX) is painted as planetary nebula now
      //TODO quasar QUASR painted as planetary nebula
    }
    n.foreach { node =>
      node.setGlobalTranslation(pos.get)
      //make big objects transparent
      //set transparency based on node
      if (config.bigObjectsTransparent) {
        var alpha = 1.0 - node.getWidth / chart.width
        //        	if(ds.mag.isEmpty) //TODO can be magnitude on deepsky undefined?
        //        		alpha = 1.0 - 4*node.getWidth/chart.width
        if (alpha < 0.25) alpha = 0.25
        if (alpha > 1   ) alpha = 1
        ChartUtils.allNodes(node).foreach {
          _.setTransparency(alpha.toFloat)
        }
      }
      if (addToLayer) {
        // XXX TODO: unused: val mag = ds.mag.getOrElse(new Magnitude(100)).mag
        chart.addNode(Layer.deepsky, node, ds, -ds.sizeMax.get.toDegree)
      }
    }
    n
  }

  def paintBrightNebula(chart: Chart, config: DeepSkyPainterConfig, ds: DeepSky): Option[PNode] = {
    if (!config.showBrightNebula) return None
    val dia = ds.sizeMax.get.toRadian / chart.pixelAngularSize.toRadian
    val shape = orOutline(ds, chart, new Ellipse2D.Double(-dia / 2, -dia / 2, dia, dia))
    val n = new PPath(shape)
    n.setStroke(new BasicStroke(0.66F))
    n.setStrokePaint(chart.colors.brightNebula)
    n.setPaint(chart.colors.brightNebulaInside)
    Some(n)
  }

  def paintDarkNebula(chart: Chart, config: DeepSkyPainterConfig, ds: DeepSky): Option[PNode] = {
    if(!config.showDarkNebula) return None
    val dia = ds.sizeMax.get.toRadian / chart.pixelAngularSize.toRadian
    val shape = orOutline(ds, chart, new Ellipse2D.Double(-dia / 2, -dia / 2, dia, dia))
    val n = new PPath(shape)
    val width = 1
    n.setStroke(new ShapeStroke(new Ellipse2D.Double(0, 0, width, width), width * 2))
    n.setStrokePaint(chart.colors.darkNebula)
    n.setPaint(chart.colors.darkNebulaInside)
    Some(n)
  }

  def paintOpenCluster(chart: Chart, config: DeepSkyPainterConfig, ds: DeepSky): Option[PNode] = {
    if (!config.showOpenCluster) return None
    val dia = ds.sizeMax.get.toRadian / chart.pixelAngularSize.toRadian
    val shape = orOutline(ds, chart, new Ellipse2D.Double(-dia / 2, -dia / 2, dia, dia))
    val n = new PPath(shape)
    val strokeWidth = 1 * 3
    n.setStroke(new ShapeStroke(new Ellipse2D.Double(0, 0, strokeWidth, strokeWidth), strokeWidth * 3))
    n.setStrokePaint(chart.colors.openCluster)
    n.setPaint(chart.colors.openClusterInside)
    Some(n)
  }

  def paintGlobularCluster(chart: Chart, config: DeepSkyPainterConfig, ds: DeepSky): Option[PNode] = {
    if (!config.showGlobularCluster) return None
    val dia = ds.sizeMax.get.toRadian / chart.pixelAngularSize.toRadian
    val shape = orOutline(ds, chart, new Ellipse2D.Double(-dia / 2, -dia / 2, dia, dia))
    val n = new PPath(shape)
    n.setStroke(new BasicStroke(1))
    val stroke = chart.colors.globularCluster
    n.setStrokePaint(stroke)
    n.setPaint(chart.colors.globularClusterInside)

    val vert = new PPath(new Line2D.Double(-dia / 2, 0, dia / 2, 0))
    vert.setStroke(new BasicStroke(1))
    vert.setStrokePaint(stroke)
    n.addChild(vert)

    val horz = new PPath(new Line2D.Double(0, -dia / 2, 0, dia / 2))
    horz.setStroke(new BasicStroke(0.66F))
    horz.setStrokePaint(stroke)
    n.addChild(horz)

    Some(n)
  }

  //paints PlanetaryNebula and SupernovaRemnant
  def paintPlanetaryNebula(chart: Chart, config: DeepSkyPainterConfig, ds: DeepSky): Option[PNode] = {
    if (!config.showPlanetaryNebula && !config.showSupernovaRemnant) return None

    val dia = ds.sizeMax.get.toRadian / chart.pixelAngularSize.toRadian
    val shape = orOutline(ds, chart, new Ellipse2D.Double(-dia / 2, -dia / 2, dia, dia))
    val n = new PPath(shape)
    n.setStroke(new BasicStroke(0.66F))
    val stroke = chart.colors.planetaryNebula
    n.setStrokePaint(stroke)
    n.setPaint(chart.colors.planetaryNebulaInside)

    val lines = List(
      new PPath(new Line2D.Double(dia / 2, 0, dia * 3 / 2, 0)),
      new PPath(new Line2D.Double(-dia / 2, 0, -dia * 3 / 2, 0)),
      new PPath(new Line2D.Double(0, -dia / 2, 0, -dia * 3 / 2)),
      new PPath(new Line2D.Double(0, dia / 2, 0, dia * 3 / 2))
    )

    lines.foreach { p: PPath =>
      p.setStroke(new BasicStroke(0.66F))
      p.setStrokePaint(stroke)
      n.addChild(p)
    }

    Some(n)
  }

  def paintGalaxy(chart: Chart, config: DeepSkyPainterConfig, ds: DeepSky): Option[PNode] = {
    if (!config.showGalaxy) return None
    val dia1 = ds.sizeMax.get.toRadian / chart.pixelAngularSize.toRadian
    val dia2 =
      if (ds.sizeMin.isEmpty) dia1
      else ds.sizeMin.get.toRadian / chart.pixelAngularSize.toRadian
    val shape = orOutline(ds, chart, new Ellipse2D.Double(-dia1 / 2, -dia2 / 2, dia1, dia2))
    val n = new PPath(shape)
    if (ds.posAngle.isDefined && !dao.findOutline(ds).isDefined) {
      val pa = 90.degree - ds.posAngle.get
      n.setRotation(pa.toRadian)
    }
    n.setStroke(new BasicStroke(0.66F))
    n.setStrokePaint(chart.colors.galaxy)
    n.setPaint(chart.colors.galaxyInside)
    Some(n)
  }

  def defaultConfig = new DeepSkyPainterConfig()

  def updateChart(chart: Chart, config: DeepSkyPainterConfig): Unit = {
    val deepSkys = dao.deepSkyByArea(chart.area)
    paintAll(chart, config, deepSkys)
  }

  def clearChart(chart:Chart): Unit =
	  chart.getLayer(Layer.deepsky).removeAllChildren()

  def outlineToShape(chart: Chart, outline: DeepSkyOutline, center: Point2d): Shape = {
    val ret = new Path2D.Double()
    val points = outline.points.flatMap(chart.wcs.project(_))
    ret.moveTo(points.head.x - center.x, points.head.y - center.y)
    points.tail.foreach { p =>
      ret.lineTo(p.x - center.x, p.y - center.y)
    }
    ret.closePath()
    ret
  }

  def orOutline(ds: DeepSky, chart: Chart, out: => Shape): Shape = {
    val center = chart.wcs.project(ds.ra, ds.de).get
    dao.findOutline(ds)
      .map(outlineToShape(chart, _, center))
      .getOrElse(out)
  }
}