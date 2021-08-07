package com.simibubi.create;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import IInventory;
import IRecipe;
import IRecipeSerializer;
import IRecipeType;
import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.content.contraptions.components.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.contraptions.components.crusher.CrushingRecipe;
import com.simibubi.create.content.contraptions.components.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.contraptions.components.fan.SplashingRecipe;
import com.simibubi.create.content.contraptions.components.millstone.MillingRecipe;
import com.simibubi.create.content.contraptions.components.mixer.CompactingRecipe;
import com.simibubi.create.content.contraptions.components.mixer.MixingRecipe;
import com.simibubi.create.content.contraptions.components.press.PressingRecipe;
import com.simibubi.create.content.contraptions.components.saw.CuttingRecipe;
import com.simibubi.create.content.contraptions.fluids.actors.FillingRecipe;
import com.simibubi.create.content.contraptions.itemAssembly.SequencedAssemblyRecipeSerializer;
import com.simibubi.create.content.contraptions.processing.BasinRecipe;
import com.simibubi.create.content.contraptions.processing.EmptyingRecipe;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipeBuilder.ProcessingRecipeFactory;
import com.simibubi.create.content.contraptions.processing.ProcessingRecipeSerializer;
import com.simibubi.create.content.curiosities.tools.SandPaperPolishingRecipe;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.lib.utility.ShapedRecipeUtil;

public enum AllRecipeTypes implements IRecipeTypeInfo {

	MECHANICAL_CRAFTING(MechanicalCraftingRecipe.Serializer::new),
	CONVERSION(ConversionRecipe::new),
	CRUSHING(CrushingRecipe::new),
	CUTTING(CuttingRecipe::new),
	MILLING(MillingRecipe::new),
	BASIN(BasinRecipe::new),
	MIXING(MixingRecipe::new),
	COMPACTING(CompactingRecipe::new),
	PRESSING(PressingRecipe::new),
	SANDPAPER_POLISHING(SandPaperPolishingRecipe::new),
	SPLASHING(SplashingRecipe::new),
	DEPLOYING(DeployerApplicationRecipe::new),
	FILLING(FillingRecipe::new),
	EMPTYING(EmptyingRecipe::new),
	SEQUENCED_ASSEMBLY(SequencedAssemblyRecipeSerializer::new),

	;

	private ResourceLocation id;
	private Supplier<IRecipeSerializer<?>> serializerSupplier;
	private Supplier<IRecipeType<?>> typeSupplier;
	private IRecipeSerializer<?> serializer;
	private IRecipeType<?> type;

	AllRecipeTypes(Supplier<IRecipeSerializer<?>> serializerSupplier, Supplier<IRecipeType<?>> typeSupplier) {
		this.id = Create.asResource(Lang.asId(name()));
		this.serializerSupplier = serializerSupplier;
		this.typeSupplier = typeSupplier;
	}

	AllRecipeTypes(Supplier<IRecipeSerializer<?>> serializerSupplier, IRecipeType<?> existingType) {
		this(serializerSupplier, () -> existingType);
	}

	AllRecipeTypes(Supplier<IRecipeSerializer<?>> serializerSupplier) {
		this.id = Create.asResource(Lang.asId(name()));
		this.serializerSupplier = serializerSupplier;
		this.typeSupplier = () -> simpleType(id);
	}

	AllRecipeTypes(ProcessingRecipeFactory<?> processingFactory) {
		this(processingSerializer(processingFactory));
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IRecipeSerializer<?>> T getSerializer() {
		return (T) serializer;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IRecipeType<?>> T getType() {
		return (T) type;
	}

	public <C extends IInventory, T extends IRecipe<C>> Optional<T> find(C inv, World world) {
		return world.getRecipeManager()
			.getRecipeFor(getType(), inv, world);
	}

	public static void register() {
		ShapedRecipeUtil.setCraftingSize(9, 9);

		for (AllRecipeTypes r : AllRecipeTypes.values()) {
			r.serializer = r.serializerSupplier.get();
			r.type = r.typeSupplier.get();
			r.serializer.setRegistryName(r.id);
			Registry.register(Registry.RECIPE_SERIALIZER, location, r.serializer);
		}
	}

	private static Supplier<IRecipeSerializer<?>> processingSerializer(ProcessingRecipeFactory<?> factory) {
		return () -> new ProcessingRecipeSerializer<>(factory);
	}

	public static <T extends Recipe<?>> RecipeType<T> simpleType(ResourceLocation id) {
		String stringId = id.toString();
		return Registry.register(Registry.RECIPE_TYPE, id, new RecipeType<T>() {
			public String toString() {
				return stringId;
			}
		});
	}

}
