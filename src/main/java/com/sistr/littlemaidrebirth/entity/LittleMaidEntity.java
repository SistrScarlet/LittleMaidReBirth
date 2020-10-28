package com.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sistr.littlemaidrebirth.entity.goal.EscortGoal;
import com.sistr.littlemaidrebirth.entity.goal.FreedomGoal;
import com.sistr.littlemaidrebirth.entity.goal.HealMyselfGoal;
import com.sistr.littlemaidrebirth.entity.goal.WaitGoal;
import com.sistr.littlemaidrebirth.entity.mode.*;
import com.sistr.littlemaidrebirth.setup.Registration;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.TameableEntity;
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
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import net.sistr.lmml.LittleMaidModelLoader;
import net.sistr.lmml.entity.IHasMultiModel;
import net.sistr.lmml.entity.MultiModelCompound;
import net.sistr.lmml.maidmodel.IModelCaps;
import net.sistr.lmml.maidmodel.ModelMultiBase;
import net.sistr.lmml.resource.manager.TextureManager;
import net.sistr.lmml.util.TextureColor;
import net.sistr.lmml.util.TextureHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

//メイドさん本体
//継承しないのが吉
//todo 啼くように、このクラスの行数を500まで減らす
public class LittleMaidEntity extends TameableEntity implements IEntityAdditionalSpawnData, IHasInventory, ITameable,
        INeedSalary, IHasMode, IArcher, IHasFakePlayer, IHasMultiModel {
    //変数群。カオス
    private static final DataParameter<String> MOVING_STATE = EntityDataManager.createKey(LittleMaidEntity.class, DataSerializers.STRING);
    private static final DataParameter<Boolean> AIMING = EntityDataManager.createKey(LittleMaidEntity.class, DataSerializers.BOOLEAN);
    private final IHasInventory littleMaidInventory = new LittleMaidInventory(this);
    //todo お給料機能とテイム機能一緒にした方がよさげ
    private final DefaultTameable tameable = new DefaultTameable(this.dataManager, MOVING_STATE);
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
    private final MultiModelCompound multiModel;

    //コンストラクタ
    public LittleMaidEntity(EntityType<LittleMaidEntity> type, World worldIn) {
        super(type, worldIn);
        multiModel = new MultiModelCompound(this,
                LittleMaidModelLoader.getInstance().getTextureManager().getTexture("Default")
                        .orElseThrow(() -> new IllegalStateException("デフォルトテクスチャが存在しません。")),
                LittleMaidModelLoader.getInstance().getTextureManager().getTexture("Default")
                        .orElseThrow(() -> new IllegalStateException("デフォルトテクスチャが存在しません。")));
    }

    public LittleMaidEntity(World world) {
        super(Registration.LITTLE_MAID_MOB.get(), world);
        multiModel = new MultiModelCompound(this,
                LittleMaidModelLoader.getInstance().getTextureManager().getTexture("Default")
                        .orElseThrow(() -> new IllegalStateException("デフォルトテクスチャが存在しません。")),
                LittleMaidModelLoader.getInstance().getTextureManager().getTexture("Default")
                        .orElseThrow(() -> new IllegalStateException("デフォルトテクスチャが存在しません。")));
    }

    public LittleMaidEntity(World world, MultiModelCompound multiModel) {
        super(Registration.LITTLE_MAID_MOB.get(), world);
        this.multiModel = multiModel;
    }

    //スタティックなメソッド

    public static AttributeModifierMap.MutableAttribute registerAttributes() {
        return MobEntity.func_233666_p_().createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.3D)
                .createMutableAttribute(Attributes.MAX_HEALTH, 8.0D)
                .createMutableAttribute(Attributes.ATTACK_DAMAGE, 2.0D)
                .createMutableAttribute(Attributes.ATTACK_SPEED)
                .createMutableAttribute(Attributes.LUCK)
                .createMutableAttribute(ForgeMod.REACH_DISTANCE.get());
    }

    public static boolean canLittleMaidSpawn(EntityType<LittleMaidEntity> littleMaid, IWorld worldIn,
                                             SpawnReason reason, BlockPos pos, Random random) {
        return worldIn.getBlockState(pos).canEntitySpawn(worldIn, pos, littleMaid)
                && worldIn.getLightSubtracted(pos, 0) > 8;
    }

    //登録メソッドたち

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(5, new HealMyselfGoal(this, this));
        //todo 手に持ったケーキや砂糖に反応するやーつ
        this.goalSelector.addGoal(10, new WaitGoal(this, this));
        this.goalSelector.addGoal(15, new ModeWrapperGoal(this));
        this.goalSelector.addGoal(20, new EscortGoal(this, this, 4, 3, 1));
        //todo ドロップアイテム回収Goal
        this.goalSelector.addGoal(20, new FreedomGoal(this, this, 1));
        this.goalSelector.addGoal(25, new WaterAvoidingRandomWalkingGoal(this, 1.0D) {
            @Override
            public boolean shouldExecute() {
                return LittleMaidEntity.this.getOwnerId() == null && super.shouldExecute();
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
    protected void registerData() {
        super.registerData();
        this.dataManager.register(AIMING, false);
        this.dataManager.register(MOVING_STATE, ITameable.NONE);
    }

    //読み書き系

    @Override
    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);

        this.writeInventory(compound);

        this.tameable.write(compound);

        this.writeSalary(compound);

        compound.putByte("SkinColor", (byte) getColor().getIndex());
        compound.putBoolean("IsContract", isContract());
        compound.putString("SkinTexture", getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            compound.putString("ArmorTextureInner" + part.getPartName(),
                    getTextureHolder(Layer.INNER, part).getTextureName());
            compound.putString("ArmorTextureOuter" + part.getPartName(),
                    getTextureHolder(Layer.OUTER, part).getTextureName());
        }

    }

    @Override
    public void readAdditional(CompoundNBT compound) {
        super.readAdditional(compound);

        this.readInventory(compound);

        this.tameable.read(compound);
        //2.2.0以前からの引継ぎ
        if (compound.hasUniqueId("OwnerId")) {
            this.setTameOwnerUuid(compound.getUniqueId("OwnerId"));
        }

        this.readSalary(compound);

        if (compound.contains("SkinColor")) {
            setColor(TextureColor.getColor(compound.getByte("SkinColor")));
        }
        setContract(compound.getBoolean("IsContract"));
        TextureManager textureManager = LittleMaidModelLoader.getInstance().getTextureManager();
        if (compound.contains("SkinTexture")) {
            textureManager.getTexture(compound.getString("SkinTexture"))
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD));
        }
        for (Part part : Part.values()) {
            String inner = "ArmorTextureInner" + part.getPartName();
            String outer = "ArmorTextureOuter" + part.getPartName();
            if (compound.contains(inner)) {
                textureManager.getTexture(compound.getString(inner))
                        .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.INNER, part));
            }
            if (compound.contains(outer)) {
                textureManager.getTexture(compound.getString(outer))
                        .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.OUTER, part));
            }
        }

    }

    //鯖
    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeEnumValue(getColor());
        buffer.writeBoolean(isContract());
        buffer.writeString(getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            buffer.writeString(getTextureHolder(Layer.INNER, part).getTextureName());
            buffer.writeString(getTextureHolder(Layer.OUTER, part).getTextureName());
        }
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        //readString()はクラ処理。このメソッドでは、クラ側なので問題なし
        setColor(additionalData.readEnumValue(TextureColor.class));
        setContract(additionalData.readBoolean());
        TextureManager textureManager = LittleMaidModelLoader.getInstance().getTextureManager();
        textureManager.getTexture(additionalData.readString())
                .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD));
        for (Part part : Part.values()) {
            textureManager.getTexture(additionalData.readString())
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.INNER, part));
            textureManager.getTexture(additionalData.readString())
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.OUTER, part));
        }
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
        if (hasTameOwner()) needSalary.tick();
        //アイテム回収処理
        fakePlayer.livingTick();
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);
        fakePlayer.onDeath(cause);
    }

    @Nullable
    @Override
    public AgeableEntity func_241840_a(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return null;
    }

    //このままだとEntitySizeが作っては捨てられてを繰り返すのでパフォーマンスがよろしくないかも
    @Nonnull
    @Override
    public EntitySize getSize(@Nonnull Pose poseIn) {
        EntitySize size;
        ModelMultiBase model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LittleMaidModelLoader.getInstance().getModelManager().getDefaultModel());
        float height = model.getHeight(getCaps());
        float width = model.getWidth(getCaps());
        size = new EntitySize(width, height, false);
        return size.scale(getRenderScale());
    }

    /**
     * 上に乗ってるエンティティへのオフセット
     * */
    @Override
    public double getMountedYOffset() {
        ModelMultiBase model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LittleMaidModelLoader.getInstance().getModelManager().getDefaultModel());
        return model.getMountedYOffset(getCaps());
    }

    /**
     * 騎乗時のオフセット
     * */
    @Override
    public double getYOffset() {
        ModelMultiBase model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LittleMaidModelLoader.getInstance().getModelManager().getDefaultModel());
        return model.getyOffset(getCaps()) - getHeight();
    }

    //防具の更新
    @Override
    public void setItemStackToSlot(EquipmentSlotType slotIn, @Nonnull ItemStack stack) {
        if (slotIn.getSlotType() == EquipmentSlotType.Group.ARMOR) {
            multiModel.updateArmor();
        }
        super.setItemStackToSlot(slotIn, stack);
    }

    //canSpawnとかでも使われる
    @Override
    public float getBlockPathWeight(BlockPos pos, IWorldReader worldIn) {
        return worldIn.getBlockState(pos.down()).getMaterial().isOpaque() ? 10.0F : worldIn.getBrightness(pos) - 0.5F;
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

    public boolean isFriend(Entity entity) {
        UUID ownerId = this.getOwnerId();
        if (ownerId != null) {
            //主はフレンド
            if (ownerId.equals(entity.getUniqueID())) {
                return true;
            }
            //同じ主を持つ者はフレンド
            if (entity instanceof ITameable && ownerId.equals(((ITameable) entity).getTameOwnerUuid().orElse(null))
                    || entity instanceof TameableEntity && ownerId.equals(((TameableEntity) entity).getOwnerId())) {
                return true;
            }
        }
        return false;
    }

    //todo 以下数メソッドにはもうちと整理が必要か

    //trueでアイテムが使用された、falseでされなかった
    //trueならItemStack.interactWithEntity()が起こらず、またアイテム使用が必ずキャンセルされる
    //継承元のコードは無視
    @Override
    public ActionResultType func_230254_b_(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSecondaryUseActive()) {
            return ActionResultType.PASS;
        }
        if (isStrike()) {
            //ケーキじゃないなら不満気にしてリターン
            if (stack.getItem() != Items.CAKE) {
                for (int i = 0; i < 5; i++) {
                    this.world.addParticle(ParticleTypes.SMOKE,
                            this.getPosX() + (0.5F - rand.nextFloat()) * 0.2F,
                            this.getPosYEye() + (0.5F - rand.nextFloat()) * 0.2F,
                            this.getPosZ() + (0.5F - rand.nextFloat()) * 0.2F,
                            0, 0.1, 0);
                }
                return ActionResultType.PASS;
            }
            //ケーキなら喜んで頂く
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
            return ActionResultType.PASS;
        }
        if (hasTameOwner()) {
            if (stack.getItem() == Items.SUGAR) {
                return changeState(player, stack);
            }
        } else {
            if (stack.getItem() == Items.CAKE) {
                return contract(player, stack);
            }
        }
        if (!player.world.isRemote && player.getUniqueID().equals(this.getOwnerId())) {
            openContainer(player);
            //Minecraft.getInstance().displayGuiScreen(new IFFScreen(new StringTextComponent(""), this, this));
        }
        return ActionResultType.PASS;
    }

    public ActionResultType changeState(PlayerEntity player, ItemStack stack) {
        this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.getRNG().nextFloat() * 0.1F + 1.0F);
        this.world.addParticle(ParticleTypes.NOTE, this
                        .getPosX(), this.getPosY() + this.getEyeHeight(), this.getPosZ(),
                0, this.rand.nextGaussian() * 0.02D, 0);
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
        return ActionResultType.func_233537_a_(world.isRemote);
    }

    public ActionResultType contract(PlayerEntity player, ItemStack stack) {
        this.world.addParticle(ParticleTypes.HEART,
                getPosX(), getPosY() + getEyeHeight(), getPosZ(),
                0, this.rand.nextGaussian() * 0.02D, 0);
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
        return ActionResultType.func_233537_a_(world.isRemote);
    }

    //GUI開くやつ
    //todo 開いている間動かないようにする
    public void openContainer(PlayerEntity player) {
        TextComponent movingState = new TranslationTextComponent(
                getType().getTranslationKey() + "." + getMovingState());
        if (getMode() != null) {
            ITextComponent modeName = new TranslationTextComponent(
                    getType().getTranslationKey() + "." + getMode().getName()
            );
            movingState.appendString(" : ").append(modeName);
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
    public Optional<LivingEntity> getTameOwner() {
        return Optional.ofNullable(getOwner());
    }

    @Override
    public void setTameOwnerUuid(UUID id) {
        setOwnerId(id);
    }

    @Override
    public Optional<UUID> getTameOwnerUuid() {
        return Optional.ofNullable(getOwnerId());
    }

    @Override
    public boolean hasTameOwner() {
        return getTameOwnerUuid().isPresent();
    }

    @Override
    public String getMovingState() {
        return tameable.getMovingState();
    }

    public void setMovingState(String string) {
        tameable.setMovingState(string);
    }

    @Override
    public boolean isTamed() {
        return hasTameOwner();
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

    @Override
    public boolean isAllowChangeTexture(@Nullable Entity entity, TextureHolder textureHolder, @Nonnull Layer layer, @Nonnull Part part) {
        return multiModel.isAllowChangeTexture(entity, textureHolder, layer, part);
    }

    @Override
    public void setTextureHolder(TextureHolder textureHolder, @Nonnull Layer layer, @Nonnull Part part) {
        multiModel.setTextureHolder(textureHolder, layer, part);
        if (layer == Layer.SKIN) {
            recalculateSize();
        }
    }

    @Override
    public TextureHolder getTextureHolder(@Nonnull Layer layer, @Nonnull Part part) {
        return multiModel.getTextureHolder(layer, part);
    }

    @Override
    public void setColor(TextureColor textureColor) {
        multiModel.setColor(textureColor);
    }

    @Override
    public TextureColor getColor() {
        return multiModel.getColor();
    }

    @Override
    public void setContract(boolean isContract) {
        multiModel.setContract(isContract);
    }

    /**
     * マルチモデルの使用テクスチャが契約時のものかどうか
     * ※実際に契約状態かどうかをチェックする場合、
     * {@link #hasTameOwner()}か、
     * {@link #getTameOwnerUuid()}の返り値が存在するかでチェックすること
     * */
    @Override
    public boolean isContract() {
        return multiModel.isContract();
    }

    @Override
    public Optional<ModelMultiBase> getModel(@Nonnull Layer layer, @Nonnull Part part) {
        return multiModel.getModel(layer, part);
    }

    @Override
    public Optional<ResourceLocation> getTexture(@Nonnull Layer layer, @Nonnull Part part, boolean isLight) {
        return multiModel.getTexture(layer, part, isLight);
    }

    @Nonnull
    @Override
    public IModelCaps getCaps() {
        return multiModel.getCaps();
    }

    @Override
    public boolean isArmorVisible(Part part) {
        return multiModel.isArmorVisible(part);
    }

    @Override
    public boolean isArmorGlint(Part part) {
        return multiModel.isArmorGlint(part);
    }

    //オーバーライドしなくても動くが、IEntityAdditionalSpawnDataが機能しない
    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

}
