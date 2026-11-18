package dev.createdtfix;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/**
 * Create-update safety. Before each mixin is applied, checks that everything it hooks into
 * still exists in the installed Create and Dynamic Trees. If something is gone, only that fix
 * is skipped (with a warning in the log) instead of crashing the game at launch.
 * Also skips this mod's saw fix if Create's own Dynamic Trees compat is no longer empty.
 * Reads the original class files from the jars (no transformers), so it sees Create's code, not ours.
 */
public final class MixinPlugin implements IMixinConfigPlugin {
	/** System property read by Config: "true" when this mod's saw fix is applied. */
	public static final String SAW_FIX_PROPERTY = "create_dt_fix.sawFixActive";

	private static final Logger LOG = LoggerFactory.getLogger("create_dt_fix");

	// Class names (internal form)
	private static final String DYNAMIC_TREE = "com/simibubi/create/compat/dynamictrees/DynamicTree";
	private static final String QUEUE = "com/simibubi/create/foundation/utility/AbstractBlockBreakQueue";
	private static final String TREE_CUTTER = "com/simibubi/create/content/kinetics/saw/TreeCutter";
	private static final String SAW_BE = "com/simibubi/create/content/kinetics/saw/SawBlockEntity";
	private static final String SAW_MB = "com/simibubi/create/content/kinetics/saw/SawMovementBehaviour";
	private static final String BREAKER_BE = "com/simibubi/create/content/kinetics/base/BlockBreakingKineticBlockEntity";
	private static final String BREAKER_MB = "com/simibubi/create/content/kinetics/base/BlockBreakingMovementBehaviour";
	private static final String MOVEMENT = "com/simibubi/create/api/behaviour/movement/MovementBehaviour";
	private static final String DEPLOYER_PLAYER = "com/simibubi/create/content/kinetics/deployer/DeployerFakePlayer";
	private static final String BRANCH = "com/dtteam/dynamictrees/block/branch/BranchBlock";
	private static final String MTH = "net/minecraft/util/Mth";

	// Descriptors
	private static final String STATE = "Lnet/minecraft/world/level/block/state/BlockState;";
	private static final String BLOCK = "Lnet/minecraft/world/level/block/Block;";
	private static final String POS = "Lnet/minecraft/core/BlockPos;";
	private static final String LEVEL = "Lnet/minecraft/world/level/Level;";
	private static final String STACK = "Lnet/minecraft/world/item/ItemStack;";
	private static final String CONTEXT = "Lcom/simibubi/create/content/contraptions/behaviour/MovementContext;";
	private static final String LIVING = "Lnet/minecraft/world/entity/LivingEntity;";
	private static final String PLAYER = "Lnet/minecraft/world/entity/player/Player;";
	private static final String CONSUMER = "Ljava/util/function/BiConsumer;";
	private static final String FIND_DT_DESC = "(" + BLOCK + POS + ")Ljava/util/Optional;";

	/** One thing a mixin needs: a class, a method, a field, or a call inside a method. */
	private record Need(String owner, String name, String desc, boolean field, String callOwner, String callName,
		String callDesc) {
		String describe() {
			String s = owner.replace('/', '.') + (name == null ? "" : "." + name + (field ? "" : desc));
			return callName == null ? s : s + " calling " + callOwner.replace('/', '.') + "." + callName + callDesc;
		}
	}

	private static Need cls(String owner) { return new Need(owner, null, null, false, null, null, null); }
	private static Need method(String owner, String name, String desc) { return new Need(owner, name, desc, false, null, null, null); }
	private static Need field(String owner, String name, String desc) { return new Need(owner, name, desc, true, null, null, null); }
	private static Need call(String owner, String name, String desc, String cOwner, String cName, String cDesc) {
		return new Need(owner, name, desc, false, cOwner, cName, cDesc);
	}

	private record Fix(String label, List<Need> needs) {}

	private static final Map<String, Fix> FIXES = new HashMap<>();
	static {
		List<Need> dtCut = List.of(
			method(TREE_CUTTER, "canDynamicTreeCutFrom", "(" + BLOCK + ")Z"),
			method(DYNAMIC_TREE, "<init>", "(" + POS + ")V"),
			method(QUEUE, "destroyBlocks", "(" + LEVEL + LIVING + CONSUMER + ")V"));

		FIXES.put("DynamicTreeMixin", new Fix("saw fix", List.of(
			method(DYNAMIC_TREE, "isDynamicBranch", "(" + BLOCK + ")Z"),
			method(DYNAMIC_TREE, "destroyBlocks", "(" + LEVEL + STACK + PLAYER + CONSUMER + ")V"),
			field(DYNAMIC_TREE, "startCutPos", POS))));
		FIXES.put("SawBlockEntityMixin", new Fix("stationary saw (fall direction, roots, saw switch)", List.of(
			call(SAW_BE, "onBlockBroken", "(" + STATE + ")V", TREE_CUTTER, "findDynamicTree", FIND_DT_DESC),
			method(SAW_BE, "isSawable", "(" + STATE + ")Z"))));
		FIXES.put("SawMovementBehaviourMixin", new Fix("moving saw (fall direction, saw switch)", List.of(
			call(SAW_MB, "onBlockBroken", "(" + CONTEXT + POS + STATE + ")V", TREE_CUTTER, "findDynamicTree", FIND_DT_DESC))));
		FIXES.put("BlockBreakingKineticBlockEntityMixin", new Fix("stationary drill and breakers", concat(dtCut, List.of(
			method(BREAKER_BE, "onBlockBroken", "(" + STATE + ")V"),
			call(BREAKER_BE, "tick", "()V", MTH, "clamp", "(III)I"),
			field(BREAKER_BE, "breakingPos", POS)))));
		FIXES.put("BlockBreakingMovementBehaviourMixin", new Fix("moving drill and breakers", concat(dtCut, List.of(
			method(BREAKER_MB, "destroyBlock", "(" + CONTEXT + POS + ")V"),
			call(BREAKER_MB, "tickBreaker", "(" + CONTEXT + ")V", MTH, "clamp", "(III)I"),
			method(MOVEMENT, "collectOrDropItem", "(" + CONTEXT + STACK + ")V")))));
		FIXES.put("BranchBlockMixin", new Fix("deployer drops", List.of(
			method(BRANCH, "futureBreak", "(" + STATE + LEVEL + POS + LIVING + ")V"),
			cls(DEPLOYER_PLAYER))));
		FIXES.put("TreeCutterMixin", new Fix("vanilla tree cutter guard", List.of(
			method(TREE_CUTTER, "isLog", "(" + STATE + ")Z"))));
		// BlockBehaviourMixin targets vanilla Minecraft, which does not change: no check.
	}

