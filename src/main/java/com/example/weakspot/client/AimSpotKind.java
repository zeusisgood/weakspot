package com.example.weakspot.client;

import com.example.weakspot.PlayerRules;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.config.SyncedSettings;
import com.example.weakspot.network.HitMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.event.FOVUpdateEvent;

/**
 * 照準のまわりの弱点の 1 つの種類（1.8.6。共通の土台）。出す・当てる・描く流れは AimSpots が回し、種類ごとに
 * 違うこと（出す条件、出し方、当てたときの効果、ゲージ）だけを、ここを継いだクラスが書く。
 * 使う種類: 乗り物・食事・はしご・走り・エリトラ・投げる物・近接・ネザーゲート・弓。釣り（水面の位置）は別（FishingSpot）。
 */
abstract class AimSpotKind {

    final HitKind kind;
    final HudSpot spot;

    AimSpotKind(HitKind kind, HudSpot spot) {
        this.kind = kind;
        this.spot = spot;
    }

    /** 共通の条件（ワールドにいる・種類がオン・PlayerRules・手を使っていない）のあとの、種類ごとの出す条件。 */
    abstract boolean wanted(EntityPlayerSP player, SyncedSettings settings);

    /** 手を使っている（弓を引く・食べる）間は出さないか。弓・食事は false。 */
    boolean blockedByUsingHand() {
        return true;
    }

    /** 出し方（HudSpot.FREE / VERTICAL / HORIZONTAL / VERTICAL_FIXED）。 */
    abstract int placement(EntityPlayerSP player);

    abstract int minHitInterval(SyncedSettings settings);

    /** 当てたときのサーバーへの通知。 */
    HitMessage message(EntityPlayerSP player, int streak) {
        return HitMessage.withoutTarget(kind, streak);
    }

    /** 当てたときの種類ごとの効果（音・コンボの数え・通知は済んでいる）。streak はヒット後の連続ヒット数。 */
    abstract void onHit(Minecraft mc, EntityPlayerSP player, SyncedSettings settings, int streak);

    /** 当てたあとも弱点を出し続けるか（false なら消す。弓の過剰チャージが上限に届いたとき）。 */
    boolean keepAfterHit(EntityPlayerSP player) {
        return true;
    }

    /** このフレームに当ててよいか（弓は、F1 で画面を隠している間は当てない）。 */
    boolean canAim(Minecraft mc) {
        return true;
    }

    /** ClientTickEvent START の、出す・消すの判定の前と後。END。 */
    void beforeTick(Minecraft mc) {
    }

    void afterTick(Minecraft mc) {
    }

    void tickEnd(Minecraft mc) {
    }

    /** 照準の上・下のゲージを描くか。描くなら drawGauge（beginOverlay と endOverlay の間）と drawAfterOverlay（文字）。 */
    boolean hasGauge(Minecraft mc, float partialTicks) {
        return false;
    }

    void drawGauge(Minecraft mc, float partialTicks) {
    }

    void drawAfterOverlay(Minecraft mc, float partialTicks) {
    }

    void onFovUpdate(FOVUpdateEvent event) {
    }

    /** 弱点の一時オフ・ワールドを出たとき。種類ごとの記憶も消すときは、継いだクラスで足す。 */
    void clear() {
        spot.clear();
    }

    final boolean eligible(Minecraft mc) {
        EntityPlayerSP player = mc.player;
        if (player == null || mc.world == null || !KindSwitches.isEnabled(kind) || !PlayerRules.canUse(player)
                || (blockedByUsingHand() && player.isHandActive())) {
            return false;
        }
        return wanted(player, ClientSettings.get());
    }
}
