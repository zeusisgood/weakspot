package com.example.weakspot.client;

import com.example.weakspot.common.FaceMath;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.FaceRect;
import com.example.weakspot.common.MarkerMotion;
import com.example.weakspot.common.WeakSpotPlacer;
import com.example.weakspot.config.SyncedSettings;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 1つのブロック面、または動物の当たり判定の箱の面に出ている弱点。
 * ブロックの弱点の座標はワールド座標系の (u, v)。動物の弱点の (u, v) は、その面の左下の角からの位置で、
 * 動物が動いても一緒に動く（面の位置は follow で更新し、origin を足してワールド座標にする）。
 * u, v は当たり判定の位置（ヒットの瞬間に移動先へ変わる）。マーカーの表示位置は motion で別に持つ（見た目だけ）。
 */
final class WeakSpot {

    final HitKind kind;
    final BlockPos pos;
    final EnumFacing face;
    final int axis;
    /** 動物の弱点のときの動物（ブロックの弱点は null）。 */
    final Entity entity;
    /** 面の平面の、法線軸方向のワールド座標。動物が動くと変わる。 */
    double plane;
    /** 動物の弱点の、面の左下の角のワールド座標 (u, v)。ブロックは 0。 */
    private double originU;
    private double originV;
    /** ブロックの弱点はワールド座標の矩形。動物の弱点は、大きさだけを持つ (0, 0)〜(幅, 高さ)。 */
    final FaceRect rect;
    final double radius;
    /** この面での、縁の余白と最小移動距離（面の大きさに合わせて決める。WeakSpotPlacer.layout）。 */
    private final double edgeMargin;
    private final double minMoveDistance;
    /** 出したときの当たり判定の箱（作物は育つと高さが変わるので、変わったら出し直す）。動物は、今の箱。 */
    AxisAlignedBB box;

    double u;
    double v;
    long lastActiveTick;
    /** マーカーの表示位置と残像。 */
    final MarkerMotion motion = new MarkerMotion(0, 0);

    private WeakSpot(HitKind kind, Entity entity, BlockPos pos, EnumFacing face, double plane, FaceRect rect,
                     WeakSpotPlacer.Layout layout, AxisAlignedBB box) {
        this.kind = kind;
        this.entity = entity;
        this.pos = pos;
        this.face = face;
        this.axis = face.getAxis().ordinal();
        this.plane = plane;
        this.rect = rect;
        this.radius = layout.radius;
        this.edgeMargin = layout.edgeMargin;
        this.minMoveDistance = layout.minMoveDistance;
        this.box = box;
    }

    /**
     * 照準位置 aim を避けて、新しい弱点を出す。面が小さすぎる（minFaceSize 未満）ときは出さずに null を返す。
     * 成長の弱点は、作物の小さい面でも当てやすいように、growthMinRadius も下限に使う。
     */
    static WeakSpot spawn(HitKind kind, World world, BlockPos pos, IBlockState state, EnumFacing face, Vec3d aim,
                          SyncedSettings settings, Random random) {
        WeakSpot spot = create(kind, world, pos, state, face, settings);
        if (spot == null) {
            return null;
        }
        double[] aimUV = spot.toUV(aim);
        double[] p = WeakSpotPlacer.place(spot.rect, spot.radius, spot.edgeMargin, aimUV[0], aimUV[1],
                spot.minMoveDistance, random);
        spot.moveTo(p[0], p[1], false, 0);
        return spot;
    }

    /**
     * 他のプレイヤーから届いた位置 (u, v) に弱点を置く（描画用）。大きさは全員同じ設定値なので、自分の側で計算する。
     * 面が小さすぎる場合は null。
     */
    static WeakSpot at(HitKind kind, World world, BlockPos pos, IBlockState state, EnumFacing face, double u, double v,
                       SyncedSettings settings) {
        WeakSpot spot = create(kind, world, pos, state, face, settings);
        if (spot != null) {
            spot.moveTo(u, v, false, 0);
        }
        return spot;
    }

