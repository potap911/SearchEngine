package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DefaultController {

    /**
     * The method generates a page from the index.html HTML file,
     * which is located in the resources/templates folder.
     */
    @RequestMapping("/")
    public String index() {
        return "index";
    }
}
