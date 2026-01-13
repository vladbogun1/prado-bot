package com.bogun.prado_bot.story.editor;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Service
public class StoryEditorService {

    private final NamedParameterJdbcTemplate jdbc;

    public StoryEditorService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<StoryCampaignDto> listCampaigns() {
        var sql = """
                SELECT campaign_key, name, description, start_node_key, cooldown_minutes
                FROM story_campaign
                ORDER BY name
                """;
        return jdbc.query(sql, Map.of(), (rs, rowNum) -> StoryCampaignDto.builder()
                .campaignKey(rs.getString("campaign_key"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .startNodeKey(rs.getString("start_node_key"))
                .cooldownMinutes(rs.getInt("cooldown_minutes"))
                .build());
    }

    public StoryGraphDto loadGraph(String campaignKey) {
        var nodes = jdbc.query("""
                        SELECT campaign_key, node_key, title, variants_json, auto_effects_json,
                               is_terminal, terminal_type, reward_json
                        FROM story_node
                        WHERE campaign_key = :campaignKey
                        ORDER BY node_key
                        """,
                Map.of("campaignKey", campaignKey),
                (rs, rowNum) -> StoryNodeDto.builder()
                        .campaignKey(rs.getString("campaign_key"))
                        .nodeKey(rs.getString("node_key"))
                        .title(rs.getString("title"))
                        .variantsJson(rs.getString("variants_json"))
                        .autoEffectsJson(rs.getString("auto_effects_json"))
                        .terminal(rs.getBoolean("is_terminal"))
                        .terminalType(rs.getString("terminal_type"))
                        .rewardJson(rs.getString("reward_json"))
                        .build()
        );
        var choices = jdbc.query("""
                        SELECT campaign_key, node_key, choice_key, label, sort_order,
                               conditions_json, check_json, success_node_key, fail_node_key,
                               success_text, fail_text, success_effects_json, fail_effects_json
                        FROM story_choice
                        WHERE campaign_key = :campaignKey
                        ORDER BY node_key, sort_order
                        """,
                Map.of("campaignKey", campaignKey),
                (rs, rowNum) -> StoryChoiceDto.builder()
                        .campaignKey(rs.getString("campaign_key"))
                        .nodeKey(rs.getString("node_key"))
                        .choiceKey(rs.getString("choice_key"))
                        .label(rs.getString("label"))
                        .sortOrder(rs.getInt("sort_order"))
                        .conditionsJson(rs.getString("conditions_json"))
                        .checkJson(rs.getString("check_json"))
                        .successNodeKey(rs.getString("success_node_key"))
                        .failNodeKey(rs.getString("fail_node_key"))
                        .successText(rs.getString("success_text"))
                        .failText(rs.getString("fail_text"))
                        .successEffectsJson(rs.getString("success_effects_json"))
                        .failEffectsJson(rs.getString("fail_effects_json"))
                        .build()
        );
        return StoryGraphDto.builder()
                .nodes(nodes)
                .choices(choices)
                .build();
    }

    @Transactional
    public StoryGraphDto replaceGraph(String campaignKey, StoryGraphDto graph) {
        jdbc.update("DELETE FROM story_choice WHERE campaign_key = :campaignKey",
                Map.of("campaignKey", campaignKey));
        jdbc.update("DELETE FROM story_node WHERE campaign_key = :campaignKey",
                Map.of("campaignKey", campaignKey));

        if (graph.getNodes() != null) {
            for (var node : graph.getNodes()) {
                createNode(campaignKey, node);
            }
        }
        if (graph.getChoices() != null) {
            for (var choice : graph.getChoices()) {
                createChoice(campaignKey, choice);
            }
        }
        return loadGraph(campaignKey);
    }

    public StoryNodeDto createNode(String campaignKey, StoryNodeDto node) {
        var sql = """
                INSERT INTO story_node (campaign_key, node_key, title, variants_json, auto_effects_json,
                                        is_terminal, terminal_type, reward_json)
                VALUES (:campaignKey, :nodeKey, :title, :variantsJson, :autoEffectsJson,
                        :terminal, :terminalType, :rewardJson)
                """;
        var params = new MapSqlParameterSource()
                .addValue("campaignKey", campaignKey)
                .addValue("nodeKey", node.getNodeKey())
                .addValue("title", node.getTitle())
                .addValue("variantsJson", node.getVariantsJson())
                .addValue("autoEffectsJson", node.getAutoEffectsJson())
                .addValue("terminal", node.isTerminal())
                .addValue("terminalType", node.getTerminalType())
                .addValue("rewardJson", node.getRewardJson());
        jdbc.update(sql, params);
        node.setCampaignKey(campaignKey);
        return node;
    }

    public StoryNodeDto updateNode(String campaignKey, String nodeKey, StoryNodeDto node) {
        var sql = """
                UPDATE story_node
                SET title = :title,
                    variants_json = :variantsJson,
                    auto_effects_json = :autoEffectsJson,
                    is_terminal = :terminal,
                    terminal_type = :terminalType,
                    reward_json = :rewardJson
                WHERE campaign_key = :campaignKey AND node_key = :nodeKey
                """;
        var params = new MapSqlParameterSource()
                .addValue("campaignKey", campaignKey)
                .addValue("nodeKey", nodeKey)
                .addValue("title", node.getTitle())
                .addValue("variantsJson", node.getVariantsJson())
                .addValue("autoEffectsJson", node.getAutoEffectsJson())
                .addValue("terminal", node.isTerminal())
                .addValue("terminalType", node.getTerminalType())
                .addValue("rewardJson", node.getRewardJson());
        jdbc.update(sql, params);
        node.setCampaignKey(campaignKey);
        node.setNodeKey(nodeKey);
        return node;
    }

    @Transactional
    public void deleteNode(String campaignKey, String nodeKey) {
        var incomingCount = jdbc.queryForObject("""
                        SELECT COUNT(*)
                        FROM story_choice
                        WHERE campaign_key = :campaignKey
                          AND (success_node_key = :nodeKey OR fail_node_key = :nodeKey)
                          AND node_key <> :nodeKey
                        """,
                Map.of("campaignKey", campaignKey, "nodeKey", nodeKey),
                Integer.class);
        if (incomingCount != null && incomingCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Нельзя удалить узел: на него ссылаются другие выборы.");
        }
        jdbc.update("""
                        DELETE FROM story_choice
                        WHERE campaign_key = :campaignKey AND node_key = :nodeKey
                        """,
                Map.of("campaignKey", campaignKey, "nodeKey", nodeKey));
        jdbc.update("""
                        DELETE FROM story_node
                        WHERE campaign_key = :campaignKey AND node_key = :nodeKey
                        """,
                Map.of("campaignKey", campaignKey, "nodeKey", nodeKey));
    }

    public StoryChoiceDto createChoice(String campaignKey, StoryChoiceDto choice) {
        var sql = """
                INSERT INTO story_choice (campaign_key, node_key, choice_key, label, sort_order,
                                          conditions_json, check_json, success_node_key, fail_node_key,
                                          success_text, fail_text, success_effects_json, fail_effects_json)
                VALUES (:campaignKey, :nodeKey, :choiceKey, :label, :sortOrder,
                        :conditionsJson, :checkJson, :successNodeKey, :failNodeKey,
                        :successText, :failText, :successEffectsJson, :failEffectsJson)
                """;
        var params = new MapSqlParameterSource()
                .addValue("campaignKey", campaignKey)
                .addValue("nodeKey", choice.getNodeKey())
                .addValue("choiceKey", choice.getChoiceKey())
                .addValue("label", choice.getLabel())
                .addValue("sortOrder", choice.getSortOrder())
                .addValue("conditionsJson", choice.getConditionsJson())
                .addValue("checkJson", choice.getCheckJson())
                .addValue("successNodeKey", choice.getSuccessNodeKey())
                .addValue("failNodeKey", choice.getFailNodeKey())
                .addValue("successText", choice.getSuccessText())
                .addValue("failText", choice.getFailText())
                .addValue("successEffectsJson", choice.getSuccessEffectsJson())
                .addValue("failEffectsJson", choice.getFailEffectsJson());
        jdbc.update(sql, params);
        choice.setCampaignKey(campaignKey);
        return choice;
    }

    public StoryChoiceDto updateChoice(String campaignKey, String choiceKey, StoryChoiceDto choice) {
        var sql = """
                UPDATE story_choice
                SET node_key = :nodeKey,
                    label = :label,
                    sort_order = :sortOrder,
                    conditions_json = :conditionsJson,
                    check_json = :checkJson,
                    success_node_key = :successNodeKey,
                    fail_node_key = :failNodeKey,
                    success_text = :successText,
                    fail_text = :failText,
                    success_effects_json = :successEffectsJson,
                    fail_effects_json = :failEffectsJson
                WHERE campaign_key = :campaignKey AND choice_key = :choiceKey
                """;
        var params = new MapSqlParameterSource()
                .addValue("campaignKey", campaignKey)
                .addValue("choiceKey", choiceKey)
                .addValue("nodeKey", choice.getNodeKey())
                .addValue("label", choice.getLabel())
                .addValue("sortOrder", choice.getSortOrder())
                .addValue("conditionsJson", choice.getConditionsJson())
                .addValue("checkJson", choice.getCheckJson())
                .addValue("successNodeKey", choice.getSuccessNodeKey())
                .addValue("failNodeKey", choice.getFailNodeKey())
                .addValue("successText", choice.getSuccessText())
                .addValue("failText", choice.getFailText())
                .addValue("successEffectsJson", choice.getSuccessEffectsJson())
                .addValue("failEffectsJson", choice.getFailEffectsJson());
        jdbc.update(sql, params);
        choice.setCampaignKey(campaignKey);
        choice.setChoiceKey(choiceKey);
        return choice;
    }

    public void deleteChoice(String campaignKey, String choiceKey) {
        jdbc.update("""
                        DELETE FROM story_choice
                        WHERE campaign_key = :campaignKey AND choice_key = :choiceKey
                        """,
                Map.of("campaignKey", campaignKey, "choiceKey", choiceKey));
    }
}
