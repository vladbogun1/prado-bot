package com.bogun.prado_bot.story.editor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/story")
public class StoryEditorController {

    private final StoryEditorService service;

    public StoryEditorController(StoryEditorService service) {
        this.service = service;
    }

    @GetMapping("/campaigns")
    public List<StoryCampaignDto> campaigns() {
        return service.listCampaigns();
    }

    @GetMapping("/{campaignKey}/graph")
    public StoryGraphDto graph(@PathVariable String campaignKey) {
        return service.loadGraph(campaignKey);
    }

    @PostMapping("/{campaignKey}/nodes")
    public StoryNodeDto createNode(@PathVariable String campaignKey, @RequestBody StoryNodeDto node) {
        return service.createNode(campaignKey, node);
    }

    @PutMapping("/{campaignKey}/nodes/{nodeKey}")
    public StoryNodeDto updateNode(@PathVariable String campaignKey,
                                   @PathVariable String nodeKey,
                                   @RequestBody StoryNodeDto node) {
        return service.updateNode(campaignKey, nodeKey, node);
    }

    @PostMapping("/{campaignKey}/choices")
    public StoryChoiceDto createChoice(@PathVariable String campaignKey, @RequestBody StoryChoiceDto choice) {
        return service.createChoice(campaignKey, choice);
    }

    @PutMapping("/{campaignKey}/choices/{choiceKey}")
    public StoryChoiceDto updateChoice(@PathVariable String campaignKey,
                                       @PathVariable String choiceKey,
                                       @RequestBody StoryChoiceDto choice) {
        return service.updateChoice(campaignKey, choiceKey, choice);
    }
}
