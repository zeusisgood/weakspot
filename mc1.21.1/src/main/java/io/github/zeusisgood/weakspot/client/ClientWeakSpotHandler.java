package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.AnimalTargets;
import io.github.zeusisgood.weakspot.PlayerRules;
import io.github.zeusisgood.weakspot.RightClickTargets;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.HitPitch;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.network.HitMessage;
import io.github.zeusisgood.weakspot.server.MachineStates;
import java.util.Random;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 採掘と右クリックの弱点（作物・苗木、機械、動物、収穫）。弱点の位置は自分のクライアントだけが持ち、一度に 1 つだけ。
 * ヒット判定は毎フレーム行う（素早く照準を動かしたときに、tick 単位だと取りこぼすため）。
 */
@EventBusSubscriber(modid = WeakSpotMod.MODID, value = Dist.CLIENT)
public final class ClientWeakSpotHandler {

    private static final Random RANDOM = new Random();

    /** ClientTickEvent.Pre で増える。MultiPlayerGameMode の進捗計算はその後に走る。 */
    static long clientTick;
    static WeakSpot spot;
    /** ヒットした瞬間を、フレームの途中の値まで含めてコンボの表示に渡すため。 */
    static float framePartialTicks;

    /** 死亡・ディメンション移動（どちらもプレイヤーが作り直される）を見分けるため。 */
    private static LocalPlayer lastPlayer;

