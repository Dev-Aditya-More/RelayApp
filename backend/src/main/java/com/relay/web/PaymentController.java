package com.relay.web;

import com.relay.domain.User;
import com.relay.service.PaymentService;
import com.relay.web.Dtos.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) { this.service = service; }

    @GetMapping
    List<PaymentView> list(@AuthenticationPrincipal User user) { return service.list(user); }

    @GetMapping("/config")
    Map<String, Object> config() { return Map.of("mock", service.isMock()); }

    @GetMapping("/{id}/invoice")
    PaymentView invoice(@AuthenticationPrincipal User user, @PathVariable Long id) { return service.invoice(user, id); }

    @PostMapping("/{id}/checkout")
    CheckoutRes checkout(@AuthenticationPrincipal User user, @PathVariable Long id) { return service.checkout(user, id); }

    @PostMapping("/verify")
    PaymentView verify(@AuthenticationPrincipal User user, @Valid @RequestBody VerifyReq req) { return service.verify(user, req); }

    @PostMapping("/{id}/mock-pay")
    PaymentView mockPay(@AuthenticationPrincipal User user, @PathVariable Long id) { return service.mockPay(user, id); }

    /** Public: authenticated by the X-Razorpay-Signature HMAC over the raw body. */
    @PostMapping("/webhook")
    Map<String, String> webhook(@RequestBody String body,
                                @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        service.handleWebhook(body, signature);
        return Map.of("status", "ok");
    }
}
