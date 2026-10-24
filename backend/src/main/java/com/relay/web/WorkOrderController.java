package com.relay.web;

import com.relay.domain.Enums.*;
import com.relay.domain.User;
import com.relay.service.ClassifierService;
import com.relay.service.PaymentService;
import com.relay.service.WorkOrderService;
import com.relay.web.Dtos.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {
    private final WorkOrderService service;
    private final PaymentService payments;
    private final ClassifierService classifier;

    public WorkOrderController(WorkOrderService service, PaymentService payments, ClassifierService classifier) {
        this.service = service;
        this.payments = payments;
        this.classifier = classifier;
    }

    @GetMapping
    List<WorkOrderView> list(@AuthenticationPrincipal User user, @RequestParam(required = false) Status status) {
        return service.list(user, status);
    }

    @GetMapping("/{id}")
    WorkOrderDetail detail(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return service.detail(user, id);
    }

    @PostMapping("/classify")
    Suggestion classify(@Valid @RequestBody ClassifyReq req) { return classifier.classify(req.text()); }

    @PreAuthorize("hasRole('CITIZEN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    WorkOrderView create(@AuthenticationPrincipal User user,
                         @RequestParam String title, @RequestParam String description,
                         @RequestParam(required = false) Category category,
                         @RequestParam(required = false) Severity severity,
                         @RequestParam(required = false) String area, @RequestParam(required = false) String address,
                         @RequestParam(required = false) Double latitude, @RequestParam(required = false) Double longitude,
                         @RequestPart(required = false) MultipartFile photo) {
        if (title.isBlank() || description.isBlank()) throw ApiException.bad("Title and description are required");
        return service.create(user, title, description, category, severity, area, address, latitude, longitude, photo);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/assign")
    WorkOrderView assign(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody AssignReq req) {
        return service.assign(user, id, req.contractorId());
    }

    @PreAuthorize("hasAnyRole('CONTRACTOR','ADMIN')")
    @PostMapping(value = "/{id}/status", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    WorkOrderView status(@AuthenticationPrincipal User user, @PathVariable Long id, @RequestParam Status status,
                         @RequestParam(required = false) String note, @RequestPart(required = false) MultipartFile photo) {
        return service.updateStatus(user, id, status, note, photo);
    }

    @PreAuthorize("hasRole('CITIZEN')")
    @PostMapping("/{id}/rate")
    WorkOrderView rate(@AuthenticationPrincipal User user, @PathVariable Long id, @Valid @RequestBody RateReq req) {
        return service.rate(user, id, req.rating(), req.feedback());
    }

    @PreAuthorize("hasRole('CITIZEN')")
    @PostMapping("/{id}/fast-track")
    CheckoutRes fastTrack(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return payments.startFastTrack(user, id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/release-payment")
    PaymentView release(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return payments.releasePayout(user, id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/fine")
    PaymentView fine(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return payments.issueFine(user, id);
    }
}
