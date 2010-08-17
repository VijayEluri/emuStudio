/*
 * DebugTableModel.java
 *
 * Created on Pondelok, 2007, marec 26, 16:29
 *
 * KEEP IT SIMPLY STUPID
 * YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package emustudio.gui.utils;

import javax.swing.table.*;

import plugins.memory.IMemory;
import plugins.cpu.ICPU;

/**
 *
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class DebugTableModel extends AbstractTableModel {
    private ICPU cpu;
    private IMemory mem;
    
    private int nextAddress;
    private int lastRow;
    private int firstRowAddress;
    private int page = 1; // pagination support
    
    private final int MAX_ROW_COUNT = 25;
    
    /** Creates a new instance of DebugTableModel */
    public DebugTableModel(ICPU cpu, IMemory mem) {
        this.cpu = cpu;
        this.mem = mem;
        lastRow = 0;
        nextAddress = this.getFirstRowAddress();
        firstRowAddress = 0;
    }

    // pocet riadkov
    public int getRowCount() {
        try {
//            int a = mem.getContext().getSize() - (cpu.getContext().getInstrPosition()+page);
  //          if (a < MAX_ROW_COUNT) return a;
    //        else
            return MAX_ROW_COUNT;
        } catch(NullPointerException e) {
            return 0;
        }
    }

    public int getColumnCount() {
        try {  return cpu.getDebugColumns().length; }
        catch(NullPointerException e) {
            return 0;
        }
    }

    @Override
    public String getColumnName(int col) {
        return cpu.getDebugColumns()[col].getName();
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return cpu.getDebugColumns()[columnIndex].getType();
    }
    
    // posun dolava o 20 instrukcii
    public void previous() { page -= 20; }
    
    // posun dolava o 20 instrukcii
    public void next() {
        if (mem.getSize() > (cpu.getInstrPosition() + page))
            page += 20;
    }

    // posun na adresu PC
    public void topc() {
        page = 1;
    }
    
    
    // zisti adresu prveho riadku v tabulke, ktory sa zobrazi
    // bug fixed: ak sa k aktualnej instrukcii neda prist od 0-ly
    // max 10 operacnych kodov sa zobrazi pred aktualnou instrukciou
    // urychlena verzia
    private int getFirstRowAddress() {
        try {
            int pc = cpu.getInstrPosition();
            int up = 0; /* ci korekcia ma stupajucu(1),
                           alebo klesajucu(2) tendenciu */

            firstRowAddress = pc-10+page; 
            							/* na zaciatku sa natvrdo nastavi 
                                             pohlad o 10 bytov dozadu
                                           */
            if ((pc+page) > mem.getSize()) {
                page = mem.getSize() - pc;
                firstRowAddress = pc-10+page;
            }

            if (firstRowAddress < 0) {
                firstRowAddress = 0; // ak je to < 0, potom je pohlad 0
                page = 0;
            }
            
            int diff = firstRowAddress;
            while (true) {
                if (diff == (pc+page)) break; /* ak sa dosiahol pc, firstRowAddress
                                          ma tu spravnu hodnotu */
                try { 
                    // inak pokracuj v hladani 
                    diff = cpu.getInstrPosition(diff); 
                } catch(ArrayIndexOutOfBoundsException w) { break; }
                if (diff > (pc+page)) { 
                    // ak sa od firstRowAddress neviem dostat k PC (iba za neho)
                    if ((up == 0) && (firstRowAddress <= 0))
                        up = 1; /* ak je firstRowAddress==0, korekcia bude mat
                                   stupajucu tendenciu */
                    else if (up == 0) 
                        up = 2; // inak klesajucu (viem znizovat firstRowAddress)
                    
                    if (firstRowAddress < (pc-20 +page)) {
                        // ak som klesol uz prilis vela, nema to zmysel, skusim stupat
                        firstRowAddress = pc-9+page; up=1;
                        diff = firstRowAddress; // ?
                        continue;
                    }
                    if (firstRowAddress > (pc+page)) {
                        // skusal som dozadu, aj dopredu a neda sa... => vtedy
                        // pouzijem klasicky sposob.. od 0-ly
                        firstRowAddress = 0;
                        while (pc > (firstRowAddress+10+page))
                            firstRowAddress = cpu.getInstrPosition(firstRowAddress);
                        return firstRowAddress;
                    }                    
                    if (up == 1) firstRowAddress++;
                    else firstRowAddress--;

                    if (firstRowAddress < 0) {
                        page--;
                        firstRowAddress = pc-10+page; 
                        if ((pc+page) > mem.getSize()) {
                            page = mem.getSize() - pc;
                            firstRowAddress = pc-10+page;
                        }
                    }
                    diff = firstRowAddress;
                }
            }
            return firstRowAddress;
        } catch(NullPointerException e) {
            return 0;
        }
    }
    
    public int getRowAddress(int rowIndex) {
        if (rowIndex == 0) return this.getFirstRowAddress();
        else if (rowIndex == (lastRow +1)) return nextAddress;
        else { int i = 0, a = 0;
            a = this.getFirstRowAddress();
            try {
                for (i = 0; i < rowIndex; i++) a = cpu.getInstrPosition(a);
            } catch(ArrayIndexOutOfBoundsException w) {
                return 0;
            }
            return a;
        }
    }
    
    public Object getValueAt(int rowIndex, int columnIndex) {
        int addr;
        try {
            // get position of first row - user can see 8 instruction before actual
            if (rowIndex == 0) { nextAddress = this.getFirstRowAddress(); lastRow = 0; }
            else if (rowIndex != (lastRow +1)) {
                // move nextAddress pointer to address for current row
                int i = 0;
                nextAddress = this.getFirstRowAddress();
                for (i = 0; i < rowIndex; i++) 
                    nextAddress = cpu.getInstrPosition(nextAddress);
                lastRow = i;
            }
            addr = nextAddress;
            nextAddress = cpu.getInstrPosition(nextAddress);
            lastRow = rowIndex;
            return cpu.getDebugValue(addr, columnIndex);
        } catch(ArrayIndexOutOfBoundsException w) {
            return null;
        }
    }
    
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        int a = getRowAddress(rowIndex);
        cpu.setDebugValue(a,columnIndex, aValue);
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return cpu.getDebugColumns()[columnIndex].isEditable();
    }
    
}