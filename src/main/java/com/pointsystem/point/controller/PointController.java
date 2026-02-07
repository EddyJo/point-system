package com.pointsystem.point.controller;

import com.pointsystem.point.controller.dto.*;
import com.pointsystem.point.domain.entity.PointGrant;
import com.pointsystem.point.domain.entity.PointSpend;
import com.pointsystem.point.service.PointGrantService;
import com.pointsystem.point.service.PointSpendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointGrantService pointGrantService;
    private final PointSpendService pointSpendService;

    @PostMapping("/grants")
    public ResponseEntity<PointGrantResponse> createGrant(@Valid @RequestBody PointGrantRequest request) {
        PointGrant grant = pointGrantService.grantPoint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PointGrantResponse.from(grant));
    }

    @PostMapping("/grants/{grantId}/cancellations")
    public ResponseEntity<PointGrantResponse> cancelGrant(@PathVariable String grantId) {
        PointGrant grant = pointGrantService.cancelPointGrant(grantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(PointGrantResponse.from(grant));
    }

    @PostMapping("/spends")
    public ResponseEntity<PointSpendResponse> createSpend(@Valid @RequestBody PointSpendRequest request) {
        PointSpend spend = pointSpendService.spendPoint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PointSpendResponse.from(spend));
    }

    @PostMapping("/spends/{spendId}/cancellations")
    public ResponseEntity<PointSpendCancelResponse> cancelSpend(
            @PathVariable String spendId,
            @Valid @RequestBody PointSpendCancelRequest request) {
        PointSpendCancelResult result = pointSpendService.cancelSpend(spendId, request.cancelAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(PointSpendCancelResponse.from(result));
    }
}
