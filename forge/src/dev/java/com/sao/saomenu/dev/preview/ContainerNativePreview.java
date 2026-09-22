package com.sao.saomenu.dev.preview;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.client.screen.vanilla.VanillaUiPolicy;
import com.sao.saomenu.config.SAOConfig;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.client.gui.CreativeTabsScreenPage;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/** Native container screen/controller/protocol scenario; preview classpath only. */
public final class ContainerNativePreview {
    private static final String PROP = "saomenu.preview.containers";
    private static final String RENAME = "ProbeName";

    private enum Leaf {
        SURVIVAL, SMALL_CHEST, LARGE_CHEST, SHULKER, HOPPER, DISPENSER, DROPPER,
        CRAFTING, FURNACE, BLAST, SMOKER, CREATIVE, BREWING, ANVIL, SMITHING,
        ENCHANT, GRINDSTONE, STONECUTTER, LOOM, CARTOGRAPHY, BEACON, MERCHANT,
        QUICK_MOVE, KEEP_UNKNOWN, DONE
    }

    private static int ticks;
    private static int stage;
    private static int age;
    private static int stall;
    private static boolean done;
    private static Leaf leaf = Leaf.SURVIVAL;
    private static AbstractContainerScreen<?> container;
    private static CompletableFuture<Void> serverWork;
    private static BlockPos origin;
    private static BlockPos smallChest;
    private static BlockPos largeChest;
    private static BlockPos shulker;
    private static BlockPos hopper;
    private static BlockPos dispenser;
    private static BlockPos dropper;
    private static BlockPos crafting;
    private static BlockPos furnace;
    private static BlockPos blast;
    private static BlockPos smoker;
    private static BlockPos brewing;
    private static BlockPos anvil;
    private static BlockPos smithing;
    private static BlockPos enchant;
    private static BlockPos grindstone;
    private static BlockPos stonecutter;
    private static BlockPos loom;
    private static BlockPos cartography;
    private static BlockPos beacon;
    private static UUID merchantId;
    private static int slotCount;
    private static int slotX;
    private static ItemStack selectedStoneResult;

    private ContainerNativePreview() {
    }

