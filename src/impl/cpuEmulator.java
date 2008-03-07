/*
 * cpuEmulator.java
 *
 * Implementation of CPU emulation
 * 
 * Created on Piatok, 2007, oktober 26, 10:45
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package impl;

import plugins.cpu.*;
import plugins.device.*;
import plugins.memory.*;

import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

/**
 * Main implementation class for CPU emulation
 * CPU works in a separate thread (parallel with other hardware)
 * 
 * @author vbmacher
 */
public class cpuEmulator implements ICPU, Runnable {
    private Thread cpuThread = null;
    private statusGUI status;
    private EventListenerList listenerList;
    private Hashtable devicesList;
    private EventObject cpuEvt = new EventObject(this);

    private IMemory mem;

    // cpu speed
    private int clockFrequency = 2000; // kHz
    private long long_cycles = 0; // count of executed cycles for runtime freq. computing
    private java.util.Timer freqScheduler;
    private RuntimeFrequencyCalculator rfc;

    // registers are public meant for only statusGUI (didnt want make it thru get() methods)
    private int PC=0; // program counter
    public int SP=0; // stack pointer
    public short B=0, C=0, D=0, E=0, H=0, L=0, Flags=2, A=0; // registre
    public final int flagS = 0x80, flagZ = 0x40, flagAC = 0x10, flagP = 0x4, flagC = 0x1;
    
    private boolean INTE = false; // povolenie / zakazanie preruseni
    private boolean isINT = false;
    private short b1 = 0; // interrupt instruction can be rst (b1) or call (b1,b2,b3)
    private short b2 = 0;
    private short b3 = 0;
    
    private HashSet breaks; // zoznam breakpointov (mnozina)
    private stateEnum run_state; // dovod zastavenia emulatora

    private byte parity_table[] = {
        1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,0,1,1,0,1,0,0,1,1,0,0,1,0,1,1,0,0,1,1,0,
        1,0,0,1,1,0,0,1,0,1,1,0,1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,0,1,1,0,1,0,0,1,
        1,0,0,1,0,1,1,0,1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,1,0,0,1,0,1,1,0,0,1,1,0,
        1,0,0,1,0,1,1,0,1,0,0,1,1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,1,0,0,1,0,1,1,0,
        1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,0,1,1,0,
        1,0,0,1,1,0,0,1,0,1,1,0,1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,0,1,1,0,1,0,0,1,
        1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1,1,0,0,1,0,1,1,0,1,0,0,1,0,1,1,0,0,1,1,0,
        1,0,0,1
    };
    
    
    /** Creates a new instance of cpuEmulator */
    public cpuEmulator() {
        run_state = stateEnum.stoppedNormal;
        breaks = new HashSet();
        listenerList = new EventListenerList();
        status = new statusGUI(this);
        devicesList = new Hashtable();
        rfc = new RuntimeFrequencyCalculator();
        freqScheduler = new java.util.Timer();
    }

    public String getDescription() { return "Intel 8080 CPU VM,"
            + " modified for use as CPU for MITS Altair 8800 computer"; }
    public String getVersion() { return "0.11b"; }
    public String getName() { return "Virtual i8080 CPU"; }
    public String getCopyright() { return "\u00A9 Copyright 2006-2008, Peter Jakubčo"; }

    /**
     * this must be called as constructor (initialization)
     */ 
    public void init(IMemory mem) {
        this.mem = mem;
        status.setMem(mem);
    }
    public void destroy() {
        run_state = stateEnum.stoppedNormal;
        setRuntimeFreqCounter(false);        
        devicesList.clear();
    }

