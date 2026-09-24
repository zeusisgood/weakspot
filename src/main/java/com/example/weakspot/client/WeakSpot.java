package com.example.weakspot.client;

import com.example.weakspot.common.FaceMath;
import com.example.weakspot.common.HitKind;
import com.example.weakspot.common.FaceRect;
import com.example.weakspot.common.WeakSpotPlacer;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** 1つのブロック面に出ている弱点。座標はワールド座標系の (u, v)。 */
final class WeakSpot {

    final HitKind kind;
    final BlockPos pos;
    final EnumFacing face;
    final int axis;
    /** 面の平面の、法線軸方向のワールド座標。 */
    final double plane;
    final FaceRect rect;
    final double radius;
    /** 出したときの当たり判定の箱（作物は育つと高さが変わるので、変わったら出し直す）。 */
    final AxisAlignedBB box;

    double u;
    double v;
    long lastActiveTick;

    private WeakSpot(HitKind kind, BlockPos pos, EnumFacing face, double plane, FaceRect rect, double radius,
                     AxisAlignedBB box) {
        this.kind = kind;
        this.pos = pos;
        this.face = face;
        this.axis = face.getAxis().ordinal();
        this.plane = plane;
        this.rect = rect;
        this.radius = radius;
        this.box = box;
    }

    /** 照準位置 aim を避けて、新しい弱点を出す。minRadius は最小の半径（0 なら比率どおり）。 */
    static WeakSpot spawn(HitKind kind, World world, BlockPos pos, IBlockState state, EnumFacing face, Vec3d aim,
                          double radiusRatio, double minRadius, double edgeMargin, double minDistance, Random random) {
        AxisAlignedBB box = state.getSelectedBoundingBox(world, pos);
        int axis = face.getAxis().ordinal();
        FaceRect rect = FaceMath.faceRect(axis, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        double[] min = {box.minX, box.minY, box.minZ};
        double[] max = {box.maxX, box.maxY, box.maxZ};
        double plane = face.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? max[axis] : min[axis];

        WeakSpot spot = new WeakSpot(kind, pos, face, plane, rect, WeakSpotPlacer.radius(rect, radiusRatio, minRadius),
                box);
        double[] aimUV = spot.toUV(aim);
        double[] p = WeakSpotPlacer.place(rect, spot.radius, edgeMargin, aimUV[0], aimUV[1], minDistance, random);
        spot.u = p[0];
        spot.v = p[1];
        return spot;
    }

    boolean matches(HitKind kind, BlockPos pos, EnumFacing face) {
        return this.kind == kind && this.pos.equals(pos) && this.face == face;
    }

    /**
     * 作物・苗木の弱点を出す面。当たり判定の箱で一番大きい面（多くは上面）。
     * 側面が一番大きいときは、プレイヤーから見える側にする。
     */
    static EnumFacing growthFace(AxisAlignedBB box, Vec3d eye) {
        int axis = FaceMath.largestFaceAxis(box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ);
        if (axis == FaceMath.AXIS_Y) {
            return EnumFacing.UP;
        }
        Vec3d center = box.getCenter();
        boolean positive = axis == FaceMath.AXIS_X ? eye.x > center.x : eye.z > center.z;
        return EnumFacing.getFacingFromAxis(
                positive ? EnumFacing.AxisDirection.POSITIVE : EnumFacing.AxisDirection.NEGATIVE,
                axis == FaceMath.AXIS_X ? EnumFacing.Axis.X : EnumFacing.Axis.Z);
    }

    double[] toUV(Vec3d point) {
        return FaceMath.toFaceUV(axis, point.x, point.y, point.z);
    }

    boolean isHitBy(Vec3d aim) {
        double[] a = toUV(aim);
        return WeakSpotPlacer.isHit(u, v, radius, a[0], a[1]);
    }

    /** 今の位置から minDistance 以上離れた場所へ移動する。 */
    void relocate(double edgeMargin, double minDistance, Random random) {
        double[] p = WeakSpotPlacer.place(rect, radius, edgeMargin, u, v, minDistance, random);
        u = p[0];
        v = p[1];
    }

    /** 面から lift だけ浮かせた位置のワールド座標。 */
    double[] worldPoint(double pu, double pv, double lift) {
        double sign = face.getAxisDirection().getOffset();
        return FaceMath.toWorld(axis, plane + sign * lift, pu, pv);
    }
}
