/*
 * Copyright (c) 2025 Supatuck. All rights reserved.
 * Licensed under the MIT License.
 */
package sh.pcx.unified.content.impl;

import sh.pcx.unified.content.advancement.*;
import sh.pcx.unified.content.advancement.structure.AdvancementDisplay;
import sh.pcx.unified.content.advancement.structure.AdvancementParent;
import sh.pcx.unified.player.UnifiedPlayer;
import sh.pcx.unified.content.advancement.TriggerData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AdvancementService} providing custom advancement
 * creation, registration, progress tracking, and reward distribution.
 *
 * <p>This implementation uses thread-safe data structures for concurrent
 * access and supports Java 21 features including records and pattern matching.
 *
 * @since 1.0.0
 * @author Supatuck
 */
public class AdvancementServiceImpl implements AdvancementService {

    private final Map<String, CustomAdvancementImpl> advancements = new ConcurrentHashMap<>();
    private final Map<String, CustomTriggerImpl> triggers = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerAdvancementData> playerData = new ConcurrentHashMap<>();

    @Override
    @NotNull
    public AdvancementBuilder create(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        validateKey(key);
        return new AdvancementBuilderImpl(key, this);
    }

    @Override
    @NotNull
    public Optional<CustomAdvancement> get(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return Optional.ofNullable(advancements.get(key));
    }

    @Override
    @NotNull
    public Collection<CustomAdvancement> getAll() {
        return Collections.unmodifiableCollection(new ArrayList<>(advancements.values()));
    }

    @Override
    public boolean unregister(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        return advancements.remove(key) != null;
    }

    @Override
    @NotNull
    public CustomTrigger createTrigger(@NotNull String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        validateKey(key);
        return triggers.computeIfAbsent(key, k -> new CustomTriggerImpl(k, this));
    }

    @Override
    public boolean grant(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement advancement) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(advancement, "Advancement cannot be null");

        PlayerAdvancementData data = getPlayerData(player);
        if (data.completedAdvancements.contains(advancement.getKey())) {
            return false;
        }

        // Grant all criteria
        for (String criterion : advancement.getCriteria().keySet()) {
            grantCriteria(player, advancement, criterion);
        }

        // Mark as completed and grant rewards
        data.completedAdvancements.add(advancement.getKey());
        advancement.getReward().ifPresent(reward -> reward.grant(player));

