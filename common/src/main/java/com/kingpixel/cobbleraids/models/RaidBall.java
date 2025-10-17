package com.kingpixel.cobbleraids.models;

import com.kingpixel.cobbleraids.CobbleRaids;
import com.kingpixel.cobbleutils.Model.ItemModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

@EqualsAndHashCode(callSuper = true)
@Data
public class RaidBall extends ItemModel {
  private String id = "default";
  private double catchChance = 1.0;

  public RaidBall() {
    super("cobblemon:poke_ball");
  }

  public static void removeRaidBalls(ServerPlayerEntity player) {
    for (DefaultedList<ItemStack> list : player.getInventory().combinedInventory) {
      for (ItemStack stack : list) {
        if (RaidBall.isRaidBall(stack)) {
          CobbleRaids.server.executeSync(() -> stack.setCount(0));
        }
      }
    }
  }

  public void giveToPlayer(ServerPlayerEntity player) {
    ItemStack itemStack = getItemStack();
    player.getInventory().insertStack(itemStack);
  }

  public void giveToPlayer(ServerPlayerEntity player, int amount) {
    ItemStack stack = getItemStack(amount);
    player.getInventory().offerOrDrop(stack);
  }

  @Override
  public ItemStack getItemStack() {
    ItemStack stack = super.getItemStack();
    applyNbt(stack);
    return stack;
  }

  @Override
  public ItemStack getItemStack(int amount) {
    ItemStack stack = super.getItemStack(amount);
    applyNbt(stack);
    return stack;
  }

  private void applyNbt(ItemStack itemStack) {
    var nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
    var nbtCompound = nbtComponent != null ? nbtComponent.copyNbt() : new NbtCompound();
    nbtCompound.putBoolean("raid_ball", true);
    nbtCompound.putString("raid_ball_id", id);
    nbtCompound.putDouble("raid_ball_chance", catchChance);

    itemStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbtCompound));
  }

  public static boolean isRaidBall(ItemStack stack) {
    var nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
    if (nbtComponent == null) return false;
    var nbt = nbtComponent.getNbt();
    return nbt != null && nbt.contains("raid_ball");
  }
}
