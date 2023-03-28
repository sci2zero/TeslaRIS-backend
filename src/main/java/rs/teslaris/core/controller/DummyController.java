package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.util.email.EmailUtil;

@RestController
@RequestMapping("/api/dummy")
@RequiredArgsConstructor
public class DummyController {

    private final EmailUtil emailUtil;

    @GetMapping
    public String returnDummyData() {
        return "TEST";
    }

    @PostMapping
    public void testEmail() {
        emailUtil.sendSimpleEmail("email@email.com", "SUBJECT", "TEXT");
    }
}
