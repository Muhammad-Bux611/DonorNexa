package com.donornexa.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "public/home";
    }

    @GetMapping("/emergencies")
    public String emergencies() {
        return "public/emergencies";
    }

    @GetMapping("/about")
    public String about() {
        return "public/about";
    }
}
