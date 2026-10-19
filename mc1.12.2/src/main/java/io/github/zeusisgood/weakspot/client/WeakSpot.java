package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.FaceMath;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.FaceRect;
import io.github.zeusisgood.weakspot.common.MarkerMotion;
import io.github.zeusisgood.weakspot.common.WeakSpotPlacer;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * ブロックの弱点の (u, v) はワールド座標。動物の弱点の (u, v) は面の左下の角からの位置で、動物が動いても一緒に動く。
 * u, v は当たり判定の位置（ヒットの瞬間に移動先へ変わる）。マーカーの表示位置は motion で別に持つ（見た目だけ）。
 */
final class WeakSpot {

    final HitKind kind;
    final BlockPos pos;
    final EnumFacing face;
    final int axis;
    /** ブロックの弱点は null。 */
    final Entity entity;
    /** 面の平面の、法線軸方向のワールド座標。動物が動くと変わる。 */
    double plane;
    /** ブロックは 0。 */
    private double originU;
    private double originV;
    /** 動物の弱点は、大きさだけを持つ (0, 0)〜(幅, 高さ)。 */
    final FaceRect rect;
    final double radius;
    private final double edgeMargin;
    private final double minMoveDistance;
    /** 作物は育つと高さが変わるので、変わったら出し直す。動物は、今の箱。 */
    AxisAlignedBB box;

    double u;
    double v;
    long lastActiveTick;
    /** 掘る前の採掘の弱点。薄く描き、ほかのプレイヤーには送らない。 */
    boolean preview;
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

    /** 面が小さすぎるときは null。成長の弱点は、作物の小さい面でも当てやすいように、growthMinRadius も下限に使う。 */
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

    /** 他のプレイヤーから届いた位置。大きさは全員同じ設定値なので、自分の側で計算する。面が小さすぎれば null。 */
    static WeakSpot at(HitKind kind, World world, BlockPos pos, IBlockState state, EnumFacing face, double u, double v,
                       SyncedSettings settings) {
        WeakSpot spot = create(kind, world, pos, state, face, settings);
        if (spot != null) {
            spot.moveTo(u, v, false, 0);
        }
        return spot;
    }

    private static WeakSpot create(HitKind kind, World world, BlockPos pos, IBlockState state, EnumFacing face,
                                   SyncedSettings settings) {
        AxisAlignedBB box = state.getSelectedBoundingBox(world, pos);
        int axis = face.getAxis().ordinal();
        FaceRect rect = FaceMath.faceRect(axis, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        if (WeakSpotPlacer.isTooSmall(rect, settings.minFaceSize)) {
            return null;
        }
        double minRadius = kind == HitKind.GROWTH || kind == HitKind.HARVEST
                ? Math.max(settings.weakSpotMinRadius, settings.growthMinRadius)
                : settings.weakSpotMinRadius;
        double size = settings.spotSize(kind);
        WeakSpotPlacer.Layout layout = WeakSpotPlacer.layout(rect, settings.weakSpotRadiusRatio * size,
                minRadius * size, settings.weakSpotMaxRadiusRatio * size, settings.edgeMargin,
                settings.minMoveDistance);
        double[] min = {box.minX, box.minY, box.minZ};
        double[] max = {box.maxX, box.maxY, box.maxZ};
        double plane = face.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? max[axis] : min[axis];
        return new WeakSpot(kind, null, pos, face, plane, rect, layout, box);
    }

    /** 面が小さすぎるときは null。 */
    static WeakSpot spawnOnEntity(HitKind kind, Entity entity, EnumFacing face, Vec3d aim, SyncedSettings settings,
                                  Random random) {
        WeakSpot spot = createOnEntity(kind, entity, face, settings);
        if (spot == null) {
            return null;
        }
        double[] aimUV = spot.toUV(aim);
        double[] p = WeakSpotPlacer.place(spot.rect, spot.radius, spot.edgeMargin, aimUV[0], aimUV[1],
                spot.minMoveDistance, random);
        spot.moveTo(p[0], p[1], false, 0);
        return spot;
    }

    static WeakSpot atEntity(Entity entity, EnumFacing face, double u, double v, SyncedSettings settings) {
        WeakSpot spot = createOnEntity(HitKind.ANIMAL, entity, face, settings);
        if (spot != null) {
            spot.moveTo(u, v, false, 0);
        }
        return spot;
    }

    private static WeakSpot createOnEntity(HitKind kind, Entity entity, EnumFacing face, SyncedSettings settings) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        int axis = face.getAxis().ordinal();
        FaceRect world = FaceMath.faceRect(axis, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        FaceRect rect = new FaceRect(0, 0, world.width(), world.height());
        if (WeakSpotPlacer.isTooSmall(rect, settings.minFaceSize)) {
            return null;
        }
        double size = settings.spotSize(kind);
        WeakSpotPlacer.Layout layout = WeakSpotPlacer.layout(rect, settings.weakSpotRadiusRatio * size,
                settings.weakSpotMinRadius * size, settings.weakSpotMaxRadiusRatio * size, settings.edgeMargin,
                settings.minMoveDistance);
        WeakSpot spot = new WeakSpot(kind, entity, entity.getPosition(), face, 0, rect, layout, box);
        spot.follow(box);
        return spot;
    }

    /** ブロックの弱点では何もしない。 */
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

    static AxisAlignedBB renderBox(Entity entity, float partialTicks) {
        double t = 1.0 - partialTicks;
        return entity.getEntityBoundingBox().offset((entity.lastTickPosX - entity.posX) * t,
                (entity.lastTickPosY - entity.posY) * t, (entity.lastTickPosZ - entity.posZ) * t);
    }

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

    /** 違えば、マーカーは動かさずにその場で切り替える。 */
    boolean sameSurface(WeakSpot other) {
        if (other != null && entity != null) {
            return matchesEntity(other.entity, other.face);
        }
        return other != null && matches(other.kind, other.pos, other.face) && box.equals(other.box);
    }

    /** keep は見えている今の面（なければ null）。 */
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

    /** animate なら、マーカーは nowMs から動いて追いつく。 */
    void relocate(Random random, boolean animate, long nowMs) {
        double[] p = WeakSpotPlacer.place(rect, radius, edgeMargin, u, v, minMoveDistance, random);
        moveTo(p[0], p[1], animate, nowMs);
    }

    void moveTo(double newU, double newV, boolean animate, long nowMs) {
        u = newU;
        v = newV;
        if (animate) {
            motion.moveTo(newU, newV, nowMs);
        } else {
            motion.jumpTo(newU, newV);
        }
    }

    double[] worldPoint(double pu, double pv, double lift) {
        double sign = face.getAxisDirection().getOffset();
        return FaceMath.toWorld(axis, plane + sign * lift, pu + originU, pv + originV);
    }
}
