package com.bogun.prado_bot.story;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class StoryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public StoryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<StoryCampaign> findCampaign(String campaignKey) {
        var sql = """
                SELECT campaign_key, name, description, start_node_key
                FROM story_campaign
                WHERE campaign_key = :campaignKey
                """;
        var params = Map.of("campaignKey", campaignKey);
        var rows = jdbc.query(sql, params, (rs, rowNum) -> new StoryCampaign(
                rs.getString("campaign_key"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("start_node_key")
        ));
        return rows.stream().findFirst();
    }

    public Optional<StoryNode> findNode(String campaignKey, String nodeKey) {
        var sql = """
                SELECT campaign_key, node_key, title, variants_json, auto_effects_json,
                       is_terminal, terminal_type, reward_json
                FROM story_node
                WHERE campaign_key = :campaignKey AND node_key = :nodeKey
                """;
        var params = Map.of("campaignKey", campaignKey, "nodeKey", nodeKey);
        var rows = jdbc.query(sql, params, (rs, rowNum) -> new StoryNode(
                rs.getString("campaign_key"),
                rs.getString("node_key"),
                rs.getString("title"),
                rs.getString("variants_json"),
                rs.getString("auto_effects_json"),
                rs.getBoolean("is_terminal"),
                rs.getString("terminal_type"),
                rs.getString("reward_json")
        ));
        return rows.stream().findFirst();
    }

    public List<StoryChoice> findChoices(String campaignKey, String nodeKey) {
        var sql = """
                SELECT campaign_key, node_key, choice_key, label, sort_order,
                       conditions_json, check_json, success_node_key, fail_node_key,
                       success_text, fail_text, success_effects_json, fail_effects_json
                FROM story_choice
                WHERE campaign_key = :campaignKey AND node_key = :nodeKey
                ORDER BY sort_order ASC
                """;
        var params = Map.of("campaignKey", campaignKey, "nodeKey", nodeKey);
        return jdbc.query(sql, params, (rs, rowNum) -> new StoryChoice(
                rs.getString("campaign_key"),
                rs.getString("node_key"),
                rs.getString("choice_key"),
                rs.getString("label"),
                rs.getInt("sort_order"),
                rs.getString("conditions_json"),
                rs.getString("check_json"),
                rs.getString("success_node_key"),
                rs.getString("fail_node_key"),
                rs.getString("success_text"),
                rs.getString("fail_text"),
                rs.getString("success_effects_json"),
                rs.getString("fail_effects_json")
        ));
    }

    public void validateCampaign(String campaignKey) {
        var campaign = findCampaign(campaignKey)
                .orElseThrow(() -> new IllegalStateException("Campaign not found: " + campaignKey));

        var nodeCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM story_node WHERE campaign_key = :campaignKey AND node_key = :nodeKey",
                Map.of("campaignKey", campaignKey, "nodeKey", campaign.startNodeKey()),
                Integer.class
        );
        if (nodeCount == null || nodeCount == 0) {
            throw new IllegalStateException("Start node missing for campaign " + campaignKey);
        }

        var sql = """
                SELECT c.choice_key
                FROM story_choice c
                LEFT JOIN story_node n1
                  ON n1.campaign_key = c.campaign_key AND n1.node_key = c.success_node_key
                LEFT JOIN story_node n2
                  ON n2.campaign_key = c.campaign_key AND n2.node_key = c.fail_node_key
                WHERE c.campaign_key = :campaignKey
                  AND (n1.node_key IS NULL OR n2.node_key IS NULL)
                """;
        var missing = jdbc.query(sql, Map.of("campaignKey", campaignKey), (rs, rowNum) -> rs.getString("choice_key"));
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing nodes for choices: " + String.join(", ", missing));
        }
    }
}
