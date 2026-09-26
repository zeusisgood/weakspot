package com.example.weakspot.client;

import com.example.weakspot.PlayerRules;
import com.example.weakspot.AnimalTargets;
import com.example.weakspot.RightClickTargets;
import com.example.weakspot.WeakSpotMod;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.config.WeakSpotConfig;
import com.example.weakspot.server.MachineStates;
import com.example.weakspot.network.HitMessage;
import java.util.Random;
import net.minecraft.block.BlockDispenser;
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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * クライアント側の弱点処理。弱点の位置は自分のクライアントだけが持ち、他プレイヤーとは共有しない。
 * 左クリックの長押し（採掘）と、右クリックの押しっぱなし（作物・苗木、機械、動物、収穫）の弱点を扱う
 * （近接は 1.8.0 から MeleeSpot）。
 * 弱点は一度に1つだけ。
 *
 * ヒット判定は毎フレーム行う（素早く照準を動かしたときに tick 単位だと取りこぼすため）。
 * 1.8.9 で、採掘のブーストを MiningBoost、共通のヒット処理（連続ヒット・間隔）を OwnHits、足元・面のバーを OwnSpotBars に分けた。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
public final class ClientWeakSpotHandler {

    private static final Random RANDOM = new Random();

    /** ClientTickEvent の START で増える。PlayerControllerMP の進捗計算はその後に走る。 */
    static long clientTick;
    static WeakSpot spot;
    /** 今のフレームの partialTicks（ヒットした瞬間を、フレームの途中の値まで含めてコンボの表示に渡す）。 */
    static float framePartialTicks;

