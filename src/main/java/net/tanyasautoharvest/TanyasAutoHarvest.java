package net.tanyasautoharvest;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TanyasAutoHarvest implements ModInitializer {
	public static final String MOD_ID = "tanyas-auto-harvest";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		UseBlockCallback.EVENT.register(TanyasAutoHarvest::tryHarvest);
		LOGGER.info("Tanya's Auto Harvest is ready.");
	}

	private static InteractionResult tryHarvest(
			Player player,
			Level level,
			InteractionHand hand,
			BlockHitResult hitResult
	) {
		if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !player.mayBuild()) {
			return InteractionResult.PASS;
		}

		BlockPos pos = hitResult.getBlockPos();
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();

		if (!isSupportedCrop(block)
				|| !(block instanceof CropBlock crop)
				|| !crop.isMaxAge(state)) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!(level instanceof ServerLevel serverLevel) || !level.mayInteract(player, pos)) {
			return InteractionResult.PASS;
		}

		BlockState replantedState = crop.getStateForAge(0);
		if (!serverLevel.setBlock(pos, replantedState, Block.UPDATE_ALL)) {
			return InteractionResult.FAIL;
		}

		if (!player.isCreative()) {
			block.playerDestroy(
					serverLevel,
					player,
					pos,
					state,
					null,
					player.getItemInHand(hand)
			);
		}

		serverLevel.levelEvent(2001, pos, Block.getId(state));
		return InteractionResult.SUCCESS;
	}

	private static boolean isSupportedCrop(Block block) {
		return block == Blocks.WHEAT
				|| block == Blocks.CARROTS
				|| block == Blocks.POTATOES
				|| block == Blocks.BEETROOTS;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
