package com.example.weakspot.client;

import com.example.weakspot.common.FaceMath;
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

    final BlockPos pos;
    final EnumFacing face;
    final int axis;
    /** 面の平面の、法線軸方向のワールド座標。 */
    final double plane;
    final FaceRect rect;
    final double radius;

    double u;
    double v;
    long lastActiveTick;
    /** このブロックで当てた回数（統計用）。 */
    int hits;

    private WeakSpot(BlockPos pos, EnumFacing face, double plane, FaceRect rect, double radius) {
        this.pos = pos;
        this.face = face;
        this.axis = face.getAxis().ordinal();
        this.plane = plane;
        this.rect = rect;
        this.radius = radius;
    }

    /** 照準位置 aim を避けて、新しい弱点を出す。 */
    static WeakSpot spawn(World world, BlockPos pos, IBlockState state, EnumFacing face, Vec3d aim,
                          double radiusRatio, double edgeMargin, double minDistance, Random random) {
        AxisAlignedBB box = state.getSelectedBoundingBox(world, pos);
        int axis = face.getAxis().ordinal();
        FaceRect rect = FaceMath.faceRect(axis, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        double[] min = {box.minX, box.minY, box.minZ};
        double[] max = {box.maxX, box.maxY, box.maxZ};
        double plane = face.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? max[axis] : min[axis];

        WeakSpot spot = new WeakSpot(pos, face, plane, rect, WeakSpotPlacer.radius(rect, radiusRatio));
        double[] aimUV = spot.toUV(aim);
        double[] p = WeakSpotPlacer.place(rect, spot.radius, edgeMargin, aimUV[0], aimUV[1], minDistance, random);
        spot.u = p[0];
        spot.v = p[1];
        return spot;
    }

    boolean matches(BlockPos pos, EnumFacing face) {
        return this.pos.equals(pos) && this.face == face;
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