    /**
     * Reset CPU (initialize before run)
     */    
    public void reset(int programStart) {
        SP = A = B = C = D = E = H = L = 0;
        Flags = 2; //0000 0010b
        PC = programStart;
        INTE = false;
        run_state = stateEnum.stoppedBreak;
        cpuThread = null;
        setRuntimeFreqCounter(false);
        fireCpuRun(cpuEvt);
        fireCpuState(cpuEvt);
    }
    /**
     * Create CPU Thread and start it until CPU halt (instruction hlt)
     * or until address fallout
     */ 
    public void execute() {
        cpuThread = new Thread(this, "i8080");
        cpuThread.start();
    }
    /**
     * Forced (external) breakpoint
     */
    public void pause() {
        run_state = stateEnum.stoppedBreak;
        setRuntimeFreqCounter(false);
        fireCpuRun(cpuEvt);
    }
    /**
     *  Stops an emulation
     */    
    public void stop() {
        run_state = stateEnum.stoppedNormal;
        setRuntimeFreqCounter(false);
        fireCpuRun(cpuEvt);
    }
    // vykona 1 krok - bez merania casov (bez real-time odozvy)
    public void step() {
        if (run_state == stateEnum.stoppedBreak) {
            try {
                synchronized(run_state) {
                    run_state = stateEnum.runned;
                    evalStep();
                    if (run_state == stateEnum.runned)
                        run_state = stateEnum.stoppedBreak;
                }
            }
            catch (IndexOutOfBoundsException e) {
                run_state = stateEnum.stoppedAdrFallout;
            }
            fireCpuRun(cpuEvt);
            fireCpuState(cpuEvt);
        }
    }
    public void interrupt(short b1, short b2, short b3) {
        if (INTE == false) return;
        isINT = true;
        this.b1 = b1;
        this.b2 = b2;
        this.b3 = b3;
    }
    
    public void addCPUListener(ICPUListener listener) {
        listenerList.add(ICPUListener.class, listener);
    }    
    public void removeCPUListener(ICPUListener listener) {
        listenerList.remove(ICPUListener.class, listener);
    }
    
    /* DOWN: GUI interaction */
    public IDebugColumns[] getDebugColumns() { return status.getDebugColumns(); }
    public void setDebugValue(int index, int col, Object value) {
        status.setDebugColVal(index, col, value);
    }
    public Object getDebugValue(int index, int col) {
        return status.getDebugColVal(index, col);
    }
    public JPanel getStatusGUI() { return status.getStatusPanel(); }    
    
    /* DOWN: Device interaction */
    // device mapping = only one device can be attached to one port
    public boolean attachDevice(IDevice.IDevListener listener, int port) {
        if (devicesList.containsKey(port)) return false;
        devicesList.put(port, listener);
        return true;
    }
    public void disattachDevice(int port) {
        if (devicesList.containsKey(port))
            devicesList.remove(port);
    }

    /* DOWN: CPU Context */
    // get the address from next instruction
    // this method exist only from a view of effectivity
    public int getNextPC(int memPos) {
        return status.getNextPosition(memPos);
    }
    public int getPC() { return PC; }
    
    public boolean setPC(int memPos) { 
        if (memPos < 0 || memPos > mem.getSize()) return false;
        PC = memPos;
        return true;
    }
    
    /* DOWN: other */
    
    public int getFrequency() { return this.clockFrequency; }
    // frequency in kHz
    public void setFrequency(int freq) { this.clockFrequency = freq; }
    
    public void setRuntimeFreqCounter(boolean run) {
        if (run) {
            try { 
                freqScheduler.purge();
                freqScheduler.scheduleAtFixedRate(rfc, 0, 1000);
            } catch(Exception e) {}
        } else {
            try {
                rfc.cancel();
                rfc = new RuntimeFrequencyCalculator();
            } catch (Exception e) {}
        }
    }
    
        
    public void setBreakpoint(int adr, boolean set) { 
        if (set) breaks.add(adr);
        else breaks.remove(adr);
    }
    public boolean getBreakpoint(int address) { return breaks.contains(address); }
    public boolean isBreakpointSupported() { return true; }
    
    /**
     * Run a CPU execution (thread).
     * 
     * Real-time CPU frequency balancing
     * *********************************
     * 
     * 1 cycle is performed in 1 periode of CPU frequency.
     * CPU_PERIODE = 1 / CPU_FREQ [micros]
     * cycles_to_execute_per_second = 1000 / CPU_PERIODE
     * 
     * cycles_to_execute_per_second = 1000 / (1/CPU_FREQ)
     * cycles_to_execute_per_second = 1000 * CPU_FREQ
     * 
     * 1000 s = 1 micros => slice_length (can vary)
     * 
     */
    
