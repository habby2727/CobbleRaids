package com.kingpixel.cobbleraids.config;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleraids.model.PokeBallRaid;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 08/02/2025 20:12
 */
@Data
public class PokeBallRaidConfig {
  public static List<PokeBallRaid> pokeBallRaids = new ArrayList<>();

  public static void init() {
    pokeBallRaids.clear();
    File folder = Utils.getAbsolutePath(CobbleRaids.PATH_POKEBALL_RAID);

    if (!folder.exists()) {
      folder.mkdirs();
      createDefaultPokeBalls();
    } else {
      File[] files = folder.listFiles();
      if (files != null) {
        for (File file : files) {
          PokeBallRaid pokeball;
          try {
            pokeball = Utils.newGson().fromJson(Utils.readFileSync(file), PokeBallRaid.class);
            if (pokeball == null) {
              CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "Error reading pokeBallRaid " + file.getAbsolutePath());
              continue;
            }
            pokeball.setId(file.getName().replace(".json", ""));
            pokeBallRaids.add(pokeball);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }

  private static void createDefaultPokeBalls() {
    for (int i = 0; i < 3; i++) {
      PokeBallRaid pokeBallRaid = new PokeBallRaid();
      pokeBallRaid.setId("pokeball" + i);
      pokeBallRaids.add(pokeBallRaid);
    }
    for (PokeBallRaid pokeBallRaid : pokeBallRaids) {
      Utils.writeFileAsync(CobbleRaids.PATH_POKEBALL_RAID, pokeBallRaid.getId() + ".json", Utils.newGson().toJson(pokeBallRaid));
    }

  }

  public static void give(ServerPlayerEntity player, String id, int amount) {
    PokeBallRaid pokeBallRaid = getPokeBallRaid(id);
    if (pokeBallRaid == null) {
      CobbleUtils.LOGGER.error(CobbleRaids.MOD_ID, "PokeBallRaid not found: " + id);
      return;
    }
    ItemStack itemStack = pokeBallRaid.getItemStack();
    NbtCompound nbtCompound = new NbtCompound();
    nbtCompound.putString("id", pokeBallRaid.getId());
    nbtCompound.putFloat("catchRate", pokeBallRaid.getRateSuccess());
    itemStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbtCompound));
    itemStack.setCount(amount);
    player.getInventory().insertStack(itemStack);
  }

  private static PokeBallRaid getPokeBallRaid(String id) {
    return pokeBallRaids.stream().filter(pokeBallRaid -> pokeBallRaid.getId().equals(id)).findFirst().orElse(null);
  }
}
