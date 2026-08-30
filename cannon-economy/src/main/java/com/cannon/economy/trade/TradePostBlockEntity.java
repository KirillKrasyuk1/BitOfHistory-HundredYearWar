package com.cannon.economy.trade;

import com.cannon.economy.CannonEconomy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TradePostBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    public static final int SLOTS = 27;
    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    @Nullable
    private String postName;
    @Nullable
    private UUID linkedFaction;

    public TradePostBlockEntity(BlockPos pos, BlockState state) {
        super(CannonEconomy.TRADE_POST_BE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Nullable
    public String getPostName() {
        return postName;
    }

    public void setPostName(@Nullable String postName) {
        this.postName = postName;
        setChanged();
    }

    @Nullable
    public UUID getLinkedFaction() {
        return linkedFaction;
    }

    public void setLinkedFaction(@Nullable UUID linkedFaction) {
        this.linkedFaction = linkedFaction;
        setChanged();
    }

    public boolean tryExtract(ItemStack requested) {
        if (requested.isEmpty()) {
            return false;
        }
        int needed = requested.getCount();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (ItemStack.isSameItemSameTags(stack, requested)) {
                int take = Math.min(needed, stack.getCount());
                stack.shrink(take);
                inventory.setStackInSlot(i, stack);
                needed -= take;
                if (needed <= 0) {
                    setChanged();
                    return true;
                }
            }
        }
        setChanged();
        return needed <= 0;
    }

    public void insert(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        for (int i = 0; i < inventory.getSlots(); i++) {
            stack = inventory.insertItem(i, stack, false);
            if (stack.isEmpty()) {
                break;
            }
        }
        if (!stack.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        }
        setChanged();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        if (tag.contains("PostName")) {
            postName = tag.getString("PostName");
        }
        if (tag.hasUUID("LinkedFaction")) {
            linkedFaction = tag.getUUID("LinkedFaction");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        if (postName != null) {
            tag.putString("PostName", postName);
        }
        if (linkedFaction != null) {
            tag.putUUID("LinkedFaction", linkedFaction);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.cannon_economy.trade_post");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return ChestMenu.threeRows(id, playerInv, new net.minecraft.world.SimpleContainer(SLOTS) {
            @Override
            public int getContainerSize() {
                return SLOTS;
            }

            @Override
            public boolean isEmpty() {
                for (int i = 0; i < SLOTS; i++) {
                    if (!inventory.getStackInSlot(i).isEmpty()) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public ItemStack getItem(int slot) {
                return inventory.getStackInSlot(slot);
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                return inventory.extractItem(slot, amount, false);
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                ItemStack stack = inventory.getStackInSlot(slot);
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
                return stack;
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                inventory.setStackInSlot(slot, stack);
            }

            @Override
            public void setChanged() {
                TradePostBlockEntity.this.setChanged();
            }

            @Override
            public boolean stillValid(Player player) {
                return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 64;
            }

            @Override
            public void clearContent() {
                for (int i = 0; i < SLOTS; i++) {
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        });
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}
