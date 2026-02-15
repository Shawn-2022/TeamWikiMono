package com.wiki.monowiki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MonoWikiApplication {

    public static void main(String[] args) {
	SpringApplication.run(MonoWikiApplication.class, args);
    }

}
