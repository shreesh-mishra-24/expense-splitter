package com.expensesplitter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI/Swagger documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Expense Splitter API")
                        .version("1.0.0")
                        .description("""
                                ## Overview

                                The Expense Splitter API helps groups manage shared expenses and calculate who owes whom.

                                ### Features

                                - **Group Management**: Create and manage expense-sharing groups
                                - **Member Management**: Add and remove members from groups
                                - **Expense Tracking**: Record expenses with details about who paid and who participated
                                - **Balance Calculation**: Calculate each member's balance within the group
                                - **Debt Simplification**: Generate optimized settlement plans with minimum transactions

                                ### Debt Simplification Example

                                Given the following transactions:
                                - A pays B $40
                                - B pays C $40
                                - C pays A $10

                                Instead of three separate repayments, the API simplifies this to:
                                - C pays A $30 (single transaction settles all balances)
                                """)
                        .contact(new Contact()
                                .name("API Support"))
                        .license(new License()
                                .name("MIT")));
    }
}
