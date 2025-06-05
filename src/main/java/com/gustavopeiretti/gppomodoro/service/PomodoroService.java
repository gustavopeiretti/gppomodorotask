package com.gustavopeiretti.gppomodoro.service;

import com.gustavopeiretti.gppomodoro.config.PomodoroConfig;
import com.gustavopeiretti.gppomodoro.model.PomodoroSession;
import com.gustavopeiretti.gppomodoro.model.PomodoroState;
import com.gustavopeiretti.gppomodoro.timer.PomodoroTimer;
import com.gustavopeiretti.gppomodoro.ui.ConsoleUI;
import org.springframework.stereotype.Service; // Para que Spring lo maneje como Singleton

@Service
public class PomodoroService {

    public void startPomodoroCycle(PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        ui.showMessage("Iniciando ciclo Pomodoro automático...");
        session.resetCyclePomodoroCount();
        startNewPomodoro(session, timer, ui);
    }

    public void startSinglePomodoro(PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        session.resetCyclePomodoroCount();
        startNewPomodoro(session, timer, ui);
    }

    private void startNewPomodoro(PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        if (timer.isRunning()) {
            ui.showMessage("Finalice el temporizador actual (" + session.getCurrentState() + ") antes de iniciar un Pomodoro.");
            return;
        }
        ui.showMessage("Iniciando Pomodoro de " + PomodoroConfig.POMODORO_DURATION_MINUTES + " minutos...");
        ui.newLine();
        session.setCurrentState(PomodoroState.POMODORO);
        timer.start(PomodoroConfig.POMODORO_DURATION_MINUTES * 60,
            (remainingSeconds) -> { // onTick lambda
                session.setRemainingSecondsInTimer(remainingSeconds);
                ui.displayTime(session.getCurrentState(), remainingSeconds);
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
                ui.displayTime(session.getCurrentState(), remainingSeconds);
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
                ui.displayTime(session.getCurrentState(), remainingSeconds);
            },
            () -> {
                onBreakFinished(session, ui);
            }
        );
    }

    private void onPomodoroFinished(PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        ui.clearLine();
        ui.showMessage("\n¡Tiempo terminado para POMODORO!");
        session.incrementPomodoroCount();
        session.incrementCyclePomodoroCount();
        ui.showMessage("Pomodoros totales completados: " + session.getPomodoroCount());
        ui.showMessage("Pomodoros en este ciclo: " + session.getCyclePomodoroCount() + "/" + PomodoroConfig.POMODOROS_UNTIL_LONG_BREAK);

        if (session.getCyclePomodoroCount() % PomodoroConfig.POMODOROS_UNTIL_LONG_BREAK == 0) {
            ui.showMessage("¡Hora de un descanso largo!");
            startLongBreak(session, timer, ui);
        } else {
            ui.showMessage("¡Hora de un descanso corto!");
            startShortBreak(session, timer, ui);
        }
    }

    private void onBreakFinished(PomodoroSession session, ConsoleUI ui) {
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
            // Guardar el tiempo restante actual en la sesión al pausar
            session.setRemainingSecondsInTimer(timer.getRemainingSeconds());
            ui.showMessage("\nTemporizador pausado.");
            ui.displayTime(session.getCurrentState(), session.getRemainingSecondsInTimer());
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
        ui.showMessage("Volviendo al menú principal...");
    }

    public void handleUserInput(String input, PomodoroSession session, PomodoroTimer timer, ConsoleUI ui) {
        if (session.getCurrentState() == PomodoroState.AWAITING_NEXT_POMODORO) {
            if ("s".equals(input)) {
                startNewPomodoro(session, timer, ui);
            } else if ("n".equals(input)) {
                ui.showMessage("Ciclo detenido. Volviendo al menú principal.");
                resetToStoppedState(session, timer, ui);
            } else if (!input.isEmpty()) {
                ui.showMessage("Opción no válida. Ingrese 's' o 'n'.");
            }
            return;
        }

        if (timer.isRunning()) {
            if (input.isEmpty()) {
                 if (!timer.isPaused()) {
                    ui.clearLine();
                    // El menú se muestra desde PomodoroApplication
                }
                return;
            }
            try {
                int choice = Integer.parseInt(input);
                switch (choice) {
                    case 4: togglePause(session, timer, ui); break;
                    case 5: finishCurrentTimerAndCycle(session, timer, ui); break;
                    case 6: exitApplication(timer, ui); break;
                    default: ui.showMessage("Opción no válida. Use 4, 5, o 6.");
                }
            } catch (NumberFormatException e) {
                ui.showMessage("Entrada inválida. Por favor, ingrese un número (4, 5, o 6).");
            }
        } else { // currentState es STOPPED
            if (input.isEmpty()) return;
            try {
                int choice = Integer.parseInt(input);
                switch (choice) {
                    case 1: startPomodoroCycle(session, timer, ui); break;
                    case 2: startSinglePomodoro(session, timer, ui); break;
                    case 3: startShortBreak(session, timer, ui); break;
                    case 4: startLongBreak(session, timer, ui); break;
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
        if (timer != null) { // El timer podría no haberse inicializado si se sale muy pronto
            timer.stopAndClear();
        }
        ui.closeScanner();
        System.exit(0);
    }
}