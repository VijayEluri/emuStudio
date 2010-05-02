/**
 *  RAMDisassembler.java
 * 
 *  KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package ramcpu.gui;

import interfaces.IRAMInstruction;
import interfaces.IRAMMemoryContext;
import ramcpu.impl.RAM;
import plugins.cpu.IDebugColumn;

public class RAMDisassembler {
	private IRAMMemoryContext mem;
	private RAM cpu;
    private IDebugColumn[] columns;
	
    /**
     * V konštruktore vytvorím stĺpce ako objekty
     * triedy ColumnInfo.
     * 
     * @param mem  kontext operačnej pamäte, ktorý bude
     *             potrebný pre dekódovanie inštrukcií
     */
	public RAMDisassembler(IRAMMemoryContext mem, RAM cpu) {
		this.mem = mem;
		this.cpu = cpu;
        columns = new IDebugColumn[3];
        IDebugColumn c1 = new ColumnInfo("breakpoint", Boolean.class,true);
        IDebugColumn c2 = new ColumnInfo("address", String.class,false);
        IDebugColumn c3 = new ColumnInfo("mnemonics", String.class,false);
        columns[0] = c1;columns[1] = c2;columns[2] = c3;
	}
	
	/**
	 * Metóda vráti stĺpce pre okno debuggera ako pole.
	 *  
	 * @return pole stĺpcov
	 */
    public IDebugColumn[] getDebugColumns() { return columns; }

    public Object getDebugColVal(int row, int col) {
        try {
            IRAMInstruction instr = (IRAMInstruction)mem.read(row);
            switch (col) {
                case 0:
                	return cpu.getBreakpoint(row);
                case 1: {
                	String s = mem.getLabel(row);
                	if (s != null)
                		return String.valueOf(row) + " (" + s + ")";
                	else
                		return String.valueOf(row);
                }
                case 2:
                	if (instr == null) return "empty";
                	return instr.getCodeStr() +" " +instr.getOperandStr();
                default: return "";
            }
        } catch(IndexOutOfBoundsException e) {
            switch (col) {
                case 0: return cpu.getBreakpoint(row);
                case 1: 
                	String s = mem.getLabel(row);
                	if (s != null)
                		return String.valueOf(row) + " (" + s + ")";
                	else
                		return String.valueOf(row);
                case 2: return "incomplete";
                default: return "";
            }
        }
    }

    public void setDebugColVal(int row, int col, Object value) {
        if (col != 0) return;
        if (value.getClass() != Boolean.class) return;
        
        boolean v = Boolean.valueOf(value.toString());
        cpu.setBreakpoint(row,v);
    }

}
