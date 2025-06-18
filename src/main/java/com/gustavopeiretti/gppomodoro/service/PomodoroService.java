package com.gustavopeiretti.gppomodoro.service;

import com.gustavopeiretti.gppomodoro.config.PomodoroConfig;
import com.gustavopeiretti.gppomodoro.model.Category;
import com.gustavopeiretti.gppomodoro.model.PomodoroSession;
import com.gustavopeiretti.gppomodoro.model.PomodoroState;
import com.gustavopeiretti.gppomodoro.model.Task;
import com.gustavopeiretti.gppomodoro.repository.CategoryRepository;
import com.gustavopeiretti.gppomodoro.repository.TaskRepository;
import com.gustavopeiretti.gppomodoro.timer.PomodoroTimer;
import com.gustavopeiretti.gppomodoro.ui.ConsoleUI;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PomodoroService {

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;

    public static final String DEFAULT_CATEGORY_NAME = "Inbox";

    @Autowired
    public PomodoroService(TaskRepository taskRepository, CategoryRepository categoryRepository) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
    }

//    @PostConstruct
//    @Transactional
//    public void initDefaultCategory() {
//        if (categoryRepository.findByName(DEFAULT_CATEGORY_NAME).isEmpty()) {
//            Category inbox = new Category(DEFAULT_CATEGORY_NAME);
//            categoryRepository.save(inbox);
//            // System.out.println("Categoría por defecto 'Inbox' creada."); // Opcional: loguear a consola
//        }
//    }

    private String getCurrentTaskName(PomodoroSession session) {
        if (session.getCurrentTaskId() != null) {
            return taskRepository.findById(session.getCurrentTaskId())
                    .map(Task::getName)
                    .orElse(null);
        }
        return null;
    }

    public void startPomodoroCycle(PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        ui.showMessage("Iniciando ciclo Pomodoro automático...");
        session.resetCyclePomodoroCount();
        startNewPomodoro(session, timer, ui);
    }

    public void startSinglePomodoro(PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        session.resetCyclePomodoroCount(); // Reinicia para pomodoros individuales
        startNewPomodoro(session, timer, ui);
    }

    private void startNewPomodoro(PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        if (timer.isRunning()) {
            ui.showMessage("Finalice el temporizador actual (" + session.getCurrentState() + ") antes de iniciar un Pomodoro.");
            return;
        }
        String taskName = getCurrentTaskName(session);
        String forTaskMessage = taskName != null ? " para la tarea '" + taskName + "'" : "";
        ui.showMessage("Iniciando Pomodoro de " + PomodoroConfig.POMODORO_DURATION_MINUTES + " minutos" + forTaskMessage + "...");
        ui.newLine();
        session.setCurrentState(PomodoroState.POMODORO);
        timer.start(PomodoroConfig.POMODORO_DURATION_MINUTES * 60,
                (remainingSeconds) -> { // onTick lambda
                    session.setRemainingSecondsInTimer(remainingSeconds);
                    ui.displayTime(session.getCurrentState(), remainingSeconds, getCurrentTaskName(session));
                },
                () -> { // onFinish lambda
                    onPomodoroFinished(session, timer, ui);
                }
        );
    }

    public void startShortBreak(PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        if (timer.isRunning()) {
            ui.showMessage("Ya hay un temporizador en ejecución. Finalícelo primero.");
            return;
        }
        ui.showMessage("Iniciando Descanso Corto de " + PomodoroConfig.SHORT_BREAK_DURATION_MINUTES + " minutos...");
        ui.newLine();
        session.setCurrentState(PomodoroState.SHORT_BREAK);
        timer.start(PomodoroConfig.SHORT_BREAK_DURATION_MINUTES * 60,
                (remainingSeconds) -> {
                    session.setRemainingSecondsInTimer(remainingSeconds);
                    ui.displayTime(session.getCurrentState(), remainingSeconds, null); // Descansos no muestran tarea
                },
                () -> {
                    onBreakFinished(session, ui);
                }
        );
    }

    public void startLongBreak(PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        if (timer.isRunning()) {
            ui.showMessage("Ya hay un temporizador en ejecución. Finalícelo primero.");
            return;
        }
        ui.showMessage("Iniciando Descanso Largo de " + PomodoroConfig.LONG_BREAK_DURATION_MINUTES + " minutos...");
        ui.newLine();
        session.setCurrentState(PomodoroState.LONG_BREAK);
        timer.start(PomodoroConfig.LONG_BREAK_DURATION_MINUTES * 60,
                (remainingSeconds) -> {
                    session.setRemainingSecondsInTimer(remainingSeconds);
                    ui.displayTime(session.getCurrentState(), remainingSeconds, null); // Descansos no muestran tarea
                },
                () -> {
                    onBreakFinished(session, ui);
                }
        );
    }

    @Transactional
    protected void onPomodoroFinished(PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        ui.clearLine();
        ui.showMessage("\n¡Tiempo terminado para POMODORO!");
        session.incrementPomodoroCount();
        session.incrementCyclePomodoroCount();
        ui.showMessage("Pomodoros totales completados: " + session.getPomodoroCount());
        ui.showMessage("Pomodoros en este ciclo: " + session.getCyclePomodoroCount() + "/" + PomodoroConfig.POMODOROS_UNTIL_LONG_BREAK);

        if (session.getCurrentTaskId() != null) {
            taskRepository.findById(session.getCurrentTaskId()).ifPresent(task -> {
                task.incrementPomodorosSpent();
                taskRepository.save(task);
                ui.showMessage("Pomodoro contado para la tarea: '" + task.getName() + "' (Total: " + task.getPomodorosSpent() + ")");
            });
        }

        if (session.getCyclePomodoroCount() % PomodoroConfig.POMODOROS_UNTIL_LONG_BREAK == 0) {
            ui.showMessage("¡Hora de un descanso largo!");
            startLongBreak(session, timer, ui);
        } else {
            ui.showMessage("¡Hora de un descanso corto!");
            startShortBreak(session, timer, ui);
        }
    }

    protected void onBreakFinished(PomodoroSession session, ConsoleUI ui) {
        ui.clearLine();
        ui.showMessage("\n¡Tiempo terminado para " + session.getCurrentState().name().replace("_", " ") + "!");
        session.setCurrentState(PomodoroState.AWAITING_NEXT_POMODORO);
    }

    public void togglePause(PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        if (!timer.isRunning()) {
            ui.showMessage("No hay un temporizador en ejecución para pausar/reanudar.");
            return;
        }
        if (timer.isPaused()) {
            timer.resume();
            ui.showMessage("\nTemporizador reanudado.");
        } else {
            timer.pause();
            session.setRemainingSecondsInTimer(timer.getRemainingSeconds());
            ui.showMessage("\nTemporizador pausado.");
            ui.displayTime(session.getCurrentState(), session.getRemainingSecondsInTimer(), getCurrentTaskName(session));
            ui.newLine();
        }
    }

    public void finishCurrentTimerAndCycle(PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        if (!timer.isRunning() && session.getCurrentState() != PomodoroState.AWAITING_NEXT_POMODORO) {
            ui.showMessage("No hay un temporizador o ciclo activo para finalizar.");
            return;
        }
        ui.showMessage("\nTemporizador/Ciclo finalizado manualmente.");
        resetToStoppedState(session, timer, ui);
    }

    private void resetToStoppedState(PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        timer.stopAndClear();
        session.setCurrentState(PomodoroState.STOPPED);
        session.resetCyclePomodoroCount();
        session.setRemainingSecondsInTimer(0);
        // No deseleccionamos la tarea aquí automáticamente, el usuario puede hacerlo explícitamente.
        // session.setCurrentTaskId(null);
        ui.showMessage("Volviendo al menú principal...");
    }

    @Transactional
    public void createTask(ConsoleUI ui) { // No necesita session para crear tarea
        String taskName = ui.prompt("Nombre de la tarea");
        if (taskName.isEmpty()) {
            ui.showMessage("El nombre de la tarea no puede estar vacío.");
            return;
        }
        String description = ui.prompt("Descripción (opcional)");

        List<Category> categories = categoryRepository.findAll();
        Long categoryId = ui.selectCategoryFromList(categories); // Puede retornar null para default

        Category selectedCategory;
        if (categoryId != null) {
            selectedCategory = categoryRepository.findById(categoryId)
                    .orElseGet(() -> categoryRepository.findByName(DEFAULT_CATEGORY_NAME)
                            .orElseThrow(() -> new IllegalStateException("Categoría 'Inbox' por defecto no encontrada y categoría seleccionada no existe.")));
        } else { // Si el usuario presionó Enter, usa Inbox
            selectedCategory = categoryRepository.findByName(DEFAULT_CATEGORY_NAME)
                    .orElseThrow(() -> new IllegalStateException("Categoría 'Inbox' por defecto no encontrada."));
        }

        Task newTask = new Task(taskName, description, selectedCategory);
        taskRepository.save(newTask);
        ui.showMessage("Tarea '" + taskName + "' creada en la categoría '" + selectedCategory.getName() + "'.");
    }

    public void listPendingTasks(ConsoleUI ui) {
        List<Task> tasks = taskRepository.findByCompletedFalseOrderByCreationDateAsc();
        ui.listTasks(tasks);
    }

    public void selectTaskForPomodoro(PomodoroSession session, ConsoleUI ui) {
        List<Task> tasks = taskRepository.findByCompletedFalseOrderByCreationDateAsc();
        if (tasks.isEmpty()){
            ui.showMessage("No hay tareas pendientes para seleccionar.");
            return;
        }
        Long taskId = ui.selectTaskFromList(tasks);
        if (taskId != null) {
            taskRepository.findById(taskId).ifPresentOrElse(task -> {
                session.setCurrentTaskId(task.getId());
                ui.showMessage("Tarea '" + task.getName() + "' seleccionada para el próximo Pomodoro.");
            }, () -> ui.showMessage("Tarea con ID " + taskId + " no encontrada."));
        } else {
            // Si el usuario presiona Enter, no se selecciona nada.
            // ui.showMessage("Ninguna tarea seleccionada."); // Opcional
        }
    }

    public void deselectCurrentTask(PomodoroSession session, ConsoleUI ui) {
        if (session.getCurrentTaskId() == null) {
            ui.showMessage("No hay ninguna tarea seleccionada actualmente.");
            return;
        }
        String taskName = getCurrentTaskName(session); // Obtener nombre antes de deseleccionar
        session.setCurrentTaskId(null);
        ui.showMessage("Tarea '" + (taskName != null ? taskName : "desconocida") + "' deseleccionada.");
    }

    @Transactional
    public void markTaskAsCompleted(PomodoroSession session, ConsoleUI ui) {
        List<Task> tasks = taskRepository.findByCompletedFalseOrderByCreationDateAsc();
        if (tasks.isEmpty()){
            ui.showMessage("No hay tareas pendientes para marcar como completadas.");
            return;
        }
        Long taskId = ui.selectTaskFromList(tasks);
        if (taskId != null) {
            taskRepository.findById(taskId).ifPresentOrElse(task -> {
                task.setCompleted(true);
                taskRepository.save(task);
                ui.showMessage("Tarea '" + task.getName() + "' marcada como completada.");
                if (session.getCurrentTaskId() != null && session.getCurrentTaskId().equals(task.getId())) {
                    session.setCurrentTaskId(null);
                    ui.showMessage("La tarea actual ha sido deseleccionada.");
                }
            }, () -> ui.showMessage("Tarea con ID " + taskId + " no encontrada."));
        }
    }

    @Transactional
    public void createCategory(ConsoleUI ui) {
        String categoryName = ui.prompt("Nombre de la nueva categoría");
        if (categoryName.isEmpty()) {
            ui.showMessage("El nombre de la categoría no puede estar vacío.");
            return;
        }
        if (categoryName.equalsIgnoreCase(DEFAULT_CATEGORY_NAME)) {
            ui.showMessage("No se puede crear una categoría con el nombre reservado '" + DEFAULT_CATEGORY_NAME + "'.");
            return;
        }
        if (categoryRepository.findByName(categoryName).isPresent()) {
            ui.showMessage("La categoría '" + categoryName + "' ya existe.");
            return;
        }
        Category newCategory = new Category(categoryName);
        categoryRepository.save(newCategory);
        ui.showMessage("Categoría '" + categoryName + "' creada.");
    }

    public void listCategories(ConsoleUI ui) {
        List<Category> categories = categoryRepository.findAll();
        ui.listCategories(categories);
    }

    public void handleUserInput(String input, PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        if (session.getCurrentState() == PomodoroState.AWAITING_NEXT_POMODORO) {
            if ("s".equals(input)) {
                startNewPomodoro(session, timer, ui);
            } else if ("n".equals(input)) {
                ui.showMessage("Ciclo detenido. Volviendo al menú principal.");
                resetToStoppedState(session, timer, ui);
            } else if (!input.isEmpty()){
                ui.showMessage("Opción no válida. Ingrese 's' o 'n'.");
            }
            return;
        }

        if (timer.isRunning()) { // Timer está activo (corriendo o pausado)
            if (input.isEmpty()) { // Usuario presionó Enter
                if (!timer.isPaused()) {
                    ui.clearLine(); // Limpia la línea del contador de tiempo
                    // El menú se mostrará desde PomodoroApplication
                }
                // Si está pausado y presiona Enter, el menú ya se mostró. No hacer nada.
                return;
            }
            try {
                int choice = Integer.parseInt(input);
                switch (choice) {
                    case 4: togglePause(session, timer, ui); break;
                    case 5: finishCurrentTimerAndCycle(session, timer, ui); break;
                    case 6: exitApplication(timer, ui); break; // exitApplication se encarga de todo
                    default: ui.showMessage("Opción no válida mientras el temporizador está activo. Use 4, 5, o 6.");
                }
            } catch (NumberFormatException e) {
                if (!input.isEmpty()) { // Evitar mensaje si solo fue Enter
                    ui.showMessage("Entrada inválida. Por favor, ingrese un número (4, 5, o 6).");
                }
            }
        } else { // Timer no está corriendo (currentState es STOPPED)
            if (input.isEmpty()) return; // No hacer nada si se presiona Enter en el menú principal
            try {
                int choice = Integer.parseInt(input);
                switch (choice) {
                    // Pomodoro
                    case 1: startPomodoroCycle(session, timer, ui); break;
                    case 2: startSinglePomodoro(session, timer, ui); break;
                    case 3: startShortBreak(session, timer, ui); break;
                    case 4: startLongBreak(session, timer, ui); break;
                    // Tareas
                    case 10: createTask(ui); break; // No necesita session para crear
                    case 11: listPendingTasks(ui); break;
                    case 12: selectTaskForPomodoro(session, ui); break;
                    case 13: markTaskAsCompleted(session, ui); break;
                    case 14: deselectCurrentTask(session, ui); break;
                    // Categorías
                    case 20: createCategory(ui); break;
                    case 21: listCategories(ui); break;
                    // Salir
                    case 6: exitApplication(timer, ui); break;
                    default: ui.showMessage("Opción no válida. Por favor, intente de nuevo.");
                }
            } catch (NumberFormatException e) {
                ui.showMessage("Entrada inválida. Por favor, ingrese un número.");
            }
        }
    }

    public void exitApplication(PomodoroTimer timer, ConsoleUI ui) {
        ui.showMessage("\nSaliendo de la aplicación Pomodoro. ¡Adiós!");
        if (timer != null) {
            timer.stopAndClear();
        }
        ui.closeScanner();
        System.exit(0);
    }
}