    private ClientWeakSpotHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        ToggleKeyHandler.remindIfOff(mc);
        ToggleKeyHandler.syncToServer(mc);
        if (mc.level == null || mc.player == null) {
            reset();
            return;
        }
        if (mc.player != lastPlayer || mc.player.getHealth() <= 0) {
            lastPlayer = mc.player;
            OwnHits.resetStreak();
        }
        if (mc.isPaused()) {
            return;
        }
        clientTick++;
        MiningProgress.sample(mc.gameMode);
        int broken = OwnHits.STREAK.expire(clientTick);
        if (broken > 0) {
            ComboHud.onBreak(broken, clientTick);
            if (broken >= HitPitch.BREAK_SOUND_FROM) {
                HitSounds.playBreak();
            }
        }
        if (!WeakSpotConfig.client.markers.weakSpotsEnabled) {
            stopOwnWeakSpots();
        }
        // 掘る前の採掘の弱点は、照準が外れたらすぐ消す（いつも出るので、残すと見回すたびに邪魔になる）
        if (spot != null && (isGone(mc.level, spot)
                || clientTick - spot.lastActiveTick > (spot.preview ? 1 : ClientSettings.get().lingerTicks))) {
            spot = null;
        }
        WeakSpotRenderer.expireFlashes(clientTick);
    }

    private static boolean isGone(Level world, WeakSpot spot) {
        if (spot.kind == HitKind.ANIMAL) {
            return spot.entity == null || spot.entity.isRemoved();
        }
        if (world.isEmptyBlock(spot.pos)) {
            return true;
        }
        BlockState state = world.getBlockState(spot.pos);
        switch (spot.kind) {
            case GROWTH:
                return !RightClickTargets.isGrowthTarget(world, spot.pos, state, ClientSettings.get());
            case HARVEST:
                return !RightClickTargets.isHarvestable(state, ClientSettings.get());
            case MACHINE:
                return !RightClickTargets.isMachine(world, spot.pos, state, ClientSettings.get());
            default:
                return false;
        }
    }

    /** 1.12.2 の RenderWorldLastEvent に当たる、ワールドの描画の最後の段。 */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        ScreenProjection.onFrame(event.getModelViewMatrix(), event.getProjectionMatrix(),
                event.getCamera().getPosition());
        framePartialTicks = partialTicks;
        double health = -1;
        double growth = -1;
        double animal = -1;
        if (WeakSpotConfig.client.markers.weakSpotsEnabled) {
            updateAim(mc);
            if (WeakSpotConfig.client.hud.blockHealthBarEnabled) {
                health = OwnSpotBars.healthRemaining(mc, partialTicks);
            }
            if (WeakSpotConfig.client.hud.growthBarEnabled) {
                growth = OwnSpotBars.growthProgress(mc);
            }
            animal = OwnSpotBars.animalProgress();
            if (spot != null && spot.entity != null) {
                // 当たり判定は updateAim で、その tick の位置に合わせた
                spot.follow(WeakSpot.renderBox(spot.entity, partialTicks));
            }
        } else {
            // J を押した直後のフレームでも、自分の弱点を出さない
            stopOwnWeakSpots();
        }
        WeakSpotRenderer.render(mc, spot, health, growth, animal, WeakSpotConfig.client.markers.weakSpotsEnabled && OwnSpotBars.machineBar(mc),
                clientTick, partialTicks);
    }

    /** 自分のマークの送信は、弱点が null になると「消えた」を送る。 */
    static void stopOwnWeakSpots() {
        spot = null;
        MiningBoost.clear();
        WeakSpotRenderer.clearFlashes();
        FishingSpot.clear();
        AimSpots.clearAll();
    }

    private static void updateAim(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (!PlayerRules.canUse(player)) {
            return;
        }
        HitResult hit = mc.hitResult;
        if (hit instanceof EntityHitResult entityHit) {
            if (!mc.gameMode.isDestroying() && isHoldingUse(mc) && KindSwitches.isEnabled(HitKind.ANIMAL)) {
                aimAnimal(mc, entityHit);
            }
            return;
        }
        if (!(hit instanceof BlockHitResult target) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (mc.gameMode.isDestroying()) {
            if (KindSwitches.isEnabled(HitKind.MINING) && ClientSettings.get().enabled(HitKind.MINING)) {
                aimMining(mc, target);
            }
        } else if (isHoldingUse(mc)) {
            aimRightClick(mc, target);
        } else if (KindSwitches.isEnabled(HitKind.MINING) && ClientSettings.get().enabled(HitKind.MINING)) {
            aimPreDig(mc, target);
        }
    }

    /** バニラが右クリックを繰り返す条件と同じ。 */
    private static boolean isHoldingUse(Minecraft mc) {
        return mc.screen == null && mc.options.keyUse.isDown() && !mc.player.isUsingItem();
    }

    private static void aimMining(Minecraft mc, BlockHitResult target) {
        BlockPos pos = target.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (!MiningBoost.isEligible(mc.level, mc.player, pos, state)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        BlockAim aim = blockAim(mc, target, WeakSpot.outline(mc.level, pos, state));
        if (spot == null || !spot.matches(HitKind.MINING, pos, aim.face)) {
            spot = WeakSpot.spawn(HitKind.MINING, mc.level, pos, state, aim.face, aim.point, settings, RANDOM);
        }
        if (spot == null) {
            return;
        }
        spot.preview = false;
        spot.lastActiveTick = clientTick;
        MiningBoost.onDigging(pos);
        if (spot.isHitBy(aim.point) && OwnHits.canHit(HitKind.MINING, settings.minHitInterval(HitKind.MINING))) {
            onHit(mc);
        }
    }

    /**
    * 当たるのは左クリックを押している間だけ（照準を合わせるだけで当たると、ブロックを交互に見るだけで
    * ヒット音とコンボの表示が増え続けるため）。
    */
    private static void aimPreDig(Minecraft mc, BlockHitResult target) {
        BlockPos pos = target.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (!MiningBoost.isPreDigEligible(mc.level, mc.player, pos, state)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        BlockAim aim = blockAim(mc, target, WeakSpot.outline(mc.level, pos, state));
        if (spot == null || !spot.matches(HitKind.MINING, pos, aim.face)) {
            spot = WeakSpot.spawn(HitKind.MINING, mc.level, pos, state, aim.face, aim.point, settings, RANDOM);
        }
        if (spot == null) {
            return;
        }
        spot.preview = true;
        spot.lastActiveTick = clientTick;
        if (mc.screen == null && mc.options.keyAttack.isDown() && !MiningBoost.hasPreHit(pos)
                && spot.isHitBy(aim.point)
                && OwnHits.canHit(HitKind.MINING, settings.minHitInterval(HitKind.MINING))) {
            onHit(mc);
        }
    }

    private static void aimRightClick(Minecraft mc, BlockHitResult target) {
        BlockPos pos = target.getBlockPos();
        SyncedSettings settings = ClientSettings.get();
        HitKind kind = RightClickTargets.classify(mc.level, mc.player, pos, settings);
        if (kind == null || !KindSwitches.isEnabled(kind)) {
            return;
        }
        BlockState state = mc.level.getBlockState(pos);
        AABB box = WeakSpot.outline(mc.level, pos, state);
        if (box == null) {
            return;
        }
        BlockAim aim = blockAim(mc, target, box);
        // 1マス全体のブロック（IC2 のゴムの木の幹など）は、上面が隠れていることが多いので、照準の面に出す
        boolean plant = kind == HitKind.GROWTH || kind == HitKind.HARVEST;
        Direction face = plant && !isFullCube(box, pos) ? growthFace(mc, pos, box, aim.face) : aim.face;
        if (spot == null || !spot.matches(kind, pos, face) || !spot.box.equals(box)) {
            spot = WeakSpot.spawn(kind, mc.level, pos, state, face, aim.point, settings, RANDOM);
        }
        if (spot == null) {
            return;
        }
        spot.lastActiveTick = clientTick;
        if (aim.face == spot.face && spot.isHitBy(aim.point)
                && OwnHits.canHit(kind, settings.minHitInterval(kind))) {
            onHit(mc);
        }
    }

    /**
    * サーバーに問い合わせ、動いているタイマーがあるという返事のときだけ出す。
    * 弱点の面は、その面がプレイヤーから見えている間は変えない（回り込んだら選び直す）。
    */
    private static void aimAnimal(Minecraft mc, EntityHitResult target) {
        Entity entity = target.getEntity();
        SyncedSettings settings = ClientSettings.get();
        if (entity == null || !AnimalTargets.isTarget(mc.player, entity, settings)) {
            return;
        }
        AnimalStates.query(entity.getId(), clientTick, false);
        AnimalStates.State state = AnimalStates.get(entity.getId(), clientTick);
        if (state == null || state.mask == 0) {
            return;
        }
        AABB box = entity.getBoundingBox();
        Direction aimed = WeakSpot.faceAt(box, target.getLocation());
        Vec3 eye = mc.player.getEyePosition(1.0F);
        if (spot == null || spot.entity != entity || !WeakSpot.isFacing(box, spot.face, eye)) {
            spot = WeakSpot.spawnOnEntity(HitKind.ANIMAL, entity, aimed, target.getLocation(), settings, RANDOM);
        }
        if (spot == null) {
            return;
        }
        spot.follow(box);
        spot.lastActiveTick = clientTick;
        if (aimed == spot.face && spot.isHitBy(target.getLocation())
                && OwnHits.canHit(HitKind.ANIMAL, settings.minHitInterval(HitKind.ANIMAL))) {
            onHit(mc);
        }
    }

    /** ブロックの弱点の照準の面と点。 */
    private static final class BlockAim {
        final Direction face;
        final Vec3 point;

        BlockAim(Direction face, Vec3 point) {
            this.face = face;
            this.point = point;
        }
    }

    /**
    * 外枠の箱と照準の線で決める（凹凸のあるブロックでは、実際に当たった面が少しの照準の動きで変わり、
    * 採掘の弱点が飛び回り、右クリックの弱点は面が合わず外れるため）。線が箱に入らない（目が箱の中など）
    * ときは、バニラの照準の面と点。
    */
    private static BlockAim blockAim(Minecraft mc, BlockHitResult target, AABB box) {
        Entity camera = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        if (box != null) {
            // バニラの照準（GameRenderer.pick）と同じ、フレームの途中の目と向き
            Vec3 eye = camera.getEyePosition(framePartialTicks);
            Vec3 look = camera.getViewVector(framePartialTicks);
            Direction face = WeakSpot.entryFace(box, eye, look);
            Vec3 point = face == null ? null : WeakSpot.rayPoint(box, face, eye, look);
            if (point != null) {
                return new BlockAim(face, point);
            }
        }
        return new BlockAim(target.getDirection(), target.getLocation());
    }

    /** 同じ大きさの側面が複数あるとき（サトウキビなど）は、今の弱点の面が見えている間は変えない（照準が隣の側面へ移ってもちらつかない）。 */
    private static Direction growthFace(Minecraft mc, BlockPos pos, AABB box, Direction aimed) {
        Vec3 eye = mc.player.getEyePosition(1.0F);
        Direction keep = spot != null && (spot.kind == HitKind.GROWTH || spot.kind == HitKind.HARVEST)
                && spot.entity == null && spot.pos.equals(pos) && spot.box.equals(box)
                && WeakSpot.isFacing(box, spot.face, eye) ? spot.face : null;
        return WeakSpot.growthFace(box, eye, aimed, keep);
    }

    private static boolean isFullCube(AABB box, BlockPos pos) {
        double eps = 1e-6;
        return Math.abs(box.minX - pos.getX()) < eps && Math.abs(box.minY - pos.getY()) < eps
                && Math.abs(box.minZ - pos.getZ()) < eps && Math.abs(box.maxX - pos.getX() - 1) < eps
                && Math.abs(box.maxY - pos.getY() - 1) < eps && Math.abs(box.maxZ - pos.getZ() - 1) < eps;
    }

    /** このフレームで照準が合っていることも見る。 */
    static boolean machineSpotActive() {
        return blockSpotActive(HitKind.MACHINE);
    }

    /** このフレームで照準が合っていることも見る。 */
    static boolean blockSpotActive(HitKind kind) {
        return spot != null && spot.kind == kind && spot.lastActiveTick == clientTick;
    }

    /** このフレームで照準が合っていることも見る。 */
    static boolean dispenserSpotActive() {
        return machineSpotActive() && Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getBlockState(spot.pos).getBlock() instanceof DispenserBlock;
    }

    static boolean crafterSpotActive() {
        return machineSpotActive() && Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getBlockState(spot.pos).getBlock() instanceof CrafterBlock;
    }

    private static void onHit(Minecraft mc) {
        HitKind kind = spot.kind;
        WeakSpotRenderer.addFlash(spot, clientTick);
        int hitStreak = OwnHits.register(kind);
        if (kind == HitKind.MACHINE && WeakSpotConfig.client.hud.machineBarEnabled
                && MachineStates.hasBar(mc.level.getBlockEntity(spot.pos))) {
            // ヒットで進んだ分を、すぐに見に行く
            MachineBars.query(spot.pos, clientTick, true);
        }
        if (kind == HitKind.MINING && spot.preview) {
            MiningBoost.onPreHit(spot.pos, hitStreak);
        } else if (kind == HitKind.MINING) {
            MiningBoost.onHit(spot.pos, hitStreak);
        }
        if (kind == HitKind.ANIMAL) {
            WeakSpotMod.network.sendToServer(HitMessage.entity(kind, spot.entity.getId(), hitStreak));
            // ヒットで進んだ分を、すぐに見に行く
            AnimalStates.query(spot.entity.getId(), clientTick, true);
        } else {
            WeakSpotMod.network.sendToServer(new HitMessage(kind, spot.pos, hitStreak));
        }

        // 当たり判定は移動先ですぐに行う
        spot.relocate(RANDOM, WeakSpotConfig.client.markers.weakSpotTrailEnabled, Util.getMillis());
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