        return true;
    }

    @Override
    public boolean grant(@NotNull UnifiedPlayer player, @NotNull String key) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");

        CustomAdvancement advancement = advancements.get(key);
        if (advancement == null) {
            return false;
        }
        return grant(player, advancement);
    }

    @Override
    public boolean grantCriteria(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement advancement,
                                  @NotNull String criterion) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(advancement, "Advancement cannot be null");
        Objects.requireNonNull(criterion, "Criterion cannot be null");

        if (!advancement.getCriteria().containsKey(criterion)) {
            return false;
        }

        PlayerAdvancementData data = getPlayerData(player);
        String advKey = advancement.getKey();

        Map<String, Integer> criteriaProgress = data.criteriaProgress.computeIfAbsent(
                advKey, _ -> new ConcurrentHashMap<>());

        Trigger trigger = advancement.getCriteria().get(criterion);
        int required = getRequiredCount(trigger);
        int current = criteriaProgress.getOrDefault(criterion, 0);

        if (current >= required) {
            return false;
        }

        criteriaProgress.put(criterion, required);

        // Check if advancement is now complete
        checkAdvancementCompletion(player, advancement);

        return true;
    }

    @Override
    public boolean revoke(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement advancement) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(advancement, "Advancement cannot be null");

        PlayerAdvancementData data = getPlayerData(player);
        String key = advancement.getKey();

        boolean wasCompleted = data.completedAdvancements.remove(key);
        data.criteriaProgress.remove(key);

        return wasCompleted;
    }

    @Override
    public boolean revoke(@NotNull UnifiedPlayer player, @NotNull String key) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");

        CustomAdvancement advancement = advancements.get(key);
        if (advancement == null) {
            return false;
        }
        return revoke(player, advancement);
    }

    @Override
    public int revokeAll(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement root) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(root, "Root advancement cannot be null");

        Set<String> toRevoke = new HashSet<>();
        collectChildren(root.getKey(), toRevoke);
        toRevoke.add(root.getKey());

        int count = 0;
        for (String key : toRevoke) {
            if (revoke(player, key)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean has(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement advancement) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(advancement, "Advancement cannot be null");

        PlayerAdvancementData data = getPlayerData(player);
        return data.completedAdvancements.contains(advancement.getKey());
    }

    @Override
    public boolean has(@NotNull UnifiedPlayer player, @NotNull String key) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");

        PlayerAdvancementData data = getPlayerData(player);
        return data.completedAdvancements.contains(key);
    }

    @Override
    public int getProgress(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement advancement,
                           @NotNull String criterion) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(advancement, "Advancement cannot be null");
        Objects.requireNonNull(criterion, "Criterion cannot be null");

        PlayerAdvancementData data = getPlayerData(player);
        Map<String, Integer> criteriaProgress = data.criteriaProgress.get(advancement.getKey());
        if (criteriaProgress == null) {
            return 0;
        }
        return criteriaProgress.getOrDefault(criterion, 0);
    }

    @Override
    public int getRequired(@NotNull CustomAdvancement advancement, @NotNull String criterion) {
        Objects.requireNonNull(advancement, "Advancement cannot be null");
        Objects.requireNonNull(criterion, "Criterion cannot be null");

        Trigger trigger = advancement.getCriteria().get(criterion);
        if (trigger == null) {
            return 0;
        }
        return getRequiredCount(trigger);
    }

    @Override
    public void setProgress(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement advancement,
                            @NotNull String criterion, int progress) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(advancement, "Advancement cannot be null");
        Objects.requireNonNull(criterion, "Criterion cannot be null");

        PlayerAdvancementData data = getPlayerData(player);
        Map<String, Integer> criteriaProgress = data.criteriaProgress.computeIfAbsent(
                advancement.getKey(), _ -> new ConcurrentHashMap<>());

        criteriaProgress.put(criterion, Math.max(0, progress));
        checkAdvancementCompletion(player, advancement);
    }

    @Override
    public int incrementProgress(@NotNull UnifiedPlayer player, @NotNull CustomAdvancement advancement,
                                  @NotNull String criterion, int amount) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(advancement, "Advancement cannot be null");
        Objects.requireNonNull(criterion, "Criterion cannot be null");

        int current = getProgress(player, advancement, criterion);
        int newProgress = current + amount;
        setProgress(player, advancement, criterion, newProgress);
        return newProgress;
    }

    @Override
    @NotNull
    public Set<CustomAdvancement> getCompleted(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");

        PlayerAdvancementData data = getPlayerData(player);
        return data.completedAdvancements.stream()
                .map(advancements::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @NotNull
    public Set<CustomAdvancement> getInProgress(@NotNull UnifiedPlayer player) {
        Objects.requireNonNull(player, "Player cannot be null");

        PlayerAdvancementData data = getPlayerData(player);
        return data.criteriaProgress.keySet().stream()
                .filter(key -> !data.completedAdvancements.contains(key))
                .map(advancements::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void reload() {
        // Re-register all advancements with the server
        // Platform-specific implementation would handle this
    }

    // === Internal Methods ===

    void registerAdvancement(CustomAdvancementImpl advancement) {
        advancements.put(advancement.getKey(), advancement);
    }

    void handleTrigger(UnifiedPlayer player, String triggerKey, TriggerData data) {
        for (CustomAdvancementImpl advancement : advancements.values()) {
            for (Map.Entry<String, Trigger> entry : advancement.getCriteria().entrySet()) {
                String criterionName = entry.getKey();
                Trigger trigger = entry.getValue();

                if (matchesTrigger(trigger, triggerKey, data)) {
                    incrementProgress(player, advancement, criterionName, 1);
                }
            }
        }
    }

    private boolean matchesTrigger(Trigger trigger, String triggerKey, TriggerData data) {
        if (trigger instanceof CustomTriggerMatcher matcher) {
            return matcher.getKey().equals(triggerKey) && matcher.matches(data);
        }
        if (trigger instanceof CountingTrigger counting) {
            return matchesTrigger(counting.delegate(), triggerKey, data);
        }
        return trigger.getType().equals(triggerKey);
    }

    private void checkAdvancementCompletion(UnifiedPlayer player, CustomAdvancement advancement) {
        if (has(player, advancement)) {
            return;
        }

        PlayerAdvancementData data = getPlayerData(player);
        Map<String, Integer> criteriaProgress = data.criteriaProgress.get(advancement.getKey());
        if (criteriaProgress == null) {
            return;
        }

        // Check requirements
        List<List<String>> requirements = advancement.getRequirements();
        boolean completed = checkRequirements(advancement, criteriaProgress, requirements);

        if (completed) {
            data.completedAdvancements.add(advancement.getKey());
            advancement.getReward().ifPresent(reward -> reward.grant(player));
        }
    }

    private boolean checkRequirements(CustomAdvancement advancement, Map<String, Integer> progress,
                                       List<List<String>> requirements) {
        if (requirements.isEmpty()) {
            // Default: all criteria required
            for (Map.Entry<String, Trigger> entry : advancement.getCriteria().entrySet()) {
                int required = getRequiredCount(entry.getValue());
                int current = progress.getOrDefault(entry.getKey(), 0);
                if (current < required) {
                    return false;
                }
            }
            return true;
        }

        // OR of ANDs
        for (List<String> orGroup : requirements) {
            boolean groupComplete = true;
            for (String criterion : orGroup) {
                Trigger trigger = advancement.getCriteria().get(criterion);
                if (trigger == null) {
                    groupComplete = false;
                    break;
                }
                int required = getRequiredCount(trigger);
                int current = progress.getOrDefault(criterion, 0);
                if (current < required) {
                    groupComplete = false;
                    break;
                }
            }
            if (groupComplete) {
                return true;
            }
        }
        return false;
    }

    private int getRequiredCount(Trigger trigger) {
        if (trigger instanceof CountingTrigger counting) {
            return counting.count();
        }
        return 1;
    }

    private void collectChildren(String parentKey, Set<String> collected) {
        for (CustomAdvancementImpl advancement : advancements.values()) {
            advancement.getParent().ifPresent(parent -> {
                parent.getKey().ifPresent(key -> {
                    if (key.equals(parentKey)) {
                        collected.add(advancement.getKey());
                        collectChildren(advancement.getKey(), collected);
                    }
                });
            });
        }
    }

    private PlayerAdvancementData getPlayerData(UnifiedPlayer player) {
        return playerData.computeIfAbsent(player.getUniqueId(), _ -> new PlayerAdvancementData());
    }

    private void validateKey(String key) {
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
        if (!key.contains(":")) {
            throw new IllegalArgumentException("Key must be namespaced (e.g., 'myplugin:advancement_name')");
        }
    }

    // === Inner Classes ===

    /**
     * Stores player advancement data.
     */
    private static class PlayerAdvancementData {
        final Set<String> completedAdvancements = ConcurrentHashMap.newKeySet();
        final Map<String, Map<String, Integer>> criteriaProgress = new ConcurrentHashMap<>();
    }

    /**
     * Implementation of {@link CustomAdvancement}.
     */
    record CustomAdvancementImpl(
            String key,
            AdvancementDisplay display,
            AdvancementParent parent,
            Map<String, Trigger> criteria,
            List<List<String>> requirements,
            AdvancementReward reward,
            boolean showToast,
            boolean announceToChat,
            boolean hidden,
            AdvancementServiceImpl service
    ) implements CustomAdvancement {

        @Override
        @NotNull
        public String getKey() {
            return key;
        }

        @Override
        @NotNull
        public AdvancementDisplay getDisplay() {
            return display;
        }

        @Override
        @NotNull
        public Optional<AdvancementParent> getParent() {
            return Optional.ofNullable(parent);
        }

        @Override
        @NotNull
        public Map<String, Trigger> getCriteria() {
            return Collections.unmodifiableMap(criteria);
        }

        @Override
        @NotNull
        public List<List<String>> getRequirements() {
            return Collections.unmodifiableList(requirements);
        }

        @Override
        @NotNull
        public Optional<AdvancementReward> getReward() {
            return Optional.ofNullable(reward);
        }

        @Override
        public boolean showsToast() {
            return showToast;
        }

        @Override
        public boolean announcesToChat() {
            return announceToChat;
        }

        @Override
        public boolean isHidden() {
            return hidden;
        }

        @Override
        @NotNull
        public AdvancementBuilder toBuilder() {
            AdvancementBuilderImpl builder = new AdvancementBuilderImpl(key, service);
            builder.display(display);
            if (parent != null) {
                builder.parent(parent);
            }
            for (Map.Entry<String, Trigger> entry : criteria.entrySet()) {
                builder.criteria(entry.getKey(), entry.getValue());
            }
            if (!requirements.isEmpty()) {
                builder.requirements(requirements);
            }
            if (reward != null) {
                builder.reward(reward);
            }
            builder.showToast(showToast);
            builder.announceToChat(announceToChat);
            builder.hidden(hidden);
            return builder;
        }
    }

    /**
     * Implementation of {@link AdvancementBuilder}.
     */
    static class AdvancementBuilderImpl implements AdvancementBuilder {

        private final String key;
        private final AdvancementServiceImpl service;
        private AdvancementDisplay display;
        private AdvancementParent parent;
        private final Map<String, Trigger> criteria = new LinkedHashMap<>();
        private List<List<String>> requirements = new ArrayList<>();
        private AdvancementReward reward;
        private Boolean showToast;
        private Boolean announceToChat;
        private boolean hidden = false;

        AdvancementBuilderImpl(String key, AdvancementServiceImpl service) {
            this.key = key;
            this.service = service;
        }

        @Override
        @NotNull
        public AdvancementBuilder display(@NotNull AdvancementDisplay display) {
            this.display = Objects.requireNonNull(display, "Display cannot be null");
            return this;
        }

        @Override
        @NotNull
        public AdvancementBuilder parent(@NotNull AdvancementParent parent) {
            this.parent = Objects.requireNonNull(parent, "Parent cannot be null");
            return this;
        }

        @Override
        @NotNull
        public AdvancementBuilder criteria(@NotNull String name, @NotNull Trigger trigger) {
            Objects.requireNonNull(name, "Criterion name cannot be null");
            Objects.requireNonNull(trigger, "Trigger cannot be null");
            criteria.put(name, trigger);
            return this;
        }

        @Override
        @NotNull
        public AdvancementBuilder requireAll() {
            // All criteria required - default behavior with empty requirements
            requirements = new ArrayList<>();
            return this;
        }

        @Override
        @NotNull
        public AdvancementBuilder requireAny() {
            // Any criterion - each criterion is its own OR group
            requirements = criteria.keySet().stream()
                    .map(List::of)
                    .collect(Collectors.toList());
            return this;
        }

        @Override
        @NotNull
        public AdvancementBuilder requirements(@NotNull List<List<String>> requirements) {
            this.requirements = new ArrayList<>(requirements);
            return this;
        }

        @Override
        @NotNull
        public AdvancementBuilder reward(@NotNull AdvancementReward reward) {
            this.reward = Objects.requireNonNull(reward, "Reward cannot be null");
            return this;
        }

        @Override
        @NotNull
        public AdvancementBuilder showToast(boolean showToast) {
            this.showToast = showToast;
            return this;
        }

        @Override
        @NotNull
        public AdvancementBuilder announceToChat(boolean announce) {
            this.announceToChat = announce;
            return this;
        }

        @Override
        @NotNull
        public AdvancementBuilder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        @Override
        @NotNull
        public CustomAdvancement build() {
            if (display == null) {
                throw new IllegalStateException("Display is required");
            }
            if (criteria.isEmpty()) {
                throw new IllegalStateException("At least one criterion is required");
            }

            boolean effectiveShowToast = showToast != null ? showToast : display.shouldShowToast();
            boolean effectiveAnnounce = announceToChat != null ? announceToChat : display.shouldAnnounceToChat();
            boolean effectiveHidden = hidden || display.isHidden();

            return new CustomAdvancementImpl(
                    key,
                    display,
                    parent,
                    new LinkedHashMap<>(criteria),
                    List.copyOf(requirements),
                    reward,
                    effectiveShowToast,
                    effectiveAnnounce,
                    effectiveHidden,
                    service
            );
        }

        @Override
        @NotNull
        public CustomAdvancement register() {
            CustomAdvancementImpl advancement = (CustomAdvancementImpl) build();
            service.registerAdvancement(advancement);
            return advancement;
        }
    }

    /**
     * Implementation of {@link CustomTrigger}.
     */
    static class CustomTriggerImpl implements CustomTrigger {

        private final String key;
        private final AdvancementServiceImpl service;

        CustomTriggerImpl(String key, AdvancementServiceImpl service) {
            this.key = key;
            this.service = service;
        }

        @Override
        @NotNull
        public String getKey() {
            return key;
        }

        @Override
        @NotNull
        public Trigger matching(@NotNull Predicate<TriggerData> predicate) {
            return new CustomTriggerMatcher(key, predicate);
        }

        @Override
        public void trigger(@NotNull UnifiedPlayer player) {
            trigger(player, TriggerData.empty());
        }

        @Override
        public void trigger(@NotNull UnifiedPlayer player, @NotNull TriggerData data) {
            Objects.requireNonNull(player, "Player cannot be null");
            Objects.requireNonNull(data, "Data cannot be null");
            service.handleTrigger(player, key, data);
        }
    }

    /**
     * A trigger that matches against custom trigger data.
     */
    record CustomTriggerMatcher(
            String key,
            Predicate<TriggerData> predicate
    ) implements Trigger {

        @Override
        @NotNull
        public String getType() {
            return key;
        }

        public String getKey() {
            return key;
        }

        public boolean matches(TriggerData data) {
            return predicate.test(data);
        }
    }
}
