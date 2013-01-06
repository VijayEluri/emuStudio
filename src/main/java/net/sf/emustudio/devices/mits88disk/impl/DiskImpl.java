/*
 * DiskImpl.java
 *
 * Created on Streda, 30 january 2008
 * 
 * Copyright (C) 2008-2012 Peter Jakubčo
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
 *
 */
package net.sf.emustudio.devices.mits88disk.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.device.AbstractDevice;
import emulib.plugins.device.DeviceContext;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.StaticDialogs;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import net.sf.emustudio.devices.mits88disk.gui.ConfigDialog;
import net.sf.emustudio.devices.mits88disk.gui.DiskFrame;
import net.sf.emustudio.intel8080.ExtendedContext;

/**
 * MITS 88-DISK Floppy Disk controller with up to eight drives (although I think
 * that the interface can actually support up to 16 drives). The connected
 * floppy drives were Pertec FD-400 8" hard-sectored floppy drives. Each
 * single-sided diskette has 77 tracks of 32, 137-byte sectors each (an
 * unformatted capacity of 337,568 bytes). The controller supported neither
 * interrupts nor DMA, so floppy access required the sustained attention of the
 * CPU. The standard I/O addresses were 10Q-12Q.
 *
 * The controller is interfaced to the CPU by use of 3 I/O addreses, standardly,
 * these are device numbers 10, 11, and 12 (octal).
 *
 * Address Mode Function ------- ---- --------
 *
 * 10 Out Selects and enables Controller and Drive 10 In Indicates status of
 * Drive and Controller 11 Out Controls Disk Function 11 In Indicates current
 * sector position of disk 12 Out Write data 12 In Read data*
 *
 * Drive Select Out (Device 10 OUT):
 *
 * +---+---+---+---+---+---+---+---+ | C | X | X | X | Device |
 * +---+---+---+---+---+---+---+---+
 *
 * C = If this bit is 1, the disk controller selected by 'device' is cleared. If
 * the bit is zero, 'device' is selected as the device being controlled by
 * subsequent I/O operations. X = not used Device = value zero thru 15, selects
 * drive to be controlled.
 *
 * Drive Status In (Device 10 IN):
 *
 * 7 6 5 4 3 2 1 0
 * +---+---+---+---+---+---+---+---+ | R | Z | I | X | X | H | M | W |
 * +---+---+---+---+---+---+---+---+
 *
 * W - When 0, write circuit ready to write another byte. M - When 0, head
 * movement is allowed H - When 0, indicates head is loaded for read/write X -
 * not used (will be 0) I - When 0, indicates interrupts enabled (not used this
 * emulator) Z - When 0, indicates head is on track 0 R - When 0, indicates that
 * read circuit has new byte to read
 *
 * Drive Control (Device 11 OUT):
 *
 * +---+---+---+---+---+---+---+---+ | W | C | D | E | U | H | O | I |
 * +---+---+---+---+---+---+---+---+
 *
 * I - When 1, steps head IN one track O - When 1, steps head OUT out track H -
 * When 1, loads head to drive surface U - When 1, unloads head E - Enables
 * interrupts (ignored this simulator) D - Disables interrupts (ignored this
 * simulator) C - When 1 lowers head current (ignored this simulator) W - When
 * 1, starts Write Enable sequence: W bit on device 10 (see above) will go 1 and
 * data will be read from port 12 until 137 bytes have been read by the
 * controller from that port. The W bit will go off then, and the sector data
 * will be written to disk. Before you do this, you must have stepped the track
 * to the desired number, and waited until the right sector number is presented
 * on device 11 IN, then set this bit.
 *
 * Sector Position (Device 11 IN):
 *
 * As the sectors pass by the read head, they are counted and the number of the
 * current one is available in this register.
 *
 * +---+---+---+---+---+---+---+---+ | X | X | Sector Number | T |
 * +---+---+---+---+---+---+---+---+
 *
 * X = Not used Sector number = binary of the sector number currently under the
 * head, 0-31. T = Sector True, is a 1 when the sector is positioned to read or
 * write.
 *
 * @author vbmacher
 */
