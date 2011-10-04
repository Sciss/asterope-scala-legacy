package org.asterope.gui

import org.asterope.util._
import javax.swing.JMenuBar
import edu.umd.cs.piccolo.PNode
import org.asterope.chart.{Layer, ChartBeans}
import collection.JavaConversions._
import org.asterope.data._

class ChartEditorTest extends ScalaTestCase
  with MainWindow
  with ChartEditorFab{

  object chartBeans extends ChartBeans with TestRecordManager

  val menu = new JMenuBar

  def chart = chartEditor.getChartBase
  def chartEditor = getFocusedEditor.asInstanceOf[ChartEditor]

  def waitForRefresh(){
    sleep(1000)
    waitUntil(!chartEditor.refreshInProgress)
  }

  def open(){
    onEDT{
      showMinimized()
      openChartOnObject("M45")
    }
    onEDTWait{
      waitUntil(getFocusedEditor.isInstanceOf[ChartEditor])
    }
  }

  override def tearDown(){
    onEDT{hide()}
  }

  def testOpenChartOnObject(){
    open()
    assert(chart.position.angle(Vector3d.asterope) < 1 * Angle.D2R)
  }

  def testResizeWindow(){
    open()
    waitForRefresh()
    val width = chart.width
    val height = chart.height

    onEDT{
      mainFrame.setSize(500,500)
    }
    //wait until size changes
    waitUntil(width!=chart.width && height!=chart.height)
  }

  def testZoomIn(){
    open()
    val oldFov = chart.fieldOfView
    chartEditor.actZoomIn.call()
    waitForRefresh()
    assert (chart.fieldOfView < oldFov);
  }

  def testZoomOut(){
    open()
    val oldFov = chart.fieldOfView;
    chartEditor.actZoomOut.call()
    waitForRefresh()
    assert (chart.fieldOfView > oldFov);
  }

  def testChartFov15d(){
    open()
    chartEditor.actFov15d.call()
    waitForRefresh()
    assert(chart.fieldOfView === 15.degree)
  }

  def testChartFov8d(){
    open()
    chartEditor.actFov8d.call()
    waitForRefresh()
    assert(chart.fieldOfView === 8.degree)
  }

  def testMapRefresh(){
    open()
    chartEditor.actRefresh.call()
    waitForRefresh()
    //check if there are some stars on chart
    val stars = chart.objects.filter(_.isInstanceOf[LiteStar])
    assert(stars.size > 10)
  }


  def findBiggestStarNode:PNode = {
    chart.getLayer(Layer.star).getChildrenIterator
      .map(_.asInstanceOf[PNode])
      .toList
      .sortWith(_.getWidth > _.getWidth)
      .head
  }

  def testBiggerStars(){
    open()
    waitForRefresh()
    val starSize = findBiggestStarNode.getWidth
    chartEditor.actBiggerStars.call()
    waitForRefresh()
    assert(starSize<findBiggestStarNode.getWidth)
  }

  def testSmallerStars(){
    open()
    waitForRefresh()
    val starSize = findBiggestStarNode.getWidth
    chartEditor.actSmallerStars.call()
    waitForRefresh()
    assert(starSize>findBiggestStarNode.getWidth)
  }

  def testLegend(){
    open()
    waitForRefresh()
    def labelCount = chart.getLayer(Layer.label).getChildrenCount
    def legendCount = chart.getLayer(Layer.legend).getChildrenCount

    assert(chartEditor.actShowLegend.selected === Some(true))
    assert(labelCount?>0)
    assert(legendCount?>0)

    chartEditor.actShowLegend.call()
    waitForRefresh()
    assert(chartEditor.actShowLegend.selected === Some(false))
    assert(labelCount?>0)
    assert(legendCount === 0)

    chartEditor.actShowLegend.call()
    waitForRefresh()
    assert(chartEditor.actShowLegend.selected === Some(true))
    assert(labelCount?>0)
    assert(legendCount?>0)
  }




}