package com.example.weakspot.client;

import com.example.weakspot.AnimalTargets;
import com.example.weakspot.RightClickTargets;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.BlockHealthBar;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.HitStreak;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.network.HitMessage;
import java.util.Arrays;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * クライアント側の弱点処理。弱点の位置は自分のクライアントだけが持ち、他プレイヤーとは共有しない。
 * 左クリックの長押し（採掘）と、右クリックの押しっぱなし（作物・苗木、機械）の弱点を扱う。弱点は一度に1つだけ。
 *
 * ヒット判定は毎フレーム行う（素早く照準を動かしたときに tick 単位だと取りこぼすため）。
 * 破壊速度のブーストは PlayerControllerMP の tick ごとの進捗に掛かるので、tick 単位で管理する。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
public final class ClientWeakSpotHandler {

    private static final Random RANDOM = new Random();

    /** ClientTickEvent の START で増える。PlayerControllerMP の進捗計算はその後に走る。 */
    static long clientTick;
    static WeakSpot spot;
    /** 今のフレームの partialTicks（ヒットした瞬間を、フレームの途中の値まで含めてコンボの表示に渡す）。 */
    private static float framePartialTicks;

    /** 種類ごとの最後のヒット（ヒット間隔の制限に使う）。 */
    private static final long[] LAST_HIT_TICK = new long[HitKind.values().length];
    /** 連続ヒット数。ブロックや種類をまたいで続き、ヒット音のピッチとコンボの表示に使う。 */
    static final HitStreak STREAK = new HitStreak();
    /** 死亡・ディメンション移動（どちらもプレイヤーが作り直される）を見分けるため。 */
    private static EntityPlayerSP lastPlayer;
    private static BlockPos boostPos;
    private static long boostHitTick = Long.MIN_VALUE / 2;
    /** 瞬間破壊の判定中は、自分のブーストを掛けない。 */
    private static boolean suppressBoost;

    static {
        Arrays.fill(LAST_HIT_TICK, Long.MIN_VALUE / 2);
    }