@PluginType(type = PLUGIN_TYPE.DEVICE,
title = "MITS 88-DISK device",
copyright = "\u00A9 Copyright 2007-2012, Peter Jakubčo",
description = "Implementation of popular floppy disk controller.")
public class DiskImpl extends AbstractDevice {
    private final static int DRIVES_COUNT = 16;
    public final static int CPU_PORT1 = 0x8;
    public final static int CPU_PORT2 = 0x9;
    public final static int CPU_PORT3 = 0xA;
    
    private int port1CPU;
    private int port2CPU;
    private int port3CPU;
    private ExtendedContext cpuContext;
    private List<Drive> drives;
    private Port1 port1;
    private Port2 port2;
    private Port3 port3;
    private int currentDrive;
    private DiskFrame gui;
    private boolean noGUI = false;

    public DiskImpl(Long pluginID) {
        super(pluginID);
        drives = new ArrayList<Drive>();
        for (int i = 0; i < DRIVES_COUNT; i++) {
            drives.add(new Drive());
        }

        this.currentDrive = 0xFF;
        port1CPU = CPU_PORT1;
        port2CPU = CPU_PORT2;
        port3CPU = CPU_PORT3;

        port1 = new Port1(this);
        port2 = new Port2(this);
        port3 = new Port3(this);
    }

    /**
     * Asks the user for new port number and tries to attach given port to this
     * port number on CPU.
     *
     * If a port of the DISK cannot be attached to the CPU, we want to ask the
     * user to provide another port number. He got only one chance.
     *
     * @param DISKport Port number in 88-DISK (1,2,3)
     * @param defaultPort Default port number on CPU
     * @param port The 88-DISK port object that needs to be attached
     * @return new port number if the attachement was successful, -1 otherwise
     */
    private int providePort(int DISKport, int defaultPort, DeviceContext port) {
        String providedPort = StaticDialogs.inputStringValue("Port "
                + String.valueOf(DISKport) + " can not be attached to default"
                + " CPU port, please enter another one: ", "88-DISK",
                String.valueOf(defaultPort));
        int portNumber;
        try {
            portNumber = Integer.decode(providedPort);
            if (cpuContext.attachDevice(port, portNumber) == false) {
                StaticDialogs.showErrorMessage("Error: the device still can't be attached");
                return -1;
            }
        } catch (NumberFormatException e) {
            StaticDialogs.showMessage("Bad number");
            return -1;
        }
        return portNumber;
    }

    @Override
    public boolean initialize(SettingsManager settings) {
        super.initialize(settings);

        try {
            cpuContext = (ExtendedContext) ContextPool.getInstance()
                    .getCPUContext(pluginID, ExtendedContext.class);
        } catch (InvalidContextException e) {
            // Error will be reported later on
        }

        if (cpuContext == null) {
            StaticDialogs.showErrorMessage("Cannot connect to the CPU", "88-DISK");
            return false;
        }

        readSettings();

        // attach device to CPU
        if (cpuContext.attachDevice(port1, port1CPU) == false) {
            port1CPU = providePort(1, port1CPU, port1);
            if (port1CPU == -1) {
                StaticDialogs.showErrorMessage("88-DISK (port1) can not be "
                        + "attached to default CPU port");
                return false;
            }
        }
        if (cpuContext.attachDevice(port2, port2CPU) == false) {
            port2CPU = providePort(2, port2CPU, port2);
            if (port2CPU == -1) {
                StaticDialogs.showErrorMessage("88-DISK (port2) can not be "
                        + "attached to default CPU port");
                return false;
            }
        }
        if (cpuContext.attachDevice(port3, port3CPU) == false) {
            port3CPU = providePort(3, port3CPU, port3);
            if (port3CPU == -1) {
                StaticDialogs.showErrorMessage("88-DISK (port3) can not be "
                        + "attached to default CPU port");
                return false;
            }
        }
        return true;
    }

