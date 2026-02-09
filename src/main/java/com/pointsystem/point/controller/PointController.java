package com.pointsystem.point.controller;

import com.pointsystem.point.controller.dto.*;
import com.pointsystem.point.domain.entity.PointGrant;
import com.pointsystem.point.domain.entity.PointSpend;
import com.pointsystem.point.service.PointGrantService;
import com.pointsystem.point.service.PointSpendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "포인트 API", description = "포인트 적립/사용/취소 관련 API")
@RestController
@RequestMapping("/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointGrantService pointGrantService;
    private final PointSpendService pointSpendService;

    @Operation(summary = "포인트 적립", description = "고객에게 포인트를 적립합니다. 수기(MANUAL)/시스템(SYSTEM) 지급을 구분합니다.")
    @PostMapping("/grants")
    public ResponseEntity<PointGrantResponse> createGrant(@Valid @RequestBody PointGrantRequest request) {
        PointGrant grant = pointGrantService.grantPoint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PointGrantResponse.from(grant));
    }

    @Operation(summary = "적립 취소", description = "미사용 적립건을 취소합니다. 일부라도 사용된 경우 취소할 수 없습니다.")
    @PostMapping("/grants/{grantId}/cancellations")
    public ResponseEntity<PointGrantResponse> cancelGrant(@PathVariable String grantId) {
        PointGrant grant = pointGrantService.cancelPointGrant(grantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(PointGrantResponse.from(grant));
    }

    @Operation(summary = "포인트 사용", description = "주문 시 포인트를 사용합니다. MANUAL 우선, 만료 임박순으로 차감됩니다.")
    @PostMapping("/spends")
    public ResponseEntity<PointSpendResponse> createSpend(@Valid @RequestBody PointSpendRequest request) {
        PointSpend spend = pointSpendService.spendPoint(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PointSpendResponse.from(spend));
    }

    @Operation(summary = "사용 취소", description = "포인트 사용을 전체 또는 부분 취소합니다. 만료된 적립은 신규 적립으로 복원됩니다.")
    @PostMapping("/spends/{spendId}/cancellations")
    public ResponseEntity<PointSpendCancelResponse> cancelSpend(
            @PathVariable String spendId,
            @Valid @RequestBody PointSpendCancelRequest request) {
        PointSpendCancelResult result = pointSpendService.cancelSpend(spendId, request.cancelAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(PointSpendCancelResponse.from(result));
    }
}