    @SuppressWarnings("static-access")
    public void run() {
        long startTime, endTime;
        int cycles_executed;
        int cycles_to_execute; // per second
        int cycles;
        
        run_state = stateEnum.runned;
        fireCpuRun(cpuEvt);
        fireCpuState(cpuEvt);
        /* 1 Hz  .... 1 tState per second
         * 1 kHz .... 1000 tStates per second
         * clockFrequency is in kHz it have to be multiplied with 1000
         */
        cycles_to_execute = 1000 * clockFrequency;
        long i = 0;
        synchronized(run_state) {
            while(run_state == stateEnum.runned) {
                i++;
                startTime = System.nanoTime();
                cycles_executed = 0;
                try { 
                    while((cycles_executed < cycles_to_execute)
                            && (run_state == stateEnum.runned)) {
                        cycles = evalStep();
                        cycles_executed += cycles;
                        long_cycles += cycles;
                        if (breaks.contains(PC) == true)
                            throw new Error();
                    }
                }
                catch (IndexOutOfBoundsException e) {
                    run_state = stateEnum.stoppedAdrFallout;
                    break;
                }
                catch (Error er) {
                    run_state = stateEnum.stoppedBreak;
                    break;
                }
                endTime = System.nanoTime() - startTime;
                if (endTime < 1000000000) {
                    // time correction
                    try { cpuThread.sleep((1000000000 - endTime)/1000000); }
                    catch(java.lang.InterruptedException e) {}
                }
            }
        }
        setRuntimeFreqCounter(false);
        fireCpuState(cpuEvt);
        fireCpuRun(cpuEvt);
    }

    /**
     * This class perform runtime frequency calculation
     * 
     * Given: time, executed cycles count
     * Frequency is defined as number of something by some period of time.
     * Hz = 1/s, kHz = 1000/s
     * time has to be in seconds
     * 
     * CC ..by.. time[s]
     * XX ..by.. 1 [s] ?
     * ---------------
     * XX:CC = 1:time
     * XX = CC / time [Hz]
     */
    private class RuntimeFrequencyCalculator extends TimerTask {
        private long startTimeSaved = 0;
        
        public void run() {
            double endTime = System.nanoTime();
            double time = endTime - startTimeSaved;

            if (long_cycles == 0) return;
            double freq = (double)long_cycles / (time / 1000000.0);
            startTimeSaved = (long)endTime;
            long_cycles = 0;
            fireFrequencyChanged(cpuEvt, (float)freq);
        }
        
    }
        
    
    /* Get an 8080 register and return it */
    private short getreg(int reg) {
        switch (reg) {
            case 0: return B;  case 1: return C;  case 2: return D;
            case 3: return E;  case 4: return H;  case 5: return L;
            case 6: return mem.read8((H << 8) | L);  case 7: return A;
        }
        return 0;
    }

    /* Put a value into an 8080 register from memory */
    private void putreg(int reg, short val) {
        switch (reg) {
            case 0: B = val; break; case 1: C = val; break;
            case 2: D = val; break; case 3: E = val; break;
            case 4: H = val; break; case 5: L = val; break;
            case 6: mem.write8((H << 8) | L, val); break;
            case 7: A = val;
        }
    }
    
    /* Put a value into an 8080 register pair */
    void putpair(int reg, int val) {
        short high, low;
        high = (short)((val >>> 8) & 0xFF); low = (short)(val & 0xFF);
        switch (reg) {
            case 0: B = high; C = low; break; case 1: D = high; E = low; break;
            case 2: H = high; L = low; break; case 3: SP = val; break;
        }
    }

    /* Return the value of a selected register pair */
    int getpair(int reg) {
        switch (reg) {
            case 0: return (B << 8) | C; case 1: return (D << 8) | E;
            case 2: return (H << 8) | L; case 3: return SP;
        }
        return 0;
    }

    /* Return the value of a selected register pair, in PUSH
       format where 3 means A& flags, not SP
     */
    private int getpush(int reg) {
        int stat;
        switch (reg) {
            case 0: return (B << 8) | C; case 1: return (D << 8) | E;
            case 2: return (H << 8) | L; case 3: stat = (A << 8) | Flags;
                stat |= 0x02; return stat;
        }
        return 0;
    }

