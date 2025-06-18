package com.vroute.ui.components;

import com.vroute.models.*;
import com.vroute.ui.utils.EstilosUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

/**
 * Panel para mostrar detalles del elemento seleccionado en el mapa
 */
public class InfoPanel extends JPanel {
    private JLabel tituloLabel;
    private JTextArea detallesArea;
    
    /**
     * Constructor del panel de información
     */
    public InfoPanel() {
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(),
            new EmptyBorder(5, 5, 5, 5)
        ));
        
        setLayout(new BorderLayout());
        
        // Título
        tituloLabel = new JLabel("Información");
        tituloLabel.setFont(EstilosUI.FUENTE_TITULO);
        add(tituloLabel, BorderLayout.NORTH);
        
        // Área de detalles
        detallesArea = new JTextArea(10, 20);
        detallesArea.setEditable(false);
        detallesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(detallesArea);
        add(scrollPane, BorderLayout.CENTER);
        
        // Tamaño preferido
        setPreferredSize(new Dimension(300, 200));
        
        limpiar();
    }
    
    /**
     * Limpia la información mostrada
     */
    public void limpiar() {
        tituloLabel.setText("Información");
        detallesArea.setText("Seleccione un elemento en el mapa para ver sus detalles.");
        repaint();
    }
    
    /**
     * Muestra la información de un vehículo
     * @param vehiculo El vehículo a mostrar
     */
    public void mostrarVehiculo(Vehiculo vehiculo) {
        tituloLabel.setText("Vehículo: " + vehiculo.getId());
        
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(vehiculo.getId()).append("\n");
        sb.append("Tipo: ").append(vehiculo.getTipo().getNombre()).append("\n");
        sb.append("Estado: ").append(vehiculo.getEstadoOperativo()).append("\n");
        sb.append("Posición: (").append(vehiculo.getPosicionActual().getX())
          .append(", ").append(vehiculo.getPosicionActual().getY()).append(")\n\n");
          
        sb.append("Carga GLP: ").append(vehiculo.getCargaGLPActual())
          .append("/").append(vehiculo.getTipo().getCapacidadGLP()).append(" m³\n");
          
        sb.append("Combustible: ").append(String.format("%.2f", vehiculo.getNivelCombustibleActual()))
          .append("/").append(String.format("%.2f", vehiculo.getTipo().getCapacidadCombustible()))
          .append(" galones");
          
        detallesArea.setText(sb.toString());
        repaint();
    }
    
    /**
     * Muestra la información de un pedido
     * @param pedido El pedido a mostrar
     */
    public void mostrarPedido(Pedido pedido) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        tituloLabel.setText("Pedido: " + pedido.getId());
        
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(pedido.getId()).append("\n");
        sb.append("Estado: ").append(pedido.getEstado()).append("\n");
        sb.append("Ubicación: (").append(pedido.getUbicacion().getX())
          .append(", ").append(pedido.getUbicacion().getY()).append(")\n\n");
          
        sb.append("GLP Solicitado: ").append(pedido.getCantidadTotalGLP()).append(" m³\n");
        sb.append("GLP Entregado: ").append(pedido.getCantidadSatisfechaGLP()).append(" m³\n\n");
        
        sb.append("Recepción: ").append(pedido.getHoraRecepcion().format(formatter)).append("\n");
        sb.append("Límite: ").append(pedido.getHoraLimite().format(formatter));
        
        detallesArea.setText(sb.toString());
        repaint();
    }
    
    /**
     * Muestra la información de un depósito
     * @param deposito El depósito a mostrar
     */
    public void mostrarDeposito(Deposito deposito) {
        tituloLabel.setText("Depósito: " + deposito.getId());
        
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(deposito.getId()).append("\n");
        sb.append("Tipo: ").append(deposito.esPlantaPrincipal() ? "Planta Principal" : "Depósito Secundario").append("\n");
        sb.append("Ubicación: (").append(deposito.getUbicacion().getX())
          .append(", ").append(deposito.getUbicacion().getY()).append(")\n\n");
          
        sb.append("Capacidad GLP: ");
        if (deposito.esPlantaPrincipal()) {
            sb.append("Ilimitada");
        } else {
            sb.append(deposito.getCapacidadGLP()).append(" m³");
        }
        
        detallesArea.setText(sb.toString());
        repaint();
    }
    
    /**
     * Muestra la información de un bloqueo
     * @param bloqueo El bloqueo a mostrar
     */
    public void mostrarBloqueo(Bloqueo bloqueo) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        tituloLabel.setText("Bloqueo");
        
        StringBuilder sb = new StringBuilder();
        sb.append("Inicio: ").append(bloqueo.getHoraInicio().format(formatter)).append("\n");
        sb.append("Fin: ").append(bloqueo.getHoraFin().format(formatter)).append("\n\n");
        
        sb.append("Tramos:\n");
        if (bloqueo.getTramos() != null) {
            for (int i = 0; i < bloqueo.getTramos().size() - 1; i++) {
                Posicion inicio = bloqueo.getTramos().get(i);
                Posicion fin = bloqueo.getTramos().get(i + 1);
                
                sb.append("- (").append(inicio.getX()).append(",").append(inicio.getY())
                  .append(") a (").append(fin.getX()).append(",").append(fin.getY())
                  .append(")\n");
            }
        }
        
        detallesArea.setText(sb.toString());
        repaint();
    }
}
