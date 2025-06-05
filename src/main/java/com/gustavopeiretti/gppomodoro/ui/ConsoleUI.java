package com.gustavopeiretti.gppomodoro.ui;

import com.gustavopeiretti.gppomodoro.model.PomodoroState;
import java.util.Scanner;

public class ConsoleUI {
    private final Scanner scanner;

    public ConsoleUI() {
        this.scanner = new Scanner(System.in);
    }

    public void displayMenu(PomodoroState currentState, boolean isTimerRunning, boolean isTimerPaused) {
        System.out.println("\n--- Menú Pomodoro ---");
        if (currentState == PomodoroState.AWAITING_NEXT_POMODORO) {
            System.out.println("Descanso terminado.");
            System.out.println("s. Iniciar siguiente Pomodoro");
            System.out.println("n. No iniciar (volver al menú principal)");
            System.out.print("Seleccione una opción: ");
            return;
        }

        if (isTimerRunning) {
            System.out.println("Temporizador actual: " + formatStateName(currentState) +
                               (isTimerPaused ? " (Pausado)" : " (En curso)"));
            System.out.println("4. " + (isTimerPaused ? "Reanudar" : "Pausar"));
            System.out.println("5. Finalizar Temporizador/Ciclo Actual");
            System.out.println("6. Salir de la Aplicación");
            System.out.print("Seleccione una opción (o presione Enter para actualizar tiempo si está en curso): ");
        } else { // currentState es STOPPED
            System.out.println("1. Iniciar Ciclo Pomodoro (Automático)");
            System.out.println("2. Iniciar Pomodoro Individual");
            System.out.println("3. Iniciar Descanso Corto");
            System.out.println("4. Iniciar Descanso Largo");
            System.out.println("6. Salir de la Aplicación");
            System.out.print("Seleccione una opción: ");
        }
    }

    public void displayTime(PomodoroState state, int remainingSeconds) {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        System.out.printf("\rTiempo restante (%s): %02d:%02d ", formatStateName(state), minutes, seconds);
    }

    public String getUserInput() {
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim().toLowerCase();
        }
        return ""; // O manejar EOF de otra manera si es necesario
    }

    public void showMessage(String message) {
        System.out.println(message);
    }
    
    public void showInlineMessage(String message) {
        System.out.print(message);
    }

    public void clearLine() {
        System.out.print("\r" + " ".repeat(80) + "\r"); // Intenta limpiar la línea actual
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
}