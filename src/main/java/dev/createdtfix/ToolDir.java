package dev.createdtfix;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Passes "which side of the tree the machine is on" from Create's breaker code to the
 * Dynamic Trees cut. Dynamic Trees uses it to make the tree fall away from the machine.
 * Server thread only; set just before the cut and read (cleared) once by the cut.
 */
public final class ToolDir {
	private static final ThreadLocal<Direction> NEXT = new ThreadLocal<>();

	private ToolDir() {}

	/** Side of the tree that faces the machine. Vertical results are ignored. */
	public static void set(Direction towardMachine) {
		if (!Config.on(Config.FALL_AWAY)) {
			NEXT.remove();
			return;
		}
		NEXT.set(towardMachine != null && towardMachine.getAxis().isHorizontal() ? towardMachine : null);
	}

	public static void setFromPositions(BlockPos cutPos, Vec3 machinePos, Vec3 motion) {
		Vec3 d = null;
		if (machinePos != null)
			d = machinePos.subtract(Vec3.atCenterOf(cutPos));
		if ((d == null || d.horizontalDistanceSqr() < 1.0E-4) && motion != null)
			d = motion.reverse();
		if (d == null || d.horizontalDistanceSqr() < 1.0E-4) {
			NEXT.remove();
			return;
		}
		set(Direction.getNearest(d.x, 0, d.z));
	}

	/** Returns the stored side (or a random horizontal side) and clears it. */
	public static Direction take(RandomSource random) {
		Direction d = NEXT.get();
		NEXT.remove();
		return d != null ? d : Direction.Plane.HORIZONTAL.getRandomDirection(random);
	}
}
