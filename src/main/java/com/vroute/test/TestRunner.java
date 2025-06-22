package com.vroute.test;

/**
 * Clase principal para ejecutar las pruebas utilizando el mini framework de testing
 */
public class TestRunner {
    
    public static void main(String[] args) {
        System.out.println("=== INICIANDO FRAMEWORK DE TESTING V-ROUTE ===");
        
        // Activar modo debug del evaluador para tests
        com.vroute.solution.Evaluator.setDebugMode(true);
        
        // Crear el runner de tests
        TestFramework.TestRunner runner = new TestFramework.TestRunner();
        
        // Registrar suites de tests
        runner.addSuite(EvaluatorTestSuite.createSuite());
        runner.addSuite(SIHSolverTestSuite.createSuite());
        
        // Ejecutar todos los tests y obtener el reporte
        TestFramework.TestReport report = runner.runAll();
        
        // Imprimir resultados
        report.printSummary();
        
        // Si se solicita reporte detallado (a través de args)
        if (args.length > 0 && args[0].equals("--detailed")) {
            report.printDetailedReport();
        }
        
        System.out.println("\n=== TESTS FINALIZADOS ===");
        
        // Si hubo fallos, terminar con código de error
        if (report.getFailedTests() > 0) {
            System.exit(1);
        }
    }
} 