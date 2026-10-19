package io.github.zeusisgood.weakspot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;

/** 馬系・豚は視線の向きに進むので、弱点は照準の真上か真下だけ。 */
public final class VehicleTargets {

    public enum Kind {
        /** 馬系とラクダ（鞍つきで、自分が操っている）、豚・ストライダー（棒で操っている）。 */
        STEERED,
        /** トロッコ（人が乗るもの）、ボート（チェスト付き・いかだを含む。自分が操っている）。 */
        FREE
    }

    private VehicleTargets() {
    }

    /** 対象でなければ null。 */
    public static Kind kind(Player player) {
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof AbstractHorse horse && !(vehicle instanceof Llama)) {
            return horse.isSaddled() && horse.getControllingPassenger() == player ? Kind.STEERED : null;
        }
        if (vehicle instanceof Pig || vehicle instanceof Strider) {
            return vehicle.getControllingPassenger() == player ? Kind.STEERED : null;
        }
        if (vehicle instanceof Minecart) {
            return Kind.FREE;
        }
        if (vehicle instanceof Boat) {
            return vehicle.getControllingPassenger() == player ? Kind.FREE : null;
        }
        return null;
    }

    /** 騎射・食事でも使う。 */
    public static boolean isSteeredByLook(Player player) {
        return kind(player) == Kind.STEERED;
    }
}
