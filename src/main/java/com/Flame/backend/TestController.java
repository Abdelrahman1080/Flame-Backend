package com.Flame.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

@RestController
public class TestController {

    private final DataSource dataSource;

    public TestController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/test-db")
    public String testConnection() throws Exception {
        return dataSource.getConnection().isValid(1) ? "DB Connected ✅" : "Not Connected ❌";
    }
}
