package com.example.autotest.scenario.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.autotest.infra.event.TemplateUpdateEvent;
import com.example.autotest.scenario.dal.dataobject.ScenarioDO;
import com.example.autotest.scenario.dal.dataobject.ScenarioStepDO;
import com.example.autotest.scenario.dal.mapper.ScenarioMapper;
import com.example.autotest.scenario.dal.mapper.ScenarioStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 监听模板变更事件，批量标记引用该模板的场景为需复核。
 */
@Component
@RequiredArgsConstructor
public class TemplateUpdateScenarioListener {

    private final ScenarioStepMapper scenarioStepMapper;
    private final ScenarioMapper scenarioMapper;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleTemplateUpdate(TemplateUpdateEvent event) {
        List<ScenarioStepDO> steps = scenarioStepMapper.selectList(new LambdaQueryWrapper<ScenarioStepDO>()
                .eq(ScenarioStepDO::getCurlVariantId, event.getVariantId()));
        if (steps.isEmpty()) {
            return;
        }
        Set<Long> scenarioIds = steps.stream()
                .map(ScenarioStepDO::getScenarioId)
                .collect(Collectors.toSet());
        if (scenarioIds.isEmpty()) {
            return;
        }
        scenarioMapper.update(null, new LambdaUpdateWrapper<ScenarioDO>()
                .in(ScenarioDO::getId, scenarioIds)
                .set(ScenarioDO::getNeedReview, true));
    }
}
