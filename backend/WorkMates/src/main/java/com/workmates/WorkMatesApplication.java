package com.workmates;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the WorkMates backend. Feature packages (account, profile, posting, ...) and
 * {@code common} sit below {@code com.workmates}, so component scanning finds them all.
 */
@SpringBootApplication
public class WorkMatesApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkMatesApplication.class, args);
    }

}
