package net.sistr.littlemaidrebirth.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.sistr.littlemaidrebirth.Config;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.entity.iff.*;
import net.sistr.littlemaidrebirth.entity.mode.*;
import net.sistr.littlemaidrebirth.item.IFFCopyBookItem;
import net.sistr.littlemaidrebirth.setup.Registration;
import net.sistr.littlemaidrebirth.tags.LMTags;
import net.sistr.littlemaidrebirth.util.LivingAccessor;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.SnowballEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import net.sistr.littlemaidrebirth.entity.goal.*;
import net.sistr.lmml.entity.compound.IHasMultiModel;
import net.sistr.lmml.entity.compound.MultiModelCompound;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.entity.compound.SoundPlayableCompound;
import net.sistr.lmml.maidmodel.IModelCaps;
import net.sistr.lmml.maidmodel.ModelMultiBase;
import net.sistr.lmml.resource.holder.ConfigHolder;
import net.sistr.lmml.resource.holder.TextureHolder;
import net.sistr.lmml.resource.manager.LMConfigManager;
import net.sistr.lmml.resource.manager.LMModelManager;
import net.sistr.lmml.resource.manager.LMTextureManager;
import net.sistr.lmml.resource.util.LMSounds;
import net.sistr.lmml.resource.util.TextureColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

