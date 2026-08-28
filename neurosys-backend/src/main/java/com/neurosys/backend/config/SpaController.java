package com.neurosys.backend.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {
        "/",
        "/dashboard",
        "/computers",
        "/computers/**",
        "/admin-laptop",
        "/readiness",
        "/pending-computers",
        "/software",
        "/analytics",
        "/alerts",
        "/settings",
        "/login"
    })
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }

    @GetMapping(value = "/{path:^(?!api|v3|swagger-ui|assets|favicon\\.ico).*$}/**")
    public String forwardUnmappedSpaRoutes() {
        return "forward:/index.html";
    }
}
