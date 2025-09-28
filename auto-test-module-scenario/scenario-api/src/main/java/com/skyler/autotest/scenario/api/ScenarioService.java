package com.skyler.autotest.scenario.api;

import com.skyler.autotest.scenario.api.dto.ScenarioCreateRequest;
import com.skyler.autotest.scenario.api.dto.ScenarioPublishRequest;
import com.skyler.autotest.scenario.api.dto.ScenarioResponse;

import java.util.List;

public interface ScenarioService {

    Long createScenario(Long tenantId, ScenarioCreateRequest request);

    void updateScenario(Long scenarioId, ScenarioCreateRequest request);

    void publishScenario(Long scenarioId, ScenarioPublishRequest request);

    ScenarioResponse getScenario(Long scenarioId);

    List<ScenarioResponse> listByProject(Long projectId);
}
