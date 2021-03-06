/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.brainduck.memory.gui;

import emulib.plugins.memory.Memory;
import emulib.plugins.memory.MemoryContext;
import java.util.Objects;
import javax.swing.JDialog;

public class MemoryGUI extends JDialog {
    private final MemoryTableModel tableModel;
    private final MemoryContext memory;

    private class MemoryListenerImpl implements Memory.MemoryListener {

        @Override
        public void memoryChanged(int memoryPosition) {
            tableModel.dataChangedAt(memoryPosition);
        }

        @Override
        public void memorySizeChanged() {
            tableModel.fireTableDataChanged();
        }
    }

    public MemoryGUI(MemoryContext memory) {
        setLocationRelativeTo(null);
        initComponents();
        
        this.memory = Objects.requireNonNull(memory);
        this.tableModel = new MemoryTableModel(memory);
        MemoryTable table = new MemoryTable(tableModel, scrollPane);
        scrollPane.setViewportView(table);
        
        lblPageCount.setText(String.valueOf(tableModel.getPageCount()));

        memory.addMemoryListener(new MemoryListenerImpl());
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        javax.swing.JButton btnPageDown = new javax.swing.JButton();
        javax.swing.JButton btnPageUp = new javax.swing.JButton();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        lblPageCount = new javax.swing.JLabel();
        txtPage = new javax.swing.JTextField();
        btnClear = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("BrainDuck Memory");

        btnPageDown.setText("Page Down");
        btnPageDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPageDownActionPerformed(evt);
            }
        });

        btnPageUp.setText("Page Up");
        btnPageUp.setToolTipText("");
        btnPageUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPageUpActionPerformed(evt);
            }
        });

        jLabel1.setFont(jLabel1.getFont());
        jLabel1.setText("Page:");

        jLabel2.setText("/");

        lblPageCount.setText("0");

        txtPage.setText("0");
        txtPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPageActionPerformed(evt);
            }
        });

        btnClear.setText("Clear");
        btnClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtPage, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblPageCount)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 332, Short.MAX_VALUE)
                .addComponent(btnClear)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnPageUp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnPageDown))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(0, 12, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnPageDown)
                    .addComponent(btnPageUp)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(lblPageCount)
                    .addComponent(txtPage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnClear)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnPageUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPageUpActionPerformed
        int page = tableModel.getPage() - 1;
        if (page >= 0) {
            tableModel.setPage(page);
        }
        
        txtPage.setText(String.valueOf(tableModel.getPage()));
    }//GEN-LAST:event_btnPageUpActionPerformed

    private void btnPageDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPageDownActionPerformed
        int page = tableModel.getPage() + 1;
        if (page < tableModel.getPageCount()) {
            tableModel.setPage(page);
        }

        txtPage.setText(String.valueOf(tableModel.getPage()));
    }//GEN-LAST:event_btnPageDownActionPerformed

    private void txtPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPageActionPerformed
        int page = Integer.decode(evt.getActionCommand());
        if (page >= 0 && page < tableModel.getPageCount()) {
            tableModel.setPage(page);
        }
        
        txtPage.setText(String.valueOf(tableModel.getPage()));
    }//GEN-LAST:event_txtPageActionPerformed

    private void btnClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearActionPerformed
        memory.clear();
        tableModel.fireTableDataChanged();
    }//GEN-LAST:event_btnClearActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClear;
    private javax.swing.JLabel lblPageCount;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JTextField txtPage;
    // End of variables declaration//GEN-END:variables
}
