package me.gamechampcrafted.normalshops.utils;

import me.gamechampcrafted.normalshops.data.Message;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Pattern;

public class HoverableMessageParametizer extends MessageParametizer {

    private static final String HOVER_FORMAT = "[%s]";

    private final Map<String, BaseComponent[]> hoverParams = new LinkedHashMap<>();
    private final Map<String, String> clickCommands = new LinkedHashMap<>();

    public HoverableMessageParametizer(Message message) {
        super(message);
    }

    public HoverableMessageParametizer putHover(String key, BaseComponent[] components) {
        this.hoverParams.put(key, components);
        return this;
    }

    /**
     * Run when the player clicks the inserted hover segment (same {@code key} as {@link #putHover}).
     * Typically used with {@link ClickEvent.Action#RUN_COMMAND}.
     */
    public HoverableMessageParametizer putClick(String key, String runCommand) {
        if (runCommand != null && !runCommand.isEmpty()) {
            this.clickCommands.put(key, runCommand);
        }
        return this;
    }

    public HoverableMessageParametizer putAllHover(Map<String, BaseComponent[]> hoverParams) {
        this.hoverParams.putAll(hoverParams);
        return this;
    }

    @Override
    public HoverableMessageParametizer setMessage(Message message) {
        super.setMessage(message);
        return this;
    }

    @Override
    protected void send(Player player, boolean sound) {
        message.getType().sendSpigot(player, toComponents(), sound);
    }

    public BaseComponent[] toComponents() {
        List<BaseComponent> parameterizedComponents = new LinkedList<>();
        for (BaseComponent component : TextComponent.fromLegacyText(toString())) {
            expandFully(parameterizedComponents, component);
        }
        return parameterizedComponents.toArray(new BaseComponent[0]);
    }

    /**
     * Replaces placeholders until none remain, so messages can contain several distinct hover keys.
     */
    private void expandFully(List<BaseComponent> out, BaseComponent component) {
        String text = component.toLegacyText();
        for (Map.Entry<String, BaseComponent[]> entry : hoverParams.entrySet()) {
            String placeholder = String.format(HOVER_FORMAT, entry.getKey());
            if (text.contains(placeholder)) {
                String[] parts = text.split(Pattern.quote(placeholder), -1);
                String clickCmd = clickCommands.get(entry.getKey());
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) {
                        for (BaseComponent hoverPart : entry.getValue()) {
                            BaseComponent copy = hoverPart.duplicate();
                            if (clickCmd != null && !clickCmd.isEmpty()) {
                                copy.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, clickCmd));
                            }
                            out.add(copy);
                        }
                    }
                    if (!parts[i].isEmpty()) {
                        for (BaseComponent sub : TextComponent.fromLegacyText(parts[i])) {
                            expandFully(out, sub);
                        }
                    }
                }
                return;
            }
        }
        out.add(component);
    }
}
