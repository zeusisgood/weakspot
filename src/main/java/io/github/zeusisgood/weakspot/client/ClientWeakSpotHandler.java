package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.PlayerRules;
import io.github.zeusisgood.weakspot.AnimalTargets;
import io.github.zeusisgood.weakspot.RightClickTargets;
import io.github.zeusisgood.weakspot.WeakSpotMod;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.HitPitch;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.config.WeakSpotConfig;
import io.github.zeusisgood.weakspot.server.MachineStates;
import io.github.zeusisgood.weakspot.network.HitMessage;
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
 * 採掘と右クリックの弱点（作物・苗木、機械、動物、収穫）。弱点の位置は自分のクライアントだけが持ち、一度に 1 つだけ。
 * ヒット判定は毎フレーム行う（素早く照準を動かしたときに、tick 単位だと取りこぼすため）。
 */
@Mod.EventBusSubscriber(modid = WeakSpotMod.MODID, value = Side.CLIENT)
public final class ClientWeakSpotHandler {

    private static final Random RANDOM = new Random();

    /** ClientTickEvent の START で増える。PlayerControllerMP の進捗計算はその後に走る。 */
    static long clientTick;
    static WeakSpot spot;
    /** ヒットした瞬間を、フレームの途中の値まで含めてコンボの表示に渡すため。 */
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
            if (broken >= HitPitch.BREAK_SOUND_FROM) {
                HitSounds.playBreak();
            }
        }
        if (!WeakSpotConfig.client.markers.weakSpotsEnabled) {
            stopOwnWeakSpots();
        }
        // 掘る前の採掘の弱点は、照準が外れたらすぐ消す（いつも出るので、残すと見回すたびに邪魔になる）
        if (spot != null && (isGone(mc.world, spot)
                || clientTick - spot.lastActiveTick > (spot.preview ? 1 : ClientSettings.get().lingerTicks))) {
            spot = null;
        }
        WeakSpotRenderer.expireFlashes(clientTick);
    }

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
                return !RightClickTargets.isGrowthTarget(world, spot.pos, state, ClientSettings.get());
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
        if (WeakSpotConfig.client.markers.weakSpotsEnabled) {
            updateAim(mc);
            if (WeakSpotConfig.client.hud.blockHealthBarEnabled) {
                health = OwnSpotBars.healthRemaining(mc, event.getPartialTicks());
            }
            if (WeakSpotConfig.client.hud.growthBarEnabled) {
                growth = OwnSpotBars.growthProgress(mc);
            }
            animal = OwnSpotBars.animalProgress();
            if (spot != null && spot.entity != null) {
                // 当たり判定は updateAim で、その tick の位置に合わせた
                spot.follow(WeakSpot.renderBox(spot.entity, event.getPartialTicks()));
            }
        } else {
            // J を押した直後のフレームでも、自分の弱点を出さない
            stopOwnWeakSpots();
        }
        WeakSpotRenderer.render(mc, spot, health, growth, animal, WeakSpotConfig.client.markers.weakSpotsEnabled && OwnSpotBars.machineBar(mc),
                clientTick, event.getPartialTicks());
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
            if (KindSwitches.isEnabled(HitKind.MINING) && ClientSettings.get().enabled(HitKind.MINING)) {
                aimMining(mc, target);
            }
        } else if (isHoldingUse(mc)) {
            aimRightClick(mc, target);
        } else if (KindSwitches.isEnabled(HitKind.MINING) && ClientSettings.get().enabled(HitKind.MINING)
                && ServerFeatures.since("1.11.2")) {
            aimPreDig(mc, target);
        }
    }

    /** バニラが右クリックを繰り返す条件と同じ。 */
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
        spot.preview = false;
        spot.lastActiveTick = clientTick;
        MiningBoost.onDigging(pos);
        if (spot.isHitBy(target.hitVec) && OwnHits.canHit(HitKind.MINING, settings.minHitInterval(HitKind.MINING))) {
            onHit(mc);
        }
    }

    /**
    * 当たるのは左クリックを押している間だけ（照準を合わせるだけで当たると、ブロックを交互に見るだけで
    * ヒット音とコンボの表示が増え続けるため）。
    */
    private static void aimPreDig(Minecraft mc, RayTraceResult target) {
        BlockPos pos = target.getBlockPos();
        IBlockState state = mc.world.getBlockState(pos);
        if (!MiningBoost.isPreDigEligible(mc.world, mc.player, pos, state)) {
            return;
        }
        SyncedSettings settings = ClientSettings.get();
        if (spot == null || !spot.matches(HitKind.MINING, pos, target.sideHit)) {
            spot = WeakSpot.spawn(HitKind.MINING, mc.world, pos, state, target.sideHit, target.hitVec, settings, RANDOM);
        }
        if (spot == null) {
            return;
        }
        spot.preview = true;
        spot.lastActiveTick = clientTick;
        if (mc.currentScreen == null && mc.gameSettings.keyBindAttack.isKeyDown() && !MiningBoost.hasPreHit(pos)
                && spot.isHitBy(target.hitVec)
                && OwnHits.canHit(HitKind.MINING, settings.minHitInterval(HitKind.MINING))) {
            onHit(mc);
        }
    }

    private static void aimRightClick(Minecraft mc, RayTraceResult target) {
        BlockPos pos = target.getBlockPos();
        SyncedSettings settings = ClientSettings.get();
        HitKind kind = RightClickTargets.classify(mc.world, mc.player, pos, settings);
        if (kind == null || !KindSwitches.isEnabled(kind)) {
            return;
        }
        IBlockState state = mc.world.getBlockState(pos);
        AxisAlignedBB box = state.getSelectedBoundingBox(mc.world, pos);
        // 1マス全体のブロック（IC2 のゴムの木の幹など）は、上面が隠れていることが多いので、照準の面に出す
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
    * サーバーに問い合わせ、動いているタイマーがあるという返事のときだけ出す。
    * 弱点の面は、その面がプレイヤーから見えている間は変えない（回り込んだら選び直す）。
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

    /** 同じ大きさの側面が複数あるとき（サトウキビなど）は、今の弱点の面が見えている間は変えない（照準が隣の側面へ移ってもちらつかない）。 */
    private static EnumFacing growthFace(Minecraft mc, BlockPos pos, AxisAlignedBB box, EnumFacing aimed) {
        Vec3d eye = mc.player.getPositionEyes(1.0F);
        EnumFacing keep = spot != null && (spot.kind == HitKind.GROWTH || spot.kind == HitKind.HARVEST)
                && spot.entity == null && spot.pos.equals(pos) && spot.box.equals(box)
                && WeakSpot.isFacing(box, spot.face, eye) ? spot.face : null;
        return WeakSpot.growthFace(box, eye, aimed, keep);
    }

    private static boolean isFullCube(AxisAlignedBB box, BlockPos pos) {
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
        return machineSpotActive() && Minecraft.getMinecraft().world != null
                && Minecraft.getMinecraft().world.getBlockState(spot.pos).getBlock() instanceof BlockDispenser;
    }

    private static void onHit(Minecraft mc) {
        HitKind kind = spot.kind;
        WeakSpotRenderer.addFlash(spot, clientTick);
        int hitStreak = OwnHits.register(kind);
        if (kind == HitKind.MACHINE && WeakSpotConfig.client.hud.machineBarEnabled
                && MachineStates.hasBar(mc.world.getTileEntity(spot.pos))) {
            // ヒットで進んだ分を、すぐに見に行く
            MachineBars.query(spot.pos, clientTick, true);
        }
        if (kind == HitKind.MINING && spot.preview) {
            MiningBoost.onPreHit(spot.pos, hitStreak);
        } else if (kind == HitKind.MINING) {
            MiningBoost.onHit(spot.pos, hitStreak);
        }
        if (kind == HitKind.ANIMAL) {
            WeakSpotMod.network.sendToServer(HitMessage.entity(kind, spot.entity.getEntityId(), hitStreak));
            // ヒットで進んだ分を、すぐに見に行く
            AnimalStates.query(spot.entity.getEntityId(), clientTick, true);
        } else {
            WeakSpotMod.network.sendToServer(new HitMessage(kind, spot.pos, hitStreak));
        }

        // 当たり判定は移動先ですぐに行う
        spot.relocate(RANDOM, WeakSpotConfig.client.markers.weakSpotTrailEnabled, Minecraft.getSystemTime());
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
