package com.sistr.lmrb.entity;

import com.google.common.collect.Sets;
import com.sistr.lmrb.entity.goal.EscortGoal;
import com.sistr.lmrb.entity.goal.FreedomGoal;
import com.sistr.lmrb.entity.goal.WaitGoal;
import com.sistr.lmrb.entity.mode.*;
import com.sistr.lmrb.setup.Registration;
import net.blacklab.lmr.entity.maidmodel.IHasMultiModel;
import net.blacklab.lmr.entity.maidmodel.ModelMultiBase;
import net.blacklab.lmr.entity.maidmodel.TextureBox;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
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
public class LittleMaidEntity extends CreatureEntity implements IEntityAdditionalSpawnData, IHasInventory, ITameable, IHasMode, IArcher, IHasMultiModel {
    //変数群 カオス
    private static final DataParameter<Boolean> AIMING = EntityDataManager.createKey(LittleMaidEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<String> MOVING_STATE = EntityDataManager.createKey(LittleMaidEntity.class, DataSerializers.STRING);
    private static final DataParameter<Optional<UUID>> OWNER_ID = EntityDataManager.createKey(LittleMaidEntity.class, DataSerializers.OPTIONAL_UNIQUE_ID);
    private final DefaultMultiModel comp = new DefaultMultiModel(this,
            ModelManager.instance.getDefaultTexture(this),
            ModelManager.instance.getDefaultTexture(this));
    //todo モードの追加方法を変えるべし
    private final DefaultModeController modeController = new DefaultModeController(this,
            Sets.newHashSet(
                    new FencerMode(this, 1.25D, false),
                    new ArcherMode(this, this, 0.1F, 10, 16)));
    private final Inventory inventory = new Inventory(18);

    //コンストラクタ
    public LittleMaidEntity(EntityType<LittleMaidEntity> type, World worldIn) {
        super(type, worldIn);
    }

    public LittleMaidEntity(World world) {
        super(Registration.LITTLE_MAID_MOB.get(), world);
    }

    //レジスタ

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(5, new WaitGoal(this, this));
        this.goalSelector.addGoal(10, new ModeWrapperGoal(this));
        this.goalSelector.addGoal(15, new EscortGoal(this,  this, 4, 3));
        this.goalSelector.addGoal(15, new FreedomGoal(this, this, 1));
        this.goalSelector.addGoal(20, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
        this.goalSelector.addGoal(25, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(25, new LookRandomlyGoal(this));
        //this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        //this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, MobEntity.class,
                5, true, false, entity ->
                entity instanceof IMob && !(entity instanceof CreeperEntity)));
    }

    @Override
    protected void registerAttributes() {
        super.registerAttributes();
        this.getAttributes().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        this.getAttributes().registerAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        this.getAttributes().registerAttribute(SharedMonsterAttributes.LUCK);
        this.getAttributes().registerAttribute(PlayerEntity.REACH_DISTANCE);
        this.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(AIMING, false);
        this.dataManager.register(MOVING_STATE, ITameable.NONE);
        this.dataManager.register(OWNER_ID, Optional.empty());
    }

    //読み込み時の読み書き系

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

        if (this.getOwnerId().isPresent()) {
            compound.putUniqueId("OwnerId", this.getOwnerId().get());
        }

        compound.putString("MovingState", getMovingState());

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

        if (compound.hasUniqueId("OwnerId")) {
            this.setOwnerId(compound.getUniqueId("OwnerId"));
            this.setContract(true);
        }

        setMovingState(compound.getString("MovingState"));

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

    //バニラメソッズ

    @Override
    public void livingTick() {
        updateArmSwingProgress();
        super.livingTick();
    }

    //todo 以下数メソッドにはもうちと整理が必要か

