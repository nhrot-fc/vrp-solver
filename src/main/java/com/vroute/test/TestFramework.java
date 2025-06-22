package com.vroute.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Mini framework de testing para V-Route
 */
public class TestFramework {
    
    /**
     * Interfaz que representa un test a ejecutar
     */
    public interface Test {
        TestResult execute();
        String getName();
    }
    
    /**
     * Clase que representa el resultado de un test
     */
    public static class TestResult {
        private final String testName;
        private final boolean passed;
        private final String message;
        private final Throwable error;
        private final long executionTimeMs;
        private final Object expected;
        private final Object obtained;
        
        private TestResult(String testName, boolean passed, String message, Throwable error, long executionTimeMs, 
                          Object expected, Object obtained) {
            this.testName = testName;
            this.passed = passed;
            this.message = message;
            this.error = error;
            this.executionTimeMs = executionTimeMs;
            this.expected = expected;
            this.obtained = obtained;
        }
        
        public static TestResult success(String testName, long executionTimeMs) {
            return new TestResult(testName, true, "Test exitoso", null, executionTimeMs, null, null);
        }
        
        public static TestResult failure(String testName, String message, long executionTimeMs, Object expected, Object obtained) {
            return new TestResult(testName, false, message, null, executionTimeMs, expected, obtained);
        }
        
        public static TestResult error(String testName, Throwable error, long executionTimeMs) {
            return new TestResult(testName, false, error.getMessage(), error, executionTimeMs, null, null);
        }
        
        public boolean isPassed() {
            return passed;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Throwable getError() {
            return error;
        }
        
        public String getTestName() {
            return testName;
        }
        
        public long getExecutionTimeMs() {
            return executionTimeMs;
        }
        
        public Object getExpected() {
            return expected;
        }
        
        public Object getObtained() {
            return obtained;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("[%s] %s: %s", 
                passed ? "PASÓ" : "FALLÓ", 
                testName, 
                message));
            
            if (error != null) {
                sb.append(" - ").append(error.getClass().getSimpleName());
            }
            
            if (!passed && expected != null && obtained != null) {
                sb.append("\n    Esperado: ").append(expected);
                sb.append("\n    Obtenido: ").append(obtained);
            }
            
            return sb.toString();
        }
    }
    
    /**
     * Clase para ejecutar tests y reportar resultados
     */
    public static class TestRunner {
        private final List<Test> tests = new ArrayList<>();
        private final Map<String, TestSuite> suites = new HashMap<>();
        
        public void addTest(Test test) {
            tests.add(test);
        }
        
        public void addSuite(TestSuite suite) {
            suites.put(suite.getName(), suite);
        }
        
        /**
         * Ejecuta todos los tests y suites registrados
         * @return Reporte con los resultados de los tests
         */
        public TestReport runAll() {
            TestReport report = new TestReport();
            
            // Ejecutar tests independientes
            for (Test test : tests) {
                TestResult result;
                try {
                    result = test.execute();
                } catch (Throwable t) {
                    result = TestResult.error(test.getName(), t, 0);
                }
                report.addResult(result);
            }
            
            // Ejecutar suites
            for (TestSuite suite : suites.values()) {
                TestReport suiteReport = suite.run();
                report.mergeReport(suiteReport);
            }
            
            return report;
        }
    }
    
    /**
     * Clase para agrupar tests relacionados
     */
    public static class TestSuite {
        private final String name;
        private final List<Test> tests = new ArrayList<>();
        
        public TestSuite(String name) {
            this.name = name;
        }
        
        public void addTest(Test test) {
            tests.add(test);
        }
        
        public String getName() {
            return name;
        }
        
        /**
         * Ejecuta todos los tests en esta suite
         * @return Reporte con los resultados de la suite
         */
        public TestReport run() {
            TestReport report = new TestReport();
            report.setSuiteName(name);
            
            for (Test test : tests) {
                TestResult result;
                try {
                    result = test.execute();
                } catch (Throwable t) {
                    result = TestResult.error(test.getName(), t, 0);
                }
                report.addResult(result);
            }
            
            return report;
        }
        
        /**
         * Crea un test basado en una función lambda
         * @param name Nombre del test
         * @param testFunction Función que implementa el test
         * @return El test creado
         */
        public Test createTest(String name, Supplier<TestResult> testFunction) {
            return new Test() {
                @Override
                public TestResult execute() {
                    return testFunction.get();
                }
                
                @Override
                public String getName() {
                    return name;
                }
            };
        }
        
