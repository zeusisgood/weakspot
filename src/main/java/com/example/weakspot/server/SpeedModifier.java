package com.example.weakspot.server;

import com.example.weakspot.common.TimedBoostMath;
import java.util.UUID;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;

/**
 * 移動速度の一時的な修正（論理サーバー。1.8.8 で MoveHits の走りと VehicleHits の馬・豚から切り出した）。
 * 固定の UUID で、合計に (1 + 値) を掛ける修正を、かけ直すたびに外して付け直す。保存しない（加速中にワールドを保存・
 * ログアウトしても、速いまま残らない）。残り時間の数え方は、使う側がそれぞれに持つ。
 */
final class SpeedModifier {

    /** 修正の種類: 合計に (1 + 値) を掛ける。 */
    private static final int MULTIPLY_TOTAL = 2;

    private final UUID id;
    private final String name;

    SpeedModifier(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    /** 倍率 multiplier でかけ直す。1 以下なら外す。 */
    void set(EntityLivingBase living, double multiplier) {
        IAttributeInstance speed = living.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(id);
        double extra = TimedBoostMath.extra(multiplier);
        if (extra > 0) {
            speed.applyModifier(new AttributeModifier(id, name, extra, MULTIPLY_TOTAL).setSaved(false));
        }
    }

    void clear(EntityLivingBase living) {
        set(living, 1);
    }
}
