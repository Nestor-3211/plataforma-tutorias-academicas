package pe.utp.tutorias.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String inicio(HttpSession session, Model model) {
        model.addAttribute("nombreUsuarioActual", session.getAttribute("nombreUsuarioActual"));
        model.addAttribute("esTutorActual", session.getAttribute("esTutorActual"));
        return "inicio";
    }
}
