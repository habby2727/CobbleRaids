package com.kingpixel.cobbleraids.events;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.events.models.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.WebHookData;
import com.kingpixel.cobbleutils.Model.messages.HiperMessage;
import com.kingpixel.cobbleutils.Model.messages.MessageType;
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
        var raid = raidFinished.getRaid();
        HiperMessage message = CobbleRaids.language.getMessageEndRaid();
        message.sendMessage(null, raid.replace(message.getRawMessage()), CobbleRaids.language.getPrefix(), false);
        var webHook = CobbleRaids.config.getWebhook();
        AtomicReference<StringBuilder> desc =
          new AtomicReference<>(new StringBuilder(CobbleRaids.language.getLeaderBoardTitle()));
        AtomicReference<Integer> amount = new AtomicReference<>(0);
        raid.getDamageMap().entrySet().stream()
          .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
          .forEach(entry -> {
            var uuid = entry.getKey();
            var userCache = CobbleRaids.server.getUserCache();
            if (userCache == null) return;
            var player = userCache.getByUuid(uuid);
            if (player.isEmpty()) return;
            amount.getAndSet(amount.get() + 1);
            var name = player.get().getName();
            desc.get().append(CobbleRaids.language.getLeaderBoardLine()
              .replace("%position%", amount.get().toString())
              .replace("%player%", name)
              .replace("%damage%", entry.getValue().toString()));
          });
        desc.get().append(CobbleRaids.language.getLeaderBoardFooter());
        String finalDesc = desc.get().toString();
        HiperMessage hiperMessage = new HiperMessage("cb:" + finalDesc, MessageType.CHAT_BROADCAST);
        hiperMessage.sendMessage(null, hiperMessage.getRawMessage(), CobbleRaids.language.getPrefix(), false);
        if (!webHook.isENABLED()) return;
        var client = getWebhookClient(webHook);
        if (client == null) return;
        var killed = raidFinished.isKilled();
        List<WebhookEmbed> embeds = new ArrayList<>();
        var builder = new WebhookEmbedBuilder()
          .setTitle(new WebhookEmbed.EmbedTitle("Raid Finished", ""));

        builder.setDescription(finalDesc);
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
        HiperMessage message = CobbleRaids.language.getMessagePreStartRaid();
        message.sendMessage(null, raidPreStarted.getRaid().replace(message.getRawMessage()), CobbleRaids.language.getPrefix(), false);
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
        HiperMessage message = CobbleRaids.language.getMessageStartRaid();
        message.sendMessage(null, raidPostStarted.getRaid().replace(message.getRawMessage()), CobbleRaids.language.getPrefix(), false);
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
        var raid = raidNewPhase.getRaid();
        HiperMessage message = CobbleRaids.language.getMessageNewPhaseRaid();
        message.sendMessage(null, raid.replace(message.getRawMessage()), CobbleRaids.language.getPrefix(), false);
        var webHook = CobbleRaids.config.getWebhook();
        if (!webHook.isENABLED()) return;
        var client = getWebhookClient(webHook);
        if (client == null) return;
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