    /** 死亡・ディメンション移動（どちらもプレイヤーが作り直される）を見分けるため。 */
    private static EntityPlayerSP lastPlayer;

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
            OwnHits.resetStreak();
        }
        if (mc.isGamePaused()) {
            return;
        }
        clientTick++;
        MiningProgress.sample(mc.playerController);
        int broken = OwnHits.STREAK.expire(clientTick);
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
            case HARVEST:
                return !RightClickTargets.isHarvestable(state, ClientSettings.get());
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
                health = OwnSpotBars.healthRemaining(mc, event.getPartialTicks());
            }
            if (WeakSpotConfig.growthBarEnabled) {
                growth = OwnSpotBars.growthProgress(mc);
            }
            animal = OwnSpotBars.animalProgress();
            if (spot != null && spot.entity != null) {
                // 動物・敵の弱点は、描く時点の位置に合わせる（当たり判定は updateAim で、その tick の位置に合わせた）
                spot.follow(WeakSpot.renderBox(spot.entity, event.getPartialTicks()));
            }
        } else {
            // 一時オフ。J を押した直後のフレームでも、自分の弱点を出さない
            stopOwnWeakSpots();
        }
        WeakSpotRenderer.render(mc, spot, health, growth, animal, WeakSpotConfig.weakSpotsEnabled && OwnSpotBars.machineBar(mc),
                clientTick, event.getPartialTicks());
    }

    /**
     * 弱点の一時オフ（HOME キー）の間、自分の弱点を出さず、ヒットも起こさない。
     * 出ていた弱点は消す（自分のマークの送信は、弱点が null になると「消えた」を送る）。ブーストも止める。
     */
    static void stopOwnWeakSpots() {
        spot = null;
        MiningBoost.clear();
        WeakSpotRenderer.clearFlashes();
        FishingSpot.clear();
        AimSpots.clearAll();
    }

    private static void updateAim(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (!PlayerRules.canUse(player)) {
            return;
        }
        RayTraceResult target = mc.objectMouseOver;
        if (target == null) {
            return;
        }
        if (target.typeOfHit == RayTraceResult.Type.ENTITY) {
            if (!mc.playerController.getIsHittingBlock() && isHoldingUse(mc) && KindSwitches.isEnabled(HitKind.ANIMAL)) {
                aimAnimal(mc, target);
            }
            return;
        }
        if (target.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        if (mc.playerController.getIsHittingBlock()) {
            if (KindSwitches.isEnabled(HitKind.MINING)) {
                aimMining(mc, target);
            }
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
        if (!MiningBoost.isEligible(mc.world, mc.player, pos, state)) {
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
        if (spot.isHitBy(target.hitVec) && OwnHits.canHit(HitKind.MINING, settings.minHitInterval(HitKind.MINING))) {
            onHit(mc);
        }
    }

    /**
     * 右クリックの弱点。植物は一番大きい面（多くは上面。サトウキビなどは側面。1マス全体のブロックは照準の面）に、
     * 機械は狙っている面に出す。
     * 照準がその面に当たっているときだけヒットにする。
     */
    private static void aimRightClick(Minecraft mc, RayTraceResult target) {
        BlockPos pos = target.getBlockPos();
        SyncedSettings settings = ClientSettings.get();
        HitKind kind = RightClickTargets.classify(mc.world, mc.player, pos, settings);
        if (kind == null || !KindSwitches.isEnabled(kind)) {
            return;
        }
        IBlockState state = mc.world.getBlockState(pos);
        AxisAlignedBB box = state.getSelectedBoundingBox(mc.world, pos);
        // 1マス全体のブロック（IC2 のゴムの木の幹など）は、上面が隠れていることが多いので、照準の面に出す（1.4.0）
        boolean plant = kind == HitKind.GROWTH || kind == HitKind.HARVEST;
        EnumFacing face = plant && !isFullCube(box, pos) ? growthFace(mc, pos, box, target.sideHit)
                : target.sideHit;
        if (spot == null || !spot.matches(kind, pos, face) || !spot.box.equals(box)) {
            spot = WeakSpot.spawn(kind, mc.world, pos, state, face, target.hitVec, settings, RANDOM);
        }
        if (spot == null) {
            return;
        }
        spot.lastActiveTick = clientTick;
        if (target.sideHit == spot.face && spot.isHitBy(target.hitVec)
                && OwnHits.canHit(kind, settings.minHitInterval(kind))) {
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
            spot = WeakSpot.spawnOnEntity(HitKind.ANIMAL, entity, aimed, target.hitVec, settings, RANDOM);
        }
        if (spot == null) {
            return;
        }
        spot.follow(box);
        spot.lastActiveTick = clientTick;
        if (aimed == spot.face && spot.isHitBy(target.hitVec)
                && OwnHits.canHit(HitKind.ANIMAL, settings.minHitInterval(HitKind.ANIMAL))) {
            onHit(mc);
        }
    }

    /**
     * 成長の弱点を出す面。同じ大きさの側面が複数あるとき（サトウキビなど）は、今の弱点の面が見えている間は変えない
     * （照準が隣の側面へ移ってもちらつかない）。見えなくなったとき（回り込んだとき）や出し直すときに選び直す。
     */
    private static EnumFacing growthFace(Minecraft mc, BlockPos pos, AxisAlignedBB box, EnumFacing aimed) {
        Vec3d eye = mc.player.getPositionEyes(1.0F);
        EnumFacing keep = spot != null && (spot.kind == HitKind.GROWTH || spot.kind == HitKind.HARVEST)
                && spot.entity == null && spot.pos.equals(pos) && spot.box.equals(box)
                && WeakSpot.isFacing(box, spot.face, eye) ? spot.face : null;
        return WeakSpot.growthFace(box, eye, aimed, keep);
    }

    /** 当たり判定の箱が、そのマス全体か。 */
    private static boolean isFullCube(AxisAlignedBB box, BlockPos pos) {
        double eps = 1e-6;
        return Math.abs(box.minX - pos.getX()) < eps && Math.abs(box.minY - pos.getY()) < eps
                && Math.abs(box.minZ - pos.getZ()) < eps && Math.abs(box.maxX - pos.getX() - 1) < eps
                && Math.abs(box.maxY - pos.getY() - 1) < eps && Math.abs(box.maxZ - pos.getZ() - 1) < eps;
    }

    /** 機械の弱点が出ていて、このフレームで照準が合っているか（コンボの「機械 ×n」の表示）。 */
    static boolean machineSpotActive() {
        return blockSpotActive(HitKind.MACHINE);
    }

    /** その種類（採掘・機械・収穫など）の弱点が出ていて、このフレームで照準が合っているか（コンボの種類の表示。1.8.7）。 */
    static boolean blockSpotActive(HitKind kind) {
        return spot != null && spot.kind == kind && spot.lastActiveTick == clientTick;
    }

    /** 機械の弱点がディスペンサー・ドロッパーに出ていて、このフレームで照準が合っているか（「発射 ×n」の表示）。 */
    static boolean dispenserSpotActive() {
        return machineSpotActive() && Minecraft.getMinecraft().world != null
                && Minecraft.getMinecraft().world.getBlockState(spot.pos).getBlock() instanceof BlockDispenser;
    }

    private static void onHit(Minecraft mc) {
        HitKind kind = spot.kind;
        WeakSpotRenderer.addFlash(spot, clientTick);
        int hitStreak = OwnHits.register(kind);
        if (kind == HitKind.MACHINE && WeakSpotConfig.machineBarEnabled
                && MachineStates.hasBar(mc.world.getTileEntity(spot.pos))) {
            // ヒットで進んだ分を、すぐに見に行く
            MachineBars.query(spot.pos, clientTick, true);
        }
        if (kind == HitKind.MINING) {
            MiningBoost.onHit(spot.pos, hitStreak);
        }
        if (kind == HitKind.ANIMAL) {
            WeakSpotMod.network.sendToServer(HitMessage.entity(kind, spot.entity.getEntityId(), hitStreak));
            // ヒットで進んだ分を、すぐに見に行く
            AnimalStates.query(spot.entity.getEntityId(), clientTick, true);
        } else {
            WeakSpotMod.network.sendToServer(new HitMessage(kind, spot.pos, hitStreak));
        }

        // 同じ面の中の移動なので、演出がオンならマーカーを動かす（当たり判定は移動先ですぐに行う）
        spot.relocate(RANDOM, WeakSpotConfig.weakSpotTrailEnabled, Minecraft.getSystemTime());
    }

    private static void reset() {
        spot = null;
        MiningProgress.clear();
        MiningBoost.clear();
        OwnHits.clearIntervals();
        AnimalStates.clear();
        MachineBars.clear();
        OtherCombos.clear();
        AimSpots.clearAll();
        FishingSpot.clear();
        lastPlayer = null;
        OwnHits.resetStreak();
        WeakSpotRenderer.clearFlashes();
        HitSounds.clear();
    }

}