	private static List<Need> concat(List<Need> a, List<Need> b) {
		List<Need> out = new ArrayList<>(a);
		out.addAll(b);
		return out;
	}

	private final Map<String, ClassNode> classes = new HashMap<>();

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		String simple = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
		Fix fix = FIXES.get(simple);
		if (fix == null)
			return true;

		for (Need need : fix.needs()) {
			String missing = check(need);
			if (missing != null) {
				LOG.warn("Create x Dynamic Trees Fix: {} is OFF. Create or Dynamic Trees changed: {} ({}). "
					+ "The other fixes still work. Please report this with your Create and Dynamic Trees versions.",
					fix.label(), need.describe(), missing);
				if (simple.equals("DynamicTreeMixin"))
					System.setProperty(SAW_FIX_PROPERTY, "false");
				return false;
			}
		}

		if (simple.equals("DynamicTreeMixin")) {
			MethodNode isBranch = findMethod(classes.get(DYNAMIC_TREE), "isDynamicBranch", "(" + BLOCK + ")Z");
			if (!returnsFalseOnly(isBranch)) {
				LOG.info("Create x Dynamic Trees Fix: Create has its own Dynamic Trees saw fix, so this mod's "
					+ "saw fix is off. The other fixes stay on.");
				System.setProperty(SAW_FIX_PROPERTY, "false");
				return false;
			}
			LOG.info("Create x Dynamic Trees Fix: saw fix is on (Create's Dynamic Trees compat is still empty).");
			System.setProperty(SAW_FIX_PROPERTY, "true");
		}
		return true;
	}

	/** Null if the need is met, else a short reason. */
	private String check(Need need) {
		ClassNode node = load(need.owner());
		if (node == null)
			return "class not found: " + loadErrors.getOrDefault(need.owner(), "unknown error");
		if (need.name() == null)
			return null;
		if (need.field()) {
			for (FieldNode f : node.fields)
				if (f.name.equals(need.name()) && f.desc.equals(need.desc()))
					return null;
			return "field not found";
		}
		MethodNode m = findMethod(node, need.name(), need.desc());
		if (m == null)
			return "method not found";
		if (need.callName() == null)
			return null;
		for (AbstractInsnNode insn : m.instructions)
			if (insn instanceof MethodInsnNode call && call.owner.equals(need.callOwner())
				&& call.name.equals(need.callName()) && call.desc.equals(need.callDesc()))
				return null;
		return "call not found in method";
	}

	private final Map<String, String> loadErrors = new HashMap<>();

	/**
	 * Reads the class file as shipped in the jar (no transformers, so no mixins: we see Create's
	 * own code). The game's mixin service cannot give untransformed classes, so it is only the
	 * fallback (it skips mixins too, as it reads classes for frame computing, not class loading).
	 */
	private ClassNode load(String internalName) {
		return classes.computeIfAbsent(internalName, n -> {
			String error;
			try (InputStream in = findClassFile(n)) {
				if (in != null) {
					ClassNode node = new ClassNode();
					new ClassReader(in).accept(node, ClassReader.SKIP_FRAMES);
					return node;
				}
				error = "class file not found";
			} catch (Throwable t) {
				error = t.toString();
			}
			try {
				return MixinService.getService().getBytecodeProvider().getClassNode(n.replace('/', '.'));
			} catch (Throwable t) {
				loadErrors.put(n, error + "; mixin service: " + t);
				return null;
			}
		});
	}

	private static InputStream findClassFile(String internalName) {
		String file = internalName + ".class";
		ClassLoader own = MixinPlugin.class.getClassLoader();
		InputStream in = own != null ? own.getResourceAsStream(file) : null;
		ClassLoader context = Thread.currentThread().getContextClassLoader();
		if (in == null && context != null && context != own)
			in = context.getResourceAsStream(file);
		return in;
	}

	private static MethodNode findMethod(ClassNode node, String name, String desc) {
		if (node != null)
			for (MethodNode m : node.methods)
				if (m.name.equals(name) && m.desc.equals(desc))
					return m;
		return null;
	}

	/** True if the method body is just "return false" (Create 6.0.10's commented-out compat). */
	private static boolean returnsFalseOnly(MethodNode m) {
		List<Integer> ops = new ArrayList<>();
		for (AbstractInsnNode insn : m.instructions)
			if (insn.getOpcode() >= 0) // skip labels, line numbers and frames
				ops.add(insn.getOpcode());
		return ops.equals(List.of(Opcodes.ICONST_0, Opcodes.IRETURN));
	}

	@Override public void onLoad(String mixinPackage) {}
	@Override public String getRefMapperConfig() { return null; }
	@Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
	@Override public List<String> getMixins() { return null; }
	@Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
	@Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
