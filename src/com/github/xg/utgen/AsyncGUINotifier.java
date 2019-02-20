package com.github.xg.utgen;


public interface AsyncGUINotifier {

    void success(String message);

    void failed(String message);

    void attachProcess(Process process);

    void detachLastProcess();

    void printOnConsole(String message);

    void clearConsole();
}
