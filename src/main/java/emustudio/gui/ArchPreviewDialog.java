/**
 * ArchPreviewDialog.java
 *
 * Created on Piatok, 2008, júl 18, 19:36
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2008-2012 Peter Jakubčo
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.gui;

import emustudio.architecture.drawing.PreviewPanel;
import emustudio.architecture.drawing.Schema;
import javax.swing.GroupLayout;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

/**
 * Preview dialog form of virtual computer.
 * 
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class ArchPreviewDialog extends JDialog {

    private PreviewPanel pan;

    /**
     * Creates new preview dialog form instance.
     *
     * @param parent parent dialog
     * @param modal whether this dialog should be modal
     * @param schema virtual computer schema that will be previewed
     */
    public ArchPreviewDialog(JDialog parent, boolean modal, Schema schema) {
        super(parent, modal);
        pan = new PreviewPanel(schema);
        initComponents(pan.getWidth(), pan.getHeight());
        scrollPane.setViewportView(pan);
        this.setLocationRelativeTo(null);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents(int panelWidth, int panelHeight) {

        scrollPane = new JScrollPane();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Preview architecture");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, panelWidth, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, panelHeight, Short.MAX_VALUE));

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables
}
