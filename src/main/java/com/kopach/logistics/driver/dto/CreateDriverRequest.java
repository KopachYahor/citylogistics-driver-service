package com.kopach.logistics.driver.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateDriverRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    public CreateDriverRequest() {
    }

    public CreateDriverRequest(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}