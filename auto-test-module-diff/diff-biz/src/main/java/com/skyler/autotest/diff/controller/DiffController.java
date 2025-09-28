package com.skyler.autotest.diff.controller;

import com.skyler.autotest.diff.api.DiffService;
import com.skyler.autotest.diff.api.dto.DiffReviewRequest;
import com.skyler.autotest.diff.api.dto.DiffSnapshotResponse;
import com.skyler.autotest.infra.core.web.CommonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 差异复核接口。
 */
@RestController
@RequestMapping("/api/diff")
@RequiredArgsConstructor
@Validated
@Tag(name = "差异复核", description = "Swagger/模板差异快照查询与复核")
public class DiffController {

    private final DiffService diffService;

    /**
     * 查询待复核差异列表。
     *
     * @param projectId 项目编号，可选
     * @return 差异列表
     */
    @GetMapping
    @Operation(summary = "查询差异列表", description = "按项目过滤待复核差异快照")
    public CommonResult<List<DiffSnapshotResponse>> list(@Parameter(description = "项目编号，可为空", example = "1001")
                                                         @RequestParam(required = false) Long projectId) {
        return CommonResult.success(diffService.listPending(projectId));
    }

    /**
     * 复核指定差异。
     *
     * @param id      差异快照编号
     * @param request 复核结果
     * @return 最新差异信息
     */
    @PostMapping("/{id}/review")
    @Operation(summary = "复核差异", description = "提交复核结果并更新差异状态")
    public CommonResult<DiffSnapshotResponse> review(@Parameter(description = "差异快照编号", example = "8001001")
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody DiffReviewRequest request) {
        return CommonResult.success(diffService.review(id, request));
    }
}
