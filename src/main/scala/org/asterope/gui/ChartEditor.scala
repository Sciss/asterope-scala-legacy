package org.asterope.gui

import org.asterope.util._
import edu.umd.cs.piccolo._
import edu.umd.cs.piccolo.event._
import java.awt.event._

import edu.umd.cs.piccolo.util.PBounds
import nodes.PPath
import org.asterope.chart._
import java.util.concurrent._
import collection.mutable.ArrayBuffer
import org.apache.commons.math.geometry.Vector3D
import java.awt.{BasicStroke, Polygon, Rectangle, Color}
import java.awt.geom.{Point2D, Ellipse2D, Area}
import org.asterope.Beans
import skyview.executive.Settings


class ChartEditor(
  protected val mainWinActions:MainWindowActions, //TODO make private
  resmap:ResourceMap,
  protected val stars:Stars, //TODO make private
  deepSky:DeepSkyPainter,
  constelBoundary:ChartConstelBoundary,
  constelLine:ChartConstelLine,
  legendBorder:LegendBorder,
  milkyWay: ChartMilkyWay
  )
  extends PCanvas 
  with ChartEditorActions
  {
  


  setPanEventHandler(null)
  setZoomEventHandler(null)
  setBackground(java.awt.Color.black)


  override def setCamera(camera:PCamera){

    //install zoom handler
    camera.addInputEventListener(new PBasicInputEventHandler {
      override def mouseClicked(event: PInputEvent) {
        //change selection with mid button
        if(event.isMouseEvent && event.getClickCount == 1 && event.getButton == 1){
          event.setHandled(true)
          val node = event.getPickedNode
          val obj = if(node==null) None else chartBase.getObjectForNode(node)
          selectObject(obj)
        } else if (event.isMouseEvent && event.getClickCount == 1 && event.getButton == 2) {
          //center on new position with mid mouse button
          event.setHandled(true);
          if(!getInteracting) setInteracting(true)

          // if middle button is pressed, center at given location
          val viewPos = event.getPosition;
          val width = chartBase.camera.getViewBounds.getWidth;
          val height = chartBase.camera.getViewBounds.getHeight;
          val bounds = new PBounds(viewPos.getX - width / 2, viewPos.getY - height / 2, width, height);
          chartBase.camera.setViewBounds(bounds);
          overview.updatePointer(chartBase,true);
          refresh()
       }
      }
      override def mouseWheelRotated(event: PInputEvent) {
        //zoom with mouse wheel
        if (event.isMouseWheelEvent) {
          event.setHandled(true);
          if(!getInteracting) setInteracting(true)

          // handles zoom event on mouse wheel
          val newScale = 1 + 0.1 * event.getWheelRotation;
          val viewPos = event.getPosition;
          chartBase.camera.scaleViewAboutPoint(newScale, viewPos.getX, viewPos.getY);
          overview.updatePointer(chartBase,true);
          refresh()

        }
      }
    })
    super.setCamera(camera)
  }




  //refresh when canvas size changes
  addComponentListener(new ComponentAdapter{
    override def componentResized(e:ComponentEvent){
      refresh()
    }
  })
  
  /**
   * Notify when chart refresh starts 
   */
  lazy val onChartRefreshStart = new Publisher[Chart]()

  /**
   * Notify when chart refresh finishes 
   */
  lazy val onChartRefreshFinish = new Publisher[Chart]()


  protected var chartBase = new Chart();
  protected var coordGridConfig = CoordinateGrid.defaultConfig
  protected var starsConfig = stars.defaultConfig
  protected var showLegend = true
  protected var showConstelBounds = true
  protected var showConstelLines = true;
  protected var deepSkyConfig = deepSky.defaultConfig
  protected var allSkyConfig:Option[AllSkySurveyMem] = None


  def getChartBase = chartBase

  private var refreshWorker:Future[Unit] = null
  private object refreshLock extends Object

  def waitForRefresh(){
    //wait until refresh starts
    refreshLock.synchronized{
      refreshLock.wait(1000)
    }
  }

  def refresh(){

    //cancel previously running tasks
    if(refreshWorker!=null ){
      refreshWorker.cancel(true);
    }

    refreshWorker = future[Unit]{
      refreshLock.synchronized{
        try{

      //wait until component is validated
      while(!isValid) Thread.sleep(10)


      if (getInteracting){
        //wait a bit in case user is zooming or performing other interactive task
        //this way new tasks may cancel this future without even starting
        Thread.sleep(500);

        //if user used mouse to move chart, center on new position and update FOV
        val bounds = chartBase.camera.getViewBounds;
        val center = chartBase.wcs.deproject(bounds.getCenter2D);
        if(center.isDefined){
          val fov = chartBase.wcs.deproject(bounds.getOrigin).map(Vector3D.angle(_,center.get) * 2).getOrElse(120 * Angle.D2R);
          chartBase = chartBase.copy(position = center.get, fieldOfView = fov.radian)
        }
      }

      //take current size of windows
      val chart = chartBase.copy(width = getWidth,
        height = if( !showLegend) getHeight else (getHeight - legendBorder.height),
        legendHeight = if(showLegend) legendBorder.height else 0
      )

      Log.debug("Refresh starts, width:"+chart.width+", height:"+chart.height+", ipixCount:"+chart.area.size()+", hash:"+System.identityHashCode(chart))

      onChartRefreshStart.firePublish(chart)

      val futures = new ArrayBuffer[Future[Unit]];

      futures+=future{
        stars.updateChart(chart,starsConfig)
      }
      futures+=future{
        deepSky.updateChart(chart,deepSkyConfig)
      }

      if(showConstelBounds) futures+=future{
         constelBoundary.updateChart(chart)
      }

      if(showConstelLines) futures+=future{
         constelLine.updateChart(chart)
      }


      futures+=future{
        milkyWay.updateChart(chart)
      }


      futures+=future{
        CoordinateGrid.updateChart(chart,coordGridConfig)
      }

      if(showLegend) futures+=future{
         legendBorder.updateChart(chart)
      }

      futures+=future{
        overview.update(chart)
      }

      //now wait for all futures to finish
      waitOrInterrupt(futures)

      //good now perform final tasks on EDT
      onEDTWait{
        //labels must be last,
        // placement algorithm depends on graphic created by other features
        Labels.updateChart(chart)
        chartBase = chart;
        getCamera.removeAllChildren();
        if(getInteracting)
           setInteracting(false) //this will cause repaint, but chart is already empty so no performance problem
        setCamera(chart.camera)
        //restore selection
        selectObject(selectedObject,selectionAfterRefresh=true)


        onChartRefreshFinish.firePublish(chartBase)
      }

      allSkyConfig.foreach{mem=>
//        futures+=future{
          AllSkySurvey.updateChart(chart,mem)
//        }
      }
      Log.debug("Refresh finished hash:"+System.identityHashCode(chart));
      }catch{
          case e:InterruptedException => { /* can be ignored */}
          case e:Throwable => {
            Log.error("Chart refresh failed",e)
          }
        }
      }

    }

  }


  def centerOnPosition(pos:Vector3D){
    chartBase = chartBase.copy(position = pos)
    refresh()
  }


  private var selectedPointer:Option[PNode] = None
  private var selectedObject:Option[Any] = None
  def getSelectedObject = selectedObject


  val onSelectionChanged = new Publisher[Chart]()

  def selectObject(obj:Option[Any], selectionAfterRefresh:Boolean = false){

    def createPointer(node:PNode):PNode = {
      val w = 18
      val h = 2
      val d = math.max(w+10,node.getFullBoundsReference.width.toInt/2 + 10)

      val area = new Area(new Ellipse2D.Double(-d-h, -d-h, (d+h)*2, (d+h)*2));
      area.subtract(new Area(new Ellipse2D.Double(-d, -d, d*2, d*2)))
      area.add(new Area(new Rectangle(-d-w,-h/2,w*2,h)))
      area.add(new Area(new Rectangle(d-w,-h/2,w*2,h)))
      area.add(new Area(new Rectangle(-h/2,-d-w,h,w*2)))
      area.add(new Area(new Rectangle(-h/2,d-w,h,w*2)))

      val n = new PPath(area)
      n.setPaint(Color.red)
      n.centerFullBoundsOnPoint(node.getFullBoundsReference.getCenterX, node.getFullBoundsReference.getCenterY)
      n;
    }

    if(obj == selectedObject && !selectionAfterRefresh){
      return
    }

    //unselect old
    selectedPointer.foreach(_.removeFromParent())
    selectedPointer = None;

    obj.foreach{o=>
      val node:PNode = chartBase.getNodeForObject(o).getOrElse(throw new IllegalArgumentException("Object not on map"))
      val pointer = createPointer(node);
      selectedPointer = Some(pointer)
      chartBase.addNode(Layer.fg, pointer)
      pointer.repaint()
    }

    if(selectedObject!=obj){
      selectedObject = obj
      onSelectionChanged.firePublish(chartBase)
    }


  }


  object overview extends PCanvas{
    private var _chart:Chart = new Chart();
    def chart = _chart;
    setPanEventHandler(null)
    setZoomEventHandler(null)
    setBackground(java.awt.Color.black)



    def update(detailChart:Chart){
      //only paint if is valid (ie added and visible)
      if(!isValid) return;

      _chart = _chart.copy(
        position=detailChart.position,
        fieldOfView = detailChart.fieldOfView * 4,
        width = getWidth,
        height =getHeight,
        colors = detailChart.colors
      )
      stars.updateChart(_chart)
      constelBoundary.updateChart(_chart)
      constelLine.updateChart(_chart)

      updatePointer(detailChart,false);

      onEDTWait{
        getCamera.removeAllChildren()
        getCamera.addChild(_chart.camera)
      }
    }


    //refresh when canvas size changes
    addComponentListener(new ComponentAdapter{
      override def componentResized(e:ComponentEvent){
        ChartEditor.this.refresh()
      }
    })


    /**paint active area on chart*/
    def updatePointer(detailChart:Chart, lowQuality:Boolean){

      val polygon = new Polygon()
      val w = detailChart.width
      val h = detailChart.height
      List(Point2d(0,0), Point2d(0,h/2), Point2d(0,h),
          Point2d(w/2,h), Point2d(w,h),Point2d(w,h/2), Point2d(w,0), Point2d(w/2,0))
        .map(p => detailChart.camera.localToView(new Point2D.Double(p.getX,p.getY)))
        .flatMap(detailChart.wcs.deproject(_))
        .flatMap(_chart.wcs.project(_))
        .foreach(p=>polygon.addPoint(p.getX.toInt,p.getY.toInt))

      val polygon2 = new PPath(polygon)
      val c = detailChart.colors.fg
      polygon2.setPaint(new Color(c.getRed,c.getGreen,c.getBlue,64))
      polygon2.setStroke(new BasicStroke(1F))
      polygon2.setStrokePaint(c)
      onEDTWait{
        setInteracting(!lowQuality)
        chart.getLayer(Layer.fg).removeAllChildren()
        chart.getLayer(Layer.fg).addChild(polygon2)
        chart.getLayer(Layer.fg).repaint()
      }
    }


  }


  /*************************************************************
   *  skyview stuff
   *************************************************************/

  private var lastSkyviewConfig = new SkyviewConfig;

  val actChartSkyview = mainWinActions.actChartSkyview.editorAction(this){
    showSkyviewSurveyDialog(lastSkyviewConfig.survey)
  }

  def showSkyviewSurveyDialog(survey:String){
    val m = lastSkyviewConfig.copy(survey = survey)
    val form = new SkyviewForm
    resmap.injectActionFields(form)
    resmap.injectComponents(form)
     val m2 = Form.showDialog(m, form, width= 600)
     if(m2.isDefined){
      lastSkyviewConfig = m2.get
      onEDT{
        //show modal dialog in separate EDT event, so it does not block us
        SkyviewProgressDialog.show()
      }

      fork{
        try{
          Skyview.updateChart(getChartBase,lastSkyviewConfig)
          onEDT{
            //hide modal dialog after we are done
            SkyviewProgressDialog.setVisible(false)
          }

        }catch{
            case e:Throwable => {
                e.printStackTrace(skyview.executive.Settings.err)
                Settings.err.println("An exception happend, hit Cancel to hide this dialog!")
                //original imager is no longer running, so start thread which would react to cancelled event
                fork{
                    while(true){
                        Settings.checkCancelled()
                        Thread.sleep(1)
                    }
                }
                throw new Exception(e)
            }
        }
      }
     }
  }





}