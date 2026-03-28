package com.example.lifesteal;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.entity.damage.DamageSource;

public class LifestealMod implements ModInitializer {
    public static final String MOD_ID = "lifesteal";

    @Override
    public void onInitialize() {
        registerCommands();
        registerEntityDeathEvent();
        registerPlayerSpawn();
    }

    private void registerEntityDeathEvent() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (damageSource.getAttacker() instanceof PlayerEntity) {
                PlayerEntity killer = (PlayerEntity) damageSource.getAttacker();
                
                // Add one heart (0.5 per half-heart) to killer
                float currentHealth = killer.getHealth();
                float maxHealth = killer.getMaxHealth();
                float newHealth = Math.min(currentHealth + 2.0f, maxHealth + 2.0f);
                
                killer.setHealth(newHealth);
                
                // Update max health if necessary
                if (newHealth > maxHealth) {
                    killer.getAttribute(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH)
                        .setBaseValue(newHealth);
                }
                
                killer.sendMessage(
                    Text.literal("§6+1 Heart from kill!"),
                    false
                );
            }
        });
    }

    private void registerPlayerSpawn() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) entity;
                // Reset to 10 hearts (20 health) on respawn
                player.setHealth(20.0f);
                player.getAttribute(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH)
                    .setBaseValue(20.0f);
            }
        });
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                net.minecraft.server.command.CommandManager
                    .literal("withdraw")
                    .then(
                        net.minecraft.server.command.CommandManager
                            .literal("heart")
                            .then(
                                net.minecraft.server.command.CommandManager
                                    .argument("amount", IntegerArgumentType.integer(1, 100))
                                    .executes(context -> {
                                        int amount = IntegerArgumentType.getInteger(context, "amount");
                                        PlayerEntity player = context.getSource().getPlayerOrThrow();
                                        
                                        float healthToRemove = amount * 2.0f; // 1 heart = 2 health
                                        float currentHealth = player.getHealth();
                                        float newHealth = Math.max(currentHealth - healthToRemove, 0.5f);
                                        
                                        player.setHealth(newHealth);
                                        
                                        player.sendMessage(
                                            Text.literal("§c-" + amount + " Heart(s)! Health: " + (int)(newHealth / 2)),
                                            false
                                        );
                                        
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                    )
            );
        });
    }
}
