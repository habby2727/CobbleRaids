package com.kingpixel.cobbleraids.events;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.events.models.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.WebHookData;
import com.kingpixel.cobbleutils.events.EventChannel;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Carlos Varas Alonso - 13/09/2025 18:51
 */
@Data
public class RaidEvents {
  public static final EventChannel<RaidFinished> RAID_FINISHED = new EventChannel<>();
  public static final EventChannel<RaidPostStarted> RAID_STARTED_POST = new EventChannel<>();
  public static final EventChannel<RaidPreStarted> RAID_STARTED_PRE = new EventChannel<>();
  public static final EventChannel<RaidKillFight> RAID_KILL_FIGHT = new EventChannel<>();
  public static final EventChannel<RaidNewPhase> RAID_NEW_PHASE = new EventChannel<>();

  public static void register() {
    // Raid Finished event
    RAID_FINISHED.subscribe(raidFinished -> {
      try {
        CobbleRaids.language.getTitleEndRaid().send(null, raidFinished.getRaid());
        var webHook = CobbleRaids.config.getWebhook();
        if (!webHook.isENABLED()) return;
        var client = getWebhookClient(webHook);
        if (client == null) return;
        var raid = raidFinished.getRaid();
        var killed = raidFinished.isKilled();
        List<WebhookEmbed> embeds = new ArrayList<>();
        var builder = new WebhookEmbedBuilder()
          .setTitle(new WebhookEmbed.EmbedTitle("Raid Finished", ""));
        AtomicReference<StringBuilder> desc = new AtomicReference<StringBuilder>(new StringBuilder("Table of " +
          "participants:\n"));
        AtomicReference<Integer> amount = new AtomicReference<>(0);
        raid.getDamageMap().entrySet().stream()
          .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
          .forEach(entry -> {
            if (amount.get() >= 10) return;
            var uuid = entry.getKey();
            var userCache = CobbleRaids.server.getUserCache();
            if (userCache == null) return;
            var player = userCache.getByUuid(uuid);
            if (player.isEmpty()) return;
            amount.getAndSet(amount.get() + 1);
            var name = player.get().getName();
            desc.get().append(amount.get()).append(" - ").append(name).append(": ").append(entry.getValue()).append(
              " " +
                "damage\n");
          });
        builder.setDescription(String.valueOf(desc.get()));
        builder.setTimestamp(Instant.now());

        embeds.add(builder.build());
        WebhookMessage webhookMessage = WebhookMessage.embeds(
          embeds
        );
        client.send(webhookMessage);
      } catch (Exception e) {
        CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Error sending raid finished webhook: " + e.getMessage());
        e.printStackTrace();
      }
    });

    // Raid Pre Started event
    RAID_STARTED_PRE.subscribe(raidPreStarted -> {
      try {
        CobbleRaids.language.getTitlePreStartRaid().send(null, raidPreStarted.getRaid());
        var webHook = CobbleRaids.config.getWebhook();
        if (!webHook.isENABLED()) return;
        var client = getWebhookClient(webHook);
        if (client == null) return;
        var raid = raidPreStarted.getRaid();
        List<WebhookEmbed> embeds = new ArrayList<>();
        var builder = new WebhookEmbedBuilder()
          .setTitle(new WebhookEmbed.EmbedTitle("Raid Starting Soon", ""));
        String desc = "A new raid is starting soon!\n" +
          " - Raid ID: " + raid.getRaidUUID() + "\n" +
          " - Category: " + raid.getCategoryRaid().getName() + "\n" +
          " - Level: " + raid.getCategoryRaid().getOverLevel() + "\n" +
          " - MinLevel: " + raid.getCategoryRaid().getMinLevel() + "\n" +
          " - Health Increment: " + raid.getHealth() + "\n" +
          " - Pokemon: " + raid.getPokemon().getDisplayName().getString() + "\n";
        builder.setDescription(desc);
        embeds.add(builder.build());
        WebhookMessage webhookMessage = WebhookMessage.embeds(
          embeds
        );
        client.send(webhookMessage);
      } catch (Exception e) {
        CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Error sending raid pre-started webhook: " + e.getMessage());
        e.printStackTrace();
      }
    });
    // Raid Post Started event
    RAID_STARTED_POST.subscribe(raidPostStarted -> {
      try {
        CobbleRaids.language.getTitleStartRaid().send(null, raidPostStarted.getRaid());
        var webHook = CobbleRaids.config.getWebhook();
        if (!webHook.isENABLED()) return;
        var client = getWebhookClient(webHook);
        if (client == null) return;
        var raid = raidPostStarted.getRaid();
        List<WebhookEmbed> embeds = new ArrayList<>();
        var builder = new WebhookEmbedBuilder()
          .setTitle(new WebhookEmbed.EmbedTitle("Raid Started", ""));
        String desc = "A new raid has started!\n" +
          " - Raid ID: " + raid.getRaidUUID() + "\n" +
          " - Category: " + raid.getCategoryRaid().getName() + "\n" +
          " - Level: " + raid.getCategoryRaid().getOverLevel() + "\n" +
          " - MinLevel: " + raid.getCategoryRaid().getMinLevel() + "\n" +
          " - Health Increment: " + raid.getHealth() + "\n" +
          " - Pokemon: " + raid.getPokemon().getDisplayName().getString() + "\n";
        builder.setDescription(desc);
        embeds.add(builder.build());
        WebhookMessage webhookMessage = WebhookMessage.embeds(
          embeds
        );
        client.send(webhookMessage);
      } catch (Exception e) {
        CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Error sending raid started webhook: " + e.getMessage());
        e.printStackTrace();
      }
    });

    // Raid new Phase event
    RAID_NEW_PHASE.subscribe(raidNewPhase -> {
      try {
        CobbleRaids.language.getTitleNewPhaseRaid().send(null, raidNewPhase.getRaid());
        var webHook = CobbleRaids.config.getWebhook();
        if (!webHook.isENABLED()) return;
        var client = getWebhookClient(webHook);
        if (client == null) return;
        var raid = raidNewPhase.getRaid();
        List<WebhookEmbed> embeds = new ArrayList<>();
        var builder = new WebhookEmbedBuilder()
          .setTitle(new WebhookEmbed.EmbedTitle("Raid New Phase", ""));
        String desc = "A raid has entered a new phase!\n" +
          " - New Phase: " + raid.getPokemon().getDisplayName().getString() + "\n";
        builder.setDescription(desc);
        embeds.add(builder.build());
        WebhookMessage webhookMessage = WebhookMessage.embeds(
          embeds
        );
        client.send(webhookMessage);
      } catch (Exception e) {
        CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Error sending raid new phase webhook: " + e.getMessage());
        e.printStackTrace();
      }
    });
  }

  @Nullable private static WebhookClient getWebhookClient(WebHookData webHook) {
    if (webHook.getURL_WEBHOOK() == null || webHook.getURL_WEBHOOK().isEmpty()) return null;
    return WebHookData.webhooks.computeIfAbsent(
      CobbleRaids.MOD_ID,
      (key) -> WebhookClient.withUrl(webHook.getURL_WEBHOOK())
    );
  }

}
