package io.github.zeusisgood.weakspot.client;

import io.github.zeusisgood.weakspot.common.FaceMath;
import io.github.zeusisgood.weakspot.common.FaceRect;
import io.github.zeusisgood.weakspot.common.HitKind;
import io.github.zeusisgood.weakspot.common.MarkerMotion;
import io.github.zeusisgood.weakspot.common.WeakSpotPlacer;
import io.github.zeusisgood.weakspot.config.SyncedSettings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;

/**
 * ブロックの弱点の (u, v) はワールド座標。動物の弱点の (u, v) は面の左下の角からの位置で、動物が動いても一緒に動く。
 * u, v は当たり判定の位置（ヒットの瞬間に移動先へ変わる）。マーカーの表示位置は motion で別に持つ（見た目だけ）。
 */
final class WeakSpot {

    final HitKind kind;
    final BlockPos pos;
    final Direction face;
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
    AABB box;
    /** 置く場所から外す、面の座標の長方形（手前の隣のブロックの形）。ほかのプレイヤーの弱点・動物は空。 */
    private List<FaceRect> blocked = Collections.emptyList();

    double u;
    double v;
    long lastActiveTick;
    /** 掘る前の採掘の弱点。薄く描き、ほかのプレイヤーには送らない。 */
    boolean preview;
    /** 次に移る位置 {u, v}（片眼鏡の予告）。ほかのプレイヤーのマークは null。 */
    double[] next;
    final MarkerMotion motion = new MarkerMotion(0, 0);

