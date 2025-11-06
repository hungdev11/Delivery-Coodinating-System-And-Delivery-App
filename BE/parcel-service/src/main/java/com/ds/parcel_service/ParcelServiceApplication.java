package com.ds.parcel_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Bật tính năng lập lịch của Spring
@EnableFeignClients
public class ParcelServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParcelServiceApplication.class, args);
	}

}
