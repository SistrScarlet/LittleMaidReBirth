package com.sistr.lmrb.entity;

import com.google.common.collect.Sets;
import com.sistr.lmrb.entity.mode.*;
import com.sistr.lmrb.setup.Registration;
import net.blacklab.lmr.entity.maidmodel.IHasMultiModel;
import net.blacklab.lmr.entity.maidmodel.ModelMultiBase;
import net.blacklab.lmr.entity.maidmodel.TextureBox;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import net.sistr.lmml.entity.DefaultMultiModel;
import net.sistr.lmml.util.manager.ModelManager;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

//メイドさん本体
//継承しないのが吉
//メイドさんはインベントリとモードとマルチモデルを持ち、契約可能なモブである
//モードは排他
public class LittleMaidEntity extends CreatureEntity implements IEntityAdditionalSpawnData, IHasMode, IHasInventory, ITameable, IHasMultiModel {
    private final DefaultMultiModel comp = new DefaultMultiModel(this,
            ModelManager.instance.getDefaultTexture(this),
            ModelManager.instance.getDefaultTexture(this));
    //todo モードの追加方法を変えるべし
    private final DefaultModeController modeController = new DefaultModeController(this,
            Sets.newHashSet(
                    new FencerMode(this, 1.5D, false),
                    new ArcherMode(this)));
    private final Inventory inventory = new Inventory(18);
    @Nullable
    private UUID ownerId;

    public LittleMaidEntity(EntityType<LittleMaidEntity> type, World worldIn) {
        super(type, worldIn);
    }

    public LittleMaidEntity(World world) {
        super(Registration.LITTLE_MAID_MOB.get(), world);
    }