    private ClientWeakSpotHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        ToggleKeyHandler.remindIfOff(mc);
        ToggleKeyHandler.syncToServer(mc);
        if (mc.world == null || mc.player == null) {
            reset();
            return;
        }
        if (mc.player != lastPlayer || mc.player.getHealth() <= 0) {
            // 死亡したとき、リスポーンやディメンション移動でプレイヤーが作り直されたときは、連続ヒットを最初に戻す
            lastPlayer = mc.player;
            resetStreak();
        }
        if (mc.isGamePaused()) {
            return;
        }
        clientTick++;
        MiningProgress.sample(mc.playerController);
        int broken = STREAK.expire(clientTick);
        if (broken > 0) {
            ComboHud.onBreak(broken, clientTick);
        }
        if (!WeakSpotConfig.weakSpotsEnabled) {
            stopOwnWeakSpots();
        }
        if (spot != null && (isGone(mc.world, spot)
                || clientTick - spot.lastActiveTick > ClientSettings.get().lingerTicks)) {
            spot = null;
        }
        WeakSpotRenderer.expireFlashes(clientTick);
    }

    /** 弱点を出したブロックがなくなった（壊れた、育ちきった、など）。 */
    private static boolean isGone(World world, WeakSpot spot) {
        if (spot.kind == HitKind.ANIMAL) {
            return spot.entity == null || spot.entity.isDead;
        }
        if (world.isAirBlock(spot.pos)) {
            return true;
        }
        IBlockState state = world.getBlockState(spot.pos);
        switch (spot.kind) {
            case GROWTH:
                return !RightClickTargets.isGrowable(world, spot.pos, state, ClientSettings.get());
            case MACHINE:
                return !RightClickTargets.isMachine(world, spot.pos, state, ClientSettings.get());
            default:
                return false;
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) {
            return;
        }
        framePartialTicks = event.getPartialTicks();
        double health = -1;
        double growth = -1;
        double animal = -1;
        if (WeakSpotConfig.weakSpotsEnabled) {
            updateAim(mc);
            if (WeakSpotConfig.blockHealthBarEnabled) {
                health = healthBarRemaining(mc, event.getPartialTicks());
            }
            if (WeakSpotConfig.growthBarEnabled) {
                growth = growthBarProgress(mc);
            }
            animal = animalBarProgress();
            if (spot != null && spot.entity != null) {
                // 動物の弱点は、描く時点の動物の位置に合わせる（当たり判定は updateAim で、その tick の位置に合わせた）
                spot.follow(WeakSpot.renderBox(spot.entity, event.getPartialTicks()));
            }
        } else {
            // 一時オフ。J を押した直後のフレームでも、自分の弱点を出さない
            stopOwnWeakSpots();
        }
        WeakSpotRenderer.render(mc, spot, health, growth, animal, clientTick, event.getPartialTicks());
    }

    /**
     * 耐久バーに出す、掘っているブロックの残りの耐久（0〜1）。出さないときは -1。
     * 採掘の弱点がこのフレームの照準の面に出ていて（弱点が出るブロックで、クリエイティブでない）、
     * そのブロックを今掘っているときだけ出す。長押しをやめた・壊れたときは、掘っていない扱いになってすぐに消える。
     */
    private static double healthBarRemaining(Minecraft mc, float partialTicks) {
        RayTraceResult target = mc.objectMouseOver;
        if (spot == null || spot.lastActiveTick != clientTick || mc.player.capabilities.isCreativeMode
                || target == null || target.typeOfHit != RayTraceResult.Type.BLOCK
                || !spot.matches(HitKind.MINING, target.getBlockPos(), target.sideHit)) {
            return -1;
        }
        double progress = MiningProgress.progress(mc.playerController, spot.pos, partialTicks);
        return progress < 0 ? -1 : BlockHealthBar.remaining(progress);
    }

    /** 成長バーに出す作物の進み具合（0〜1）。成長の弱点が今出ているときだけ。出さないときは -1。 */
    private static double growthBarProgress(Minecraft mc) {
        if (spot == null || spot.kind != HitKind.GROWTH || spot.lastActiveTick != clientTick) {
            return -1;
        }
        return GrowthBar.progress(mc.world, spot.pos);
    }

    /** 動物の足元のバーに出す進み具合（0〜1）。動物の弱点が今出ていて、サーバーの返事があるときだけ。出さないときは -1。 */
    private static double animalBarProgress() {
        if (spot == null || spot.kind != HitKind.ANIMAL || spot.lastActiveTick != clientTick) {
            return -1;
        }
        AnimalStates.State state = AnimalStates.get(spot.entity.getEntityId(), clientTick);
        return state == null ? -1 : state.barProgress();
    }

    /**
     * 弱点の一時オフ（J キー）の間、自分の弱点を出さず、ヒットも起こさない。
     * 出ていた弱点は消す（自分のマークの送信は、弱点が null になると「消えた」を送る）。ブーストも止める。
     */
    static void stopOwnWeakSpots() {
        spot = null;
        boostPos = null;
        boostHitTick = Long.MIN_VALUE / 2;
        WeakSpotRenderer.clearFlashes();
        FishingSpot.clear();
    }

    private static void updateAim(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player.capabilities.isCreativeMode || player.isSpectator()) {
            return;
        }
        RayTraceResult target = mc.objectMouseOver;
        if (target == null) {
            return;
        }
        if (target.typeOfHit == RayTraceResult.Type.ENTITY) {
            if (!mc.playerController.getIsHittingBlock() && isHoldingUse(mc)) {
                aimAnimal(mc, target);
            }
            return;
        }
        if (target.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        if (mc.playerController.getIsHittingBlock()) {
            aimMining(mc, target);
        } else if (isHoldingUse(mc)) {
            aimRightClick(mc, target);
        }
    }

    /** 右クリックを押しっぱなしにしているか（バニラが右クリックを繰り返す条件と同じ）。 */
    private static boolean isHoldingUse(Minecraft mc) {
        return mc.currentScreen == null && mc.gameSettings.keyBindUseItem.isKeyDown() && !mc.player.isHandActive();
    }

    private static void aimMining(Minecraft mc, RayTraceResult target) {
        BlockPos pos = target.getBlockPos();
        IBlockState state = mc.world.getBlockState(pos);
        if (!isEligible(mc.world, mc.player, pos, state)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        if (spot == null || !spot.matches(HitKind.MINING, pos, target.sideHit)) {
            spot = WeakSpot.spawn(HitKind.MINING, mc.world, pos, state, target.sideHit, target.hitVec, settings, RANDOM);
        }
        if (spot == null) {
            return;
        }
        spot.lastActiveTick = clientTick;
        if (spot.isHitBy(target.hitVec) && canHit(HitKind.MINING, settings.minHitIntervalTicks)) {
            onHit(mc);
        }
    }

    /**
     * 右クリックの弱点。植物は一番大きい面（多くは上面。サトウキビなどは側面）に、機械は狙っている面に出す。
     * 照準がその面に当たっているときだけヒットにする。
     */
    private static void aimRightClick(Minecraft mc, RayTraceResult target) {
        BlockPos pos = target.getBlockPos();
        SyncedSettings settings = ClientSettings.get();
        HitKind kind = RightClickTargets.classify(mc.world, mc.player, pos, settings);
        if (kind == null) {
            return;
        }
        IBlockState state = mc.world.getBlockState(pos);
        AxisAlignedBB box = state.getSelectedBoundingBox(mc.world, pos);
        EnumFacing face = kind == HitKind.GROWTH ? growthFace(mc, pos, box, target.sideHit) : target.sideHit;
        if (spot == null || !spot.matches(kind, pos, face) || !spot.box.equals(box)) {
            spot = WeakSpot.spawn(kind, mc.world, pos, state, face, target.hitVec, settings, RANDOM);
        }
        if (spot == null) {
            return;
        }
        spot.lastActiveTick = clientTick;
        if (target.sideHit == spot.face && spot.isHitBy(target.hitVec)
                && canHit(kind, minHitInterval(kind, settings))) {
            onHit(mc);
        }
    }

    /**
     * 動物の弱点。素手（しゃがみが要る動物ではしゃがみ+素手）で右クリックを押しっぱなしにして動物に照準を合わせている間、
     * サーバーに状態を問い合わせ、動いているタイマーがあるという返事のときだけ、照準が当たっている面に出す。
     * 弱点の面は、その面がプレイヤーから見えている間は変えない。見えなくなったとき（回り込んだとき）と、消えたあとの
     * 出し直しで、照準が当たっている面に選び直す。
     */
    private static void aimAnimal(Minecraft mc, RayTraceResult target) {
        Entity entity = target.entityHit;
        SyncedSettings settings = ClientSettings.get();
        if (entity == null || !AnimalTargets.isTarget(mc.player, entity, settings)) {
            return;
        }
        AnimalStates.query(entity.getEntityId(), clientTick, false);
        AnimalStates.State state = AnimalStates.get(entity.getEntityId(), clientTick);
        if (state == null || state.mask == 0) {
            return;
        }
        AxisAlignedBB box = entity.getEntityBoundingBox();
        EnumFacing aimed = WeakSpot.faceAt(box, target.hitVec);
        Vec3d eye = mc.player.getPositionEyes(1.0F);
        if (spot == null || spot.entity != entity || !WeakSpot.isFacing(box, spot.face, eye)) {
            spot = WeakSpot.spawnOnEntity(entity, aimed, target.hitVec, settings, RANDOM);
        }
        if (spot == null) {
            return;
        }
        spot.follow(box);
        spot.lastActiveTick = clientTick;
        if (aimed == spot.face && spot.isHitBy(target.hitVec)
                && canHit(HitKind.ANIMAL, settings.animalMinHitIntervalTicks)) {
            onHit(mc);
        }
    }

    /**
     * 成長の弱点を出す面。同じ大きさの側面が複数あるとき（サトウキビなど）は、今の弱点の面が見えている間は変えない
     * （照準が隣の側面へ移ってもちらつかない）。見えなくなったとき（回り込んだとき）や出し直すときに選び直す。
     */
    private static EnumFacing growthFace(Minecraft mc, BlockPos pos, AxisAlignedBB box, EnumFacing aimed) {
        Vec3d eye = mc.player.getPositionEyes(1.0F);
        EnumFacing keep = spot != null && spot.kind == HitKind.GROWTH && spot.pos.equals(pos) && spot.box.equals(box)
                && WeakSpot.isFacing(box, spot.face, eye) ? spot.face : null;
        return WeakSpot.growthFace(box, eye, aimed, keep);
    }

    private static int minHitInterval(HitKind kind, SyncedSettings settings) {
        switch (kind) {
            case MACHINE:
                return settings.machineMinHitIntervalTicks;
            case ANIMAL:
                return settings.animalMinHitIntervalTicks;
            default:
                return settings.growthMinHitIntervalTicks;
        }
    }

    private static boolean canHit(HitKind kind, int minInterval) {
        return clientTick - LAST_HIT_TICK[kind.ordinal()] >= minInterval;
    }

    /** 壊せないブロック（硬度が負）と、今の破壊速度で1tick以内に壊れるブロックは対象外。 */
    private static boolean isEligible(World world, EntityPlayerSP player, BlockPos pos, IBlockState state) {
        if (state.getBlock().isAir(state, world, pos) || state.getBlockHardness(world, pos) < 0) {
            return false;
        }
        suppressBoost = true;
        try {
            return state.getPlayerRelativeBlockHardness(player, world, pos) < 1.0F;
        } finally {
            suppressBoost = false;
        }
    }

    /** 前のヒットから minInterval tick あいているか（釣りの弱点からも使う）。 */
    static boolean canHitNow(HitKind kind, int minInterval) {
        return canHit(kind, minInterval);
    }

    /**
     * どの種類のヒットにも共通の処理: 連続ヒット、ヒット音、コンボの表示、ヒット間隔の記録。ヒット後の連続ヒット数を返す。
     */
    static int registerHit(HitKind kind) {
        int hitStreak = STREAK.hit(clientTick);
        HitSounds.playOwn(hitStreak);
        ComboHud.onHit(hitStreak, clientTick + framePartialTicks);
        LAST_HIT_TICK[kind.ordinal()] = clientTick;
        return hitStreak;
    }

    private static void onHit(Minecraft mc) {
        HitKind kind = spot.kind;
        WeakSpotRenderer.addFlash(spot, clientTick);
        int hitStreak = registerHit(kind);
        if (kind == HitKind.MINING) {
            boostHitTick = clientTick;
            boostPos = spot.pos;
        }
        if (kind == HitKind.ANIMAL) {
            WeakSpotMod.network.sendToServer(HitMessage.animal(spot.entity.getEntityId(), hitStreak));
            // ヒットで進んだ分を、すぐに見に行く
            AnimalStates.query(spot.entity.getEntityId(), clientTick, true);
        } else {
            WeakSpotMod.network.sendToServer(new HitMessage(kind, spot.pos, hitStreak));
        }

        // 同じ面の中の移動なので、演出がオンならマーカーを動かす（当たり判定は移動先ですぐに行う）
        spot.relocate(RANDOM, WeakSpotConfig.weakSpotTrailEnabled, Minecraft.getSystemTime());
    }

    /** ヒット後の次の tick から boostDurationTicks 回分の進捗計算に倍率を掛ける。 */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getEntityPlayer().world.isRemote || suppressBoost || !WeakSpotConfig.weakSpotsEnabled) {
            return;
        }
        if (event.getEntityPlayer() != Minecraft.getMinecraft().player || !event.getPos().equals(boostPos)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        long sinceHit = clientTick - boostHitTick;
        if (sinceHit > 0 && sinceHit <= settings.boostDurationTicks) {
            event.setNewSpeed((float) (event.getNewSpeed() * settings.boostMultiplier));
        }
    }

    private static void reset() {
        spot = null;
        MiningProgress.clear();
        boostPos = null;
        Arrays.fill(LAST_HIT_TICK, Long.MIN_VALUE / 2);
        boostHitTick = Long.MIN_VALUE / 2;
        AnimalStates.clear();
        FishingSpot.clear();
        lastPlayer = null;
        resetStreak();
        WeakSpotRenderer.clearFlashes();
        HitSounds.clear();
    }

    private static void resetStreak() {
        STREAK.reset();
        ComboHud.clear();
    }
}
