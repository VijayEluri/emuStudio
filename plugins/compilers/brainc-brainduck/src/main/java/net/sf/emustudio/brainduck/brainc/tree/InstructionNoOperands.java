/*
 * InstructionNoOperands.java
 * 
 * Copyright (C) 2009-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.brainduck.brainc.tree;

import emulib.runtime.HEXFileManager;

public class InstructionNoOperands implements Instruction {

    public final static int HALT = 0;
    public final static int INC = 1;
    public final static int DEC = 2;
    public final static int INCV = 3;
    public final static int DECV = 4;
    public final static int PRINT = 5;
    public final static int LOAD = 6;
    public final static int LOOP = 7;
    public final static int ENDL = 8;
    private int instr;

    public InstructionNoOperands(int instr) {
        this.instr = instr;
    }

    @Override
    public int firstPass(int addressStart) throws Exception {
        return addressStart + 1;
    }

    @Override
    public void secondPass(HEXFileManager hex) {
        hex.putCode(String.format("%1$02X", instr));
    }
}