package com.neurosys.backend.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {"/", "/index"})
    public String index() {
        return "forward:/index.html";
    }
}
