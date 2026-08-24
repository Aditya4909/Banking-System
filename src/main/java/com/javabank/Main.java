package com.javabank;

import com.javabank.ui.BankingApp;

/**
 * Root entry point for the JavaBank application.
 * Redirects execution to JavaFX Application launcher to avoid modular runtime configuration errors.
 */
public class Main {
    public static void main(String[] args) {
        BankingApp.main(args);
    }
}