    private WeakSpot(HitKind kind, Entity entity, BlockPos pos, Direction face, double plane, FaceRect rect,
                     WeakSpotPlacer.Layout layout, AABB box) {
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
    static WeakSpot spawn(HitKind kind, Level world, BlockPos pos, BlockState state, Direction face, Vec3 aim,
                          SyncedSettings settings, Random random) {
        WeakSpot spot = create(kind, world, pos, state, face, settings, OwnSize.of(kind));
        if (spot == null) {
            return null;
        }
        spot.blocked = frontShapes(world, pos, face);
        double[] aimUV = spot.toUV(aim);
        double[] p = WeakSpotPlacer.place(spot.rect, spot.radius, spot.edgeMargin, aimUV[0], aimUV[1],
                spot.minMoveDistance, spot.blocked, random);
        spot.moveTo(p[0], p[1], false, 0);
        spot.planNext(random);
        return spot;
    }

    /** 他のプレイヤーから届いた位置。大きさは全員同じ設定値なので、自分の側で計算する。面が小さすぎれば null。 */
    static WeakSpot at(HitKind kind, Level world, BlockPos pos, BlockState state, Direction face, double u, double v,
                       SyncedSettings settings) {
        WeakSpot spot = create(kind, world, pos, state, face, settings, settings.spotSize(kind));
        if (spot != null) {
            spot.moveTo(u, v, false, 0);
        }
        return spot;
    }

    private static WeakSpot create(HitKind kind, Level world, BlockPos pos, BlockState state, Direction face,
                                   SyncedSettings settings, double size) {
        AABB box = outline(world, pos, state);
        if (box == null) {
            return null;
        }
        int axis = face.getAxis().ordinal();
        FaceRect rect = FaceMath.faceRect(axis, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        if (WeakSpotPlacer.isTooSmall(rect, settings.minFaceSize)) {
            return null;
        }
        double minRadius = kind == HitKind.GROWTH || kind == HitKind.HARVEST
                ? Math.max(settings.weakSpotMinRadius, settings.growthMinRadius)
                : settings.weakSpotMinRadius;
        WeakSpotPlacer.Layout layout = WeakSpotPlacer.layout(rect, settings.weakSpotRadiusRatio * size,
                minRadius * size, settings.weakSpotMaxRadiusRatio * size, settings.edgeMargin,
                settings.minMoveDistance);
        double[] min = {box.minX, box.minY, box.minZ};
        double[] max = {box.maxX, box.maxY, box.maxZ};
        double plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? max[axis] : min[axis];
        return new WeakSpot(kind, null, pos, face, plane, rect, layout, box);
    }

    /** ブロックの弱点を描き、当たりを決める外枠の箱（ワールド座標）。形がなければ null。 */
    static AABB outline(Level world, BlockPos pos, BlockState state) {
        VoxelShape shape = state.getShape(world, pos);
        return shape.isEmpty() ? null : shape.bounds().move(pos);
    }

    /**
     * 面の真正面の隣のブロック（パイプなど）の形を、面の座標に写した長方形。その後ろに出した弱点は、
     * バニラの照準が手前のブロックを指すので当てられない。パイプの本体と腕を別々に避けるため、箱ごとに分ける。
     * 空気・1 マスの立方体（面が見えない）は空。
     */
    private static List<FaceRect> frontShapes(Level world, BlockPos pos, Direction face) {
        BlockPos front = pos.relative(face);
        BlockState state = world.getBlockState(front);
        if (state.isAir()) {
            return Collections.emptyList();
        }
        VoxelShape shape = state.getShape(world, front);
        if (shape.isEmpty() || Block.isShapeFullBlock(shape)) {
            return Collections.emptyList();
        }
        int axis = face.getAxis().ordinal();
        List<FaceRect> rects = new ArrayList<>();
        for (AABB b : shape.toAabbs()) {
            AABB w = b.move(front);
            rects.add(FaceMath.faceRect(axis, w.minX, w.minY, w.minZ, w.maxX, w.maxY, w.maxZ));
        }
        return rects;
    }

    /** 照準の線（目 eye から向き dir）が外枠の箱に外から入る面。入らなければ null。 */
    static Direction entryFace(AABB box, Vec3 eye, Vec3 dir) {
        int[] face = FaceMath.rayEntryFace(new double[] {eye.x, eye.y, eye.z}, new double[] {dir.x, dir.y, dir.z},
                new double[] {box.minX, box.minY, box.minZ}, new double[] {box.maxX, box.maxY, box.maxZ});
        if (face == null) {
            return null;
        }
        return Direction.fromAxisAndDirection(Direction.Axis.values()[face[0]],
                face[1] > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }

    /** 照準の線と、箱の face の面の平面の交点。目の後ろなら null。 */
    static Vec3 rayPoint(AABB box, Direction face, Vec3 eye, Vec3 dir) {
        int axis = face.getAxis().ordinal();
        double plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? new double[] {box.maxX, box.maxY, box.maxZ}[axis] : new double[] {box.minX, box.minY, box.minZ}[axis];
        double[] p = FaceMath.rayPlane(new double[] {eye.x, eye.y, eye.z}, new double[] {dir.x, dir.y, dir.z}, axis,
                plane);
        return p == null ? null : new Vec3(p[0], p[1], p[2]);
    }

    /** 面が小さすぎるときは null。 */
    static WeakSpot spawnOnEntity(HitKind kind, Entity entity, Direction face, Vec3 aim, SyncedSettings settings,
                                  Random random) {
        WeakSpot spot = createOnEntity(kind, entity, face, settings, OwnSize.of(kind));
        if (spot == null) {
            return null;
        }
        double[] aimUV = spot.toUV(aim);
        double[] p = WeakSpotPlacer.place(spot.rect, spot.radius, spot.edgeMargin, aimUV[0], aimUV[1],
                spot.minMoveDistance, random);
        spot.moveTo(p[0], p[1], false, 0);
        spot.planNext(random);
        return spot;
    }

    static WeakSpot atEntity(Entity entity, Direction face, double u, double v, SyncedSettings settings) {
        WeakSpot spot = createOnEntity(HitKind.ANIMAL, entity, face, settings, settings.spotSize(HitKind.ANIMAL));
        if (spot != null) {
            spot.moveTo(u, v, false, 0);
        }
        return spot;
    }

    private static WeakSpot createOnEntity(HitKind kind, Entity entity, Direction face, SyncedSettings settings,
                                           double size) {
        AABB box = entity.getBoundingBox();
        int axis = face.getAxis().ordinal();
        FaceRect world = FaceMath.faceRect(axis, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        FaceRect rect = new FaceRect(0, 0, world.width(), world.height());
        if (WeakSpotPlacer.isTooSmall(rect, settings.minFaceSize)) {
            return null;
        }
        WeakSpotPlacer.Layout layout = WeakSpotPlacer.layout(rect, settings.weakSpotRadiusRatio * size,
                settings.weakSpotMinRadius * size, settings.weakSpotMaxRadiusRatio * size, settings.edgeMargin,
                settings.minMoveDistance);
        WeakSpot spot = new WeakSpot(kind, entity, entity.blockPosition(), face, 0, rect, layout, box);
        spot.follow(box);
        return spot;
    }

    /** ブロックの弱点では何もしない。 */
    void follow(AABB newBox) {
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
        plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? max[axis] : min[axis];
    }

    static AABB renderBox(Entity entity, float partialTicks) {
        double t = 1.0 - partialTicks;
        return entity.getBoundingBox().move((entity.xOld - entity.getX()) * t,
                (entity.yOld - entity.getY()) * t, (entity.zOld - entity.getZ()) * t);
    }

    static Direction faceAt(AABB box, Vec3 hit) {
        int[] face = FaceMath.nearestFace(new double[] {hit.x, hit.y, hit.z},
                new double[] {box.minX, box.minY, box.minZ}, new double[] {box.maxX, box.maxY, box.maxZ});
        return Direction.fromAxisAndDirection(Direction.Axis.values()[face[0]],
                face[1] > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }

    boolean matchesEntity(Entity entity, Direction face) {
        return this.entity == entity && this.face == face;
    }

    boolean matches(HitKind kind, BlockPos pos, Direction face) {
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
    static Direction growthFace(AABB box, Vec3 eye, Direction aimed, Direction keep) {
        Vec3 center = box.getCenter();
        int axis = FaceMath.growthFaceAxis(box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ,
                keep == null ? -1 : keep.getAxis().ordinal(), aimed.getAxis().ordinal(),
                eye.x - center.x, eye.z - center.z);
        if (axis == FaceMath.AXIS_Y) {
            return Direction.UP;
        }
        if (keep != null && keep.getAxis().ordinal() == axis) {
            return keep;
        }
        if (aimed.getAxis().ordinal() == axis) {
            return aimed;
        }
        boolean positive = axis == FaceMath.AXIS_X ? eye.x > center.x : eye.z > center.z;
        return Direction.fromAxisAndDirection(axis == FaceMath.AXIS_X ? Direction.Axis.X : Direction.Axis.Z,
                positive ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }

    static boolean isFacing(AABB box, Direction face, Vec3 eye) {
        int axis = face.getAxis().ordinal();
        double[] e = {eye.x, eye.y, eye.z};
        return face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? e[axis] > new double[] {box.maxX, box.maxY, box.maxZ}[axis]
                : e[axis] < new double[] {box.minX, box.minY, box.minZ}[axis];
    }

    double[] toUV(Vec3 point) {
        double[] uv = FaceMath.toFaceUV(axis, point.x, point.y, point.z);
        return new double[] {uv[0] - originU, uv[1] - originV};
    }

    boolean isHitBy(Vec3 aim) {
        double[] a = toUV(aim);
        return WeakSpotPlacer.isHit(u, v, radius, a[0], a[1]);
    }

    /** 予告した位置へ移り、その次を決める。animate なら、マーカーは nowMs から動いて追いつく。 */
    void relocate(Random random, boolean animate, long nowMs) {
        double[] p = next != null ? next : WeakSpotPlacer.place(rect, radius, edgeMargin, u, v, minMoveDistance,
                blocked, random);
        moveTo(p[0], p[1], animate, nowMs);
        planNext(random);
    }

    /** 予告は片眼鏡を付けているときだけ描くが、付け外しで移り方が変わらないよう、いつも前もって決めておく。 */
    private void planNext(Random random) {
        next = WeakSpotPlacer.place(rect, radius, edgeMargin, u, v, minMoveDistance, blocked, random);
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
        double sign = face.getAxisDirection().getStep();
        return FaceMath.toWorld(axis, plane + sign * lift, pu + originU, pv + originV);
    }
}
