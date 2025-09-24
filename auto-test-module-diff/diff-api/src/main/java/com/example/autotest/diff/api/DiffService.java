package com.example.autotest.diff.api;

import com.example.autotest.diff.api.dto.DiffReviewRequest;
import com.example.autotest.diff.api.dto.DiffSnapshotResponse;

import java.util.List;

public interface DiffService {

    List<DiffSnapshotResponse> listPending(Long projectId);

    DiffSnapshotResponse review(Long diffId, DiffReviewRequest request);
}
