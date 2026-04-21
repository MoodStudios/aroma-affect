package com.ovrtechnology.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.ovrtechnology.command.SubCommand;
import com.ovrtechnology.util.Texts;
import com.ovrtechnology.variant.CustomNoseItem;
import com.ovrtechnology.variant.CustomNoseRegistry;
import com.ovrtechnology.variant.NoseVariant;
import com.ovrtechnology.variant.NoseVariantRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GiveVariantSubCommand implements SubCommand {

    private static final SuggestionProvider<CommandSourceStack> VARIANT_SUGGESTIONS = (ctx, builder) -> {
        for (ResourceLocation id : NoseVariantRegistry.all().keySet()) {
            builder.suggest(id.toString());
        }
        return builder.buildFuture();
    };

    @Override
    public String getName() {
        return "give_variant";
    }

    @Override
    public String getDescription() {
        return "Gives a fully-configured custom nose variant stack (admin/testing)";
    }

    @Override
    public ArgumentBuilder<CommandSourceStack, ?> build(LiteralArgumentBuilder<CommandSourceStack> builder) {
        return builder.then(
                Commands.argument("variant_id", StringArgumentType.string())
                        .suggests(VARIANT_SUGGESTIONS)
                        .executes(this::execute)
        );
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String raw = StringArgumentType.getString(context, "variant_id");
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            source.sendFailure(Texts.lit("§cInvalid variant id: " + raw));
            return 0;
        }
        Optional<NoseVariant> variantOpt = NoseVariantRegistry.get(id);
        if (variantOpt.isEmpty()) {
            source.sendFailure(Texts.lit("§cVariant not registered: " + id));
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Texts.lit("§cMust be executed by a player"));
            return 0;
        }
        if (!CustomNoseRegistry.getCUSTOM_NOSE().isPresent()) {
            source.sendFailure(Texts.lit("§cCustomNoseItem not registered"));
            return 0;
        }
        ItemStack stack = CustomNoseItem.stackFor(
                CustomNoseRegistry.getCUSTOM_NOSE().get(), id, variantOpt.get());
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        source.sendSuccess(() -> Texts.lit("§aGave " + id), true);
        return Command.SINGLE_SUCCESS;
    }
}