    private void readSettings() {
        String s;
        s = settings.readSetting(pluginID, "nogui");
        if (s != null && s.toUpperCase().equals("TRUE")) {
            noGUI = true;
        } else {
            noGUI = false;
        }
        s = settings.readSetting(pluginID, "port1CPU");
        if (s != null) {
            try {
                port1CPU = Integer.decode(s);
            } catch (NumberFormatException e) {
                port1CPU = CPU_PORT1;
            }
        }
        s = settings.readSetting(pluginID, "port2CPU");
        if (s != null) {
            try {
                port2CPU = Integer.decode(s);
            } catch (NumberFormatException e) {
                port2CPU = CPU_PORT2;
            }
        }
        s = settings.readSetting(pluginID, "port3CPU");
        if (s != null) {
            try {
                port3CPU = Integer.decode(s);
            } catch (NumberFormatException e) {
                port3CPU = CPU_PORT3;
            }
        }
        for (int i = 0; i < 16; i++) {
            s = settings.readSetting(pluginID, "image" + i);
            if (s != null) {
                try {
                    drives.get(i).mount(s);
                } catch (IOException ex) {
                    StaticDialogs.showErrorMessage(ex.getMessage());
                }
            }
        }

        if (noGUI) {
            return;
        }

        gui = new DiskFrame(drives);
        s = settings.readSetting(pluginID, "always_on_top");
        if (s != null && s.toUpperCase().equals("TRUE")) {
            gui.setAlwaysOnTop(true);
        } else {
            gui.setAlwaysOnTop(false);
        }
    }

    @Override
    public void showGUI() {
        if (gui != null) {
            gui.setVisible(true);
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.devices.mits88disk.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public void destroy() {
        if (gui != null) {
            gui.dispose();
        }
        if (cpuContext != null) {
            cpuContext.detachDevice(0x8);
            cpuContext.detachDevice(0x9);
            cpuContext.detachDevice(0xA);
        }
        drives.clear();
    }

    @Override
    public void showSettings() {
        if (noGUI) {
            return;
        }
        new ConfigDialog(pluginID, settings, drives, gui).setVisible(true);
    }
    
    public Drive getCurrentDrive() {
        return drives.get(currentDrive);
    }
    
    public void setCurrentDrive(int index) {
        currentDrive = index;
    }

    @Override
    public boolean isShowSettingsSupported() {
        return !noGUI;
    }
    private static boolean ARG_LIST = false;
    private static String IMAGE_FILE = null;
    private static boolean ARG_HELP = false;
    private static boolean ARG_INFO = false;
    private static boolean ARG_VERSION = false;

    /**
     * This method parsers the command line parameters. It sets internal class
     * data members accordingly.
     *
     * @param args The command line arguments
     */
    private static void parseCommandLine(String[] args) {
        // process arguments
        int size = args.length;
        for (int i = 0; i < size; i++) {
            String arg = args[i].toUpperCase();
            try {
                if (arg.equals("--LIST")) {
                    // list files in the image
                    ARG_LIST = true;
                } else if (arg.equals("--IMAGE")) {
                    i++;
                    // the image file
                    if (IMAGE_FILE != null) {
                        System.out.println("Image file already defined,"
                                + " ignoring this one: " + args[i]);
                    } else {
                        IMAGE_FILE = args[i];
                        System.out.println("Image file name: " + IMAGE_FILE);
                    }
                } else if (arg.equals("--VERSION")) {
                    ARG_VERSION = true;
                } else if (arg.equals("--HELP")) {
                    ARG_HELP = true;
                } else if (arg.equals("--INFO")) {
                    ARG_INFO = true;
                } else {
                    System.out.println("Error: Invalid command line argument "
                            + "(" + arg + ")!");
                }
            } catch (ArrayIndexOutOfBoundsException e) {
            }
        }
    }

    /**
     * The plug-in is able to transfer files from/to CP/M images by command line
     *
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("MITS 88-DISK emuStudio plug-in");
        parseCommandLine(args);

        if (ARG_HELP) {
            // only show help and EXIT (ignore other arguments)
            System.out.println("\nThe 88-DISK will accept the following command line"
                    + " parameters:\n"
                    + "\n--list        : list all files in the image"
                    + "\n--info        : return some drive information"
                    + "\n--image name  : use the image file given by the file name"
                    + "\n--version     : print version"
                    + "\n--help        : output this message");
            return;
        }
        
        if (ARG_VERSION) {
            System.out.println(new DiskImpl(0L).getVersion());
            return;
        }

        if (IMAGE_FILE == null) {
            System.out.println("Error: Image file cannot be null!");
            System.exit(0);
            return;
        }

        CPMFS cpmfs = new CPMFS(IMAGE_FILE);

        if (ARG_INFO) {
            System.out.println(cpmfs.getInfo());
        }
        
        if (ARG_LIST) {
            System.out.println(cpmfs.getFiles());
        }
    }
}
