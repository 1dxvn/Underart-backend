package com.underart.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Hidden
@Controller
public class AppController {

    @GetMapping({"/app", "/app/"})
    public RedirectView app() {
        return new RedirectView("/app/index.html", true);
    }
}
