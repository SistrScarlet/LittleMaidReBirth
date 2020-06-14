package com.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sistr.littlemaidrebirth.LittleMaidInventory;
import com.sistr.littlemaidrebirth.entity.goal.EscortGoal;
import com.sistr.littlemaidrebirth.entity.goal.FreedomGoal;
import com.sistr.littlemaidrebirth.entity.goal.HealMyselfGoal;
import com.sistr.littlemaidrebirth.entity.goal.WaitGoal;
import com.sistr.littlemaidrebirth.entity.mode.*;
import com.sistr.littlemaidrebirth.setup.Registration;
import net.blacklab.lmr.entity.maidmodel.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import net.sistr.lmml.entity.DefaultMultiModel;
import net.sistr.lmml.util.manager.ModelManager;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

import static net.sistr.lmml.util.ConvertColor.convertDyeColor;

//メイドさん本体
//継承しないのが吉
//todo 啼くように、このクラスの行数を500まで減らす
public class LittleMaidEntity extends CreatureEntity implements IEntityAdditionalSpawnData, IHasInventory, ITameable,
        INeedSalary, IHasMode, IArcher, IHasFakePlayer, IHasMultiModel {
    //変数群。カオス
    private static final DataParameter<String> MOVING_STATE = EntityDataManager.createKey(LittleMaidEntity.class, DataSerializers.STRING);
    private static final DataParameter<Optional<UUID>> OWNER_ID = EntityDataManager.createKey(LittleMaidEntity.class, DataSerializers.OPTIONAL_UNIQUE_ID);
    private static final DataParameter<Boolean> AIMING = EntityDataManager.createKey(LittleMaidEntity.class, DataSerializers.BOOLEAN);
    private final IHasInventory littleMaidInventory = new LittleMaidInventory(this);
    //todo お給料機能とテイム機能一緒にした方がよさげ
    private final ITameable tameable = new DefaultTameable(this, this.dataManager, MOVING_STATE, OWNER_ID);
    private final TickTimeBaseNeedSalary needSalary = new TickTimeBaseNeedSalary(this, this,
            7, Lists.newArrayList(Items.SUGAR));
    //todo モードの追加方法を変えるべし
    private final DefaultModeController modeController = new DefaultModeController(this, this,
            Sets.newHashSet(
                    new FencerMode(this, this, 1D, true),
                    new ArcherMode(this, this, this,
                            0.1F, 10, 16),
                    new CookingMode(this, this),
                    new RipperMode(this),
                    new TorcherMode(this, this, this)));
    private final LittleMaidFakePlayer fakePlayer = new LittleMaidFakePlayer(this, this);
    private final DefaultMultiModel multiModel = new DefaultMultiModel(this,
            ModelManager.instance.getDefaultTexture(this),
            ModelManager.instance.getDefaultTexture(this));
    private final IModelCaps caps = new LivingEntityCaps(this);

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
        this.goalSelector.addGoal(5, new HealMyselfGoal(this, this));
        //todo 手に持ったケーキや砂糖に反応するやーつ
        this.goalSelector.addGoal(10, new WaitGoal(this, this));
        this.goalSelector.addGoal(15, new ModeWrapperGoal(this));
        //todo ドロップアイテム回収Goal
        this.goalSelector.addGoal(20, new EscortGoal(this, this, 4, 3, 1));
        this.goalSelector.addGoal(20, new FreedomGoal(this, this, 1));
        this.goalSelector.addGoal(25, new WaterAvoidingRandomWalkingGoal(this, 1.0D) {
            @Override
            public boolean shouldExecute() {
                return !getOwnerId().isPresent() && super.shouldExecute();
            }
        });
        this.goalSelector.addGoal(30, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(30, new LookRandomlyGoal(this));
        //todo 主への攻撃者絶対殺すマン
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

        this.writeInventory(compound);

        this.writeTameable(compound);

        this.writeSalary(compound);

        multiModel.write(compound);

    }

    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);

        this.readInventory(compound);

        this.readTameable(compound);

        this.readSalary(compound);

        multiModel.read(compound);

        sync();

    }

    //todo hasMultiModelに内包するか？

    //鯖
    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        TextureBox[] box = multiModel.getTextureBox();
        String main = "";
        String armor = "";
        if (box[0] != null && box[0].textureName != null) {
            main = box[0].textureName;
        }
        if (box[1] != null && box[1].textureName != null) {
            armor = box[1].textureName;
        }
        buffer.writeString(main);
        buffer.writeString(armor);
        buffer.writeByte(getColor());
        buffer.writeBoolean(getContract());
    }

    //蔵
    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        String main = additionalData.readString();
        String armor = additionalData.readString();
        if (!main.isEmpty()) setTextureBox(0, ModelManager.instance.getTextureBox(main));
        if (!armor.isEmpty()) setTextureBox(1, ModelManager.instance.getTextureBox(armor));
        setColor(additionalData.readByte());
        setContract(additionalData.readBoolean());
        updateTextures();
    }

    //バニラメソッズ

    @Override
    public void tick() {
        super.tick();
        //FakePlayer用。このフィールドLivingEntityにもあるくせにPlayerEntityでしか使われてねえ…
        ++this.ticksSinceLastSwing;
    }

    @Override
    public void livingTick() {
        updateArmSwingProgress();
        super.livingTick();
        if (getContract()) needSalary.tick();
        //アイテム回収処理
        fakePlayer.livingTick();
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        tameable.onDeath(cause);
        fakePlayer.onDeath(cause);
    }

    //todo hasMultiModelに内包するか？
    @Override
    public EntitySize getSize(Pose poseIn) {
        float width = getWidth();
        float height = getHeight();
        ModelMultiBase modelMultiBase = this.getMultiModels()[0];
        if (modelMultiBase != null) {
            width = modelMultiBase.getWidth(caps);
            height = modelMultiBase.getHeight(caps);
        }
        return EntitySize.flexible(width, height);
    }

    //canSpawnとかでも使われる
    @Override
    public float getBlockPathWeight(BlockPos pos, IWorldReader worldIn) {
        return worldIn.getBlockState(pos.down()).getMaterial().isOpaque() ? 10.0F : worldIn.getBrightness(pos) - 0.5F;
    }

    @Override
    public double getYOffset() {
        return this.multiModel.getMultiModels()[0].getyOffset(caps);
    }

    @Override
    public double getMountedYOffset() {
        return this.multiModel.getMultiModels()[0].getMountedYOffset(caps);
    }

    @Override
    protected float getDropChance(EquipmentSlotType slotIn) {
        return 0;
    }

    @Override
    public boolean canDespawn(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && !isFriend(target);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        Entity attacker = source.getTrueSource();
        //Friend及び、自身と同じUUIDの者(自身のFakePlayer)を除外
        if (attacker != null && (isFriend(attacker) || attacker.getUniqueID().equals(this.getUniqueID()))) {
            return false;
        }
        return super.attackEntityFrom(source, amount);
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
        if (isStrike()) {
            if (stack.getItem() == Items.CAKE) {
                this.world.addParticle(ParticleTypes.HEART,
                        getPosX(), getPosY() + getEyeHeight(), getPosZ(),
                        0, this.rand.nextGaussian() * 0.02D, 0);
                while (receiveSalary(1)) ;
                if (!player.abilities.isCreativeMode) {
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        player.inventory.deleteStack(stack);
                    }
                }
                return true;
            }
            for (int i = 0; i < 5; i++) {
                this.world.addParticle(ParticleTypes.SMOKE,
                        this.getPosX() + (0.5F - rand.nextFloat()) * 0.2F,
                        this.getPosYEye() + (0.5F - rand.nextFloat()) * 0.2F,
                        this.getPosZ() + (0.5F - rand.nextFloat()) * 0.2F,
                        0, 0.1, 0);
            }
            return false;
        }
        if (stack.getItem() == Items.SUGAR) {
            return tryChangeState(player, stack);
        }
        if (stack.getItem() == Items.CAKE) {
            return tryContract(player, stack);
        }
        if (stack.getItem().isIn(Tags.Items.DYES)) {
            return tryColorChange(player, stack);
        }
        if (!player.world.isRemote && getOwnerId().isPresent() && getOwnerId().get().equals(player.getUniqueID())) {
            openContainer(player);
        }
        return true;
    }

    public boolean tryChangeState(PlayerEntity player, ItemStack stack) {
        if (!getContract()) {
            return false;
        }
        this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.getRNG().nextFloat() * 0.1F + 1.0F);
        this.world.addParticle(ParticleTypes.NOTE, this
                        .getPosX(), this.getPosY() + this.getEyeHeight(), this.getPosZ(),
                0, this.rand.nextGaussian() * 0.02D, 0);
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
            default:
                setMovingState(ITameable.WAIT);
                break;
        }
        if (!player.abilities.isCreativeMode) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.inventory.deleteStack(stack);
            }
        }
        return true;
    }

    public boolean tryContract(PlayerEntity player, ItemStack stack) {
        if (getContract()) {
            return false;
        }
        this.world.addParticle(ParticleTypes.HEART,
                getPosX(), getPosY() + getEyeHeight(), getPosZ(),
                0, this.rand.nextGaussian() * 0.02D, 0);
        if (player.world.isRemote) {
            return true;
        }
        while (receiveSalary(1)) ;//ここに給料処理が混じってるのがムカつく
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

    public boolean tryColorChange(PlayerEntity player, ItemStack stack) {
        if (!getContract()) {
            return false;
        }
        byte color = convertDyeColor(stack.getItem());
        if (color < 0 || !getTextureBox()[0].hasColor(color)) {
            color = (byte) getTextureBox()[0].getRandomContractColor(rand);
        }
        if (getColor() == color) {
            return false;
        }
        if (player.world.isRemote) {
            return true;
        }
        if (!player.abilities.isCreativeMode) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.inventory.deleteStack(stack);
            }
        }
        setColor(color);
        updateTextures();
        sync();
        return true;
    }

    //GUI開くやつ
    //todo 開いている間動かないようにする
    public void openContainer(PlayerEntity player) {
        ITextComponent movingState = new TranslationTextComponent(
                getType().getTranslationKey() + "." + getMovingState());
        if (getMode() != null) {
            ITextComponent modeName = new TranslationTextComponent(
                    getType().getTranslationKey() + "." + getMode().getName()
            );
            movingState.appendText(" : ").appendSibling(modeName);
        }
        player.openContainer(new SimpleNamedContainerProvider((windowId, inv, playerEntity) ->
                new LittleMaidContainer(windowId, inv, this), movingState));
    }

    //インベントリ関連

    @Override
    public IInventory getInventory() {
        return this.littleMaidInventory.getInventory();
    }

    @Override
    public void writeInventory(CompoundNBT nbt) {
        this.littleMaidInventory.writeInventory(nbt);
    }

    @Override
    public void readInventory(CompoundNBT nbt) {
        this.littleMaidInventory.readInventory(nbt);
    }

    @Override
    public boolean replaceItemInInventory(int inventorySlot, ItemStack itemStackIn) {
        IInventory inventory = this.getInventory();
        if (0 <= inventorySlot && inventorySlot < inventory.getSizeInventory()) {
            inventory.setInventorySlotContents(inventorySlot, itemStackIn);
            return true;
        } else {
            return super.replaceItemInInventory(inventorySlot, itemStackIn);
        }
    }

    //テイム関連

    @Override
    public void writeTameable(CompoundNBT nbt) {
        tameable.writeTameable(nbt);
    }

    @Override
    public void readTameable(CompoundNBT nbt) {
        tameable.readTameable(nbt);
    }

    @Override
    public Optional<Entity> getOwner() {
        return tameable.getOwner();
    }

    @Override
    public void setOwnerId(UUID id) {
        tameable.setOwnerId(id);
    }

    @Override
    public Optional<UUID> getOwnerId() {
        return tameable.getOwnerId();
    }

    @Override
    public String getMovingState() {
        return tameable.getMovingState();
    }

    public void setMovingState(String string) {
        tameable.setMovingState(string);
    }

    public boolean isFriend(Entity entity) {
        return tameable.isFriend(entity);
    }

    //お給料

    @Override
    public boolean receiveSalary(int num) {
        return needSalary.receiveSalary(num);
    }

    @Override
    public boolean consumeSalary(int num) {
        return needSalary.consumeSalary(num);
    }

    @Override
    public int getSalary() {
        return needSalary.getSalary();
    }

    @Override
    public boolean isSalary(ItemStack stack) {
        return needSalary.isSalary(stack);
    }

    @Override
    public boolean isStrike() {
        return needSalary.isStrike();
    }

    @Override
    public void writeSalary(CompoundNBT nbt) {
        needSalary.writeSalary(nbt);
    }

    @Override
    public void readSalary(CompoundNBT nbt) {
        needSalary.readSalary(nbt);
    }

    //モード機能

    @Override
    @Nullable
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
        return this.dataManager.get(AIMING) && this.isHandActive();
    }

    @Override
    public void setAimingBow(boolean aiming) {
        this.dataManager.set(AIMING, aiming);
    }

    //Fake関連、クライアントで実行するとクラッシュする

    @Override
    public FakePlayer getFakePlayer() {
        return fakePlayer.getFakePlayer();
    }

    @Override
    public void syncToFakePlayer() {
        fakePlayer.syncToFakePlayer();
    }

    @Override
    public void syncToOrigin() {
        fakePlayer.syncToOrigin();
    }

    //マルチモデル関連

    //このメソッドを発動させたサイドから逆サイドに同期する
    public void sync() {
        recalculateSize();
        multiModel.sync();
    }

    @Override
    public void updateTextures() {
        multiModel.updateTextures();
    }

    @Override
    public void setTextures(int index, ResourceLocation[] names) {
        multiModel.setTextures(index, names);
    }

    @Override
    public ResourceLocation[] getTextures(int index) {
        return multiModel.getTextures(index);
    }

    @Override
    public void setMultiModels(int index, ModelMultiBase models) {
        multiModel.setMultiModels(index, models);
    }

    @Override
    public ModelMultiBase[] getMultiModels() {
        return multiModel.getMultiModels();
    }

    @Override
    public void setColor(byte color) {
        multiModel.setColor(color);
    }

    @Override
    public byte getColor() {
        return multiModel.getColor();
    }

    @Override
    public void setContract(boolean isContract) {
        multiModel.setContract(isContract);
    }

    @Override
    public boolean getContract() {
        return getOwnerId().isPresent();
    }

    @Override
    public void setTextureBox(int index, @Nullable TextureBox textureBox) {
        multiModel.setTextureBox(index, textureBox);
    }

    @Override
    public TextureBox[] getTextureBox() {
        return multiModel.getTextureBox();
    }

    @Override
    public boolean canRenderArmor(int index) {
        return multiModel.canRenderArmor(index);
    }

    @Override
    public void setCanRenderArmor(int index, boolean canRender) {
        multiModel.setCanRenderArmor(index, canRender);
    }

    //オーバーライドしなくても動くが、IEntityAdditionalSpawnDataが機能しない
    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

}
