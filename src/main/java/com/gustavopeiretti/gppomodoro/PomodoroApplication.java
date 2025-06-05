package com.gustavopeiretti.gppomodoro;

import com.gustavopeiretti.gppomodoro.model.PomodoroSession;
import com.gustavopeiretti.gppomodoro.model.PomodoroState;
import com.gustavopeiretti.gppomodoro.service.PomodoroService;
import com.gustavopeiretti.gppomodoro.timer.PomodoroTimer;
import com.gustavopeiretti.gppomodoro.ui.ConsoleUI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PomodoroApplication implements CommandLineRunner {

    @Autowired
    private PomodoroService pomodoroService;

    private ConsoleUI consoleUI;
    private PomodoroSession pomodoroSession;
    private PomodoroTimer pomodoroTimer;


    public static void main(String[] args) {
        SpringApplication.run(PomodoroApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        this.consoleUI = new ConsoleUI();
        this.pomodoroSession = new PomodoroSession();
        this.pomodoroTimer = new PomodoroTimer();

        consoleUI.showMessage("¡Bienvenido a la aplicación Pomodoro!");

        while (true) {
            PomodoroState currentState = pomodoroSession.getCurrentState();
            boolean isTimerRunning = pomodoroTimer.isRunning();
            boolean isTimerPaused = pomodoroTimer.isPaused();

            if (currentState == PomodoroState.STOPPED ||
                currentState == PomodoroState.AWAITING_NEXT_POMODORO ||
                (isTimerRunning && isTimerPaused)) {
                consoleUI.displayMenu(currentState, isTimerRunning, isTimerPaused);
            }

            String input = consoleUI.getUserInput();

            if (isTimerRunning && !isTimerPaused && input.isEmpty()) {
                consoleUI.clearLine();
                consoleUI.displayMenu(currentState, isTimerRunning, isTimerPaused);
            }
            
            pomodoroService.handleUserInput(input, pomodoroSession, pomodoroTimer, consoleUI);

            // Si el servicio decidió salir, el System.exit() ya habrá ocurrido.
            // Este sleep es para ceder CPU si no hay interacciones.
            try {
                Thread.sleep(100); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                consoleUI.showMessage("Hilo principal interrumpido. Saliendo...");
                pomodoroService.exitApplication(pomodoroTimer, consoleUI);
            }
        }
    }
}