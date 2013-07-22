package org.asterope.gui

import java.awt.event._
import org.asterope.util._
import javax.swing._
import event.{ChangeEvent, ChangeListener}

class StatusBar extends JPanel {

  setLayout(MigLayout("fill, insets 0 3 0 3"))
  val statusLabel = new JLabel()
  add(statusLabel, "growx")
  addSeparator()
  add(new StatusBarMemoryUsage, "w 80")

  //show menu tooltips in status bar
  MenuSelectionManager.defaultManager().addChangeListener(toolTipMenuListener)

  //TODO add this for toolbar when created

  def addSeparator(): Unit = {
    val s = new JSeparator(SwingConstants.VERTICAL)
    add(s, "growy")
  }

  /** Shows menu tooltips on status bar */
  private object toolTipMenuListener extends ChangeListener {

    def getTooltip(source: Any): Option[String] =
      source match {
        case b: AbstractButton =>
          Option(b.getAction).flatMap(a => Option(a.toolTip))

        case component: JComponent =>
          val s = component.getToolTipText
          Option(s)

        case _ => None
      }

    def stateChanged(e: ChangeEvent): Unit =
      e.getSource match {
        case man: MenuSelectionManager => onEDT {
          if (man.getSelectedPath != null && !man.getSelectedPath.isEmpty) {
            val item  = man.getSelectedPath.last.getComponent
            val t     = getTooltip(item)
            statusLabel.setText(t.getOrElse(""))
          } else {
            statusLabel.setText("")
          }
        }
        case _ =>
      }
	}
}

class StatusBarMemoryUsage extends JLabel {

  def update(): Unit = {
    val max   = (Runtime.getRuntime.totalMemory() * 1e-6).toInt
    val used  = (max - Runtime.getRuntime.freeMemory() * 1e-6).toInt
    val str   = used + "M / " + max + "M"
    setText(str)
  }

  /** run garbage collector, triggered when user click on this progressbar*/
  val runGC = act {
    fork {
      System.gc()
    }
  }

  //on mouse click call gc
  addMouseListener(new MouseAdapter {
    override def mouseClicked(e: MouseEvent): Unit = runGC.call()
  })

  //run timer and update status every 2 sec
  protected val timer = new javax.swing.Timer(1000, Bind.ActionListener(update()))
  timer.setRepeats(true)
  timer.start()
}