    @Override
    protected void registerGoals() {
        //this.sitGoal = new SitGoal(this);
        this.goalSelector.addGoal(0, new SwimGoal(this));
        //this.goalSelector.addGoal(2, this.sitGoal);
        //this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        //this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0D, true));
        //this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F, false));
        //this.goalSelector.addGoal(15, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
        this.goalSelector.addGoal(10, new ModeWrapperGoal(this));
        this.goalSelector.addGoal(20, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(20, new LookRandomlyGoal(this));
        //this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        //this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, MobEntity.class,
                5, true, false, entity ->
                entity instanceof IMob && !(entity instanceof CreeperEntity)));
        //registerModes();
    }

    @Override
    protected void registerAttributes() {
        super.registerAttributes();
        this.getAttributes().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        this.getAttributes().registerAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        this.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23);
    }

    //todo IHasInventoryに同梱しとけい

    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        comp.write(compound);

        ListNBT listnbt = new ListNBT();
        for (int i = 0; i < this.inventory.getSizeInventory(); ++i) {
            ItemStack itemstack = this.inventory.getStackInSlot(i);
            listnbt.add(itemstack.write(new CompoundNBT()));
        }
        compound.put("Inventory", listnbt);

    }

    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);
        comp.read(compound);

        ListNBT listnbt = compound.getList("Inventory", 10);
        for (int i = 0; i < listnbt.size(); ++i) {
            ItemStack itemstack = ItemStack.read(listnbt.getCompound(i));
            if (!itemstack.isEmpty()) {
                this.inventory.setInventorySlotContents(i, itemstack);
            }
        }

        sync();

    }

    //鯖
    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        TextureBox[] box = comp.getTextureBox();
        buffer.writeString(box[0].textureName);
        buffer.writeString(box[1].textureName);
        buffer.writeByte(getColor());
        buffer.writeBoolean(getContract());
    }

    //蔵
    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        setTextureBox(0, ModelManager.instance.getTextureBox(additionalData.readString()));
        setTextureBox(1, ModelManager.instance.getTextureBox(additionalData.readString()));
        setColor(additionalData.readByte());
        setContract(additionalData.readBoolean());
        updateTextures();
    }

    @Override
    public IMode getMode() {
        return modeController.getMode();
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        modeController.tick();
    }

    //以下数メソッドはインベントリ関連
    //todo 未実装

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    @Override
    public ItemStack getItemStackFromSlot(EquipmentSlotType slotIn) {
        return super.getItemStackFromSlot(slotIn);
    }

    @Override
    public void setItemStackToSlot(EquipmentSlotType slotIn, ItemStack stack) {
        super.setItemStackToSlot(slotIn, stack);
    }

    @Override
    public boolean replaceItemInInventory(int inventorySlot, ItemStack itemStackIn) {
        return super.replaceItemInInventory(inventorySlot, itemStackIn);
    }

    //todo 主従未実装

    @Override
    public void setOwner(Entity owner) {
        this.ownerId = owner.getUniqueID();
    }

    @Override
    public Optional<Entity> getOwner() {
        if (this.world instanceof ServerWorld && this.ownerId != null) {
            return Optional.ofNullable(((ServerWorld) this.world).getEntityByUuid(this.ownerId));
        }
        return Optional.empty();
    }

    @Override
    public Optional<UUID> getOwnerId() {
        return Optional.ofNullable(this.ownerId);
    }

    //todo 以下数メソッドにはもうちと整理が必要か

    @Override
    protected boolean processInteract(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.getItem() == Items.CAKE) {
            return tryContract(player, stack);
        }
        if (stack.getItem().isIn(ItemTags.WOOL)) {
            return tryChangeModel(player);
        }
        if (stack.getItem().isIn(Tags.Items.DYES)) {
            return tryColorChange(player);
        }
        if (!player.world.isRemote) {
            openContainer(player, new StringTextComponent("test"));
        }
        return false;
    }

    public boolean tryContract(PlayerEntity player, ItemStack stack) {
        if (getContract()) {
            return false;
        }
        if (player.world.isRemote) {
            return false;
        }
        setOwner(player);
        setContract(true);
        if (!player.abilities.isCreativeMode) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.inventory.deleteStack(stack);
            }
        }
        updateTextures();
        sync();
        return false;
    }

    public boolean tryChangeModel(PlayerEntity player) {
        if (!getContract()) {
            return false;
        }
        if (player.world.isRemote) {
            return false;
        }
        TextureBox[] box = new TextureBox[2];
        box[0] = box[1] = ModelManager.instance.getTextureBox(ModelManager.instance.getRandomTextureString(getRNG()));
        setColor((byte) box[0].getRandomContractColor(rand));
        setTextureBox(0, box[0]);
        setTextureBox(1, box[1]);
        updateTextures();
        sync();
        return false;
    }

    public boolean tryColorChange(PlayerEntity player) {
        if (!getContract()) {
            return false;
        }
        if (player.world.isRemote) {
            return false;
        }
        setColor((byte) getTextureBox()[0].getRandomContractColor(getRNG()));
        updateTextures();
        sync();
        return false;
    }

    //GUI開くやつ
    public void openContainer(PlayerEntity player, ITextComponent text) {
        player.openContainer(new SimpleNamedContainerProvider((windowId, inv, playerEntity) ->
                new LittleMaidContainer(windowId, inv, this), text));
    }

    public void sync() {
        if (world.isRemote) {
            return;
        }
        comp.sync();
    }

    @Override
    public void updateTextures() {
        comp.updateTextures();
    }

    @Override
    public void setTextures(int index, ResourceLocation[] names) {
        comp.setTextures(index, names);
    }

    @Override
    public ResourceLocation[] getTextures(int index) {
        return comp.getTextures(index);
    }

    @Override
    public void setMultiModels(int index, ModelMultiBase models) {
        comp.setMultiModels(index, models);
    }

    @Override
    public ModelMultiBase[] getMultiModels() {
        return comp.getMultiModels();
    }

    @Override
    public void setColor(byte color) {
        comp.setColor(color);
    }

    @Override
    public byte getColor() {
        return comp.getColor();
    }

    @Override
    public void setContract(boolean isContract) {
        comp.setContract(isContract);
    }

    @Override
    public boolean getContract() {
        return comp.getContract();
    }

    @Override
    public void setTextureBox(int index, @Nullable TextureBox textureBox) {
        comp.setTextureBox(index, textureBox);
    }

    @Override
    public TextureBox[] getTextureBox() {
        return comp.getTextureBox();
    }

    //オーバーライドしなくても動くが、IEntityAdditionalSpawnDataが機能しない
    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