//メイドさん本体
//継承しないのが吉
//todo 啼くように、このクラスの行数を500まで減らす
public class LittleMaidEntity extends TameableEntity implements IEntityAdditionalSpawnData, InventorySupplier, Tameable,
        NeedSalary, ModeSupplier, HasIFF, AimingPoseable, FakePlayerSupplier, IHasMultiModel, SoundPlayable {
    //変数群。カオス
    private static final DataParameter<Byte> MOVING_STATE = EntityDataManager.createKey(LittleMaidEntity.class, DataSerializers.BYTE);
    private static final DataParameter<Boolean> AIMING = EntityDataManager.createKey(LittleMaidEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> BEGGING = EntityDataManager.createKey(LittleMaidEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<String> MODE_NAME = EntityDataManager.createKey(LittleMaidEntity.class, DataSerializers.STRING);
    private final LMFakePlayerSupplier fakePlayer = new LMFakePlayerSupplier(this);
    private final InventorySupplier littleMaidInventory = new LMInventorySupplier(this, fakePlayer);
    //todo お給料機能とテイム機能一緒にした方がよさげ
    private final TickTimeBaseNeedSalary needSalary =
            new TickTimeBaseNeedSalary(this, this, 7, Lists.newArrayList(LMTags.Items.MAIDS_SALARY.getAllElements()));
    private final ModeController modeController = new ModeController(this, this, Sets.newHashSet());
    private final MultiModelCompound multiModel;
    private final SoundPlayableCompound soundPlayer;
    private final HasIFF iff;
    private final IModelCaps caps = new LittleMaidModelCaps(this);
    private BlockPos freedomPos;
    private LivingEntity prevTarget;
    @OnlyIn(Dist.CLIENT)
    private float interestedAngle;
    @OnlyIn(Dist.CLIENT)
    private float prevInterestedAngle;

    //コンストラクタ
    public LittleMaidEntity(EntityType<LittleMaidEntity> type, World worldIn) {
        super(type, worldIn);
        this.moveController = new FixedMoveControl(this);
        ((GroundPathNavigator) getNavigator()).setBreakDoors(true);
        multiModel = new MultiModelCompound(this,
                LMTextureManager.INSTANCE.getTexture("Default")
                        .orElseThrow(() -> new IllegalStateException("デフォルトテクスチャが存在しません。")),
                LMTextureManager.INSTANCE.getTexture("Default")
                        .orElseThrow(() -> new IllegalStateException("デフォルトテクスチャが存在しません。")));
        soundPlayer = new SoundPlayableCompound(this, () ->
                multiModel.getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        addDefaultModes(this);
        iff = new IFFImpl(IFFTypeManager.getINSTANCE().getIFFTypes(world).stream()
                .map(IFFType::createIFF).collect(Collectors.toList()));
    }

    //基本使わない
    public LittleMaidEntity(World world) {
        this(Registration.LITTLE_MAID_MOB.get(), world);
    }

    //スタティックなメソッド

    public static AttributeModifierMap.MutableAttribute registerAttributes() {
        return TameableEntity.registerAttributes()
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.3D)
                .createMutableAttribute(Attributes.ATTACK_DAMAGE)
                .createMutableAttribute(Attributes.ATTACK_SPEED)
                .createMutableAttribute(Attributes.LUCK)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 16D)
                .createMutableAttribute(ForgeMod.REACH_DISTANCE.get());
    }

    public static boolean canLittleMaidSpawn(IWorld worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos.down()).isOpaqueCube(worldIn, pos)
                && worldIn.getLightSubtracted(pos, 0) > 8;
    }

    //登録メソッドたち

    public void addDefaultModes(LittleMaidEntity maid) {
        maid.addMode(new FencerMode<>(maid, 1D, true));
        maid.addMode(new ArcherMode<>(maid, 15F,
                entity -> entity instanceof LivingEntity && isFriend((LivingEntity) entity)));
        maid.addMode(new CookingMode<>(maid, 1, 1 + 18));
        maid.addMode(new RipperMode(maid, 8));
        maid.addMode(new TorcherMode<>(maid, 8));
        maid.addMode(new HealerMode<>(maid, 0, 1 + 18));
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(5, new HealMyselfGoal<>(this,
                Sets.newHashSet(LMTags.Items.MAIDS_SALARY.getAllElements()), 2, 1));
        this.goalSelector.addGoal(10, new WaitGoal(this, this));
        //todo 挙動が怪しい
        /*this.goalSelector.addGoal();(12, new WaitWhenOpenGUIGoal<>(this, this,
                LittleMaidScreenHandler.class));*/
        this.goalSelector.addGoal(13, new EscortGoal<>(this, 1.5D,
                8F, 16F, 24F, true));
        this.goalSelector.addGoal(15, new ModeWrapperGoal(this));
        this.goalSelector.addGoal(16, new FollowAtHeldItemGoal(this, this, true,
                Sets.newHashSet(LMTags.Items.MAIDS_SALARY.getAllElements())));
        this.goalSelector.addGoal(17, new LMStareAtHeldItemGoal(this, this, false
                , Sets.newHashSet(LMTags.Items.MAIDS_EMPLOYABLE.getAllElements())));
        this.goalSelector.addGoal(17, new LMStareAtHeldItemGoal(this, this, true,
                Sets.newHashSet(LMTags.Items.MAIDS_SALARY.getAllElements())));
        this.goalSelector.addGoal(18, new LMMoveToDropItemGoal(this, 8, 1D));
        this.goalSelector.addGoal(19, new EscortGoal<>(this, 1.5D,
                6F, 8F, 12F, true));
        this.goalSelector.addGoal(20, new EscortGoal<>(this, 1.0D,
                4F, 5F, 12F, true));
        this.goalSelector.addGoal(20, new FreedomGoal<>(this, 0.8D, 16D));
        this.goalSelector.addGoal(30, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(30, new LookRandomlyGoal(this));

        this.targetSelector.addGoal(3, new PredicateRevengeGoal(this, entity -> !isFriend(entity)));
        this.targetSelector.addGoal(4, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(5, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 5, true, false,
                this::isEnemy));
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(MOVING_STATE, (byte) 0);
        this.dataManager.register(AIMING, false);
        this.dataManager.register(BEGGING, false);
        this.dataManager.register(MODE_NAME, "");
    }

    //読み書き系

    @Override
    public void writeAdditional(CompoundNBT tag) {
        super.writeAdditional(tag);

        writeInventory(tag);

        tag.putInt("MovingState", getMovingState().getId());

        if (freedomPos != null)
            tag.put("FreedomPos", NBTUtil.writeBlockPos(freedomPos));

        needSalary.writeSalary(tag);

        writeModeData(tag);

        tag.putByte("SkinColor", (byte) getColor().getIndex());
        tag.putBoolean("IsContract", isContract());
        tag.putString("SkinTexture", getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            tag.putString("ArmorTextureInner" + part.getPartName(),
                    getTextureHolder(Layer.INNER, part).getTextureName());
            tag.putString("ArmorTextureOuter" + part.getPartName(),
                    getTextureHolder(Layer.OUTER, part).getTextureName());
        }

        tag.putString("SoundConfigName", getConfigHolder().getName());

        writeIFF(tag);

    }

    @Override
    public void readAdditional(CompoundNBT tag) {
        super.readAdditional(tag);
        readInventory(tag);

        setMovingState(MovingState.fromId(tag.getInt("MovingState")));
        //後で消す
        String old = tag.getString("MovingState");
        if (!old.isEmpty()) {
            setMovingState(MovingState.fromName(old));
        }

        if (tag.contains("FreedomPos"))
            freedomPos = NBTUtil.readBlockPos(tag.getCompound("FreedomPos"));

        needSalary.readSalary(tag);

        readModeData(tag);

        if (tag.contains("SkinColor"))
            setColor(TextureColors.getColor(tag.getByte("SkinColor")));
        setContract(tag.getBoolean("IsContract"));
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        if (tag.contains("SkinTexture")) {
            textureManager.getTexture(tag.getString("SkinTexture"))
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD));
        }
        for (Part part : Part.values()) {
            String inner = "ArmorTextureInner" + part.getPartName();
            String outer = "ArmorTextureOuter" + part.getPartName();
            if (tag.contains(inner)) {
                textureManager.getTexture(tag.getString(inner))
                        .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.INNER, part));
            }
            if (tag.contains(outer)) {
                textureManager.getTexture(tag.getString(outer))
                        .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.OUTER, part));
            }
        }

        if (tag.contains("SoundConfigName"))
            LMConfigManager.INSTANCE.getConfig(tag.getString("SoundConfigName"))
                    .ifPresent(this::setConfigHolder);

        readIFF(tag);
    }

    //鯖
    @Override
    public void writeSpawnData(PacketBuffer buf) {
        //モデル
        buf.writeEnumValue(getColor());
        buf.writeBoolean(isContract());
        buf.writeString(getTextureHolder(Layer.SKIN, Part.HEAD).getTextureName());
        for (Part part : Part.values()) {
            buf.writeString(getTextureHolder(Layer.INNER, part).getTextureName());
            buf.writeString(getTextureHolder(Layer.OUTER, part).getTextureName());
        }
        //サウンド
        buf.writeString(getConfigHolder().getName());
    }

    @Override
    public void readSpawnData(PacketBuffer buf) {
        //モデル
        //readString()はクラ処理。このメソッドはクラ側なので問題なし
        setColor(buf.readEnumValue(TextureColors.class));
        setContract(buf.readBoolean());
        LMTextureManager textureManager = LMTextureManager.INSTANCE;
        textureManager.getTexture(buf.readString())
                .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.SKIN, Part.HEAD));
        for (Part part : Part.values()) {
            textureManager.getTexture(buf.readString())
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.INNER, part));
            textureManager.getTexture(buf.readString())
                    .ifPresent(textureHolder -> setTextureHolder(textureHolder, Layer.OUTER, part));
        }
        //サウンド
        LMConfigManager.INSTANCE.getConfig(buf.readString())
                .ifPresent(this::setConfigHolder);
    }

    //バニラメソッズ

    @Override
    public void tick() {
        super.tick();
        fakePlayer.tick();
        updateArmSwingProgress();
        if (hasTameOwner()) needSalary.tick();
        if (world.isRemote) {
            tickInterestedAngle();
        }
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        modeController.tick();
        LivingEntity target = getAttackTarget();
        if (target != null && target != prevTarget) {
            play(LMSounds.FIND_TARGET_N);
        }
        prevTarget = target;
    }

    @Override
    public boolean canDespawn(double distanceToClosestPlayer) {
        return Config.CAN_DESPAWN_LM.get() && !hasTameOwner();
    }

    //canSpawnとかでも使われる
    @Override
    public float getBlockPathWeight(BlockPos pos, IWorldReader worldIn) {
        return worldIn.getBlockState(pos.down()).isOpaqueCube(worldIn, pos) ? 10.0F : worldIn.getBrightness(pos) - 0.5F;
    }

    @Override
    protected float getDropChance(EquipmentSlotType slotIn) {
        return 0;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && !isFriend(target);
    }

    @Nullable
    @Override
    public AgeableEntity func_241840_a(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return null;
    }

    /**
     * 上に乗ってるエンティティへのオフセット
     */
    @Override
    public double getMountedYOffset() {
        ModelMultiBase model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LMModelManager.INSTANCE.getDefaultModel());
        return model.getMountedYOffset(getCaps());
    }

    /**
     * 騎乗時のオフセット
     */
    @Override
    public double getYOffset() {
        ModelMultiBase model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LMModelManager.INSTANCE.getDefaultModel());
        return model.getyOffset(getCaps()) - getHeight();
    }

    //このままだとEntitySizeが作っては捨てられてを繰り返すのでパフォーマンスがよろしくないかも
    @Nonnull
    @Override
    public EntitySize getSize(@Nonnull Pose poseIn) {
        EntitySize size;
        ModelMultiBase model = getModel(Layer.SKIN, Part.HEAD)
                .orElse(LMModelManager.INSTANCE.getDefaultModel());
        float height = model.getHeight(getCaps());
        float width = model.getWidth(getCaps());
        size = EntitySize.flexible(width, height);
        return size.scale(getRenderScale());
    }

    @Override
    public void playAmbientSound() {
        if (world.isRemote) {
            return;
        }
        if (0.2F < rand.nextFloat()) {
            return;
        }
        //todo ここ拡張可能にする
        if (getHealth() / getMaxHealth() < 0.3F) {
            play(LMSounds.LIVING_WHINE);
        } else if (this.getHeldItemMainhand().getItem() == Items.CLOCK) {
            long time = world.getDayTime();
            if (0 <= time && time < 1000) {
                if (time % 2 == 0)
                    play(LMSounds.GOOD_MORNING);
                else
                    play(LMSounds.LIVING_MORNING);
            } else if (12542 <= time && time < 13500) {
                if (time % 2 == 0)
                    play(LMSounds.GOOD_NIGHT);
                else
                    play(LMSounds.LIVING_NIGHT);
            }
        } else if (world.isRaining()) {
            Biome biome = this.world.getBiome(getPosition());
            if (biome.getPrecipitation() == Biome.RainType.RAIN)
                play(LMSounds.LIVING_RAIN);
            else if (biome.getPrecipitation() == Biome.RainType.SNOW)
                play(LMSounds.LIVING_SNOW);
        } else {
            Biome biome = this.world.getBiome(getPosition());
            float temperature = biome.getTemperature(getPosition());
            if (temperature < 0.1F) {
                play(LMSounds.LIVING_COLD);
            } else if (1 < temperature) {
                play(LMSounds.LIVING_HOT);
            } else {
                play(LMSounds.LIVING_DAYTIME);
            }
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        play(LMSounds.DEATH);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!world.isRemote) {
            //味方のが当たってもちゃんと動くようにフレンド判定より前
            if (amount <= 0 && source.getImmediateSource() instanceof SnowballEntity) {
                play(LMSounds.HURT_SNOW);
                return false;
            }
        }
        Entity attacker = source.getTrueSource();
        //Friendからの攻撃を除外
        if (attacker instanceof LivingEntity && isFriend((LivingEntity) attacker)) {
            return false;
        }
        boolean isHurtTime = 0 < this.hurtTime;
        boolean result = super.attackEntityFrom(source, amount);
        if (!world.isRemote && !isHurtTime) {
            if (!result || amount <= 0F) {
                play(LMSounds.HURT_NO_DAMAGE);
            } else if (amount > 0F && ((LivingAccessor) this).blockedByShield_LM(source)) {
                play(LMSounds.HURT_GUARD);
            } else if (source == DamageSource.FALL) {
                play(LMSounds.HURT_FALL);
            } else if (source.isFireDamage()) {
                play(LMSounds.HURT_FIRE);
            } else {
                play(LMSounds.HURT);
            }
        }
        return result;
    }

    //todo 以下数メソッドにはもうちと整理が必要か

    //trueでアイテムが使用された、falseでされなかった(1.16からはSUCCESS(true)とPASS(false))
    //trueならItemStack.interactWithEntity()が起こらず、またアイテム使用が必ずキャンセルされる
    //継承元のコードは無視
    @Override
    public ActionResultType func_230254_b_(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSecondaryUseActive() || stack.getItem() instanceof IFFCopyBookItem) {
            return ActionResultType.PASS;
        }
        if (!hasTameOwner()) {
            if (LMTags.Items.MAIDS_EMPLOYABLE.contains(stack.getItem())) {
                return contract(player, stack, false);
            }
            return ActionResultType.PASS;
        }
        if (!player.getUniqueID().equals(this.getOwnerId())) {
            return ActionResultType.PASS;
        }
        if (isStrike()) {
            if (LMTags.Items.MAIDS_EMPLOYABLE.contains(stack.getItem())) {
                return contract(player, stack, true);
            } else if (world instanceof ServerWorld) {
                ((ServerWorld) world).spawnParticle(ParticleTypes.SMOKE,
                        this.getPosX() + (0.5F - rand.nextFloat()) * 0.2F,
                        this.getPosYEye() + (0.5F - rand.nextFloat()) * 0.2F,
                        this.getPosZ() + (0.5F - rand.nextFloat()) * 0.2F,
                        5,
                        0, 1, 0, 0.1);
            }
            return ActionResultType.PASS;
        }
        if (LMTags.Items.MAIDS_SALARY.contains(stack.getItem())) {
            return changeState(player, stack);
        }
        if (!player.world.isRemote) {
            openContainer(player);
        }
        return ActionResultType.func_233537_a_(world.isRemote);
    }

    public ActionResultType changeState(PlayerEntity player, ItemStack stack) {
        this.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, this.rand.nextFloat() * 0.1F + 1.0F);
        this.world.addParticle(ParticleTypes.NOTE, this
                        .getPosX(), this.getPosY() + this.getEyeHeight(), this.getPosZ(),
                0, this.rand.nextGaussian() * 0.02D, 0);
        this.getNavigator().clearPath();
        changeMovingState();
        if (!player.abilities.isCreativeMode) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.inventory.deleteStack(stack);
            }
        }
        return ActionResultType.func_233537_a_(world.isRemote);
    }

    public void changeMovingState() {
        MovingState state = this.getMovingState();
        if (state == MovingState.WAIT) {
            setMovingState(MovingState.ESCORT);
        } else {
            setMovingState(MovingState.WAIT);
        }
    }

    public ActionResultType contract(PlayerEntity player, ItemStack stack, boolean isReContract) {
        this.world.addParticle(ParticleTypes.HEART,
                getPosX(), getPosY() + getEyeHeight(), getPosZ(),
                0, this.rand.nextGaussian() * 0.02D, 0);
        if (!world.isRemote) {
            if (isReContract) {
                play(LMSounds.RECONTRACT);
            } else {
                play(LMSounds.GET_CAKE);
            }
        }
        if (isReContract) {
            setStrike(false);
        }
        while (receiveSalary(1)) ;//ここに給料処理が混じってるのがちょっとムカつく
        getNavigator().clearPath();
        this.setOwnerId(player.getUniqueID());
        setMovingState(MovingState.ESCORT);
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
    public void openContainer(PlayerEntity player) {
        setAttackTarget(null);
        getNavigator().clearPath();
        setModeName(getMode().map(Mode::getName).orElse(""));
        NetworkHooks.openGui((ServerPlayerEntity) player,
                new SimpleNamedContainerProvider((windowId, inv, playerEntity) ->
                        new LittleMaidContainer(windowId, inv, this.getEntityId()), new StringTextComponent("")),
                buf -> buf.writeVarInt(this.getEntityId()));
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

    //防具の更新およびオフハンドの位置ズラし
    @Override
    public void setItemStackToSlot(EquipmentSlotType slot, ItemStack stack) {
        if (slot.getSlotType() == EquipmentSlotType.Group.ARMOR) {
            IInventory inv = getInventory();
            inv.setInventorySlotContents(1 + 18 + slot.getIndex(), stack);
            multiModel.updateArmor();
        } else if (slot == EquipmentSlotType.MAINHAND) {
            getInventory().setInventorySlotContents(0, stack);
        } else if (slot == EquipmentSlotType.OFFHAND) {
            getInventory().setInventorySlotContents(18 + 4 + 1, stack);
        }
    }

    @Override
    public ItemStack getItemStackFromSlot(EquipmentSlotType slot) {
        if (slot.getSlotType() == EquipmentSlotType.Group.ARMOR) {
            return getInventory().getStackInSlot(1 + 18 + slot.getIndex());
        } else if (slot == EquipmentSlotType.MAINHAND) {
            return getInventory().getStackInSlot(0);
        } else if (slot == EquipmentSlotType.OFFHAND) {
            return getInventory().getStackInSlot(18 + 4 + 1);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Iterable<ItemStack> getHeldEquipment() {
        return () -> Lists.newArrayList(getHeldItemMainhand(), getHeldItemOffhand()).iterator();
    }

    @Override
    public Iterable<ItemStack> getArmorInventoryList() {
        return Lists.newArrayList(
                getItemStackFromSlot(EquipmentSlotType.FEET),
                getItemStackFromSlot(EquipmentSlotType.LEGS),
                getItemStackFromSlot(EquipmentSlotType.CHEST),
                getItemStackFromSlot(EquipmentSlotType.HEAD));
    }

    @Override
    protected void dropInventory() {
        //鯖側でしか動かないが一応チェック
        IInventory inv = this.getInventory();
        if (inv instanceof PlayerInventory)
            ((LMInventorySupplier.LMInventory) inv).dropAllItems();
    }

    @Override
    protected void damageArmor(DamageSource damageSource, float damage) {
        super.damageArmor(damageSource, damage);
        ((PlayerInventory) getInventory()).func_234563_a_(damageSource, damage);
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
    public MovingState getMovingState() {
        int num = this.dataManager.get(MOVING_STATE);
        return MovingState.fromId(num);
    }

    @Override
    public void setMovingState(MovingState movingState) {
        int num = movingState.getId();
        this.dataManager.set(MOVING_STATE, (byte) num);
    }

    @Override
    public void setFreedomPos(BlockPos freedomPos) {
        this.freedomPos = freedomPos;
    }

    @Override
    public BlockPos getFreedomPos() {
        if (freedomPos == null) freedomPos = getPosition();
        return freedomPos;
    }

    @Override
    public void setSleeping(boolean p_233686_1_) {

    }

    @Override
    public boolean isEntitySleeping() {
        return false;
    }

    @Override
    public void func_233687_w_(boolean sitting) {
        setMovingState(sitting ? MovingState.WAIT : MovingState.ESCORT);
    }

    @Override
    public boolean isSitting() {
        return getMovingState() == MovingState.WAIT;
    }

    @Override
    public boolean isTamed() {
        return hasTameOwner();
    }

    public boolean isBegging() {
        return this.dataManager.get(BEGGING);
    }

    public void setBegging(boolean begging) {
        this.dataManager.set(BEGGING, begging);
    }

    @OnlyIn(Dist.CLIENT)
    public float getInterestedAngle(float tickDelta) {
        return (prevInterestedAngle + (interestedAngle - prevInterestedAngle) * tickDelta) *
                ((getEntityId() % 2 == 0 ? 0.08F : -0.08F) * (float) Math.PI);
    }

    @OnlyIn(Dist.CLIENT)
    private void tickInterestedAngle() {
        prevInterestedAngle = interestedAngle;
        if (isBegging()) {
            interestedAngle = interestedAngle + (1.0F - interestedAngle) * 0.4F;
        } else {
            interestedAngle = interestedAngle + (0.0F - interestedAngle) * 0.4F;
        }
    }

    //お給料

    @Override
    public boolean receiveSalary(int num) {
        return needSalary.receiveSalary(num);
    }

    @Override
    public boolean consumeSalary(int num) {
        boolean result = needSalary.consumeSalary(num);
        if (result) {
            this.playSound(SoundEvents.ENTITY_ITEM_PICKUP,
                    1.0F, this.getRNG().nextFloat() * 0.1F + 1.0F);
            this.swingArm(Hand.MAIN_HAND);
        }
        return result;
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
    public void setStrike(boolean strike) {
        needSalary.setStrike(strike);
    }

    //モード機能

    @Override
    public Optional<Mode> getMode() {
        return modeController.getMode();
    }

    @Override
    public void writeModeData(CompoundNBT tag) {
        modeController.writeModeData(tag);
    }

    @Override
    public void readModeData(CompoundNBT tag) {
        modeController.readModeData(tag);
    }

    public void addMode(Mode mode) {
        modeController.addMode(mode);
    }

    public void setModeName(String modeName) {
        this.dataManager.set(MODE_NAME, modeName);
    }

    @OnlyIn(Dist.CLIENT)
    public Optional<String> getModeName() {
        String modeName = this.dataManager.get(MODE_NAME);
        if (modeName.isEmpty()) return Optional.empty();
        return Optional.of(modeName);
    }

    //IFF

    @Override
    public IFFTag identify(LivingEntity target) {
        return iff.identify(target);
    }

    @Override
    public void setIFFs(List<IFF> iffs) {
        iff.setIFFs(iffs);
    }

    @Override
    public List<IFF> getIFFs() {
        return iff.getIFFs();
    }

    @Override
    public void writeIFF(CompoundNBT tag) {
        iff.writeIFF(tag);
    }

    @Override
    public void readIFF(CompoundNBT tag) {
        iff.readIFF(tag);
    }

    @Override
    public boolean shouldAttackEntity(LivingEntity target, LivingEntity owner) {
        return identify(target) != IFFTag.FRIEND;
    }

    public boolean isFriend(LivingEntity entity) {
        UUID ownerId = this.getOwnerId();
        if (ownerId != null) {
            //主はフレンド
            if (ownerId.equals(entity.getUniqueID())) {
                return true;
            }
            //同じ主を持つ者はフレンド
            if (entity instanceof Tameable && ownerId.equals(((Tameable) entity).getTameOwnerUuid().orElse(null))
                    || entity instanceof TameableEntity && ownerId.equals(((TameableEntity) entity).getOwnerId())) {
                return true;
            }
        }
        return identify(entity) == IFFTag.FRIEND;
    }

    public boolean isEnemy(LivingEntity entity) {
        return identify(entity) == IFFTag.ENEMY;
    }

    //エイム

    @Override
    public boolean isAimingBow() {
        return this.dataManager.get(AIMING);
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
    public void setColor(TextureColors textureColor) {
        multiModel.setColor(textureColor);
    }

    @Override
    public TextureColors getColor() {
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
     * {@link #getOwnerId()}の返り値が存在するかでチェックすること
     */
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
        return caps;
    }

    @Override
    public boolean isArmorVisible(Part part) {
        return multiModel.isArmorVisible(part);
    }

    @Override
    public boolean isArmorGlint(Part part) {
        return multiModel.isArmorGlint(part);
    }

    //音声関係

    @Override
    public void play(String soundName) {
        soundPlayer.play(soundName);
    }

    @Override
    public void setConfigHolder(ConfigHolder configHolder) {
        soundPlayer.setConfigHolder(configHolder);
    }

    @Override
    public ConfigHolder getConfigHolder() {
        return soundPlayer.getConfigHolder();
    }

    //オーバーライドしなくても動くが、IEntityAdditionalSpawnDataが機能しない
    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public static class LMMoveToDropItemGoal extends MoveToDropItemGoal {
        private final LittleMaidEntity maid;

        public LMMoveToDropItemGoal(LittleMaidEntity maid, int range, double speed) {
            super(maid, range, speed);
            this.maid = maid;
        }

        @Override
        public boolean shouldExecute() {
            return ((PlayerInventory) maid.getInventory()).getFirstEmptyStack() != -1 && super.shouldExecute();
        }
    }

    public static class LMStareAtHeldItemGoal extends TameableStareAtHeldItemGoal {
        private final LittleMaidEntity maid;

        public LMStareAtHeldItemGoal(LittleMaidEntity maid, Tameable tameable, boolean isTamed, Set<Item> items) {
            super(maid, tameable, isTamed, items);
            this.maid = maid;
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            maid.setBegging(true);
        }

        @Override
        public void resetTask() {
            super.resetTask();
            maid.setBegging(false);
        }
    }

}