    /** ブロックの当たり判定の箱から、その面の矩形・平面・半径を決める（位置 u, v はまだ決めない）。 */
    private static WeakSpot create(HitKind kind, World world, BlockPos pos, IBlockState state, EnumFacing face,
                                   SyncedSettings settings) {
        AxisAlignedBB box = state.getSelectedBoundingBox(world, pos);
        int axis = face.getAxis().ordinal();
        FaceRect rect = FaceMath.faceRect(axis, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        if (WeakSpotPlacer.isTooSmall(rect, settings.minFaceSize)) {
            return null;
        }
        double minRadius = kind == HitKind.GROWTH
                ? Math.max(settings.weakSpotMinRadius, settings.growthMinRadius)
                : settings.weakSpotMinRadius;
        WeakSpotPlacer.Layout layout = WeakSpotPlacer.layout(rect, settings.weakSpotRadiusRatio, minRadius,
                settings.weakSpotMaxRadiusRatio, settings.edgeMargin, settings.minMoveDistance);
        double[] min = {box.minX, box.minY, box.minZ};
        double[] max = {box.maxX, box.maxY, box.maxZ};
        double plane = face.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? max[axis] : min[axis];
        return new WeakSpot(kind, null, pos, face, plane, rect, layout, box);
    }

    /**
     * 動物の当たり判定の箱の、面 face に弱点を出す。面が小さすぎるときは null。
     * 小さい動物（子どものニワトリなど）は、ブロックの小さい面と同じ配置のルールに従う。
     */
    static WeakSpot spawnOnEntity(Entity entity, EnumFacing face, Vec3d aim, SyncedSettings settings, Random random) {
        WeakSpot spot = createOnEntity(entity, face, settings);
        if (spot == null) {
            return null;
        }
        double[] aimUV = spot.toUV(aim);
        double[] p = WeakSpotPlacer.place(spot.rect, spot.radius, spot.edgeMargin, aimUV[0], aimUV[1],
                spot.minMoveDistance, random);
        spot.moveTo(p[0], p[1], false, 0);
        return spot;
    }

    /** 他のプレイヤーから届いた、動物の面の中の位置 (u, v) に弱点を置く（描画用）。 */
    static WeakSpot atEntity(Entity entity, EnumFacing face, double u, double v, SyncedSettings settings) {
        WeakSpot spot = createOnEntity(entity, face, settings);
        if (spot != null) {
            spot.moveTo(u, v, false, 0);
        }
        return spot;
    }

    private static WeakSpot createOnEntity(Entity entity, EnumFacing face, SyncedSettings settings) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        int axis = face.getAxis().ordinal();
        FaceRect world = FaceMath.faceRect(axis, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        FaceRect rect = new FaceRect(0, 0, world.width(), world.height());
        if (WeakSpotPlacer.isTooSmall(rect, settings.minFaceSize)) {
            return null;
        }
        WeakSpotPlacer.Layout layout = WeakSpotPlacer.layout(rect, settings.weakSpotRadiusRatio,
                settings.weakSpotMinRadius, settings.weakSpotMaxRadiusRatio, settings.edgeMargin,
                settings.minMoveDistance);
        WeakSpot spot = new WeakSpot(HitKind.ANIMAL, entity, entity.getPosition(), face, 0, rect, layout, box);
        spot.follow(box);
        return spot;
    }

    /** 動物の今の（または描く時点の）箱に合わせて、面の位置を更新する。ブロックの弱点では何もしない。 */
    void follow(AxisAlignedBB newBox) {
        if (entity == null) {
            return;
        }
        box = newBox;
        FaceRect world = FaceMath.faceRect(axis, newBox.minX, newBox.minY, newBox.minZ, newBox.maxX, newBox.maxY,
                newBox.maxZ);
        originU = world.minU;
        originV = world.minV;
        double[] min = {newBox.minX, newBox.minY, newBox.minZ};
        double[] max = {newBox.maxX, newBox.maxY, newBox.maxZ};
        plane = face.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? max[axis] : min[axis];
    }

    /** 動物の箱を、描く時点（partialTicks）の位置に合わせた箱。 */
    static AxisAlignedBB renderBox(Entity entity, float partialTicks) {
        double t = 1.0 - partialTicks;
        return entity.getEntityBoundingBox().offset((entity.lastTickPosX - entity.posX) * t,
                (entity.lastTickPosY - entity.posY) * t, (entity.lastTickPosZ - entity.posZ) * t);
    }

    /** 動物の箱の、照準の点 hit に一番近い面（照準が当たっている面）。 */
    static EnumFacing faceAt(AxisAlignedBB box, Vec3d hit) {
        int[] face = FaceMath.nearestFace(new double[] {hit.x, hit.y, hit.z},
                new double[] {box.minX, box.minY, box.minZ}, new double[] {box.maxX, box.maxY, box.maxZ});
        return EnumFacing.getFacingFromAxis(
                face[1] > 0 ? EnumFacing.AxisDirection.POSITIVE : EnumFacing.AxisDirection.NEGATIVE,
                EnumFacing.Axis.values()[face[0]]);
    }

    boolean matchesEntity(Entity entity, EnumFacing face) {
        return this.entity == entity && this.face == face;
    }

    boolean matches(HitKind kind, BlockPos pos, EnumFacing face) {
        return this.entity == null && this.kind == kind && this.pos.equals(pos) && this.face == face;
    }

    /** 同じブロックの同じ面（形も同じ）か。違えば、マーカーは動かさずにその場で切り替える。 */
    boolean sameSurface(WeakSpot other) {
        if (other != null && entity != null) {
            return matchesEntity(other.entity, other.face);
        }
        return other != null && matches(other.kind, other.pos, other.face) && box.equals(other.box);
    }

    /**
     * 成長の弱点を出す面。当たり判定の箱で一番大きい面（多くは上面）。
     * 側面が一番大きいとき（サトウキビなど）は、見えている今の面 keep（なければ null）、照準が当たっている面 aimed、
     * プレイヤーから見える側、の順に選ぶ（FaceMath.growthFaceAxis）。
     */
    static EnumFacing growthFace(AxisAlignedBB box, Vec3d eye, EnumFacing aimed, EnumFacing keep) {
        Vec3d center = box.getCenter();
        int axis = FaceMath.growthFaceAxis(box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ,
                keep == null ? -1 : keep.getAxis().ordinal(), aimed.getAxis().ordinal(),
                eye.x - center.x, eye.z - center.z);
        if (axis == FaceMath.AXIS_Y) {
            return EnumFacing.UP;
        }
        if (keep != null && keep.getAxis().ordinal() == axis) {
            return keep;
        }
        if (aimed.getAxis().ordinal() == axis) {
            return aimed;
        }
        boolean positive = axis == FaceMath.AXIS_X ? eye.x > center.x : eye.z > center.z;
        return EnumFacing.getFacingFromAxis(
                positive ? EnumFacing.AxisDirection.POSITIVE : EnumFacing.AxisDirection.NEGATIVE,
                axis == FaceMath.AXIS_X ? EnumFacing.Axis.X : EnumFacing.Axis.Z);
    }

    /** 箱の面 face が、視点 eye から見える側を向いているか（視点が面の平面より外側にある）。 */
    static boolean isFacing(AxisAlignedBB box, EnumFacing face, Vec3d eye) {
        int axis = face.getAxis().ordinal();
        double[] e = {eye.x, eye.y, eye.z};
        return face.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE
                ? e[axis] > new double[] {box.maxX, box.maxY, box.maxZ}[axis]
                : e[axis] < new double[] {box.minX, box.minY, box.minZ}[axis];
    }

    double[] toUV(Vec3d point) {
        double[] uv = FaceMath.toFaceUV(axis, point.x, point.y, point.z);
        return new double[] {uv[0] - originU, uv[1] - originV};
    }

    boolean isHitBy(Vec3d aim) {
        double[] a = toUV(aim);
        return WeakSpotPlacer.isHit(u, v, radius, a[0], a[1]);
    }

    /** 今の位置から minDistance 以上離れた場所へ移動する。animate なら、マーカーは nowMs から動いて追いつく。 */
    void relocate(Random random, boolean animate, long nowMs) {
        double[] p = WeakSpotPlacer.place(rect, radius, edgeMargin, u, v, minMoveDistance, random);
        moveTo(p[0], p[1], animate, nowMs);
    }

    /** 当たり判定の位置はすぐに変え、マーカーは animate なら動かし、そうでなければその場で切り替える。 */
    void moveTo(double newU, double newV, boolean animate, long nowMs) {
        u = newU;
        v = newV;
        if (animate) {
            motion.moveTo(newU, newV, nowMs);
        } else {
            motion.jumpTo(newU, newV);
        }
    }

    /** 面から lift だけ浮かせた位置のワールド座標。 */
    double[] worldPoint(double pu, double pv, double lift) {
        double sign = face.getAxisDirection().getOffset();
        return FaceMath.toWorld(axis, plane + sign * lift, pu + originU, pv + originV);
    }
}
