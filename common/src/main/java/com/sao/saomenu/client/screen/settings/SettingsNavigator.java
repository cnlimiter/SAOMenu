package com.sao.saomenu.client.screen.settings;

import com.sao.saomenu.api.settings.Setting;
import com.sao.saomenu.api.settings.SettingsGroup;

import java.util.List;

/**
 * Focus, activation target and bounded scroll for the settings screen.
 * Does not own P5 animation; the timeline still gates when input is live.
 */
final class SettingsNavigator {
    private List<SettingsGroup> groups = List.of();
    private boolean root = true;
    private int groupIndex = -1;
    private int optionIndex = 0;
    private int groupScroll = 0;
    private int optionScroll = 0;
    private int visibleGroups = SettingsLayout.VISIBLE_CATS;
    private int visibleOptions = 1;

    void sync(List<SettingsGroup> groups) {
        this.groups = groups == null ? List.of() : groups;
        if (this.groups.isEmpty()) {
            root = true;
            groupIndex = -1;
            optionIndex = 0;
            groupScroll = 0;
            optionScroll = 0;
            return;
        }
        if (!root) {
            groupIndex = Math.min(Math.max(0, groupIndex), this.groups.size() - 1);
        }
        clamp();
    }

    void setVisibleGroups(int visible) {
        this.visibleGroups = Math.max(1, visible);
        clamp();
    }

    void setVisibleOptions(int visible) {
        this.visibleOptions = Math.max(1, visible);
        clamp();
    }

    boolean isRoot() {
        return root;
    }

    int groupIndex() {
        return groupIndex;
    }

    int optionIndex() {
        return optionIndex;
    }

    int groupScroll() {
        return groupScroll;
    }

    int optionScroll() {
        return optionScroll;
    }

    int visibleGroups() {
        return visibleGroups;
    }

    int visibleOptions() {
        return visibleOptions;
    }

    SettingsGroup currentGroup() {
        if (root || groupIndex < 0 || groupIndex >= groups.size()) {
            return null;
        }
        return groups.get(groupIndex);
    }

    Setting currentSetting() {
        SettingsGroup group = currentGroup();
        if (group == null) {
            return null;
        }
        List<Setting> options = group.options();
        if (optionIndex < 0 || optionIndex >= options.size()) {
            return null;
        }
        return options.get(optionIndex);
    }

    void enterGroup(int index) {
        if (groups.isEmpty()) {
            return;
        }
        groupIndex = SettingsLayout.clampScroll(index, groups.size(), 1);
        root = false;
        optionIndex = 0;
        optionScroll = 0;
        ensureGroupVisible();
        clamp();
    }

    void returnToRoot() {
        root = true;
        optionIndex = 0;
        optionScroll = 0;
        if (groupIndex >= 0) {
            ensureGroupVisible();
        }
        clamp();
    }

    void focusGroup(int index) {
        if (groups.isEmpty()) {
            return;
        }
        groupIndex = SettingsLayout.clampScroll(index, groups.size(), 1);
        ensureGroupVisible();
    }

    void focusOption(int index) {
        SettingsGroup group = currentGroup();
        if (group == null) {
            return;
        }
        optionIndex = SettingsLayout.clampScroll(index, group.options().size(), 1);
        ensureOptionVisible();
    }

    boolean moveGroup(int delta) {
        if (groups.isEmpty()) {
            return false;
        }
        int from = groupIndex < 0 ? (delta > 0 ? -1 : groups.size()) : groupIndex;
        int next = SettingsLayout.clampScroll(from + delta, groups.size(), 1);
        if (next == groupIndex) {
            return false;
        }
        groupIndex = next;
        ensureGroupVisible();
        return true;
    }

    boolean moveOption(int delta) {
        SettingsGroup group = currentGroup();
        if (group == null || group.options().isEmpty()) {
            return false;
        }
        int next = SettingsLayout.clampScroll(optionIndex + delta, group.options().size(), 1);
        if (next == optionIndex) {
            return false;
        }
        optionIndex = next;
        ensureOptionVisible();
        return true;
    }

    boolean scrollGroups(int delta) {
        int next = SettingsLayout.clampScroll(groupScroll + delta, groups.size(), visibleGroups);
        if (next == groupScroll) {
            return false;
        }
        groupScroll = next;
        if (groupIndex < groupScroll) {
            groupIndex = groupScroll;
        } else if (groupIndex >= groupScroll + visibleGroups) {
            groupIndex = groupScroll + visibleGroups - 1;
        }
        return true;
    }

    boolean scrollOptions(int delta) {
        SettingsGroup group = currentGroup();
        if (group == null) {
            return false;
        }
        int count = group.options().size();
        int next = SettingsLayout.clampScroll(optionScroll + delta, count, visibleOptions);
        if (next == optionScroll) {
            return false;
        }
        optionScroll = next;
        if (optionIndex < optionScroll) {
            optionIndex = optionScroll;
        } else if (optionIndex >= optionScroll + visibleOptions) {
            optionIndex = optionScroll + visibleOptions - 1;
        }
        return true;
    }

    private void ensureGroupVisible() {
        if (groupIndex < 0) {
            return;
        }
        if (groupIndex < groupScroll) {
            groupScroll = groupIndex;
        } else if (groupIndex >= groupScroll + visibleGroups) {
            groupScroll = groupIndex - visibleGroups + 1;
        }
        groupScroll = SettingsLayout.clampScroll(groupScroll, groups.size(), visibleGroups);
    }

    private void ensureOptionVisible() {
        SettingsGroup group = currentGroup();
        if (group == null) {
            optionScroll = 0;
            return;
        }
        int count = group.options().size();
        if (optionIndex < optionScroll) {
            optionScroll = optionIndex;
        } else if (optionIndex >= optionScroll + visibleOptions) {
            optionScroll = optionIndex - visibleOptions + 1;
        }
        optionScroll = SettingsLayout.clampScroll(optionScroll, count, visibleOptions);
    }

    private void clamp() {
        groupScroll = SettingsLayout.clampScroll(groupScroll, groups.size(), visibleGroups);
        if (groupIndex >= groups.size()) {
            groupIndex = groups.isEmpty() ? -1 : groups.size() - 1;
        }
        SettingsGroup group = currentGroup();
        int count = group == null ? 0 : group.options().size();
        if (optionIndex >= count) {
            optionIndex = Math.max(0, count - 1);
        }
        optionScroll = SettingsLayout.clampScroll(optionScroll, count, visibleOptions);
        ensureGroupVisible();
        ensureOptionVisible();
    }
}
