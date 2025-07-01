package com.vroute.test;

/**
 * Clase principal para ejecutar las pruebas utilizando el mini framework de
 * testing
 */
public class TestRunner {

    public static void main(String[] args) {
        System.out.println("=== INICIANDO FRAMEWORK DE TESTING V-ROUTE ===");

        // Crear el runner de tests
        TestFramework.TestRunner runner = new TestFramework.TestRunner();

        // Registrar suites de tests
        //runner.addSuite(RandomDistributorTest.createSuite());
        //runner.addSuite(RouteFixerTest.createSuite());
        //runner.addSuite(EvaluatorTest.createSuite());
        runner.addSuite(CompleteTest.createSuite());

        // Ejecutar todos los tests y obtener el reporte
        TestFramework.TestReport report = runner.runAll();

        // Imprimir resultados
        report.printDetailedReport();

        System.out.println("\n=== TESTS FINALIZADOS ===");
        // Si hubo fallos, terminar con código de error
        if (report.getFailedTests() > 0) {
            System.exit(1);
        }
    }
}