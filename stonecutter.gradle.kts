plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    properties {
        tags(version, loader)
    }

    constants {
        match(loader, "fabric", "neoforge")
    }

    // Mojang renamed ResourceLocation to Identifier in 1.21.11
    replacements {
        string(eval(current.version, ">=1.21.11")) {
            replace("ResourceLocation", "Identifier")
        }
    }

    // Before 26.x the shared code is pointed at the shims in src/legacy (Balm 21 callbacks, 26.x GUI/input API)
    val modern = eval(current.version, ">=26")
    val legacyTypes = mapOf(
        "net.blay09.mods.balm.platform.event.callback." to "com.ovrtechnology.legacy.balm.callback.",
        "net.blay09.mods.balm.client.platform.event.callback." to "com.ovrtechnology.legacy.balm.client.callback.",
        "net.blay09.mods.balm.fabric.platform.runtime.FabricLoadContext" to "com.ovrtechnology.legacy.balm.FabricLoadContext",
        "net.blay09.mods.balm.neoforge.platform.runtime.NeoForgeLoadContext" to "net.blay09.mods.balm.neoforge.NeoForgeLoadContext",
        "net.blay09.mods.balm.Balm;" to "net.blay09.mods.balm.api.Balm;",
        "net.blay09.mods.balm.client.BalmClient;" to "net.blay09.mods.balm.api.client.BalmClient;",
        "net.blay09.mods.balm.world.BalmMenuFactory" to "net.blay09.mods.balm.api.menu.BalmMenuFactory",
        "net.blay09.mods.balm.world.BalmMenuProvider" to "net.blay09.mods.balm.api.menu.BalmMenuProvider",
        "net.minecraft.client.gui.GuiGraphicsExtractor" to "com.ovrtechnology.legacy.gui.GuiGraphicsExtractor",
        "net.minecraft.client.renderer.RenderPipelines" to "com.ovrtechnology.legacy.gui.RenderPipelines",
        "net.minecraft.client.input.MouseButtonEvent" to "com.ovrtechnology.legacy.input.MouseButtonEvent",
        "net.minecraft.client.input.MouseButtonInfo" to "com.ovrtechnology.legacy.input.MouseButtonInfo",
        "net.minecraft.client.input.KeyEvent" to "com.ovrtechnology.legacy.input.KeyEvent",
        "net.minecraft.client.input.CharacterEvent" to "com.ovrtechnology.legacy.input.CharacterEvent",
        "net.minecraft.client.input.InputWithModifiers" to "com.ovrtechnology.legacy.input.InputWithModifiers",
        "net.minecraft.util.ARGB;" to "com.ovrtechnology.legacy.util.ARGB;",
        "net.minecraft.util.Util;" to "net.minecraft.Util;",
        "net.minecraft.world.entity.npc.villager.Villager;" to "net.minecraft.world.entity.npc.Villager;",
        "net.minecraft.world.entity.npc.villager.VillagerData;" to "net.minecraft.world.entity.npc.VillagerData;",
        "net.minecraft.world.entity.npc.villager.VillagerProfession;" to "net.minecraft.world.entity.npc.VillagerProfession;",
        "net.minecraft.world.entity.npc.villager.AbstractVillager;" to "net.minecraft.world.entity.npc.AbstractVillager;",
        "net.minecraft.world.entity.monster.zombie.ZombieVillager;" to "net.minecraft.world.entity.monster.ZombieVillager;",
        "net.minecraft.world.entity.animal.golem.IronGolem;" to "net.minecraft.world.entity.animal.IronGolem;",
        "net.minecraft.world.entity.animal.golem.AbstractGolem;" to "net.minecraft.world.entity.animal.AbstractGolem;",
        "net.minecraft.world.entity.animal.squid.Squid;" to "net.minecraft.world.entity.animal.Squid;",
        "net.minecraft.world.entity.animal.parrot.ShoulderRidingEntity;" to "net.minecraft.world.entity.animal.ShoulderRidingEntity;",
        "net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;" to "net.minecraft.world.entity.vehicle.AbstractMinecart;",
        "net.minecraft.client.model.npc.VillagerModel;" to "net.minecraft.client.model.VillagerModel;",
        "net.minecraft.client.model.animal.sniffer.SnifferModel;" to "net.minecraft.client.model.SnifferModel;",
        "net.minecraft.client.model.animal.golem.IronGolemModel;" to "net.minecraft.client.model.IronGolemModel;",
        "net.minecraft.advancements.triggers.UsedTotemTrigger;" to "net.minecraft.advancements.critereon.UsedTotemTrigger;",
        // Renamed members. Each legacy form must not already appear in src so the reverse switch stays exact.
        ".setScreenAndShow(" to ".setScreen(",
        "minecraft.gui.screen()" to "minecraft.screen",
        "mc.gui.screen()" to "mc.screen",
        "level.getMinY()" to "level.getMinBuildHeight()",
        "level.getMaxY()" to "(level.getMaxBuildHeight() - 1)",
        "dimension().identifier()" to "dimension().location()",
        "dimension.identifier()" to "dimension.location()",
        "biomeKey.identifier()" to "biomeKey.location()",
        "key.identifier()" to "key.location()",
        "k.identifier()" to "k.location()",
        "registryAccess().lookupOrThrow(" to "registryAccess().registryOrThrow(",
        "registryAccess.lookupOrThrow(" to "registryAccess.registryOrThrow(",
        "BuiltInRegistries.ITEM.get(" to "BuiltInRegistries.ITEM.getHolder(",
        "BuiltInRegistries.BLOCK.get(" to "BuiltInRegistries.BLOCK.getHolder(",
        "BuiltInRegistries.SOUND_EVENT.get(" to "BuiltInRegistries.SOUND_EVENT.getHolder(",
        "BuiltInRegistries.ENTITY_TYPE.get(" to "BuiltInRegistries.ENTITY_TYPE.getHolder(",
        "chunkPos.x()" to "chunkPos.x",
        "chunkPos.z()" to "chunkPos.z",
    )
    replacements {
        for ((modernName, legacyName) in legacyTypes) {
            string(modern) {
                replace(legacyName, modernName)
            }
        }
    }
}
