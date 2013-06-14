/*
 * TerminalSettings.java
 *
 * Copyright (C) 2013 Peter Jakubčo
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
package net.sf.emustudio.devices.adm3a.impl;

import emulib.emustudio.SettingsManager;
import emulib.runtime.LoggerFactory;
import emulib.runtime.interfaces.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TerminalSettings {
    private final static Logger LOGGER = LoggerFactory.getLogger(TerminalSettings.class);

    private final static String DEFAULT_INPUT_FILE_NAME = "terminalADM-3A.in";
    private final static String DEFAULT_OUTPUT_FILE_NAME = "terminalADM-3A.out";
    private final static String NO_GUI = "nogui";
    private final static String AUTO = "auto";
    private final static String ANTI_ALIASING = "antiAliasing";
    private final static String HALF_DUPLEX = "halfDuplex";
    private final static String ALWAYS_ON_TOP = "alwaysOnTop";
    private final static String INPUT_FILE_NAME = "inputFileName";
    private final static String OUTPUT_FILE_NAME = "outputFileName";
    private final static String INPUT_READ_DELAY = "inputReadDelay";
    private final static String NOGUI_MODE = "noGUIMode";

    private long pluginID;
    private SettingsManager settingsManager;

    private boolean emuStudioNoGUI = false;
    private boolean emuStudioAuto = false;
    private boolean noGUIMode = false;
    private boolean halfDuplex = false;
    private boolean antiAliasing = true;
    private boolean alwaysOnTop = false;
    private String inputFileName = DEFAULT_INPUT_FILE_NAME;
    private String outputFileName = DEFAULT_OUTPUT_FILE_NAME;
    private int inputReadDelay = 0;

    private List<ChangedObserver> observers = new ArrayList<ChangedObserver>();
    private ReadWriteLock settingsLock = new ReentrantReadWriteLock();

    public interface ChangedObserver {
        void settingsChanged();
    }

    public TerminalSettings(long pluginID) {
        this.pluginID = pluginID;
    }

    public void addChangedObserver(ChangedObserver observer) {
        observers.add(observer);
    }

    public void removeChangedObserver(ChangedObserver observer) {
        observers.remove(observer);
    }

    public void setSettingsManager(SettingsManager settingsManager) {
        settingsLock.writeLock().lock();
        try {
            this.settingsManager = settingsManager;
        } finally {
            settingsLock.writeLock().unlock();
        }
    }

    private void notifyObservers() {
        for (ChangedObserver observer : observers) {
            observer.settingsChanged();
        }
    }

    public boolean isNoGUI() {
        settingsLock.readLock().lock();
        try {
            return emuStudioNoGUI;
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public void setNoGUI(boolean noGUI) {
        settingsLock.writeLock().lock();
        try {
            this.emuStudioNoGUI = noGUI;
        } finally {
            settingsLock.writeLock().unlock();
        }
        notifyObservers();
    }

    public boolean isNoGUIMode() {
        settingsLock.readLock().lock();
        try {
            return noGUIMode;
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public void setNoGUIMode(boolean noGUIMode) {
        settingsLock.writeLock().lock();
        try {
            this.noGUIMode = noGUIMode;
        } finally {
            settingsLock.writeLock().unlock();
        }
        notifyObservers();
    }

    public int getInputReadDelay() {
        settingsLock.readLock().lock();
        try {
            return inputReadDelay;
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public void setInputReadDelay(int inputReadDelay) {
        settingsLock.writeLock().lock();
        try {
            this.inputReadDelay = inputReadDelay;
        } finally {
            settingsLock.writeLock().unlock();
        }
        notifyObservers();
    }

    public boolean isAuto() {
        settingsLock.readLock().lock();
        try {
            return emuStudioAuto;
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public void setAuto(boolean auto) {
        settingsLock.writeLock().lock();
        try {
            this.emuStudioAuto = auto;
        } finally {
            settingsLock.writeLock().unlock();
        }
        notifyObservers();
    }

    public boolean isHalfDuplex() {
        settingsLock.readLock().lock();
        try {
            return halfDuplex;
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public void setHalfDuplex(boolean halfDuplex) {
        settingsLock.writeLock().lock();
        try {
            this.halfDuplex = halfDuplex;
        } finally {
            settingsLock.writeLock().unlock();
        }
        notifyObservers();
    }

    public String getInputFileName() {
        settingsLock.readLock().lock();
        try {
            return inputFileName;
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public void setInputFileName(String inputFileName) {
        settingsLock.writeLock().lock();
        try {
            this.inputFileName = inputFileName;
        } finally {
            settingsLock.writeLock().unlock();
        }
        notifyObservers();
    }

    public String getOutputFileName() {
        settingsLock.readLock().lock();
        try {
            return outputFileName;
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public void setOutputFileName(String outputFileName) {
        settingsLock.writeLock().lock();
        try {
            this.outputFileName = outputFileName;
        } finally {
            settingsLock.writeLock().unlock();
        }
        notifyObservers();
    }

    public boolean isAntiAliasing() {
        settingsLock.readLock().lock();
        try {
            return antiAliasing;
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public void setAntiAliasing(boolean antiAliasing) {
        settingsLock.writeLock().lock();
        try {
            this.antiAliasing = antiAliasing;
        } finally {
            settingsLock.writeLock().unlock();
        }
        notifyObservers();
    }

    public boolean isAlwaysOnTop() {
        settingsLock.readLock().lock();
        try {
            return alwaysOnTop;
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        settingsLock.writeLock().lock();
        try {
            this.alwaysOnTop = alwaysOnTop;
        } finally {
            settingsLock.writeLock().unlock();
        }
        notifyObservers();
    }

    public void write() {
        settingsLock.readLock().lock();
        try {
            if (settingsManager == null) {
                return;
            }
            settingsManager.writeSetting(pluginID, NO_GUI, String.valueOf(emuStudioNoGUI));
            settingsManager.writeSetting(pluginID, AUTO, String.valueOf(emuStudioAuto));
            settingsManager.writeSetting(pluginID, HALF_DUPLEX, String.valueOf(halfDuplex));
            settingsManager.writeSetting(pluginID, ALWAYS_ON_TOP, String.valueOf(alwaysOnTop));
            settingsManager.writeSetting(pluginID, ANTI_ALIASING, String.valueOf(antiAliasing));
            settingsManager.writeSetting(pluginID, INPUT_FILE_NAME, inputFileName);
            settingsManager.writeSetting(pluginID, OUTPUT_FILE_NAME, outputFileName);
            settingsManager.writeSetting(pluginID, INPUT_READ_DELAY, String.valueOf(inputReadDelay));
            settingsManager.writeSetting(pluginID, NOGUI_MODE, String.valueOf(noGUIMode));
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public void read() {
        settingsLock.writeLock().lock();
        try {
            if (settingsManager == null) {
                return;
            }
            emuStudioNoGUI = Boolean.parseBoolean(settingsManager.readSetting(pluginID, NO_GUI));
            emuStudioAuto = Boolean.parseBoolean(settingsManager.readSetting(pluginID, AUTO));
            noGUIMode = Boolean.parseBoolean(settingsManager.readSetting(pluginID, NOGUI_MODE));
            halfDuplex = Boolean.parseBoolean(settingsManager.readSetting(pluginID, HALF_DUPLEX));
            alwaysOnTop = Boolean.parseBoolean(settingsManager.readSetting(pluginID, ALWAYS_ON_TOP));
            antiAliasing = Boolean.parseBoolean(settingsManager.readSetting(pluginID, ANTI_ALIASING));
            inputFileName = settingsManager.readSetting(pluginID, INPUT_FILE_NAME);
            if (inputFileName == null) {
                inputFileName = DEFAULT_INPUT_FILE_NAME;
            }
            outputFileName = settingsManager.readSetting(pluginID, OUTPUT_FILE_NAME);
            if (outputFileName == null) {
                outputFileName = DEFAULT_OUTPUT_FILE_NAME;
            }
            String tmp = settingsManager.readSetting(pluginID, INPUT_READ_DELAY);
            if (tmp != null) {
                try {
                    inputReadDelay = Integer.decode(tmp);
                } catch (NumberFormatException e) {
                    LOGGER.error("Could not read setting: input read delay for terminal", e);
                }
            }
        } finally {
            settingsLock.writeLock().unlock();
        }
        notifyObservers();
    }


}