    //trueでアイテムが使用された、falseでされなかった
    //trueならItemStack.interactWithEntity()が起こらず、またアイテム使用が必ずキャンセルされる
    @Override
    protected boolean processInteract(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSecondaryUseActive()) {
            return false;
        }
        if (stack.getItem() == Items.SUGAR) {
            return tryChangeState(player, stack);
        }
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
        return true;
    }

    public boolean tryChangeState(PlayerEntity player, ItemStack stack) {
        if (!getContract()) {
            return false;
        }
        if (player.world.isRemote) {
            return true;
        }
        getNavigator().clearPath();
        String state = this.getMovingState();
        switch (state) {
            case ITameable.WAIT:
                setMovingState(ITameable.ESCORT);
                break;
            case ITameable.ESCORT:
                setMovingState(ITameable.FREEDOM);
                break;
            case ITameable.FREEDOM:
                setMovingState(ITameable.WAIT);
                break;
        }
        return true;
    }

    public boolean tryContract(PlayerEntity player, ItemStack stack) {
        if (getContract()) {
            return false;
        }
        if (player.world.isRemote) {
            return true;
        }
        getNavigator().clearPath();
        setOwnerId(player.getUniqueID());
        setMovingState(ITameable.ESCORT);
        setContract(true);
        if (!player.abilities.isCreativeMode) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.inventory.deleteStack(stack);
            }
        }
        updateTextures();
        sync();
        return true;
    }

    public boolean tryChangeModel(PlayerEntity player) {
        if (!getContract()) {
            return false;
        }
        if (player.world.isRemote) {
            return true;
        }
        TextureBox[] box = new TextureBox[2];
        box[0] = box[1] = ModelManager.instance.getTextureBox(ModelManager.instance.getRandomTextureString(getRNG()));
        setColor((byte) box[0].getRandomContractColor(rand));
        setTextureBox(0, box[0]);
        setTextureBox(1, box[1]);
        updateTextures();
        sync();
        return true;
    }

    public boolean tryColorChange(PlayerEntity player) {
        if (!getContract()) {
            return false;
        }
        if (player.world.isRemote) {
            return true;
        }
        setColor((byte) getTextureBox()[0].getRandomContractColor(getRNG()));
        updateTextures();
        sync();
        return true;
    }

    //GUI開くやつ
    public void openContainer(PlayerEntity player, ITextComponent text) {
        player.openContainer(new SimpleNamedContainerProvider((windowId, inv, playerEntity) ->
                new LittleMaidContainer(windowId, inv, this), text));
    }

    //インベントリ関連
    //todo 一部未実装

    @Override
    public IInventory getInventory() {
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

    //テイム関連

    @Override
    public Optional<Entity> getOwner() {
        Optional<UUID> optional = this.getOwnerId();
        if (!optional.isPresent()) {
            return Optional.empty();
        }
        UUID ownerId = optional.get();
        PlayerEntity player = this.world.getPlayerByUuid(ownerId);
        if (player != null) {
            return Optional.of(player);
        }
        if (this.world instanceof ServerWorld) {
            return Optional.ofNullable(((ServerWorld) this.world).getEntityByUuid(ownerId));
        }
        return Optional.empty();
    }

    @Override
    public void setOwnerId(UUID id) {
        this.dataManager.set(OWNER_ID, Optional.of(id));
    }

    @Override
    public Optional<UUID> getOwnerId() {
        return this.dataManager.get(OWNER_ID);
    }

    @Override
    public String getMovingState() {
        return this.dataManager.get(MOVING_STATE);
    }

    public void setMovingState(String string) {
        this.dataManager.set(MOVING_STATE, string);
    }

    //モード機能

    @Override
    public IMode getMode() {
        return modeController.getMode();
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        modeController.tick();
    }

    @Override
    public boolean getAimingBow() {
        return this.dataManager.get(AIMING);
    }

    @Override
    public void setAimingBow(boolean aiming) {
        this.dataManager.set(AIMING, aiming);
    }

    //マルチモデル関連

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
        return getOwnerId().isPresent();
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
