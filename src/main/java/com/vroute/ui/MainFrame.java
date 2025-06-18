package com.vroute.ui;

import com.vroute.models.*;
import com.vroute.ui.map.MapPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ventana principal de la aplicación de visualización
 */
public class MainFrame extends JFrame {
    private MapPanel mapPanel;
    private JLabel statusLabel;
    private JLabel timeLabel; // Etiqueta para mostrar el tiempo de simulación de forma prominente
    private Entorno entorno;
    private Timer actualizacionTimer;
    
    /**
     * Constructor de la ventana principal
     */
    public MainFrame() {
        super("V-Route - Visualizador Logístico");
        initComponents();
        
        // Crear entorno inicial de demo
        crearEntornoDemo();
        
        // Configurar timer para actualizaciones periódicas
        actualizacionTimer = new Timer(1000, (ActionEvent e) -> {
            actualizarStatus();
        });
        actualizacionTimer.start();
    }
    
    /**
     * Inicializa los componentes de la interfaz
     */
    private void initComponents() {
        // Configurar ventana
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLayout(new BorderLayout());
        
        // Crear panel central del mapa
        mapPanel = new MapPanel();
        add(mapPanel, BorderLayout.CENTER);
        
        // Crear panel de controles en la parte superior
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton btnZoomIn = new JButton("Zoom +");
        btnZoomIn.addActionListener(e -> zoomIn());
        
        JButton btnZoomOut = new JButton("Zoom -");
        btnZoomOut.addActionListener(e -> zoomOut());
        
        JButton btnFit = new JButton("Ajustar");
        btnFit.addActionListener(e -> mapPanel.fitToMap());
        
        JButton btnToggleGrid = new JButton("Grid");
        btnToggleGrid.addActionListener(e -> mapPanel.toggleGrid());
        
        JButton btnGenerarDemo = new JButton("Generar Demo");
        btnGenerarDemo.addActionListener(e -> generarRutasDemo());
        
        JButton btnLimpiar = new JButton("Limpiar");
        btnLimpiar.addActionListener(e -> limpiarRutas());
        
        toolBar.add(btnZoomIn);
        toolBar.add(btnZoomOut);
        toolBar.add(btnFit);
        toolBar.addSeparator();
        toolBar.add(btnToggleGrid);
        toolBar.addSeparator();
        toolBar.add(btnGenerarDemo);
        toolBar.add(btnLimpiar);
        
        // Añadir controles de tiempo de simulación
        toolBar.addSeparator(new Dimension(20, 10)); // Separador más ancho
        
        JButton btnAvanzar1h = new JButton("+1h");
        btnAvanzar1h.addActionListener(e -> avanzarTiempo(1));
        btnAvanzar1h.setToolTipText("Avanzar 1 hora");
        
        JButton btnAvanzar6h = new JButton("+6h");
        btnAvanzar6h.addActionListener(e -> avanzarTiempo(6));
        btnAvanzar6h.setToolTipText("Avanzar 6 horas");
        
        JButton btnAvanzar24h = new JButton("+1d");
        btnAvanzar24h.addActionListener(e -> avanzarTiempo(24));
        btnAvanzar24h.setToolTipText("Avanzar 1 día");
        
        // Panel para los botones de tiempo con borde y color de fondo
        JPanel timeControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        timeControlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180)),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        timeControlPanel.setBackground(new Color(240, 240, 240));
        
        timeControlPanel.add(new JLabel("Avanzar tiempo: "));
        timeControlPanel.add(btnAvanzar1h);
        timeControlPanel.add(btnAvanzar6h);
        timeControlPanel.add(btnAvanzar24h);
        
        toolBar.add(timeControlPanel);
        
        // Añadir una etiqueta de tiempo destacada
        toolBar.addSeparator(new Dimension(10, 10));
        timeLabel = new JLabel("FECHA SIMULACIÓN: --/--/---- --:--:--");
        timeLabel.setFont(new Font(timeLabel.getFont().getName(), Font.BOLD, 14));
        timeLabel.setForeground(new Color(0, 102, 204)); // Azul destacado
        timeLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        toolBar.add(timeLabel);
        
        add(toolBar, BorderLayout.NORTH);
        
        // Crear panel de estado en la parte inferior
        statusLabel = new JLabel("Listo");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        add(statusLabel, BorderLayout.SOUTH);
        
        // Hacer visible la ventana
        setVisible(true);
    }
    
    /**
     * Aumentar el zoom del mapa
     */
    private void zoomIn() {
        // Zoom in (este método es simplificado, en realidad el zoom lo maneja MapPanel directamente)
        // En una implementación más avanzada, podríamos usar MouseWheelEvent para simular el zoom
        mapPanel.repaint();
    }
    
    /**
     * Disminuir el zoom del mapa
     */
    private void zoomOut() {
        // Zoom out (similar al zoom in, simplificado)
        mapPanel.repaint();
    }
    
    /**
     * Crea un entorno de prueba con vehículos, depósitos, etc.
     */
    private void crearEntornoDemo() {
        // Fecha y hora de simulación - usando la fecha actual del sistema como base
        LocalDateTime horaActual = LocalDateTime.now(); // Esto podría venir de una configuración global
        
        // Crear depósitos
        List<Deposito> depositos = new ArrayList<>();
        depositos.add(new Deposito("Planta Principal", new Posicion(12, 8), true));
        depositos.add(new Deposito("Depósito Norte", new Posicion(42, 42), false));
        depositos.add(new Deposito("Depósito Este", new Posicion(63, 3), false));
        
        // Crear vehículos
        List<Vehiculo> vehiculos = new ArrayList<>();
        
        // Vehículos de diferentes tipos en diferentes estados
        vehiculos.add(new Vehiculo("TA01", TipoVehiculo.TA, new Posicion(12, 8), 25, 25.0, Vehiculo.EstadoOperativo.DISPONIBLE));
        vehiculos.add(new Vehiculo("TA02", TipoVehiculo.TA, new Posicion(20, 15), 18, 20.0, Vehiculo.EstadoOperativo.EN_RUTA));
        vehiculos.add(new Vehiculo("TB01", TipoVehiculo.TB, new Posicion(30, 25), 10, 15.0, Vehiculo.EstadoOperativo.EN_RUTA));
        vehiculos.add(new Vehiculo("TB02", TipoVehiculo.TB, new Posicion(12, 8), 15, 25.0, Vehiculo.EstadoOperativo.DISPONIBLE));
        vehiculos.add(new Vehiculo("TC01", TipoVehiculo.TC, new Posicion(42, 42), 5, 10.0, Vehiculo.EstadoOperativo.EN_MANTENIMIENTO));
        vehiculos.add(new Vehiculo("TD01", TipoVehiculo.TD, new Posicion(35, 18), 2, 5.0, Vehiculo.EstadoOperativo.AVERIADO));
        
        // Crear pedidos
        List<Pedido> pedidos = new ArrayList<>();
        
        // Pedidos en diferentes estados
        pedidos.add(new Pedido("P001", new Posicion(25, 30), 10,
                horaActual.minusHours(2), horaActual.plusHours(6), 0, Pedido.Estado.PENDIENTE));
        pedidos.add(new Pedido("P002", new Posicion(40, 15), 15,
                horaActual.minusHours(3), horaActual.plusHours(1), 0, Pedido.Estado.EN_RUTA));
        pedidos.add(new Pedido("P003", new Posicion(55, 25), 20,
                horaActual.minusHours(5), horaActual.plusHours(3), 10, Pedido.Estado.PARCIALMENTE_COMPLETADO));
        pedidos.add(new Pedido("P004", new Posicion(18, 40), 5,
                horaActual.minusHours(8), horaActual.minusHours(1), 5, Pedido.Estado.COMPLETADO));
        
        // Crear bloqueos
        List<Bloqueo> bloqueos = new ArrayList<>();
        
        // Bloqueo activo
        List<Posicion> tramosActivos = new ArrayList<>();
        tramosActivos.add(new Posicion(30, 30));
        tramosActivos.add(new Posicion(35, 30));
        tramosActivos.add(new Posicion(35, 35));
        bloqueos.add(new Bloqueo(horaActual.minusHours(1), horaActual.plusHours(5), tramosActivos));
        
        // Bloqueo futuro
        List<Posicion> tramosFuturos = new ArrayList<>();
        tramosFuturos.add(new Posicion(50, 20));
        tramosFuturos.add(new Posicion(50, 25));
        tramosFuturos.add(new Posicion(55, 25));
        bloqueos.add(new Bloqueo(horaActual.plusHours(2), horaActual.plusHours(24), tramosFuturos));
        
        // Construir el entorno
        entorno = new Entorno(horaActual, vehiculos, pedidos, depositos, bloqueos, null);
        
        // Actualizar el mapa
        mapPanel.setEntorno(entorno);
    }
    
    /**
     * Genera rutas de demostración
     */
    private void generarRutasDemo() {
        // Limpiar rutas existentes
        mapPanel.limpiarRutas();
        
        // Generar rutas aleatorias para demo
        Random random = new Random();
        
        // Ruta 1: Planta principal a un pedido
        if (entorno != null && entorno.getDepositos() != null && entorno.getDepositos().size() > 0 &&
            entorno.getPedidos() != null && entorno.getPedidos().size() > 0) {
            
            List<Posicion> ruta1 = new ArrayList<>();
            
            // Partir de la planta principal
            Posicion inicio = entorno.getDepositos().get(0).getUbicacion();
            ruta1.add(inicio);
            
            // Generar algunos puntos intermedios
            Posicion destino = entorno.getPedidos().get(0).getUbicacion();
            
            // Ruta en zigzag
            int x = inicio.getX();
            int y = inicio.getY();
            
            // Movimiento horizontal
            while (x < destino.getX()) {
                x += random.nextInt(3) + 1;
                ruta1.add(new Posicion(x, y));
            }
            
            // Movimiento vertical
            while (y < destino.getY()) {
                y += random.nextInt(3) + 1;
                ruta1.add(new Posicion(x, y));
            }
            
            // Añadir punto final
            ruta1.add(destino);
            
            // Agregar la ruta al mapa
            mapPanel.agregarRuta("Ruta1", ruta1);
        }
        
        // Ruta 2: Entre depósitos
        if (entorno != null && entorno.getDepositos() != null && entorno.getDepositos().size() > 1) {
            List<Posicion> ruta2 = new ArrayList<>();
            
            // Del depósito 1 al 2
            Posicion inicio = entorno.getDepositos().get(1).getUbicacion();
            Posicion destino = entorno.getDepositos().get(2).getUbicacion();
            
            ruta2.add(inicio);
            
            // Generar ruta diagonal y luego recta
            int x = inicio.getX();
            int y = inicio.getY();
            
            // Movimiento diagonal
            while (x > destino.getX() + 10 && y > destino.getY() + 10) {
                x -= random.nextInt(3) + 1;
                y -= random.nextInt(3) + 1;
                ruta2.add(new Posicion(x, y));
            }
            
            // Movimiento horizontal
            while (x > destino.getX()) {
                x -= random.nextInt(3) + 1;
                ruta2.add(new Posicion(x, y));
            }
            
            // Movimiento vertical
            while (y > destino.getY()) {
                y -= 1;
                ruta2.add(new Posicion(x, y));
            }
            
            // Añadir punto final
            ruta2.add(destino);
            
            // Agregar la ruta al mapa
            mapPanel.agregarRuta("Ruta2", ruta2);
        }
        
        // Actualizar estado
        statusLabel.setText("Rutas de demostración generadas");
    }
    
    /**
     * Limpia todas las rutas del mapa
     */
    private void limpiarRutas() {
        mapPanel.limpiarRutas();
        statusLabel.setText("Rutas eliminadas");
    }
    
    /**
     * Actualiza la información de estado en la barra inferior
     */
    private void actualizarStatus() {
        if (entorno != null) {
            // Formatear la fecha y hora para la etiqueta principal
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            
            // Actualizar la etiqueta principal de tiempo con un formato más destacado
            timeLabel.setText("FECHA SIMULACIÓN: " + entorno.getHoraActual().format(timeFormatter));
            
            int vehiculosDisponibles = 0;
            if (entorno.getVehiculos() != null) {
                for (Vehiculo v : entorno.getVehiculos()) {
                    if (v.getEstadoOperativo() == Vehiculo.EstadoOperativo.DISPONIBLE) {
                        vehiculosDisponibles++;
                    }
                }
            }
            
            int pedidosPendientes = 0;
            if (entorno.getPedidos() != null) {
                for (Pedido p : entorno.getPedidos()) {
                    if (p.getEstado() == Pedido.Estado.PENDIENTE) {
                        pedidosPendientes++;
                    }
                }
            }
            
            // Obtener conteo de bloqueos por estado
            int bloqueosActivos = entorno.getBloqueosActivos().size();
            
            statusLabel.setText("Vehículos disponibles: " + vehiculosDisponibles + 
                              " | Pedidos pendientes: " + pedidosPendientes +
                              " | Bloqueos activos: " + bloqueosActivos);
        } else {
            statusLabel.setText("No hay datos de simulación disponibles");
        }
    }
    
    /**
     * Avanza la hora de simulación por una cantidad específica de tiempo
     * @param horas Número de horas a avanzar
     */
    private void avanzarTiempo(int horas) {
        if (entorno != null) {
            // Crear un nuevo entorno con la hora avanzada
            LocalDateTime nuevaHora = entorno.getHoraActual().plusHours(horas);
            
            // Recrear el entorno con la nueva hora
            Entorno nuevoEntorno = new Entorno(
                nuevaHora,
                entorno.getVehiculos(),
                entorno.getPedidos(),
                entorno.getDepositos(),
                entorno.getBloqueos(),
                entorno.getMantenimientos()
            );
            
            // Actualizar el entorno
            entorno = nuevoEntorno;
            mapPanel.setEntorno(entorno);
            
            // Actualizar la interfaz
            actualizarStatus();
        }
    }
    
    /**
     * Punto de entrada para ejecutar la aplicación directamente
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new MainFrame();
        });
    }
}