    /* Place data into the indicated register pair, in PUSH
       format where 3 means A& flags, not SP
     */
    private void putpush(int reg, int val) {
        short high, low;
        high = (short)((val >>> 8) & 0xFF); low = (short)(val & 0xFF);
        switch (reg) {
            case 0: B = high; C = low; break; case 1: D = high; E = low; break;
            case 2: H = high; L = low; break;
            case 3: A = (short)((val >>> 8) & 0xFF);
                Flags = (short)(val & 0xFF);
                break;
        }
    }

    /* Set the <C>arry, <S>ign, <Z>ero and <P>arity flags following
       an arithmetic operation on 'reg'. 8080 changes AC flag
    */
    private void setarith(int reg, int before) {
        if ((reg & 0x100) != 0) Flags |= flagC; else Flags &= (~flagC);
        if ((reg & 0x80) != 0) Flags |= flagS;  else Flags &= (~flagS);
        if ((reg & 0xff) == 0) Flags |= flagZ;  else Flags &= (~flagZ);
        // carry from 3.bit to 4.
        if (((before & 8) == 8) && ((reg & 0x10) == 0x10)) Flags |= flagAC;
        else Flags &= (~flagAC);
        parity(reg);
    }

    /* Set the <S>ign, <Z>ero amd <P>arity flags following
       an INR/DCR operation on 'reg'.
    */
    private void setinc(int reg, int before) {
        if ((reg & 0x80) != 0) Flags |= flagS; else Flags &= (~flagS);
        if ((reg & 0xff) == 0) Flags |= flagZ; else Flags &= (~flagZ);
        if (((before & 8) == 8) && ((reg & 0x10) == 0x10)) Flags |= flagAC;
        else Flags &= (~flagAC);
        parity(reg);
    }

    /* Set the <C>arry, <S>ign, <Z>ero amd <P>arity flags following
       a logical (bitwise) operation on 'reg'.
    */
    private void setlogical(int reg) {
        Flags &= (~flagC);
        if ((reg & 0x80) != 0) Flags |= flagS; else Flags &= (~flagS);
        if ((reg & 0xff) == 0) Flags |= flagZ; else Flags &= (~flagZ);
        Flags &= (~flagAC); parity(reg);
    }

    /* Set the Parity (P) flag based on parity of 'reg', i.e., number
       of bits on even: P=0200000, else P=0
    */
    private void parity(int reg)
    {
        if (parity_table[reg & 0xFF] == 1) Flags |= flagP;
        else Flags &= (~flagP);
    }

    /* Test an 8080 flag condition and return 1 if true, 0 if false */
    private int cond(int con) {
        switch (con) {
            case 0: if ((Flags & flagZ) == 0) return 1; break;
            case 1: if ((Flags & flagZ) != 0) return 1; break;
            case 2: if ((Flags & flagC) == 0) return 1; break;
            case 3: if ((Flags & flagC) != 0) return 1; break;
            case 4: if ((Flags & flagP) == 0) return 1; break;
            case 5: if ((Flags & flagP) != 0) return 1; break;
            case 6: if ((Flags & flagS) == 0) return 1; break;
            case 7: if ((Flags & flagS) != 0) return 1; break;
        }
        return 0;
    }

