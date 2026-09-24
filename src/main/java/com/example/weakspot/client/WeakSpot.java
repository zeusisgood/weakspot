package com.example.weakspot.client;

import com.example.weakspot.common.FaceMath;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.FaceRect;
import com.example.weakspot.common.MarkerMotion;
import com.example.weakspot.common.WeakSpotPlacer;
import com.example.weakspot.config.SyncedSettings;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 1つのブロック面に出ている弱点。座標はワールド座標系の (u, v)。
 * u, v は当たり判定の位置（ヒットの瞬間に移動先へ変わる）。マーカーの表示位置は motion で別に持つ（見た目だけ）。
 */
final class WeakSpot {

    final HitKind kind;
    final BlockPos pos;
    final EnumFacing face;
    final int axis;
    /** 面の平面の、法線軸方向のワールド座標。 */
    final double plane;
    final FaceRect rect;
    final double radius;
    /** この面での、縁の余白と最小移動距離（面の大きさに合わせて決める。WeakSpotPlacer.layout）。 */
    private final double edgeMargin;
    private final double minMoveDistance;
    /** 出したときの当たり判定の箱（作物は育つと高さが変わるので、変わったら出し直す）。 */
    final AxisAlignedBB box;

    double u;
    double v;
    long lastActiveTick;
    /** マーカーの表示位置と残像。 */
    final MarkerMotion motion = new MarkerMotion(0, 0);

    private WeakSpot(HitKind kind, BlockPos pos, EnumFacing face, double plane, FaceRect rect,
                     WeakSpotPlacer.Layout layout, AxisAlignedBB box) {
        this.kind = kind;
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
        return new WeakSpot(kind, pos, face, plane, rect, layout, box);
    }

    boolean matches(HitKind kind, BlockPos pos, EnumFacing face) {
        return this.kind == kind && this.pos.equals(pos) && this.face == face;
    }

    /** 同じブロックの同じ面（形も同じ）か。違えば、マーカーは動かさずにその場で切り替える。 */
    boolean sameSurface(WeakSpot other) {
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
        return FaceMath.toFaceUV(axis, point.x, point.y, point.z);
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
        return FaceMath.toWorld(axis, plane + sign * lift, pu, pv);
    }
}
