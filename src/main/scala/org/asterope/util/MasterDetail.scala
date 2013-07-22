package org.asterope.util

import javax.swing._
import java.awt.CardLayout
import scala.collection.mutable

/** Show master/detail form. It display list of items in JList,
  * for each selected item it shows more detail form.
  */
abstract class MasterDetail[E] {

  /** Override and set to false if you want vertical */
  val horizontal = true

  /** list of master items. Is also model for master JList */
  lazy val items = new JListModel[E]()
  /** master JLIst */
  lazy val master = new JList /* [E] */ (items) {
    val delayedSelectionChange = Bind.delayed(100, true, {
      masterSelectionChanged(getSelectedValue.asInstanceOf[E])
    })
    Bind.change(this, delayedSelectionChange.run())

  }

  /** layered panel which shows and swaps detail forms */
  lazy val detailPane = new JLayeredPane() {
    setLayout(new CardLayout())
  }

  /** map of opened items and detail forms */
  lazy val openedItems = new mutable.WeakHashMap[E, JComponent]()

  /** root component, add this to your Swing hierarchy */
  lazy val root = new JSplitPane(
    if (horizontal) JSplitPane.HORIZONTAL_SPLIT else JSplitPane.VERTICAL_SPLIT,
    new JScrollPane(master), detailPane
  )

  /** Called when selection in master list has changed,
    * it creates and activates new detail form for an item
    */
  def masterSelectionChanged(item: E): Unit = {
    val layout = detailPane.getLayout.asInstanceOf[CardLayout]
    if (item == null) {
      layout.show(detailPane, null)
      return
    }

    if (!openedItems.contains(item)) {
      val form = createDetail(item)
      openedItems.put(item, form)
      //TODO using item.toString is probably not best idea
      detailPane.add(form, item.toString)
    }
    /* val detail = */ openedItems(item)

    layout.show(detailPane, item.toString)
    detailPane.revalidate()
  }

  def removeAllItems(): Unit = {
    items.clear()
    openedItems.clear()
    detailPane.removeAll()
  }

  /** creates detail form for an item, implement it */
  def createDetail(item: E): JComponent
}
