package com.gustavopeiretti.gppomodoro.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.lang.Runnable;

public class PomodoroTimer {
    private ScheduledExecutorService scheduler;
    // No necesitamos totalDurationSeconds aquí, se gestiona externamente
    private int remainingSeconds;
    private boolean isRunning;
    private boolean isPaused;

    private Consumer<Integer> onTickCallback;
    private Runnable onFinishCallback;

    public PomodoroTimer() {
        this.isRunning = false;
        this.isPaused = false;
    }

    public void start(int durationSeconds, Consumer<Integer> onTick, Runnable onFinish) {
        if (this.scheduler != null && !this.scheduler.isShutdown()) {
            this.scheduler.shutdownNow();
        }
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.remainingSeconds = durationSeconds; // El timer solo conoce los segundos que le dieron para contar
        this.onTickCallback = onTick;
        this.onFinishCallback = onFinish;
        this.isRunning = true;
        this.isPaused = false;

        this.scheduler.scheduleAtFixedRate(() -> {
            if (isPaused) {
                return;
            }
            if (remainingSeconds > 0) {
                remainingSeconds--;
                if (onTickCallback != null) {
                    // Pasamos los segundos restantes actuales del timer
                    onTickCallback.accept(this.remainingSeconds);
                }
            } else {
                // Detener el scheduler ANTES de llamar a onFinish para evitar llamadas concurrentes
                // o que onFinish intente reiniciar un timer que aún está técnicamente "vivo".
                ScheduledExecutorService currentScheduler = this.scheduler;
                this.isRunning = false; // Marcar como no corriendo antes de que onFinish pueda intentar algo
                this.isPaused = false;
                if (currentScheduler != null) {
                    currentScheduler.shutdown();
                }
                
                if (onFinishCallback != null) {
                    onFinishCallback.run();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void pause() {
        if (isRunning && !isPaused) {
            this.isPaused = true;
        }
    }

    public void resume() {
        if (isRunning && isPaused) {
            this.isPaused = false;
        }
    }

    public void stopAndClear() {
        if (this.scheduler != null) {
            this.scheduler.shutdownNow();
        }
        this.isRunning = false;
        this.isPaused = false;
        this.remainingSeconds = 0;
        this.onTickCallback = null;
        this.onFinishCallback = null;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isPaused() {
        return isPaused;
    }
}