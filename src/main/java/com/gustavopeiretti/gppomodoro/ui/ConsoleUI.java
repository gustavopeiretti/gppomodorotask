package com.gustavopeiretti.gppomodoro.ui;

import com.gustavopeiretti.gppomodoro.model.Category;
import com.gustavopeiretti.gppomodoro.model.PomodoroState;
import com.gustavopeiretti.gppomodoro.model.Task;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    private final Scanner scanner;

    public ConsoleUI() {
        this.scanner = new Scanner(System.in);
    }

    public void displayMenu(PomodoroState currentState, boolean isTimerRunning, boolean isTimerPaused, String currentTaskName) {
        System.out.println("\n--- Menú Pomodoro ---");

        if (currentTaskName != null && !currentTaskName.isEmpty()) {
            System.out.println(">> Tarea Actual: " + currentTaskName + " <<");
        }

        if (currentState == PomodoroState.AWAITING_NEXT_POMODORO) {
            System.out.println("Descanso terminado.");
            System.out.println("s. Iniciar siguiente Pomodoro" + (currentTaskName != null ? " (para '" + currentTaskName + "')" : ""));
            System.out.println("n. No iniciar (volver al menú principal)");
            System.out.print("Seleccione una opción: ");
            return;
        }

        if (isTimerRunning) {
            System.out.println("Temporizador actual: " + formatStateName(currentState) +
                    (isTimerPaused ? " (Pausado)" : " (En curso)"));
            System.out.println("4. " + (isTimerPaused ? "Reanudar" : "Pausar"));
            System.out.println("5. Finalizar Temporizador/Ciclo Actual");
            // Opciones de Tareas mientras el timer corre podrían ser limitadas o no existir
            System.out.println("6. Salir de la Aplicación");
            System.out.print("Seleccione una opción (o presione Enter para actualizar tiempo si está en curso): ");
        } else { // currentState es STOPPED
            System.out.println("--- Gestión de Pomodoros ---");
            System.out.println("1. Iniciar Ciclo Pomodoro" + (currentTaskName != null ? " (para '" + currentTaskName + "')" : ""));
            System.out.println("2. Iniciar Pomodoro Individual" + (currentTaskName != null ? " (para '" + currentTaskName + "')" : ""));
            System.out.println("3. Iniciar Descanso Corto");
            System.out.println("4. Iniciar Descanso Largo");
            System.out.println("--- Gestión de Tareas ---");
            System.out.println("10. Crear Nueva Tarea");
            System.out.println("11. Listar Tareas Pendientes");
            System.out.println("12. Seleccionar Tarea para Pomodoro");
            System.out.println("13. Marcar Tarea como Completada");
            System.out.println("14. Deseleccionar Tarea Actual");
            System.out.println("--- Gestión de Categorías ---");
            System.out.println("20. Crear Nueva Categoría");
            System.out.println("21. Listar Categorías");
            System.out.println("--- Salir ---");
            System.out.println("6. Salir de la Aplicación");
            System.out.print("Seleccione una opción: ");
        }
    }

    public void displayTime(PomodoroState state, int remainingSeconds, String taskName) {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        String taskInfo = taskName != null && !taskName.isEmpty() ? " [" + taskName + "]" : "";
        System.out.printf("\rTiempo restante (%s)%s: %02d:%02d ", formatStateName(state), taskInfo, minutes, seconds);
    }

    public String getUserInput() {
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim().toLowerCase();
        }
        return "";
    }

    public String prompt(String message) {
        System.out.print(message + ": ");
        return scanner.nextLine().trim();
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showInlineMessage(String message) {
        System.out.print(message);
    }

    public void clearLine() {
        System.out.print("\r" + " ".repeat(120) + "\r"); // Aumentar el espacio si es necesario
    }

    public void newLine() {
        System.out.println();
    }

    private String formatStateName(PomodoroState state) {
        if (state == null) return "N/A";
        return state.name().replace("_", " ");
    }

    public void closeScanner() {
        scanner.close();
    }

    public void listTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            showMessage("No hay tareas para mostrar.");
            return;
        }
        showMessage("\n--- Tareas ---");
        for (Task task : tasks) {
            showMessage(task.toString());
        }
    }

    public void listCategories(List<Category> categories) {
        if (categories.isEmpty()) {
            showMessage("No hay categorías para mostrar.");
            return;
        }
        showMessage("\n--- Categorías ---");
        for (Category category : categories) {
            showMessage(category.getId() + ". " + category.getName());
        }
    }

    public Long selectTaskFromList(List<Task> tasks) {
        if (tasks.isEmpty()) {
            showMessage("No hay tareas disponibles para seleccionar.");
            return null;
        }
        listTasks(tasks);
        String input = prompt("Ingrese el ID de la tarea a seleccionar (o presione Enter para cancelar)");
        if (input.isEmpty()) return null;
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            showMessage("ID inválido.");
            return null;
        }
    }

    public Long selectCategoryFromList(List<Category> categories) {
        if (categories.isEmpty()) {
            showMessage("No hay categorías disponibles.");
            return null;
        }
        listCategories(categories);
        String input = prompt("Ingrese el ID de la categoría (o presione Enter para 'Inbox' por defecto)");
        if (input.isEmpty()) return null; // Indica usar Inbox por defecto
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            showMessage("ID inválido.");
            return null; // Podría reintentar o usar inbox
        }
    }
}