    private int evalStep() {
        short OP;
        int DAR;
        
        /* if interrupt is waiting, instruction won't be read from memory
         * but from one or all of 3 bytes (b1,b2,b3) which represents either
         * rst or call instruction incomed from external peripheral device
         */
        if (isINT == true) {
            if (INTE == true) {
                if ((b1 & 0xC7) == 0xC7) {                      /* RST */
                    mem.write16(SP-2,PC); SP -= 2; PC = b1 & 0x38; return 11;
                } else if (b1 == 0315) {                        /* CALL */
                    mem.write16(SP-2, PC+2); SP -= 2; 
                    PC = (int)(((b3 & 0xFF) << 8) | (b2 & 0xFF));
                    return 17;
                }
            }
            isINT = false;
        }        
        OP = mem.read8(PC++);
        if (OP == 118) { // hlt?
            run_state = stateEnum.stoppedNormal;
            return 7;
        }

       /* Handle below all operations which refer to registers or register pairs.
          After that, a large switch statement takes care of all other opcodes */
        if ((OP & 0xC0) == 0x40) {                             /* MOV */
            putreg((OP >>> 3) & 0x07, getreg(OP & 0x07)); 
            if (((OP & 0x07) == 6) || (((OP >>> 3) & 0x07) == 6)) return 7; else return 5;
        } else if ((OP & 0xC7) == 0x06) {                      /* MVI */
            putreg((OP >>> 3) & 0x07, mem.read8(PC++));
            if (((OP >>> 3) & 0x07) == 6) return 10; else return 7;
        } else if ((OP & 0xCF) == 0x01) {                      /* LXI */
            putpair((OP >>> 4) & 0x03, mem.read16(PC)); PC += 2; return 10;
        } else if ((OP & 0xEF) == 0x0A) {                      /* LDAX */
            putreg(7, mem.read8(getpair((OP >>> 4) & 0x03))); return 7;
        } else if ((OP & 0xEF) == 0x02) {                      /* STAX */
            mem.write8(getpair((OP >>> 4) & 0x03), getreg(7)); return 7;
        } else if ((OP & 0xF8) == 0xB8) {                      /* CMP */
            int X = A; DAR = A & 0xFF; DAR -= getreg(OP & 0x07);
            setarith(DAR, X); if ((OP & 0x07) == 6) return 7; else return 4;
        } else if ((OP & 0xC7) == 0xC2) {                      /* JMP <condition> */
            if (cond((OP >>> 3) & 0x07) == 1) PC = mem.read16(PC);
            else PC += 2; return 10;
        } else if ((OP & 0xC7) == 0xC4) {                      /* CALL <condition> */
            if (cond((OP >>> 3) & 0x07) == 1) {
                DAR = mem.read16(PC); PC += 2; mem.write16(SP-2,PC); SP -= 2;
                PC = DAR; return 17;
            } else { PC += 2; return 11; }
        } else if ((OP & 0xC7) == 0xC0) {                      /* RET <condition> */
            if (cond((OP >>> 3) & 0x07) == 1) {
                PC = mem.read16(SP); SP += 2;
            } return 10;
        } else if ((OP & 0xC7) == 0xC7) {                      /* RST */
            mem.write16(SP-2,PC); SP -= 2; PC = OP & 0x38; return 11;
        } else if ((OP & 0xCF) == 0xC5) {                      /* PUSH */
            DAR = getpush((OP >>> 4) & 0x03); mem.write16(SP-2,DAR); SP -= 2;
            return 11;
        } else if ((OP & 0xCF) == 0xC1) {                      /*POP */
            DAR = mem.read16(SP); SP += 2; putpush((OP >>> 4) & 0x03, DAR);
            return 10;
        } else if ((OP & 0xF8) == 0x80) {                      /* ADD */
            int X = A; DAR = A & 0xF0; A += getreg(OP & 0x07); setarith(A,X);
            A = (short)(A & 0xFF); if ((OP & 0x07) == 6) return 7; return 4;
        } else if ((OP & 0xF8) == 0x88) {                      /* ADC */
            int X = A; A += getreg(OP & 0x07); if ((Flags & flagC) != 0) A++; 
            setarith(A,X); A = (short)(A & 0xFF); if ((OP & 0x07) == 6) return 7; 
            return 4;
        } else if ((OP & 0xF8) == 0x90) {                      /* SUB */
            int X = A; A -= getreg(OP & 0x07); setarith(A,X);
            A = (short)(A & 0xFF); if ((OP & 0x07) == 6) return 7; return 4;
        } else if ((OP & 0xF8) == 0x98) {                      /* SBB */
            int X = A; A -= (getreg(OP & 0x07)); if ((Flags & flagC) != 0) A--; 
            setarith(A,X); A = (short)(A & 0xFF); if ((OP & 0x07) == 6) return 7;
            return 4;
        } else if ((OP & 0xC7) == 0x04) {                      /* INR */
            DAR = getreg((OP >>> 3) & 0x07) + 1; setinc(DAR, DAR-1);
            DAR = DAR & 0xFF; putreg((OP >>> 3) & 0x07, (short)DAR); return 5;
        } else if ((OP & 0xC7) == 0x05) {                      /* DCR */
            DAR = getreg((OP >>> 3) & 0x07) - 1; setinc(DAR,DAR+1);
            DAR = DAR & 0xFF; putreg((OP >>> 3) & 0x07, (short)DAR); return 5;
        } else if ((OP & 0xCF) == 0x03) {                      /* INX */
            DAR = getpair((OP >>> 4) & 0x03) + 1; DAR = DAR & 0xFFFF;
            putpair((OP >>> 4) & 0x03, DAR); return 5;
        } else if ((OP & 0xCF) == 0x0B) {                      /* DCX */
            DAR = getpair((OP >>> 4) & 0x03) - 1; DAR = DAR & 0xFFFF;
            putpair((OP >>> 4) & 0x03, DAR); return 5;
        } else if ((OP & 0xCF) == 0x09) {                      /* DAD */
            DAR = getpair((OP >>> 4) & 0x03); DAR += getpair(2);
            if ((DAR & 0x10000) != 0) Flags |= flagC; else Flags &= (~flagC);
            DAR = DAR & 0xFFFF; putpair(2, DAR); return 10;
        } else if ((OP & 0xF8) == 0xA0) {                      /* ANA */
            A &= getreg(OP & 0x07); setlogical(A); A &= 0xFF; return 4;
        } else if ((OP & 0xF8) == 0xA8) {                      /* XRA */
            A ^= getreg(OP & 0x07); setlogical(A); A &= 0xFF; return 4;
        } else if ((OP & 0xF8) == 0xB0) {                      /* ORA */
            A |= getreg(OP & 0x07); setlogical(A); A &= 0xFF; return 4;
        }
        /* The Big Instruction Decode Switch */
        switch (OP) {
            /* Logical instructions */
            case 0376: {                                     /* CPI */
                int X = A; DAR = A & 0xFF; DAR -= mem.read8(PC++); setarith(DAR,X);
                return 7; }
            case 0346:                                     /* ANI */
                A &= mem.read8(PC++); Flags &= (~flagC); Flags &= (~flagAC);
                setlogical(A); A &= 0xFF; return 7;
            case 0356:                                     /* XRI */
                A ^= mem.read8(PC++); Flags &= (~flagC); Flags &= (~flagAC);
                setlogical(A); A &= 0xFF; return 7;
            case 0366:                                     /* ORI */
                A |= mem.read8(PC++); Flags &= (~flagC); Flags &= (~flagAC);
                setlogical(A); A &= 0xFF; return 7;
            /* Jump instructions */
            case 0303:                                     /* JMP */
                PC = mem.read16(PC); return 10;
            case 0351:                                     /* PCHL */
                PC = (H << 8) | L; return 5;
            case 0315:                                     /* CALL */
                mem.write16(SP-2, PC+2); SP -= 2; PC = mem.read16(PC);
                return 17;
            case 0311:                                     /* RET */
                PC = mem.read16(SP); SP += 2; return 10;
            /* Data Transfer Group */
            case 062:                                      /* STA */
                DAR = mem.read16(PC); PC += 2; mem.write8(DAR, A); return 13;
            case 072:                                      /* LDA */
                DAR = mem.read16(PC); PC += 2; A = mem.read8(DAR); return 13;
            case 042:                                      /* SHLD */
                DAR = mem.read16(PC); PC += 2; mem.write16(DAR, (H << 8) | L);
                return 16;
            case 052:                                      /* LHLD BUG !*/
                DAR = mem.read16(PC); PC += 2;
                L = mem.read8(DAR); H = mem.read8(DAR+1);
                return 16;
            case 0353:                                     /* XCHG */
                short x = H, y = L; H = D; L = E; D = x; E = y; return 4;
            /* Arithmetic Group */
            case 0306:                                     /* ADI */
                DAR = A; A += mem.read8(PC++); setarith(A,DAR); A = (short)(A & 0xFF);
                return 7;
            case 0316:                                     /* ACI */
                DAR = A; A += mem.read8(PC++); if ((Flags & flagC) != 0) A++;
                setarith(A,DAR); A = (short)(A & 0xFF); return 7;
            case 0326:                                     /* SUI */
                DAR = A; A -= mem.read8(PC++); setarith(A,DAR);
                A = (short)(A & 0xFF); return 7;
            case 0336:                                     /* SBI */
                DAR = A; A -= mem.read8(PC++); if ((Flags & flagC) != 0) A--;
                setarith(A,DAR); A = (short)(A & 0xFF); return 7;
            case 047:                                      /* DAA */
                DAR = A & 0x0F;
                if ((DAR > 9) || ((Flags & flagAC) != 0)) {
                    DAR += 6; A &= 0xF0; A |= DAR & 0x0F;
                    if ((DAR & 0x10) != 0) Flags |= flagAC;
                    else Flags &= (~flagAC);
                }
                DAR = (A >>> 4) & 0x0F;
                if ((DAR > 9) || ((Flags & flagAC) != 0)) {
                    DAR += 6; if ((Flags & flagAC) != 0) DAR++;
                    A &= 0x0F; A |= (DAR << 4);
                }
                if (((DAR << 4) & 0x100) != 0) Flags |= flagC; 
                else Flags &= (~flagC);
                if ((A & 0x80) != 0) Flags |= flagS; else Flags &= (~flagS);
                if ((A & 0xff) == 0) Flags |= flagZ; else Flags &= (~flagZ);
                parity(A); A = (short)(A & 0xFF); return 4;
            case 07: {                                     /* RLC */
                int xx = (A << 9) & 0200000; if (xx != 0) Flags |= flagC;
                else Flags &= (~flagC); A = (short)((A << 1) & 0xFF);
                if (xx != 0) A |= 0x01; return 4; }
            case 017: {                                     /* RRC */
                if ((A & 0x01) == 1) Flags |= flagC; else Flags &= (~flagC);
                A = (short)((A >>> 1) & 0xFF); 
                if ((Flags & flagC) != 0) A |= 0x80; return 4; }
            case 027: {                                    /* RAL */
                int xx = (A << 9) & 0200000; A = (short)((A << 1) & 0xFF);
                if ((Flags & flagC) != 0) A |= 1; else A &= 0xFE;
                if (xx != 0) Flags |= flagC; else Flags &= (~flagC); return 4; }
            case 037: {                                    /* RAR */
                int xx = 0; if ((A & 0x01) == 1) xx |= 0200000;
                A = (short)((A >>> 1) & 0xFF);
                if ((Flags & flagC) != 0) A |= 0x80; else A &= 0x7F;
                if (xx != 0) Flags |= flagC; else Flags &= (~flagC); return 4; }
            case 057:                                      /* CMA */
                A = (short)(~A); A &= 0xFF; return 4;
            case 077:                                      /* CMC */
                if ((Flags & flagC) != 0) Flags &= (~flagC); else Flags |= flagC;
                return 4;
            case 067:                                      /* STC */
                Flags |= flagC; return 4;
            /* Stack, I/O & Machine Control Group */
            case 0:                                        /* NOP */
                return 4;
            case 0343:                                     /* XTHL */
                DAR = mem.read16(SP); mem.write16(SP, (H << 8) | L);
                H = (short)((DAR >>> 8) & 0xFF); L = (short)(DAR & 0xFF);
                return 18;
            case 0371:                                     /* SPHL */
                SP = (H << 8) | L; return 5;
            case 0373:                                     /* EI */
                INTE = true; return 4;
            case 0363:                                     /* DI */
                INTE = false; return 4;
            case 0333:                                     /* IN */
                DAR = mem.read8(PC++);
                fireIO(DAR, true);
                return 10;
            case 0323:                                     /* OUT */
                DAR = mem.read8(PC++);
                fireIO(DAR, false); 
                return 10;
        }
        run_state = stateEnum.stoppedBadInstr;
        return 0;
    }
    

    private void fireCpuRun(EventObject evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i] == ICPUListener.class)
                ((ICPUListener)listeners[i+1]).cpuRunChanged(evt, run_state);
        }
        status.updateGUI();
    }
    
    private void fireCpuState(EventObject evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i] == ICPUListener.class)
                ((ICPUListener)listeners[i+1]).cpuStateUpdated(evt);
        }
    }

    private void fireFrequencyChanged(EventObject evt, float freq) {
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i] == ICPUListener.class)
                ((ICPUListener)listeners[i+1]).frequencyChanged(evt,freq);
        }
    }
    

    private void fireIO(int port, boolean read) {
        if (devicesList.containsKey(port) == false) return;
        
        if (read == true)
            A = (short)((IDevice.IDevListener)devicesList.get(port)).
                    devIN(cpuEvt);
        else
            ((IDevice.IDevListener)devicesList.get(port)).
                    devOUT(cpuEvt,A);
    }
}