        /**
         * Crea un test basado en una función lambda, usando el nombre del método
         * que lo llamó como nombre del test
         * @param testFunction Función que implementa el test
         * @return El test creado
         */
        public Test createTest(Supplier<TestResult> testFunction) {
            String callerMethodName = getCallerMethodName();
            return createTest(callerMethodName, testFunction);
        }
        
        /**
         * Obtiene el nombre del método que llamó a este método
         * @return Nombre del método llamador
         */
        private String getCallerMethodName() {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace.length >= 4) {
                return stackTrace[3].getMethodName();
            }
            return "Unknown Test";
        }
    }
    
    /**
     * Clase para recopilar resultados de tests
     */
    public static class TestReport {
        private String suiteName;
        private final List<TestResult> results = new ArrayList<>();
        private final Map<String, TestReport> suiteReports = new HashMap<>();
        
        public void addResult(TestResult result) {
            results.add(result);
        }
        
        public void addSuiteReport(String suiteName, TestReport report) {
            suiteReports.put(suiteName, report);
        }
        
        public void mergeReport(TestReport other) {
            if (other.suiteName != null) {
                addSuiteReport(other.suiteName, other);
            } else {
                results.addAll(other.results);
                suiteReports.putAll(other.suiteReports);
            }
        }
        
        public void setSuiteName(String suiteName) {
            this.suiteName = suiteName;
        }
        
        public int getTotalTests() {
            int count = results.size();
            for (TestReport suiteReport : suiteReports.values()) {
                count += suiteReport.getTotalTests();
            }
            return count;
        }
        
        public int getPassedTests() {
            int count = (int) results.stream().filter(TestResult::isPassed).count();
            for (TestReport suiteReport : suiteReports.values()) {
                count += suiteReport.getPassedTests();
            }
            return count;
        }
        
        public int getFailedTests() {
            return getTotalTests() - getPassedTests();
        }
        
        /**
         * Imprime un resumen del reporte
         */
        public void printSummary() {
            int total = getTotalTests();
            int passed = getPassedTests();
            int failed = getFailedTests();
            
            System.out.println("\n=== RESUMEN DE TESTS ===");
            System.out.println("Total de tests: " + total);
            System.out.println("Tests exitosos: " + passed + " (" + (total > 0 ? passed * 100 / total : 0) + "%)");
            System.out.println("Tests fallidos: " + failed);
            
            if (failed > 0) {
                System.out.println("\nDetalle de tests fallidos:");
                printFailedTests("");
            }
        }
        
        /**
         * Imprime detalle de los tests fallidos
         */
        private void printFailedTests(String prefix) {
            for (TestResult result : results) {
                if (!result.isPassed()) {
                    System.out.println(prefix + result);
                    if (result.getError() != null) {
                        result.getError().printStackTrace(System.out);
                    }
                }
            }
            
            for (Map.Entry<String, TestReport> entry : suiteReports.entrySet()) {
                String suiteName = entry.getKey();
                TestReport suiteReport = entry.getValue();
                
                if (suiteReport.getFailedTests() > 0) {
                    System.out.println(prefix + "En suite " + suiteName + ":");
                    suiteReport.printFailedTests(prefix + "  ");
                }
            }
        }
        
        /**
         * Imprime un reporte detallado de todos los tests
         */
        public void printDetailedReport() {
            System.out.println("\n=== REPORTE DETALLADO DE TESTS ===");
            printDetailedResults("");
        }
        
        private void printDetailedResults(String prefix) {
            for (TestResult result : results) {
                System.out.println(prefix + result + " (" + result.getExecutionTimeMs() + "ms)");
            }
            
            for (Map.Entry<String, TestReport> entry : suiteReports.entrySet()) {
                String suiteName = entry.getKey();
                TestReport suiteReport = entry.getValue();
                
                System.out.println(prefix + "Suite: " + suiteName);
                suiteReport.printDetailedResults(prefix + "  ");
            }
        }
    }
    
    /**
     * Clase con métodos de aserción para tests
     */
    public static class Assertions {
        /**
         * Verifica que una condición sea verdadera
         */
        public static void assertTrue(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError("Aserción fallida: " + message, 
                                         new ComparisonError(true, condition));
            }
        }
        
        /**
         * Verifica que una condición sea falsa
         */
        public static void assertFalse(boolean condition, String message) {
            if (condition) {
                throw new AssertionError("Aserción fallida: " + message,
                                        new ComparisonError(false, condition));
            }
        }
        
        /**
         * Verifica que dos objetos sean iguales
         */
        public static void assertEquals(Object expected, Object actual, String message) {
            if (expected == actual) {
                return;
            }
            
            if (expected != null && expected.equals(actual)) {
                return;
            }
            
            throw new AssertionError("Aserción fallida: " + message +
                    ". Esperado: " + expected + ", Actual: " + actual,
                    new ComparisonError(expected, actual));
        }
        
        /**
         * Verifica que un objeto sea nulo
         */
        public static void assertNull(Object object, String message) {
            if (object != null) {
                throw new AssertionError("Aserción fallida: " + message +
                        ". El objeto debería ser null pero es: " + object,
                        new ComparisonError(null, object));
            }
        }
        
        /**
         * Verifica que un objeto no sea nulo
         */
        public static void assertNotNull(Object object, String message) {
            if (object == null) {
                throw new AssertionError("Aserción fallida: " + message +
                        ". El objeto es null.",
                        new ComparisonError("no null", null));
            }
        }
        
        /**
         * Verifica que dos números estén cerca uno del otro dentro de un delta
         */
        public static void assertNearEquals(double expected, double actual, double delta, String message) {
            if (Math.abs(expected - actual) > delta) {
                throw new AssertionError("Aserción fallida: " + message +
                        ". Esperado: " + expected + " ± " + delta + ", Actual: " + actual,
                        new ComparisonError(expected + " ± " + delta, actual));
            }
        }
        
        /**
         * Verifica que un código lance una excepción del tipo esperado
         */
        public static <T extends Throwable> void assertThrows(Class<T> expectedException, Runnable code, String message) {
            try {
                code.run();
                throw new AssertionError("Aserción fallida: " + message +
                        ". Se esperaba una excepción de tipo " + expectedException.getSimpleName() +
                        " pero no se lanzó ninguna.",
                        new ComparisonError(expectedException.getSimpleName(), "ninguna excepción"));
            } catch (Throwable t) {
                if (!expectedException.isInstance(t)) {
                    throw new AssertionError("Aserción fallida: " + message +
                            ". Se esperaba una excepción de tipo " + expectedException.getSimpleName() +
                            " pero se lanzó " + t.getClass().getSimpleName(),
                            new ComparisonError(expectedException.getSimpleName(), t.getClass().getSimpleName()));
                }
            }
        }
        
        /**
         * Clase interna para capturar errores de comparación
         */
        private static class ComparisonError extends Error {
            private final Object expected;
            private final Object actual;
            
            public ComparisonError(Object expected, Object actual) {
                this.expected = expected;
                this.actual = actual;
            }
            
            public Object getExpected() {
                return expected;
            }
            
            public Object getActual() {
                return actual;
            }
        }
    }
    
    /**
     * Clase abstracta que facilita la implementación de tests
     */
    public static abstract class AbstractTest implements Test {
        private final String name;
        
        protected AbstractTest(String name) {
            this.name = name;
        }
        
        /**
         * Constructor que usa el nombre de la clase como nombre del test
         */
        protected AbstractTest() {
            this.name = getClass().getSimpleName();
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public TestResult execute() {
            long startTime = System.currentTimeMillis();
            try {
                runTest();
                long endTime = System.currentTimeMillis();
                return TestResult.success(getName(), endTime - startTime);
            } catch (AssertionError e) {
                long endTime = System.currentTimeMillis();
                
                Object expected = null;
                Object obtained = null;
                
                if (e.getCause() instanceof Assertions.ComparisonError) {
                    Assertions.ComparisonError compError = (Assertions.ComparisonError) e.getCause();
                    expected = compError.getExpected();
                    obtained = compError.getActual();
                }
                
                return TestResult.failure(getName(), e.getMessage(), endTime - startTime, expected, obtained);
            } catch (Throwable t) {
                long endTime = System.currentTimeMillis();
                return TestResult.error(getName(), t, endTime - startTime);
            }
        }
        
        /**
         * Implementación del test
         * @throws Throwable si ocurre algún error durante la ejecución del test
         */
        protected abstract void runTest() throws Throwable;
    }
} 