package com.gustavopeiretti.gppomodoro.model;

public class PomodoroSession {
    private PomodoroState currentState;
    private int pomodoroCount;
    private int cyclePomodoroCount;
    private int remainingSecondsInTimer;

    public PomodoroSession() {
        this.currentState = PomodoroState.STOPPED;
        this.pomodoroCount = 0;
        this.cyclePomodoroCount = 0;
        this.remainingSecondsInTimer = 0;
    }

    public PomodoroState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(PomodoroState currentState) {
        this.currentState = currentState;
    }

    public int getPomodoroCount() {
        return pomodoroCount;
    }

    public void setPomodoroCount(int pomodoroCount) {
        this.pomodoroCount = pomodoroCount;
    }

    public void incrementPomodoroCount() {
        this.pomodoroCount++;
    }

    public int getCyclePomodoroCount() {
        return cyclePomodoroCount;
    }

    public void setCyclePomodoroCount(int cyclePomodoroCount) {
        this.cyclePomodoroCount = cyclePomodoroCount;
    }
    
    public void incrementCyclePomodoroCount() {
        this.cyclePomodoroCount++;
    }

    public void resetCyclePomodoroCount() {
        this.cyclePomodoroCount = 0;
    }

    public int getRemainingSecondsInTimer() {
        return remainingSecondsInTimer;
    }

    public void setRemainingSecondsInTimer(int remainingSecondsInTimer) {
        this.remainingSecondsInTimer = remainingSecondsInTimer;
    }
}