package com.pointsystem.point.controller;

import com.pointsystem.point.controller.dto.PointGrantRequest;
import com.pointsystem.point.controller.dto.PointGrantResponse;
import com.pointsystem.point.domain.entity.PointGrant;
import com.pointsystem.point.service.PointGrantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/points")
@RequiredArgsConstructor
public class PointController {

    private PointGrantService pointGrantService;

    @PostMapping("/grants")
    public ResponseEntity<PointGrantResponse> createGrant(@Valid @RequestBody PointGrantRequest request) {
        PointGrant grant = pointGrantService.grantPoint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PointGrantResponse.from(grant));
    }

    @PostMapping("/{grantId}/cancellations")
    public ResponseEntity<PointGrantResponse> cancelGrant(@PathVariable String grantId) {
        PointGrant grant = pointGrantService.cancelPointGrant(grantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(PointGrantResponse.from(grant));
    }
}
