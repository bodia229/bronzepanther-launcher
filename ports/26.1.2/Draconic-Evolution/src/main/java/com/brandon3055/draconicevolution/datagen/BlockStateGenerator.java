package com.brandon3055.draconicevolution.datagen;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.blocks.DislocatorReceptacle;
import com.brandon3055.draconicevolution.blocks.Portal;
import com.brandon3055.draconicevolution.blocks.RainSensor;
import com.brandon3055.draconicevolution.blocks.machines.EnergyPylon;
import com.brandon3055.draconicevolution.blocks.machines.Generator;
import com.brandon3055.draconicevolution.blocks.machines.Grinder;
import com.brandon3055.draconicevolution.init.DEContent;
import com.google.gson.JsonObject;
import com.mojang.math.Quadrant;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import static net.minecraft.core.Direction.DOWN;
import static net.minecraft.core.Direction.UP;

/** Native model definitions retain the authored models, rotations and multipart conditions. */
public class BlockStateGenerator extends ModelProvider {
    private BlockModelGenerators states;
    private final Set<Identifier> emittedModels = new HashSet<>();

    public BlockStateGenerator(DataGenerator gen, ResourceManager resources) {
        super(gen.getPackOutput(), DraconicEvolution.MODID);
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() { return Stream.empty(); }

    @Override
    protected void registerModels(BlockModelGenerators blocks, ItemModelGenerators items) {
        states = blocks;
        emittedModels.clear();
        //Simple Blocks
        simpleBlock(DEContent.DRACONIUM_BLOCK);
        simpleBlock(DEContent.AWAKENED_DRACONIUM_BLOCK, cubeBottomTop("awakened_draconium_block", modLoc("block/awakened_draconium_block_side"), modLoc("block/awakened_draconium_block"), modLoc("block/awakened_draconium_block")));
        simpleBlock(DEContent.INFUSED_OBSIDIAN);
        simpleBlock(DEContent.ENERGY_CORE);
        simpleBlock(DEContent.ENERGY_CORE_STABILIZER, modLoc("block/energy_core_stabilizer"));
        simpleBlock(DEContent.CREATIVE_OP_CAPACITOR);
        simpleBlock(DEContent.STABILIZED_SPAWNER, modLoc("block/stabilized_spawner"));
        simpleBlock(DEContent.PARTICLE_GENERATOR, modLoc("block/particle_generator"));
        simpleBlock(DEContent.CRAFTING_CORE, modLoc("block/crafting/fusion_crafting_core"));
        simpleBlock(DEContent.DISLOCATION_INHIBITOR, cubeBottomTop("dislocation_inhibitor", modLoc("block/dislocation_inhibitor"), modLoc("block/parts/machine_top"), modLoc("block/parts/machine_top")));

        //TODO
//        multiLayerBlock(DEContent.OVERWORLD_DRACONIUM_ORE, mcLoc("block/stone"), modLoc("block/draconium_ore_overlay"));
//        multiLayerBlock(DEContent.NETHER_DRACONIUM_ORE, mcLoc("block/netherrack"), modLoc("block/draconium_ore_overlay"));
//        multiLayerBlock(DEContent.END_DRACONIUM_ORE, mcLoc("block/end_stone"), modLoc("block/draconium_ore_overlay"));
//        multiLayerBlock(DEContent.DEEPSLATE_DRACONIUM_ORE, mcLoc("block/deepslate"), modLoc("block/draconium_ore_overlay"));

        simpleBlock(DEContent.OVERWORLD_DRACONIUM_ORE);
        simpleBlock(DEContent.NETHER_DRACONIUM_ORE);
        simpleBlock(DEContent.END_DRACONIUM_ORE);
        simpleBlock(DEContent.DEEPSLATE_DRACONIUM_ORE);

        directionalBlock(DEContent.BASIC_CRAFTING_INJECTOR, modLoc("block/crafting/crafting_injector_draconium"));
        directionalBlock(DEContent.WYVERN_CRAFTING_INJECTOR, modLoc("block/crafting/crafting_injector_wyvern"));
        directionalBlock(DEContent.AWAKENED_CRAFTING_INJECTOR, modLoc("block/crafting/crafting_injector_draconic"));
        directionalBlock(DEContent.CHAOTIC_CRAFTING_INJECTOR, modLoc("block/crafting/crafting_injector_chaotic"));

        directionalFromNorth(DEContent.FLUID_GATE, modLoc("block/fluid_gate"));
        directionalFromNorth(DEContent.FLUX_GATE, modLoc("block/flux_gate"));

        states.blockStateOutput.accept(MultiVariantGenerator.dispatch(DEContent.RAIN_SENSOR.get())
                .with(PropertyDispatch.initial(RainSensor.ACTIVE).generate(active -> variant(modLoc(active ? "block/rain_sensor_active" : "block/rain_sensor")))));

        directionalBlock(DEContent.POTENTIOMETER, modLoc("block/potentiometer"));

        simpleBlock(DEContent.ENERGY_TRANSFUSER, modLoc("block/energy_transfuser"));

        simpleBlock(DEContent.DISENCHANTER, modLoc("block/disenchanter"));
        simpleBlock(DEContent.CELESTIAL_MANIPULATOR, modLoc("block/celestial_manipulator"));
        simpleBlock(DEContent.ENTITY_DETECTOR, modLoc("block/entity_detector"));
        simpleBlock(DEContent.ENTITY_DETECTOR_ADVANCED, modLoc("block/entity_detector_advanced"));

        dummyBlock(DEContent.BASIC_IO_CRYSTAL);
        dummyBlock(DEContent.WYVERN_IO_CRYSTAL);
        dummyBlock(DEContent.DRACONIC_IO_CRYSTAL);
//        dummyBlock(DEContent.  CRYSTAL_IO_CHAOTIC);
        dummyBlock(DEContent.BASIC_RELAY_CRYSTAL);
        dummyBlock(DEContent.WYVERN_RELAY_CRYSTAL);
        dummyBlock(DEContent.DRACONIC_RELAY_CRYSTAL);
//        dummyBlock(DEContent.  CRYSTAL_RELAY_CHAOTIC);
        dummyBlock(DEContent.BASIC_WIRELESS_CRYSTAL);
        dummyBlock(DEContent.WYVERN_WIRELESS_CRYSTAL);
        dummyBlock(DEContent.DRACONIC_WIRELESS_CRYSTAL);
//        dummyBlock(DEContent.  CRYSTAL_WIRELESS_CHAOTIC);
        dummyBlock(DEContent.STRUCTURE_BLOCK);
        dummyBlock(DEContent.CHAOS_CRYSTAL);
        dummyBlock(DEContent.PLACED_ITEM);
        dummyBlock(DEContent.CHAOS_CRYSTAL_PART);
        dummyBlock(DEContent.COMET_SPAWNER);

        dummyBlock(DEContent.DRACONIUM_CHEST);
        dummyBlock(DEContent.REACTOR_CORE);
        dummyBlock(DEContent.REACTOR_STABILIZER);
        dummyBlock(DEContent.REACTOR_INJECTOR);


        var pylonModels = new java.util.EnumMap<EnergyPylon.Mode, Identifier>(EnergyPylon.Mode.class);
        for (EnergyPylon.Mode mode : EnergyPylon.Mode.values()) {
            String io = mode == EnergyPylon.Mode.OUTPUT ? "output" : "input";
            pylonModels.put(mode, cubeBottomTop("energy_pylon_" + io, modLoc("block/energy_pylon/energy_pylon_" + io), modLoc("block/energy_pylon/energy_pylon_" + io), modLoc("block/energy_pylon/energy_pylon_active_face")));
        }
        blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(DEContent.ENERGY_PYLON.get())
                .with(PropertyDispatch.initial(EnergyPylon.MODE, EnergyPylon.FACING).generate((mode, dir) ->
                        variant(pylonModels.get(mode), dir == UP ? 0 : dir == DOWN ? 180 : 90,
                                dir.getAxis() == Direction.Axis.Y ? 0 : 180 + 90 * dir.get2DDataValue()))));
        simpleBlock(DEContent.DISLOCATOR_PEDESTAL, modLoc("block/dislocator_pedestal"));
        Identifier inactive = cubeAll("dislocator_receptacle_inactive", modLoc("block/dislocator_receptacle_inactive"));
        Identifier active = cubeAll("dislocator_receptacle_active", modLoc("block/dislocator_receptacle_active"));
        blocks.blockStateOutput.accept(MultiVariantGenerator.dispatch(DEContent.DISLOCATOR_RECEPTACLE.get())
                .with(PropertyDispatch.initial(DislocatorReceptacle.CAMO, DislocatorReceptacle.ACTIVE)
                        .generate((camo, on) -> variant(camo ? modLoc("block/infused_obsidian") : on ? active : inactive))));

        var portal = MultiPartGenerator.multiPart(DEContent.PORTAL.get());
        for (Direction.Axis axis : Direction.Axis.values()) {
            Identifier wallWestEast = modLoc(axis == Direction.Axis.Z ? "block/portal/portal_wall_x" : axis == Direction.Axis.Y ? "block/portal/portal_wall_y" : "block/portal/portal_wall_z");
            Identifier wallUpDown = modLoc(axis == Direction.Axis.Z || axis == Direction.Axis.Y ? "block/portal/portal_wall_y" : "block/portal/portal_wall_z");
            portal.with(new ConditionBuilder().term(Portal.VISIBLE, true).term(Portal.AXIS, axis), variant(modLoc("block/portal/portal"), axis != Direction.Axis.Y ? 90 : 0, axis == Direction.Axis.X ? 90 : 0));
            portal.with(new ConditionBuilder().term(Portal.DRAW_UP, true).term(Portal.AXIS, axis), variant(wallUpDown, axis == Direction.Axis.X || axis == Direction.Axis.Z ? -90 : 0, 0));
            portal.with(new ConditionBuilder().term(Portal.DRAW_DOWN, true).term(Portal.AXIS, axis), variant(wallUpDown, axis == Direction.Axis.X || axis == Direction.Axis.Z ? 90 : 0, axis == Direction.Axis.Y ? 180 : 0));
            portal.with(new ConditionBuilder().term(Portal.DRAW_WEST, true).term(Portal.AXIS, axis), variant(wallWestEast, 0, axis == Direction.Axis.Z ? 180 : axis == Direction.Axis.Y ? -90 : 0));
            portal.with(new ConditionBuilder().term(Portal.DRAW_EAST, true).term(Portal.AXIS, axis), variant(wallWestEast, axis == Direction.Axis.X ? 180 : 0, axis == Direction.Axis.Y ? 90 : 0));
        }
        blocks.blockStateOutput.accept(portal);
        var generator = MultiPartGenerator.multiPart(DEContent.GENERATOR.get());
        var grinder = MultiPartGenerator.multiPart(DEContent.GRINDER.get());
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int angle = (int) dir.getOpposite().toYRot();
            generator.with(new ConditionBuilder().term(Generator.FACING, dir), variant(modLoc("block/generator/generator"), 0, angle));
            generator.with(new ConditionBuilder().term(Generator.FACING, dir).term(Generator.ACTIVE, true), variant(modLoc("block/generator/generator_flame"), 0, angle));
            grinder.with(new ConditionBuilder().term(Grinder.FACING, dir), variant(modLoc("block/grinder/grinder"), 0, angle));
            grinder.with(new ConditionBuilder().term(Grinder.FACING, dir).term(Grinder.ACTIVE, true), variant(modLoc("block/grinder/grinder_eyes"), 0, angle));
        }
        blocks.blockStateOutput.accept(generator);
        blocks.blockStateOutput.accept(grinder);
    }

    private Identifier modLoc(String path) { return Identifier.fromNamespaceAndPath(DraconicEvolution.MODID, path); }
    private MultiVariant variant(Identifier model) { return BlockModelGenerators.plainVariant(model); }
    private MultiVariant variant(Identifier model, int x, int y) {
        return NativeBlockStateModels.variant(model, x, y);
    }
    private Identifier cubeAll(String name, Identifier texture) {
        Identifier model = modLoc("block/" + name);
        if (emittedModels.add(model)) ModelTemplates.CUBE_ALL.create(model, new TextureMapping().put(TextureSlot.ALL, new Material(texture)), states.modelOutput);
        return model;
    }
    private Identifier cubeBottomTop(String name, Identifier side, Identifier bottom, Identifier top) {
        Identifier model = modLoc("block/" + name);
        if (emittedModels.add(model)) ModelTemplates.CUBE_BOTTOM_TOP.create(model, new TextureMapping().put(TextureSlot.SIDE, new Material(side)).put(TextureSlot.BOTTOM, new Material(bottom)).put(TextureSlot.TOP, new Material(top)), states.modelOutput);
        return model;
    }
    private void simpleBlock(Supplier<? extends Block> block) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block.get());
        simpleBlock(block, cubeAll(id.getPath(), modLoc("block/" + id.getPath())));
    }
    private void simpleBlock(Supplier<? extends Block> block, Identifier model) {
        states.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.get(), variant(model)));
    }
    private void dummyBlock(Supplier<? extends Block> block) {
        Identifier id = modLoc("block/dummy");
        if (emittedModels.add(id)) states.modelOutput.accept(id, () -> {
            JsonObject model = new JsonObject(), textures = new JsonObject();
            model.addProperty("parent", "minecraft:block/block");
            textures.addProperty("particle", "minecraft:block/glass");
            model.add("textures", textures);
            return model;
        });
        simpleBlock(block, id);
    }
    private void directionalBlock(Supplier<? extends Block> block, Identifier model) {
        states.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.get())
                .with(PropertyDispatch.initial(BlockStateProperties.FACING).generate(dir -> variant(model,
                        dir == DOWN ? 180 : dir == UP ? 0 : 90,
                        dir.getAxis().isVertical() ? 0 : (int) dir.toYRot() + 180))));
    }
    private void directionalFromNorth(Supplier<? extends Block> block, Identifier model) {
        states.blockStateOutput.accept(MultiVariantGenerator.dispatch(block.get())
                .with(PropertyDispatch.initial(BlockStateProperties.FACING).generate(dir -> variant(model,
                        dir == DOWN ? 90 : dir == UP ? -90 : 0,
                        dir.getAxis().isVertical() ? 0 : (int) dir.toYRot() + 180))));
    }
    @Override public String getName() { return "Draconic Evolution Blockstates"; }
}
