package com.simibubi.create.content.curiosities.toolbox;

import java.util.Optional;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllTileEntities;
import com.simibubi.create.foundation.block.ITE;

import com.simibubi.create.lib.entity.FakePlayer;

import com.simibubi.create.lib.utility.NetworkUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;

public class ToolboxBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock, ITE<ToolboxTileEntity> {

	private final DyeColor color;

	public ToolboxBlock(Properties p_i48440_1_, DyeColor color) {
		super(p_i48440_1_);
		this.color = color;
		registerDefaultState(super.defaultBlockState().setValue(WATERLOGGED, false));
	}

	@Override
	public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> p_149666_2_) {
		if (group != CreativeModeTab.TAB_SEARCH && color != DyeColor.BROWN)
			return;
		super.fillItemCategory(group, p_149666_2_);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(WATERLOGGED)
			.add(FACING));
	}

	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(worldIn, pos, state, placer, stack);
		if (worldIn.isClientSide)
			return;
		if (stack == null)
			return;
		withTileEntityDo(worldIn, pos, te -> {
			te.readInventory(stack.getOrCreateTag()
				.getCompound("Inventory"));
			if (stack.hasCustomHoverName())
				te.setCustomName(stack.getHoverName());
		});
	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moving) {
		if (state.hasBlockEntity() && (!state.is(newState.getBlock()) || !newState.hasBlockEntity()))
			world.removeBlockEntity(pos);
	}

	@Override
	public void attack(BlockState state, Level world, BlockPos pos, Player player) {
		if (player instanceof FakePlayer)
			return;
		if (world.isClientSide)
			return;
		withTileEntityDo(world, pos, ToolboxTileEntity::unequipTracked);
		if (world instanceof ServerLevel) {
			ItemStack cloneItemStack = getCloneItemStack(world, pos, state);
			world.destroyBlock(pos, false);
			if (world.getBlockState(pos) != state)
				player.getInventory().placeItemBackInInventory(cloneItemStack);
		}
	}

	@Override
	public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
		ItemStack item = new ItemStack(this);
		Optional<ToolboxTileEntity> tileEntityOptional = getTileEntityOptional(world, pos);

		CompoundTag tag = item.getOrCreateTag();
		CompoundTag inv = tileEntityOptional.map(tb -> tb.inventory.serializeNBT())
			.orElse(new CompoundTag());
		tag.put("Inventory", inv);

		Component customName = tileEntityOptional.map(ToolboxTileEntity::getCustomName)
			.orElse(null);
		if (customName != null)
			item.setHoverName(customName);
		return item;
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState blockState2, LevelAccessor world, BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED))
			world.getLiquidTicks()
					.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		return state;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter p_220053_2_, BlockPos p_220053_3_,
							   CollisionContext p_220053_4_) {
		return AllShapes.TOOLBOX.get(state.getValue(FACING));
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
								 BlockHitResult ray) {

		if (player == null || player.isCrouching())
			return InteractionResult.PASS;
		if (player instanceof FakePlayer)
			return InteractionResult.PASS;
		if (world.isClientSide)
			return InteractionResult.SUCCESS;

		withTileEntityDo(world, pos,
			toolbox -> NetworkUtil.openGUI(((ServerPlayer) player), toolbox, toolbox::sendToContainer));
		return InteractionResult.SUCCESS;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return AllTileEntities.TOOLBOX.create(blockPos, blockState);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState ifluidstate = context.getLevel()
			.getFluidState(context.getClickedPos());
		return super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection()
			.getOpposite())
			.setValue(WATERLOGGED, Boolean.valueOf(ifluidstate.getType() == Fluids.WATER));
	}

	@Override
	public Class<ToolboxTileEntity> getTileEntityClass() {
		return ToolboxTileEntity.class;
	}

	public DyeColor getColor() {
		return color;
	}

	public static Ingredient getMainBox() {
		return Ingredient.of(AllBlocks.TOOLBOXES.get(DyeColor.BROWN)
			.get());
	}

}
