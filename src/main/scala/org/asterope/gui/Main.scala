package org.asterope.gui

import javax.swing._
import org.asterope.util._
import org.asterope._
import java.awt.Component

/**
 * Main object which starts Asterope GUI
 */
object Main extends App {
  /** assert that all repaints are in GUI thread */
  RepaintManager.setCurrentManager(new RepaintManager {
    override def addInvalidComponent(component: JComponent): Unit = {
      requireEDT()
      super.addInvalidComponent(component)
    }

    override def addDirtyRegion(component: JComponent, x: Int, y: Int, w: Int, h: Int): Unit = {
      requireEDT()
      super.addDirtyRegion(component, x, y, w, h)
    }
  })

  object beans extends Beans

  lazy val messageView = new MessageView(beans.resmap)

  object overviewView extends beans.mainWin.EditorBoundView {
    override def editorOpened(editor: Component): JComponent =
      editor match {
        case editor1: ChartEditor => editor1.overview
        case _ => null
      }

    override def editorClosed(editor: Component, subview: JComponent) = ()
  }

  Log.debug("Asterope GUI is starting")
  sys.props("apple.laf.useScreenMenuBar")                       = "true"
  sys.props("com.apple.mrj.application.apple.menu.about.name")  = "Asterope"
  onEDTWait {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)
    beans.mainWinMenu //make sure menu is hooked
    beans.mainWin.show()
    beans.mainWin.addEditor         ("welcomeEditor", beans.welcomeEditor.panel)
    beans.mainWin.addLeftTopView    ("objectsView"  , new JLabel())
    beans.mainWin.addLeftBottomView ("overviewView" , overviewView)
    beans.mainWin.addBottomBarView  ("messageView"  , messageView)
  }
}