    public static boolean requested() {
        return SAOMenuPreview.requested() && Boolean.getBoolean(PROP);
    }

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(ContainerNativePreview::tick);
    }

    private static void tick(Minecraft client) {
        if (done) return;
        if (++ticks > 4800) {
            throw new IllegalStateException("Container native preview timed out at " + stage + "/" + leaf + ":" + age
                    + " screen=" + name(client.screen));
        }
        if (stage >= 2 && client.screen instanceof PauseScreen) {
            client.screen.onClose();
            return;
        }
        if (stage == 0) {
            if (client.getOverlay() != null || SAOConfig.path() == null) return;
            SaoUi.setEnabled(true);
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            client.options.pauseOnLostFocus = false;
            client.options.guiScale().set(2);
            client.resizeDisplay();
            client.setScreen(new TitleScreen());
            stage = 1;
            age = 0;
            return;
        }
        if (stage == 1) {
            if (++age < 8) return;
            client.createWorldOpenFlows().createFreshLevel("saomenu-containers-" + System.currentTimeMillis(),
                    new LevelSettings("SAOMenu native containers", GameType.SURVIVAL, false, Difficulty.PEACEFUL,
                            true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(20260922L, false, false),
                    access -> access.registryOrThrow(Registries.WORLD_PRESET)
                            .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            stage = 2;
            age = 0;
            return;
        }
        if (stage == 2) {
            if (client.player == null || client.level == null || client.screen != null) return;
            stage = 3;
            age = 0;
            server(client, ContainerNativePreview::installFixtures);
            return;
        }
        if (!serverReady()) return;
        dispatch(client);
    }

    private static void dispatch(Minecraft client) {
        switch (leaf) {
            case SURVIVAL -> survival(client, ++age);
            case SMALL_CHEST -> smallChest(client, ++age);
            case LARGE_CHEST -> largeChest(client, ++age);
            case SHULKER, HOPPER, DISPENSER, DROPPER -> storage(client, ++age);
            case CRAFTING -> crafting(client, ++age);
            case FURNACE, BLAST, SMOKER -> furnaceFamily(client, ++age);
            case CREATIVE -> creative(client, ++age);
            case BREWING -> brewing(client, ++age);
            case ANVIL -> anvil(client, ++age);
            case SMITHING -> smithing(client, ++age);
            case ENCHANT -> enchant(client, ++age);
            case GRINDSTONE -> grindstone(client, ++age);
            case STONECUTTER -> stonecutter(client, ++age);
            case LOOM -> loom(client, ++age);
            case CARTOGRAPHY -> cartography(client, ++age);
            case BEACON -> beacon(client, ++age);
            case MERCHANT -> merchant(client, ++age);
            case QUICK_MOVE -> quickMove(client, ++age);
            case KEEP_UNKNOWN -> unknown(client, ++age);
            case DONE -> finish(client);
        }
    }

    private static void survival(Minecraft client, int at) {
        switch (at) {
            case 1 -> server(client, player -> {
                player.setGameMode(GameType.SURVIVAL);
                clear(player);
                player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
                player.inventoryMenu.broadcastChanges();
            });
            case 2 -> {
                if (!serverReady()) hold();
            }
            case 3 -> press(client, GLFW.GLFW_KEY_E);
            case 5 -> {
                if (!mounted(client, InventoryScreen.class)) hold();
                else check(container.getMenu() == client.player.inventoryMenu, "Survival inventory replaced the native controller");
            }
            case 7 -> shot(client, "container_inventory.png");
            case 8 -> {
                child(client.screen, ImageButton.class);
                clickWidget(client.screen, child(client.screen, ImageButton.class));
                check(((InventoryScreen) client.screen).getRecipeBookComponent().isVisible(),
                        "Inventory recipe book toggle did not stay on the native component");
            }
            case 10 -> shot(client, "container_inventory_recipe.png");
            case 11 -> {
                clickWidget(client.screen, child(client.screen, ImageButton.class));
                clickPlayer(0);
                clickMenu(1, 1);
                clickMenu(3, 1);
            }
            case 14 -> {
                check(container.getMenu().getSlot(0).getItem().is(Items.STICK),
                        "Survival 2x2 craft did not produce sticks");
                clickMenu(0);
                check(container.getMenu().getCarried().is(Items.STICK), "Craft result was not picked up through the native slot");
            }
            case 16 -> server(client, player -> check(player.inventoryMenu.getCarried().is(Items.STICK),
                    "Server did not receive the crafted stick cursor"));
            case 17 -> {
                if (!serverReady()) hold();
            }
            case 18 -> closeUi(client);
            case 20 -> next();
            default -> { }
        }
    }

    private static void smallChest(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                player.setGameMode(GameType.SURVIVAL);
                clear(player);
                player.getInventory().setItem(0, new ItemStack(Items.APPLE, 8));
                player.inventoryMenu.broadcastChanges();
                open(player, smallChest);
            });
            case 4 -> {
                if (!mounted(client, ContainerScreen.class)) hold();
                else {
                    check(((ChestMenu) container.getMenu()).getRowCount() == 3, "Small chest did not mount GENERIC_9x3");
                    slotCount = container.getMenu().slots.size();
                    slotX = container.getMenu().getSlot(0).x;
                }
            }
            case 6 -> shot(client, "container_chest_small.png");
            case 7 -> {
                clickPlayer(0);
                check(container.getMenu().getCarried().getCount() == 8, "Native chest pickup lost the cursor stack");
                press(client, GLFW.GLFW_KEY_F8);
                check(!SaoUi.enabled(), "F8 did not restore native UI on a live container");
                check(client.screen == container, "F8 replaced the live container screen");
                check(container.getMenu().getCarried().getCount() == 8, "F8 dropped the native cursor stack");
                check(container.getMenu().slots.size() == slotCount && container.getMenu().getSlot(0).x == slotX,
                        "F8 mutated native slot identities");
            }
            case 9 -> shot(client, "container_chest_small_f8.png");
            case 10 -> {
                press(client, GLFW.GLFW_KEY_F8);
                check(SaoUi.enabled() && client.screen == container, "F8 re-enable lost the live container");
                clickMenu(0);
            }
            case 12 -> server(client, player -> {
                check(player.containerMenu.getSlot(0).getItem().is(Items.APPLE)
                        && player.containerMenu.getSlot(0).getItem().getCount() == 8,
                        "Server rejected the native chest deposit");
                check(player.getInventory().getItem(0).isEmpty(), "Server retained the deposited source stack");
            });
            case 13 -> {
                if (!serverReady()) hold();
            }
            case 14 -> closeUi(client);
            case 16 -> next();
            default -> { }
        }
    }

    private static void largeChest(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                player.getInventory().setItem(0, new ItemStack(Items.GOLD_INGOT, 16));
                player.inventoryMenu.broadcastChanges();
                BlockState state = player.serverLevel().getBlockState(largeChest);
                check(state.getValue(ChestBlock.TYPE) != ChestType.SINGLE, "Adjacent chests did not form a double chest");
                open(player, largeChest);
            });
            case 4 -> {
                if (!mounted(client, ContainerScreen.class)) hold();
                else check(((ChestMenu) container.getMenu()).getRowCount() == 6, "Large chest did not mount GENERIC_9x6");
            }
            case 6 -> shot(client, "container_chest_large.png");
            case 7 -> {
                clickPlayer(0);
                clickMenu(53);
            }
            case 9 -> server(client, player -> check(player.containerMenu.getSlot(53).getItem().is(Items.GOLD_INGOT),
                    "Double-chest slot transfer was not server-authoritative"));
            case 10 -> {
                if (!serverReady()) hold();
            }
            case 11 -> closeUi(client);
            case 13 -> next();
            default -> { }
        }
    }

    private static void storage(Minecraft client, int at) {
        Class<? extends Screen> type = switch (leaf) {
            case SHULKER -> ShulkerBoxScreen.class;
            case HOPPER -> HopperScreen.class;
            case DISPENSER, DROPPER -> DispenserScreen.class;
            default -> throw new IllegalStateException();
        };
        BlockPos pos = switch (leaf) {
            case SHULKER -> shulker;
            case HOPPER -> hopper;
            case DISPENSER -> dispenser;
            case DROPPER -> dropper;
            default -> throw new IllegalStateException();
        };
        String shot = switch (leaf) {
            case SHULKER -> "container_shulker.png";
            case HOPPER -> "container_hopper.png";
            case DISPENSER -> "container_dispenser.png";
            case DROPPER -> "container_dropper.png";
            default -> throw new IllegalStateException();
        };
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                player.getInventory().setItem(0, new ItemStack(Items.APPLE, 4));
                player.inventoryMenu.broadcastChanges();
                open(player, pos);
            });
            case 4 -> {
                if (!mounted(client, type)) hold();
            }
            case 6 -> shot(client, shot);
            case 7 -> {
                clickPlayer(0);
                clickMenu(0);
            }
            case 9 -> server(client, player -> check(player.containerMenu.getSlot(0).getItem().is(Items.APPLE),
                    leaf + " did not keep a native slot transfer"));
            case 10 -> {
                if (!serverReady()) hold();
            }
            case 11 -> closeUi(client);
            case 13 -> next();
            default -> { }
        }
    }

    private static void crafting(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                player.getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
                player.inventoryMenu.broadcastChanges();
                open(player, crafting);
            });
            case 4 -> {
                if (!mounted(client, CraftingScreen.class)) hold();
            }
            case 6 -> shot(client, "container_crafting.png");
            case 7 -> {
                clickWidget(client.screen, child(client.screen, ImageButton.class));
                check(((CraftingScreen) client.screen).getRecipeBookComponent().isVisible(),
                        "Crafting recipe book did not remain the native component");
            }
            case 9 -> shot(client, "container_crafting_book.png");
            case 10 -> {
                clickWidget(client.screen, child(client.screen, ImageButton.class));
                clickPlayer(0);
                clickMenu(1, 1);
                clickMenu(4, 1);
            }
            case 13 -> {
                check(container.getMenu().getSlot(0).getItem().is(Items.STICK), "Crafting table did not form sticks");
                clickMenu(0);
            }
            case 15 -> shot(client, "container_crafting_result.png");
            case 16 -> server(client, player -> {
                check(player.containerMenu.getCarried().is(Items.STICK), "Server did not carry the crafted sticks");
                check(player.containerMenu.getSlot(1).getItem().isEmpty()
                        && player.containerMenu.getSlot(4).getItem().isEmpty(), "Craft did not consume ingredients");
            });
            case 17 -> {
                if (!serverReady()) hold();
            }
            case 18 -> closeUi(client);
            case 20 -> next();
            default -> { }
        }
    }

    private static void furnaceFamily(Minecraft client, int at) {
        Class<? extends Screen> type = switch (leaf) {
            case FURNACE -> FurnaceScreen.class;
            case BLAST -> BlastFurnaceScreen.class;
            case SMOKER -> SmokerScreen.class;
            default -> throw new IllegalStateException();
        };
        BlockPos pos = switch (leaf) {
            case FURNACE -> furnace;
            case BLAST -> blast;
            case SMOKER -> smoker;
            default -> throw new IllegalStateException();
        };
        Item input = leaf == Leaf.SMOKER ? Items.PORKCHOP : Items.RAW_IRON;
        String shot = switch (leaf) {
            case FURNACE -> "container_furnace.png";
            case BLAST -> "container_blast_furnace.png";
            case SMOKER -> "container_smoker.png";
            default -> throw new IllegalStateException();
        };
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                player.getInventory().setItem(0, new ItemStack(Items.COAL, 8));
                player.getInventory().setItem(1, new ItemStack(input, 4));
                player.inventoryMenu.broadcastChanges();
                open(player, pos);
            });
            case 4 -> {
                if (!mounted(client, type)) hold();
                else check(client.screen instanceof AbstractFurnaceScreen, leaf + " is not the native furnace screen");
            }
            case 5 -> {
                clickPlayer(0);
                clickMenu(1);
                clickPlayer(1);
                clickMenu(0);
            }
            case 7 -> {
                if (!(container.getMenu() instanceof AbstractFurnaceMenu menu)) {
                    throw new IllegalStateException(leaf + " lost AbstractFurnaceMenu");
                }
                if (!menu.isLit() && stall++ < 60) {
                    hold();
                    return;
                }
                stall = 0;
                check(menu.isLit(), leaf + " never became lit after a native fuel insert");
                check(menu.getLitProgress() > 0, leaf + " had no live flame progress");
            }
            case 9 -> {
                if (((AbstractFurnaceMenu) container.getMenu()).getBurnProgress() < 12) {
                    check(++stall < 260, leaf + " did not advance native cooking progress");
                    hold();
                } else stall = 0;
            }
            case 11 -> shot(client, shot);
            case 12 -> {
                Item output = leaf == Leaf.SMOKER ? Items.COOKED_PORKCHOP : Items.IRON_INGOT;
                if (!container.getMenu().getSlot(2).getItem().is(output)) {
                    check(++stall < 260, leaf + " never completed a native smelt");
                    hold();
                } else stall = 0;
            }
            case 14 -> {
                shot(client, shot.replace(".png", "_ready.png"));
                clickMenu(2);
            }
            case 16 -> server(client, player -> {
                Item output = leaf == Leaf.SMOKER ? Items.COOKED_PORKCHOP : Items.IRON_INGOT;
                check(player.containerMenu.getCarried().is(output)
                                && player.containerMenu.getCarried().getCount() == 1,
                        "Server did not accept the completed " + leaf + " output");
                check(player.containerMenu.getSlot(0).getItem().getCount() == 3,
                        "Smelting did not consume exactly one ingredient");
            });
            case 18 -> closeUi(client);
            case 20 -> next();
            default -> { }
        }
    }

    private static void creative(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                player.setGameMode(GameType.CREATIVE);
                clear(player);
            });
            case 4 -> {
                if (!serverReady()) hold();
                else if (client.gameMode == null || client.gameMode.getPlayerMode() != GameType.CREATIVE) {
                    if (stall++ > 80) throw new IllegalStateException("Creative mode did not sync to the client");
                    hold();
                } else stall = 0;
            }
            case 5 -> press(client, GLFW.GLFW_KEY_E);
            case 7 -> {
                if (!mounted(client, CreativeModeInventoryScreen.class)) hold();
            }
            case 9 -> shot(client, "container_creative.png");
            case 10 -> {
                CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) client.screen;
                clickCreativeTab(screen, CreativeModeTabs.searchTab());
            }
            case 12 -> {
                EditBox search = child(client.screen, EditBox.class);
                clickWidget(client.screen, search);
                for (char character : "oak planks".toCharArray()) {
                    check(client.screen.charTyped(character, 0), "Creative search rejected a typed character");
                }
                check(search.getValue().equals("oak planks"), "Creative search field lost native input");
            }
            case 13 -> {
                var matches = container.getMenu().slots.stream()
                        .filter(slot -> slot.index < 45 && slot.hasItem()).toList();
                check(matches.size() == 2 && matches.stream().allMatch(slot ->
                                slot.getItem().is(Items.OAK_PLANKS) || slot.getItem().is(Items.DARK_OAK_PLANKS)),
                        "Creative screen did not filter the grid through its native search controller");
                Slot result = container.getMenu().slots.stream()
                        .filter(slot -> slot.getItem().is(Items.OAK_PLANKS)).findFirst()
                        .orElseThrow(() -> new IllegalStateException("Creative search did not return oak planks"));
                clickSlot(result);
                check(container.getMenu().getCarried().is(Items.OAK_PLANKS), "Native creative pickup failed");
                clickPlayer(0);
            }
            case 14 -> shot(client, "container_creative_search.png");
            case 15 -> {
                CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) client.screen;
                CreativeModeTab inventory = CreativeModeTabs.tabs().stream()
                        .filter(tab -> tab.getType() == CreativeModeTab.Type.INVENTORY)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Missing creative inventory tab"));
                clickCreativeTab(screen, inventory);
                check(container.getMenu().slots.stream().anyMatch(slot -> slot.getItem().is(Items.OAK_PLANKS)),
                        "Creative inventory tab lost the inserted stack");
                server(client, player -> check(player.getInventory().getItem(0).is(Items.OAK_PLANKS),
                        "Server did not accept the creative slot insertion"));
            }
            case 17 -> closeUi(client);
            case 18 -> server(client, player -> player.setGameMode(GameType.SURVIVAL));
            case 19 -> {
                if (!serverReady()) hold();
                else if (client.gameMode != null && client.gameMode.getPlayerMode() != GameType.SURVIVAL) {
                    if (stall++ > 80) throw new IllegalStateException("Survival mode did not return after creative");
                    hold();
                } else stall = 0;
            }
            case 20 -> next();
            default -> { }
        }
    }

    private static void brewing(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                player.getInventory().setItem(0, new ItemStack(Items.BLAZE_POWDER, 4));
                player.getInventory().setItem(1, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER));
                player.getInventory().setItem(2, new ItemStack(Items.NETHER_WART, 4));
                player.inventoryMenu.broadcastChanges();
                open(player, brewing);
            });
            case 4 -> {
                if (!mounted(client, BrewingStandScreen.class)) hold();
            }
            case 6 -> {
                clickPlayer(0);
                clickMenu(4);
                clickPlayer(1);
                clickMenu(0);
                clickPlayer(2);
                clickMenu(3);
            }
            case 8 -> {
                BrewingStandMenu menu = (BrewingStandMenu) container.getMenu();
                if (menu.getBrewingTicks() <= 0 && stall++ < 60) {
                    hold();
                    return;
                }
                stall = 0;
                check(menu.getFuel() > 0, "Brewing stand had no fuel after a native insert");
                check(menu.getBrewingTicks() > 0, "Brewing stand never started a live brew");
            }
            case 10 -> shot(client, "container_brewing.png");
            case 11 -> {
                if (PotionUtils.getPotion(container.getMenu().getSlot(0).getItem()) != Potions.AWKWARD) {
                    check(++stall < 460, "Brewing never completed the native awkward potion");
                    hold();
                } else stall = 0;
            }
            case 13 -> {
                shot(client, "container_brewing_ready.png");
                clickMenu(0);
            }
            case 15 -> server(client, player -> {
                check(PotionUtils.getPotion(player.containerMenu.getCarried()) == Potions.AWKWARD,
                        "Server did not accept the brewed potion pickup");
                check(player.containerMenu.getSlot(3).getItem().getCount() == 3,
                        "Brewing did not consume exactly one nether wart");
            });
            case 17 -> closeUi(client);
            case 19 -> next();
            default -> { }
        }
    }

    private static void anvil(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                player.giveExperienceLevels(12);
                player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
                player.inventoryMenu.broadcastChanges();
                open(player, anvil);
            });
            case 4 -> {
                if (!mounted(client, AnvilScreen.class)) hold();
            }
            case 6 -> shot(client, "container_anvil.png");
            case 7 -> clickPlayer(0);
            case 8 -> clickMenu(0);
            case 10 -> {
                EditBox name = child(client.screen, EditBox.class);
                name.setFocused(true);
                name.setValue(RENAME);
                check(name.getValue().equals(RENAME), "Anvil EditBox did not keep the typed name");
            }
            case 13 -> {
                AnvilMenu menu = (AnvilMenu) container.getMenu();
                check(menu.getCost() > 0, "Anvil rename produced no server cost");
                check(menu.getSlot(2).hasItem(), "Anvil result slot stayed empty after a native rename");
                shot(client, "container_anvil_ready.png");
                clickMenu(2);
                check(container.getMenu().getCarried().hasCustomHoverName(), "Renamed result was not carried");
                check(container.getMenu().getCarried().getHoverName().getString().equals(RENAME),
                        "Carried anvil result lost the native hover name");
            }
            case 15 -> shot(client, "container_anvil_named.png");
            case 16 -> server(client, player -> check(player.containerMenu.getCarried().hasCustomHoverName()
                    && player.containerMenu.getCarried().getHoverName().getString().equals(RENAME),
                    "Server did not accept the anvil rename result"));
            case 17 -> {
                if (!serverReady()) hold();
            }
            case 18 -> closeUi(client);
            case 20 -> next();
            default -> { }
        }
    }

    private static void smithing(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                player.getInventory().setItem(0, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
                player.getInventory().setItem(1, new ItemStack(Items.DIAMOND_CHESTPLATE));
                player.getInventory().setItem(2, new ItemStack(Items.NETHERITE_INGOT));
                player.inventoryMenu.broadcastChanges();
                open(player, smithing);
            });
            case 4 -> {
                if (!mounted(client, SmithingScreen.class)) hold();
            }
            case 6 -> {
                clickPlayer(0);
                clickMenu(0);
                clickPlayer(1);
                clickMenu(1);
                clickPlayer(2);
                clickMenu(2);
            }
            case 9 -> {
                if (!container.getMenu().getSlot(3).getItem().is(Items.NETHERITE_CHESTPLATE) && stall++ < 40) {
                    hold();
                    return;
                }
                stall = 0;
                check(container.getMenu().getSlot(3).getItem().is(Items.NETHERITE_CHESTPLATE),
                        "Smithing table did not form netherite chestplate");
                shot(client, "container_smithing_ready.png");
                clickMenu(3);
            }
            case 11 -> shot(client, "container_smithing.png");
            case 12 -> server(client, player -> check(player.containerMenu.getCarried().is(Items.NETHERITE_CHESTPLATE),
                    "Server did not carry the smithing result"));
            case 13 -> {
                if (!serverReady()) hold();
            }
            case 14 -> closeUi(client);
            case 16 -> next();
            default -> { }
        }
    }

    private static void enchant(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                player.giveExperienceLevels(40);
                player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_PICKAXE));
                player.getInventory().setItem(1, new ItemStack(Items.LAPIS_LAZULI, 16));
                player.inventoryMenu.broadcastChanges();
                open(player, enchant);
            });
            case 4 -> {
                if (!mounted(client, EnchantmentScreen.class)) hold();
            }
            case 6 -> {
                clickPlayer(0);
                clickMenu(0);
                clickPlayer(1);
                clickMenu(1);
            }
            case 9 -> {
                EnchantmentMenu menu = (EnchantmentMenu) container.getMenu();
                if (menu.costs[0] <= 0 && stall++ < 60) {
                    hold();
                    return;
                }
                stall = 0;
                check(menu.costs[0] > 0, "Enchantment offers never populated");
                shot(client, "container_enchantment_offers.png");
                clickAt(container.getGuiLeft() + 60 + 54, container.getGuiTop() + 14 + 9);
            }
            case 12 -> {
                check(container.getMenu().getSlot(0).getItem().isEnchanted(),
                        "Enchanting table click did not enchant the native item");
            }
            case 14 -> shot(client, "container_enchantment.png");
            case 15 -> server(client, player -> check(player.containerMenu.getSlot(0).getItem().isEnchanted(),
                    "Server did not keep the enchanted pickaxe"));
            case 16 -> {
                if (!serverReady()) hold();
            }
            case 17 -> closeUi(client);
            case 19 -> next();
            default -> { }
        }
    }

    private static void grindstone(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                ItemStack sword = new ItemStack(Items.IRON_SWORD);
                EnchantmentHelper.enchantItem(player.getRandom(), sword, 20, false);
                check(sword.isEnchanted(), "Failed to prepare an enchanted grindstone input");
                player.getInventory().setItem(0, sword);
                player.inventoryMenu.broadcastChanges();
                open(player, grindstone);
            });
            case 4 -> {
                if (!mounted(client, GrindstoneScreen.class)) hold();
            }
            case 6 -> {
                clickPlayer(0);
                clickMenu(0);
            }
            case 9 -> {
                if (!container.getMenu().getSlot(2).hasItem() && stall++ < 40) {
                    hold();
                    return;
                }
                stall = 0;
                check(container.getMenu().getSlot(2).hasItem()
                        && !container.getMenu().getSlot(2).getItem().isEnchanted(),
                        "Grindstone did not produce a disenchanted result");
                shot(client, "container_grindstone_ready.png");
                clickMenu(2);
            }
            case 11 -> shot(client, "container_grindstone.png");
            case 12 -> server(client, player -> check(player.containerMenu.getCarried().is(Items.IRON_SWORD)
                    && !player.containerMenu.getCarried().isEnchanted(),
                    "Server did not accept the grindstone result"));
            case 13 -> {
                if (!serverReady()) hold();
            }
            case 14 -> closeUi(client);
            case 16 -> next();
            default -> { }
        }
    }

    private static void stonecutter(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                player.getInventory().setItem(0, new ItemStack(Items.STONE, 16));
                player.inventoryMenu.broadcastChanges();
                open(player, stonecutter);
            });
            case 4 -> {
                if (!mounted(client, StonecutterScreen.class)) hold();
            }
            case 6 -> {
                clickPlayer(0);
                clickMenu(0);
            }
            case 8 -> {
                StonecutterMenu menu = (StonecutterMenu) container.getMenu();
                if ((!menu.hasInputItem() || menu.getNumRecipes() <= 0) && stall++ < 40) {
                    hold();
                    return;
                }
                stall = 0;
                check(menu.getNumRecipes() > 0, "Stonecutter had no selectable recipes");
                clickAt(container.getGuiLeft() + 52 + 8, container.getGuiTop() + 14 + 9);
            }
            case 11 -> {
                StonecutterMenu menu = (StonecutterMenu) container.getMenu();
                check(menu.getSelectedRecipeIndex() >= 0, "Stonecutter recipe click did not select");
                check(menu.getSlot(1).hasItem(), "Stonecutter result stayed empty after recipe selection");
                selectedStoneResult = menu.getRecipes().get(menu.getSelectedRecipeIndex())
                        .getResultItem(client.level.registryAccess()).copy();
                shot(client, "container_stonecutter_ready.png");
                clickMenu(1);
            }
            case 13 -> shot(client, "container_stonecutter.png");
            case 14 -> server(client, player -> {
                check(ItemStack.isSameItemSameTags(player.containerMenu.getCarried(), selectedStoneResult)
                                && player.containerMenu.getCarried().getCount() == selectedStoneResult.getCount(),
                        "Server result disagreed with the selected stonecutting recipe");
                check(player.containerMenu.getSlot(0).getItem().getCount() == 15,
                        "Stonecutting did not consume one stone");
            });
            case 15 -> {
                if (!serverReady()) hold();
            }
            case 16 -> closeUi(client);
            case 18 -> next();
            default -> { }
        }
    }

    private static void loom(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                player.getInventory().setItem(0, new ItemStack(Items.WHITE_BANNER));
                player.getInventory().setItem(1, new ItemStack(Items.RED_DYE));
                player.inventoryMenu.broadcastChanges();
                open(player, loom);
            });
            case 4 -> {
                if (!mounted(client, LoomScreen.class)) hold();
            }
            case 6 -> {
                clickPlayer(0);
                clickMenu(0);
                clickPlayer(1);
                clickMenu(1);
            }
            case 9 -> {
                LoomMenu menu = (LoomMenu) container.getMenu();
                if (menu.getSelectablePatterns().isEmpty() && stall++ < 40) {
                    hold();
                    return;
                }
                stall = 0;
                check(!menu.getSelectablePatterns().isEmpty(), "Loom had no selectable patterns");
                clickAt(container.getGuiLeft() + 60 + 7, container.getGuiTop() + 13 + 7);
            }
            case 12 -> {
                LoomMenu menu = (LoomMenu) container.getMenu();
                check(menu.getSelectedBannerPatternIndex() >= 0, "Loom pattern click did not select");
                check(menu.getResultSlot().hasItem(), "Loom result stayed empty after pattern selection");
                shot(client, "container_loom_ready.png");
                clickSlot(menu.getResultSlot());
            }
            case 14 -> shot(client, "container_loom.png");
            case 15 -> server(client, player -> {
                ItemStack banner = player.containerMenu.getCarried();
                var tag = net.minecraft.world.item.BlockItem.getBlockEntityData(banner);
                check(banner.is(Items.WHITE_BANNER) && tag != null
                                && tag.getList("Patterns", 10).size() == 1
                                && tag.getList("Patterns", 10).getCompound(0).getInt("Color")
                                == net.minecraft.world.item.DyeColor.RED.getId(),
                        "Server did not carry the selected red banner pattern");
            });
            case 16 -> {
                if (!serverReady()) hold();
            }
            case 17 -> closeUi(client);
            case 19 -> next();
            default -> { }
        }
    }

    private static void cartography(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                ItemStack map = MapItem.create(player.serverLevel(), player.getBlockX(), player.getBlockZ(), (byte) 0, true, false);
                check(!map.isEmpty(), "Failed to create a filled map");
                player.getInventory().setItem(0, map);
                player.getInventory().setItem(1, new ItemStack(Items.PAPER, 8));
                player.inventoryMenu.broadcastChanges();
                open(player, cartography);
            });
            case 4 -> {
                if (!mounted(client, CartographyTableScreen.class)) hold();
            }
            case 6 -> {
                clickPlayer(0);
                clickMenu(0);
                clickPlayer(1);
                clickMenu(1);
            }
            case 9 -> {
                if (!container.getMenu().getSlot(2).hasItem() && stall++ < 40) {
                    hold();
                    return;
                }
                stall = 0;
                check(container.getMenu().getSlot(2).hasItem(), "Cartography result stayed empty");
                shot(client, "container_cartography_ready.png");
                clickMenu(2);
            }
            case 11 -> shot(client, "container_cartography.png");
            case 12 -> server(client, player -> {
                ItemStack map = player.containerMenu.getCarried();
                var data = MapItem.getSavedData(map, player.serverLevel());
                check(map.is(Items.FILLED_MAP) && data != null && data.scale == 1,
                        "Server did not persist the enlarged map");
                check(player.containerMenu.getSlot(1).getItem().getCount() == 7,
                        "Cartography did not consume one paper");
            });
            case 13 -> {
                if (!serverReady()) hold();
            }
            case 14 -> closeUi(client);
            case 16 -> next();
            default -> { }
        }
    }

    private static void beacon(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                player.getInventory().setItem(0, new ItemStack(Items.IRON_INGOT, 4));
                player.inventoryMenu.broadcastChanges();
                player.teleportTo(beacon.getX() + 0.5, beacon.getY() + 1, beacon.getZ() + 0.5);
                open(player, beacon);
            });
            case 4 -> {
                if (!mounted(client, BeaconScreen.class)) hold();
            }
            case 6 -> {
                clickPlayer(0);
                clickMenu(0);
            }
            case 8 -> {
                BeaconMenu menu = (BeaconMenu) container.getMenu();
                if ((menu.getLevels() <= 0 || !menu.hasPayment()) && stall++ < 80) {
                    hold();
                    return;
                }
                stall = 0;
                check(menu.getLevels() > 0, "Beacon pyramid never reported a live level");
                check(menu.hasPayment(), "Beacon payment slot was empty after a native insert");
                AbstractWidget speed = client.screen.children().stream()
                        .filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                        .filter(widget -> widget.getClass().getSimpleName().equals("BeaconPowerButton")
                                && widget.getY() == container.getGuiTop() + 22)
                        .min(java.util.Comparator.comparingInt(AbstractWidget::getX)).orElseThrow();
                clickWidget(client.screen, speed);
            }
            case 9 -> shot(client, "container_beacon.png");
            case 10 -> clickAt(container.getGuiLeft() + 164 + 8, container.getGuiTop() + 107 + 8);
            case 13 -> check(client.screen == null, "Beacon confirmation did not close its native screen");
            case 14 -> server(client, player -> {
                var data = player.serverLevel().getBlockEntity(beacon).getUpdateTag();
                check(data.getInt("Primary") == net.minecraft.world.effect.MobEffect.getId(
                                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED),
                        "Beacon confirmation did not persist the selected primary effect");
            });
            case 15 -> {
                if (!serverReady()) hold();
            }
            case 16 -> closeUi(client);
            case 18 -> next();
            default -> { }
        }
    }

    private static void merchant(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                player.getInventory().setItem(0, new ItemStack(Items.EMERALD, 16));
                player.inventoryMenu.broadcastChanges();
                Villager villager = (Villager) player.serverLevel().getEntity(merchantId);
                check(villager != null, "Merchant villager is missing");
                player.teleportTo(villager.getX(), villager.getY(), villager.getZ() + 1);
                villager.setTradingPlayer(player);
                villager.openTradingScreen(player, Component.literal("Probe merchant"), 2);
            });
            case 4 -> {
                if (!mounted(client, MerchantScreen.class)) hold();
            }
            case 6 -> shot(client, "container_merchant.png");
            case 7 -> {
                MerchantMenu menu = (MerchantMenu) container.getMenu();
                check(menu.getOffers().size() >= 2, "Merchant did not sync multiple offers");
                AbstractWidget offer = namedWidget(client.screen, "TradeOfferButton");
                clickWidget(client.screen, offer);
            }
            case 10 -> {
                MerchantMenu menu = (MerchantMenu) container.getMenu();
                if (!menu.getSlot(0).hasItem() && stall++ < 40) {
                    hold();
                    return;
                }
                stall = 0;
                check(menu.getSlot(0).hasItem(), "Merchant offer click did not fill payment via native tryMoveItems");
                clickMenu(2);
            }
            case 13 -> server(client, player -> {
                check(player.containerMenu.getCarried().is(Items.BREAD)
                                && player.containerMenu.getCarried().getCount() == 4,
                        "Server did not produce the selected merchant trade");
                Villager villager = (Villager) player.serverLevel().getEntity(merchantId);
                check(villager.getOffers().get(0).getUses() == 1
                                && player.getInventory().countItem(Items.EMERALD)
                                + player.containerMenu.getSlot(0).getItem().getCount() == 15,
                        "Trade did not consume its emerald and increment offer usage");
            });
            case 14 -> {
                if (!serverReady()) hold();
            }
            case 15 -> closeUi(client);
            case 17 -> next();
            default -> { }
        }
    }

    /** Real OS Shift-click: synthetic KeyboardHandler events do not set GLFW modifier state. */
    private static void quickMove(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> server(client, player -> {
                clear(player);
                ((BaseContainerBlockEntity) player.serverLevel().getBlockEntity(smallChest))
                        .setItem(0, new ItemStack(Items.APPLE, 8));
                open(player, smallChest);
            });
            case 4 -> {
                if (!mounted(client, ContainerScreen.class)) hold();
            }
            case 6 -> {
                shot(client, "container_quick_move_before.png");
                Slot source = container.getMenu().getSlot(0);
                SAOMenu.LOGGER.info("[SAOMenu] awaiting native Shift-click at GUI {},{}",
                        container.getGuiLeft() + source.x + 8, container.getGuiTop() + source.y + 8);
            }
            case 7 -> {
                check(client.screen == container, "Quick-move screen changed while awaiting native input");
                if (container.getMenu().getSlot(0).hasItem()) {
                    hold();
                    return;
                }
                check(container.getMenu().getCarried().isEmpty()
                                && client.player.getInventory().countItem(Items.APPLE) == 8,
                        "Expected native Shift-click transfer, not a carried cursor stack");
                server(client, player -> check(player.containerMenu.getSlot(0).getItem().isEmpty()
                                && player.containerMenu.getCarried().isEmpty()
                                && player.getInventory().countItem(Items.APPLE) == 8,
                        "Server did not accept the native quick-move"));
            }
            case 9 -> shot(client, "container_quick_move_after.png");
            case 10 -> closeUi(client);
            case 12 -> next();
            default -> { }
        }
    }

    private static void unknown(Minecraft client, int at) {
        switch (at) {
            case 1 -> closeUi(client);
            case 3 -> {
                Screen keep = new InventoryScreen(client.player) { };
                check(VanillaUiPolicy.mode(keep) == VanillaUiPolicy.Mode.KEEP,
                        "An unknown inventory subclass inherited container ownership");
                client.setScreen(keep);
            }
            case 5 -> {
                check(client.screen != null && client.screen.getClass() != InventoryScreen.class,
                        "Unknown subclass was replaced with the named inventory class");
                check(VanillaUiPolicy.mode(client.screen) == VanillaUiPolicy.Mode.KEEP,
                        "Mounted unknown subclass did not remain KEEP");
                shot(client, "container_unknown_keep.png");
            }
            case 6 -> closeUi(client);
            case 8 -> next();
            default -> { }
        }
    }

    private static void installFixtures(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        player.setGameMode(GameType.SURVIVAL);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, player.server);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, player.server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, player.server);
        origin = player.blockPosition();
        int y = origin.getY();
        smallChest = new BlockPos(origin.getX() + 2, y, origin.getZ() + 2);
        largeChest = new BlockPos(origin.getX() + 4, y, origin.getZ() + 2);
        BlockPos largeChestEast = largeChest.east();
        shulker = new BlockPos(origin.getX() + 7, y, origin.getZ() + 2);
        hopper = new BlockPos(origin.getX() + 9, y, origin.getZ() + 2);
        dispenser = new BlockPos(origin.getX() + 11, y, origin.getZ() + 2);
        dropper = new BlockPos(origin.getX() + 13, y, origin.getZ() + 2);
        crafting = new BlockPos(origin.getX() + 2, y, origin.getZ() + 4);
        furnace = new BlockPos(origin.getX() + 4, y, origin.getZ() + 4);
        blast = new BlockPos(origin.getX() + 6, y, origin.getZ() + 4);
        smoker = new BlockPos(origin.getX() + 8, y, origin.getZ() + 4);
        brewing = new BlockPos(origin.getX() + 10, y, origin.getZ() + 4);
        anvil = new BlockPos(origin.getX() + 12, y, origin.getZ() + 4);
        smithing = new BlockPos(origin.getX() + 2, y, origin.getZ() + 6);
        grindstone = new BlockPos(origin.getX() + 12, y, origin.getZ() + 6);
        stonecutter = new BlockPos(origin.getX() + 14, y, origin.getZ() + 6);
        loom = new BlockPos(origin.getX() + 16, y, origin.getZ() + 6);
        cartography = new BlockPos(origin.getX() + 18, y, origin.getZ() + 6);
        enchant = new BlockPos(origin.getX() + 24, y, origin.getZ() + 6);
        beacon = new BlockPos(origin.getX() + 16, y, origin.getZ() + 16);
        place(level, smallChest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
        place(level, largeChest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH)
                .setValue(ChestBlock.TYPE, ChestType.LEFT));
        place(level, largeChestEast, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH)
                .setValue(ChestBlock.TYPE, ChestType.RIGHT));
        place(level, shulker, Blocks.SHULKER_BOX.defaultBlockState());
        place(level, hopper, Blocks.HOPPER.defaultBlockState());
        place(level, dispenser, Blocks.DISPENSER.defaultBlockState());
        place(level, dropper, Blocks.DROPPER.defaultBlockState());
        place(level, crafting, Blocks.CRAFTING_TABLE.defaultBlockState());
        place(level, furnace, Blocks.FURNACE.defaultBlockState());
        place(level, blast, Blocks.BLAST_FURNACE.defaultBlockState());
        place(level, smoker, Blocks.SMOKER.defaultBlockState());
        place(level, brewing, Blocks.BREWING_STAND.defaultBlockState());
        place(level, anvil, Blocks.ANVIL.defaultBlockState());
        place(level, smithing, Blocks.SMITHING_TABLE.defaultBlockState());
        place(level, grindstone, Blocks.GRINDSTONE.defaultBlockState());
        place(level, stonecutter, Blocks.STONECUTTER.defaultBlockState());
        place(level, loom, Blocks.LOOM.defaultBlockState());
        place(level, cartography, Blocks.CARTOGRAPHY_TABLE.defaultBlockState());
        place(level, enchant, Blocks.ENCHANTING_TABLE.defaultBlockState());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 2) continue;
                place(level, enchant.offset(dx, 0, dz), Blocks.BOOKSHELF.defaultBlockState());
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                place(level, beacon.offset(dx, -1, dz), Blocks.IRON_BLOCK.defaultBlockState());
            }
        }
        place(level, beacon, Blocks.BEACON.defaultBlockState());
        Villager villager = EntityType.VILLAGER.create(level);
        check(villager != null, "Villager factory returned null");
        villager.moveTo(origin.getX() + 8.5, y, origin.getZ() + 8.5, 0, 0);
        villager.setNoAi(true);
        villager.setInvulnerable(true);
        villager.setVillagerData(villager.getVillagerData().setProfession(VillagerProfession.FARMER).setLevel(2));
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemStack(Items.EMERALD), new ItemStack(Items.BREAD, 4), 12, 2, 0.05F));
        offers.add(new MerchantOffer(new ItemStack(Items.EMERALD, 3), new ItemStack(Items.APPLE, 8), 8, 4, 0.05F));
        villager.setOffers(offers);
        check(level.addFreshEntity(villager), "Villager failed to spawn");
        merchantId = villager.getUUID();
        player.teleportTo(origin.getX() + 0.5, y, origin.getZ() + 0.5);
        clear(player);
        player.inventoryMenu.broadcastChanges();
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        check(level.setBlock(pos, state, 3), "Failed to place " + state.getBlock() + " at " + pos);
    }

    private static void open(ServerPlayer player, BlockPos pos) {
        BlockState state = player.serverLevel().getBlockState(pos);
        MenuProvider provider = state.getMenuProvider(player.serverLevel(), pos);
        check(provider != null, "No native MenuProvider at " + pos + " for " + state);
        player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 1.5);
        check(player.openMenu(provider).isPresent(), "openMenu did not assign a container id for " + state.getBlock());
    }

    private static void clear(ServerPlayer player) {
        player.getInventory().clearContent();
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        if (player.serverLevel().getBlockEntity(smallChest) instanceof BaseContainerBlockEntity be) be.clearContent();
        if (player.serverLevel().getBlockEntity(largeChest) instanceof BaseContainerBlockEntity be) be.clearContent();
        if (player.serverLevel().getBlockEntity(shulker) instanceof BaseContainerBlockEntity be) be.clearContent();
        if (player.serverLevel().getBlockEntity(hopper) instanceof BaseContainerBlockEntity be) be.clearContent();
        if (player.serverLevel().getBlockEntity(dispenser) instanceof BaseContainerBlockEntity be) be.clearContent();
        if (player.serverLevel().getBlockEntity(dropper) instanceof BaseContainerBlockEntity be) be.clearContent();
        if (player.serverLevel().getBlockEntity(furnace) instanceof BaseContainerBlockEntity be) be.clearContent();
        if (player.serverLevel().getBlockEntity(blast) instanceof BaseContainerBlockEntity be) be.clearContent();
        if (player.serverLevel().getBlockEntity(smoker) instanceof BaseContainerBlockEntity be) be.clearContent();
        if (player.serverLevel().getBlockEntity(brewing) instanceof BaseContainerBlockEntity be) be.clearContent();
    }

    private static void server(Minecraft client, Consumer<ServerPlayer> task) {
        var server = client.getSingleplayerServer();
        var id = client.player.getUUID();
        serverWork = CompletableFuture.runAsync(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            check(player != null, "Missing native server player");
            task.accept(player);
        }, server);
        stall = 0;
    }

    private static boolean serverReady() {
        if (serverWork == null) return true;
        if (!serverWork.isDone()) {
            if (++stall > 200) throw new IllegalStateException("Server work timed out at " + leaf + ":" + age);
            return false;
        }
        try {
            serverWork.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw e;
        }
        serverWork = null;
        stall = 0;
        return true;
    }

    private static boolean mounted(Minecraft client, Class<? extends Screen> type) {
        if (!serverReady()) return false;
        if (client.screen == null || client.screen.getClass() != type) {
            if (++stall > 80) {
                throw new IllegalStateException("Did not mount " + type.getSimpleName() + ", got " + name(client.screen));
            }
            return false;
        }
        stall = 0;
        container = (AbstractContainerScreen<?>) client.screen;
        check(container.getMenu() == client.player.containerMenu,
                "Native controller was replaced for " + type.getSimpleName());
        client.getToasts().clear();
        return true;
    }

    private static void clickPlayer(int inventoryIndex) {
        clickSlot(playerSlot(inventoryIndex));
    }

    private static void clickMenu(int index) {
        clickMenu(index, 0);
    }

    private static void clickMenu(int index, int button) {
        clickSlot(container.getMenu().getSlot(index), button);
    }

    private static Slot playerSlot(int inventoryIndex) {
        Minecraft client = Minecraft.getInstance();
        return container.getMenu().slots.stream()
                .filter(slot -> slot.container == client.player.getInventory() && slot.getContainerSlot() == inventoryIndex)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing player inventory slot " + inventoryIndex));
    }

    private static void clickSlot(Slot slot) {
        clickSlot(slot, 0);
    }

    private static void clickSlot(Slot slot, int button) {
        double x = container.getGuiLeft() + slot.x + 8;
        double y = container.getGuiTop() + slot.y + 8;
        container.mouseClicked(x, y, button);
        container.mouseReleased(x, y, button);
    }

    private static void clickAt(double x, double y) {
        container.mouseClicked(x, y, 0);
        container.mouseReleased(x, y, 0);
    }

    private static void clickWidget(Screen screen, AbstractWidget widget) {
        double x = widget.getX() + widget.getWidth() / 2.0;
        double y = widget.getY() + widget.getHeight() / 2.0;
        check(screen.mouseClicked(x, y, 0), "Native widget rejected pointer: " + widget.getClass().getSimpleName());
        screen.mouseReleased(x, y, 0);
    }

    private static void clickCreativeTab(CreativeModeInventoryScreen screen, CreativeModeTab tab) {
        CreativeTabsScreenPage page = screen.getCurrentPage();
        int column = page.getColumn(tab);
        int tabX = tab.isAlignedRight()
                ? screen.getXSize() - 27 * (7 - column) + 1
                : 27 * column;
        int tabY = page.isTop(tab) ? -32 : screen.getYSize();
        double x = screen.getGuiLeft() + tabX + 13;
        double y = screen.getGuiTop() + tabY + 16;
        check(screen.mouseClicked(x, y, 0), "Creative tab click was ignored: " + tab.getDisplayName().getString());
        screen.mouseReleased(x, y, 0);
    }

    private static <T> T child(Screen screen, Class<T> type) {
        for (var child : screen.children()) {
            if (type.isInstance(child)) return type.cast(child);
        }
        throw new IllegalStateException("Missing " + type.getSimpleName() + " on " + screen.getClass().getSimpleName());
    }

    private static AbstractWidget namedWidget(Screen screen, String simpleName) {
        for (var child : screen.children()) {
            if (child instanceof AbstractWidget widget && widget.getClass().getSimpleName().equals(simpleName)) {
                return widget;
            }
        }
        throw new IllegalStateException("Missing native control " + simpleName + " on " + screen.getClass().getSimpleName());
    }

    private static void closeUi(Minecraft client) {
        if (client.screen != null) client.screen.onClose();
        container = null;
    }

    private static void key(Minecraft client, int code, int action) {
        client.keyboardHandler.keyPress(client.getWindow().getWindow(), code, 0, action, 0);
    }

    private static void press(Minecraft client, int code) {
        key(client, code, GLFW.GLFW_PRESS);
        key(client, code, GLFW.GLFW_RELEASE);
    }

    private static void shot(Minecraft client, String name) {
        SAOMenuPreview.grab(client, System.getProperty("saomenu.preview"), name);
    }

    private static void next() {
        Leaf[] values = Leaf.values();
        leaf = values[leaf.ordinal() + 1];
        age = 0;
        stall = 0;
        container = null;
        SAOMenu.LOGGER.info("[SAOMenu] container native leaf {}", leaf);
    }

    private static void finish(Minecraft client) {
        done = true;
        SAOMenu.LOGGER.info("[SAOMenu] containers native checks passed");
        if (!Boolean.getBoolean("saomenu.preview.keepOpen")) client.stop();
    }

    private static void hold() {
        age--;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static String name(Screen screen) {
        return screen == null ? "null" : screen.getClass().getName();
    }
}
