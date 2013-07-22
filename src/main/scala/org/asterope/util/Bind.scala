package org.asterope.util

import javax.swing._
import javax.swing.text._
import javax.swing.event._
import java.awt.event._
import java.awt.Component
import org.jdesktop.swingx.renderer.{IconValue, StringValue}

object Bind {
  /** Binds JTextComponent to Publisher. Publisher is fired every time text component changes */
  def change(j: JTextComponent, block: => Unit): Unit =
    j.getDocument.addDocumentListener(new DocumentListener() {
      def insertUpdate (e: DocumentEvent): Unit = onEDT(block)
      def removeUpdate (e: DocumentEvent): Unit = onEDT(block)
      def changedUpdate(e: DocumentEvent): Unit = onEDT(block)
    })

  def change /* [E] */ (j: JList /* [E] */ , block: => Unit): Unit =
    j.addListSelectionListener(new ListSelectionListener() {
      override def valueChanged(e: ListSelectionEvent): Unit = onEDT(block)
    })

  def change(j: JTable, block: => Unit): Unit =
    j.getSelectionModel.addListSelectionListener(new ListSelectionListener() {
      override def valueChanged(e: ListSelectionEvent): Unit = onEDT(block)
    })

  def change(j: JSlider, block: => Unit): Unit =
    j.addChangeListener(new ChangeListener() {
      override def stateChanged(e: ChangeEvent): Unit = onEDT(block)
    })

  //  /** bind EventList to component */
  //  def bindChange[E](el: ca.odell.glazedlists.EventList[E], jl:JList){
  //	  jl.setModel(new  ca.odell.glazedlists.swing.EventListModel[E](el));
  //  }
  def action /* [E] */ (comp: JComboBox /* [E] */ , block: => Unit): Unit =
    comp.addActionListener(new ActionListener() {
      override def actionPerformed(e: ActionEvent): Unit = onEDT(block)
    })

  def action(comp: JTextComponent, block: => Unit): Unit =
    comp.addKeyListener(new KeyAdapter() {
      override def keyPressed(e: KeyEvent): Unit =
        if (e.getKeyCode == KeyEvent.VK_ENTER)
          onEDT(block)
    })

  def action(comp: Component, block: => Unit): Unit =
    comp match {
      case button: AbstractButton =>
        button.addActionListener(ActionListener(block))

      case _ =>
        comp.addKeyListener(new KeyAdapter() {
          override def keyPressed(e: KeyEvent): Unit =
            if (e.getKeyCode == KeyEvent.VK_ENTER)
              onEDT(block)
        })

        comp.addMouseListener(new MouseAdapter() {
          override def mouseClicked(e: MouseEvent): Unit =
            if (e.getClickCount == 2)
              onEDT(block)
        })
    }

  /** converts blocks to ActionListener */
  def ActionListener(block: => Unit) = new ActionListener {
    def actionPerformed(e: ActionEvent): Unit = block
  }

  def delayed(delay: Long, coalesce: Boolean = false, f: => Unit): Runnable = {
    val timer = new javax.swing.Timer(delay.toInt, Bind.ActionListener(f))
    timer.setRepeats(false)
    timer.setCoalesce(coalesce)
    org.asterope.util.Runnable(onEDTWait {
      timer.restart()
    })
  }

  def stringValue[E](e: E => String) = new StringValue {
    override def getString(value: AnyRef): String =
      e(value.asInstanceOf[E])
  }

  def iconValue[E](e: E => Icon) = new IconValue {
    override def getIcon(value: AnyRef): Icon =
      e(value.asInstanceOf[E])
  }
}