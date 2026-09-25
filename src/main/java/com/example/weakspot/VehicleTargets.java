package com.example.weakspot;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecartEmpty;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 乗り物の弱点の対象（両側。1.6.0）。馬系・豚は視線の向きに進む（STEERED。弱点は照準の真上か真下だけ）、
 * トロッコ・ボートは視線と関係なく進む（FREE）。
 */
public final class VehicleTargets {

    public enum Kind {
        /** 馬・ロバ・ラバ・スケルトンホース・ゾンビホース（鞍つきで、自分が操っている）、豚（ニンジン付きの棒で操っている）。 */
        STEERED,
        /** トロッコ（人が乗るもの）、ボート（自分が操っている）。 */
        FREE
    }

    private VehicleTargets() {
    }

    /** そのプレイヤーが乗っている、対象の乗り物の種類。対象でなければ null。 */
    public static Kind kind(EntityPlayer player) {
        Entity vehicle = player.getRidingEntity();
        if (vehicle instanceof AbstractHorse && !(vehicle instanceof EntityLlama)) {
            AbstractHorse horse = (AbstractHorse) vehicle;
            return horse.isHorseSaddled() && horse.getControllingPassenger() == player ? Kind.STEERED : null;
        }
        if (vehicle instanceof EntityPig) {
            return ((EntityPig) vehicle).canBeSteered() && vehicle.getControllingPassenger() == player
                    ? Kind.STEERED : null;
        }
        if (vehicle instanceof EntityMinecartEmpty) {
            return Kind.FREE;
        }
        if (vehicle instanceof EntityBoat) {
            return vehicle.getControllingPassenger() == player ? Kind.FREE : null;
        }
        return null;
    }

    /** 馬系・豚に乗っていて、弱点を照準の真上か真下だけに出すか（騎射・食事でも使う）。 */
    public static boolean isSteeredByLook(EntityPlayer player) {
        return kind(player) == Kind.STEERED;
    }
}
