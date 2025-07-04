package com.vroute;

import com.vroute.ui.SimulationApp;

import javax.swing.*;

/**
 * Main entry point for the simulation visualization application
 */
public class SimulationLauncher {
    
    public static void main(String[] args) {
        // Launch the simulation application
        SwingUtilities.invokeLater(() -> {
            try {
                // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                // Use default look and feel
            }
            
            SimulationApp app = new SimulationApp();
            app.setVisible(true);
        });
    }
}
