package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.PlayerRules;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import io.github.zeusisgood.weakspot.network.HitMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.event.FOVUpdateEvent;

/** 照準のまわりの弱点の種類の土台。出す・当てる・描く流れは AimSpots が回し、種類ごとに違うことだけをここで書く。 */
abstract class AimSpotKind {

    final HitKind kind;
    final HudSpot spot;
    int missedTicks;

    /** サーバーの受け付け MoveHits.RECENT_TICKS と同じ。 */
    static final int MOVING_KEEP_TICKS = 10;

    AimSpotKind(HitKind kind, HudSpot spot) {
        this.kind = kind;
        this.spot = spot;
    }

    /** 共通の条件（allowed）のあとの、種類ごとの条件。 */
    abstract boolean wanted(EntityPlayerSP player, SyncedSettings settings);

    /**
    * wanted から外れても、弱点を消さずに残す tick。走りは壁にこすれた瞬間にバニラが一度切る、段差・着地の瞬間は
    * 1 tick の移動が小さい、など一瞬外れることがあるので、動いている間に出す種類は残す（残している間は当たらない）。
    */
    int keepTicks() {
        return 0;
    }

    /** 弓・食事は false。 */
    boolean blockedByUsingHand() {
        return true;
    }

    /** HudSpot.FREE / VERTICAL / HORIZONTAL / VERTICAL_FIXED。 */
    abstract int placement(EntityPlayerSP player);

    final int minHitInterval(SyncedSettings settings) {
        return settings.minHitInterval(kind);
    }

    HitMessage message(EntityPlayerSP player, int streak) {
        return HitMessage.withoutTarget(kind, streak);
    }

    /** 音・コンボの数え・通知は済んでいる。streak はヒット後の数。 */
    abstract void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak);

    /** false なら消す（弓の過剰チャージが上限に届いたとき）。 */
    boolean keepAfterHit(EntityPlayerSP player) {
        return true;
    }

    /** 弓は、F1 で画面を隠している間は当てない。 */
    boolean canAim(Minecraft mc) {
        return true;
    }

    /** ClientTickEvent START の、出す・消すの判定の前。 */
    void beforeTick(Minecraft mc) {
    }

    void afterTick(Minecraft mc) {
    }

    void tickEnd(Minecraft mc) {
    }

    /** 描くなら drawGauge（beginOverlay と endOverlay の間）と drawAfterOverlay（文字）。 */
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return false;
    }

    void drawGauge(Minecraft mc, float partialTicks) {
    }

    void drawAfterOverlay(Minecraft mc, float partialTicks) {
    }

    void onFovUpdate(FOVUpdateEvent event) {
    }

    /** 種類ごとの記憶も消すときは、継いだクラスで足す。 */
    void clear() {
        spot.clear();
        missedTicks = 0;
    }

    final boolean eligible(Minecraft mc) {
        return allowed(mc) && wanted(mc.player, ClientSettings.get());
    }

    /** これが外れたとき（一時オフ・種類のオフ・的当てなど）は、keepTicks に関係なくすぐ消す。 */
    final boolean allowed(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null || !KindSwitches.isEnabled(kind) || !PlayerRules.canUse(player)
                || (blockedByUsingHand() && player.isHandActive())) {
            return false;
        }
        return ClientSettings.get().enabled(kind);
    }
}
