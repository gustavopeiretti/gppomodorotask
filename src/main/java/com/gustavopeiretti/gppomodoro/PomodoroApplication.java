package com.gustavopeiretti.gppomodoro;

import com.gustavopeiretti.gppomodoro.model.PomodoroSession;
import com.gustavopeiretti.gppomodoro.model.PomodoroState;
import com.gustavopeiretti.gppomodoro.model.Task; // Necesario para el Optional<Task>
import com.gustavopeiretti.gppomodoro.repository.TaskRepository;
import com.gustavopeiretti.gppomodoro.service.PomodoroService;
import com.gustavopeiretti.gppomodoro.timer.PomodoroTimer;
import com.gustavopeiretti.gppomodoro.ui.ConsoleUI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Optional;

@SpringBootApplication
public class PomodoroApplication implements CommandLineRunner {

    @Autowired
    private PomodoroService pomodoroService;

    @Autowired
    private TaskRepository taskRepository;

    // Componentes de la sesión/UI, no son beans de Spring en este diseño
    private ConsoleUI consoleUI;
    private PomodoroSession pomodoroSession;
    private PomodoroTimer pomodoroTimer;


    public static void main(String[] args) {
        SpringApplication.run(PomodoroApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Inicializar componentes específicos de esta ejecución de la aplicación
        this.consoleUI = new ConsoleUI();
        this.pomodoroSession = new PomodoroSession();
        this.pomodoroTimer = new PomodoroTimer(); // Un timer por sesión de aplicación

        // La categoría 'Inbox' se crea a través de @PostConstruct en PomodoroService

        consoleUI.showMessage("¡Bienvenido a la aplicación Pomodoro con gestión de Tareas!");

        while (true) {
            PomodoroState currentState = pomodoroSession.getCurrentState();
            boolean isTimerRunning = pomodoroTimer.isRunning();
            boolean isTimerPaused = pomodoroTimer.isPaused();

            String currentTaskName = null;
            if (pomodoroSession.getCurrentTaskId() != null) {
                // Buscamos la tarea por ID para obtener su nombre actual
                Optional<Task> taskOpt = taskRepository.findById(pomodoroSession.getCurrentTaskId());
                if (taskOpt.isPresent()) {
                    currentTaskName = taskOpt.get().getName();
                } else {
                    // La tarea pudo haber sido eliminada o ID es inválido, deseleccionarla
                    consoleUI.showMessage("Advertencia: La tarea seleccionada (ID: " + pomodoroSession.getCurrentTaskId() + ") ya no existe. Deseleccionando.");
                    pomodoroSession.setCurrentTaskId(null);
                }
            }

            // Lógica para decidir cuándo mostrar el menú
            if (currentState == PomodoroState.STOPPED ||
                    currentState == PomodoroState.AWAITING_NEXT_POMODORO ||
                    (isTimerRunning && isTimerPaused)) {
                // Mostrar menú si:
                // 1. El timer está detenido (STOPPED).
                // 2. Se espera confirmación para el siguiente pomodoro (AWAITING_NEXT_POMODORO).
                // 3. El timer está corriendo pero pausado.
                consoleUI.displayMenu(currentState, isTimerRunning, isTimerPaused, currentTaskName);
            }
            // Si el timer está corriendo y no pausado, el tiempo se actualiza por el callback.
            // El menú solo se mostrará si el usuario presiona Enter.

            String input = consoleUI.getUserInput();

            // Si el timer está corriendo y NO pausado, y el usuario presiona Enter (input vacío),
            // se limpia la línea de tiempo y se muestra el menú de opciones contextuales.
            if (isTimerRunning && !isTimerPaused && input.isEmpty()) {
                consoleUI.clearLine(); // Limpiar la línea donde se estaba mostrando el tiempo
                consoleUI.displayMenu(currentState, isTimerRunning, isTimerPaused, currentTaskName);
                // El input vacío será manejado por PomodoroService, que usualmente lo ignora en este caso.
            }

            // Pasar todos los objetos de estado/UI al servicio sin estado para que opere
            pomodoroService.handleUserInput(input, pomodoroSession, pomodoroTimer, consoleUI);

            // Si pomodoroService.exitApplication() fue llamado, System.exit() ya terminó el programa.
            // Este sleep es para ceder CPU si no hay interacciones y el bucle sigue.
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaurar el estado de interrupción
                consoleUI.showMessage("Hilo principal interrumpido. Saliendo...");
                // Asegurar que el timer y scanner se cierran al interrumpir
                pomodoroService.exitApplication(pomodoroTimer, consoleUI);
            }
        }
    }
}