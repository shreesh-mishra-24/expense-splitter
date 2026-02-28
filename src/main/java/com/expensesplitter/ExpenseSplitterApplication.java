package com.expensesplitter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Expense Splitter application.
 *
 * This application provides REST APIs for managing shared expenses among groups,
 * calculating balances, and generating optimized settlement plans.
 */
@SpringBootApplication
public class ExpenseSplitterApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseSplitterApplication.class, args);
    }
}
