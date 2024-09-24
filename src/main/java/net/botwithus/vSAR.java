package net.botwithus;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.events.impl.ChatMessageEvent;
import net.botwithus.rs3.events.impl.InventoryUpdateEvent;
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.login.LoginManager;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.minimenu.actions.SelectableAction;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.queries.results.EntityResultSet;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.scene.entities.characters.npc.Npc;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.characters.player.Player;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.game.vars.VarManager;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.game.login.World;
import net.botwithus.rs3.game.queries.builders.worlds.WorldQuery;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class vSAR extends LoopingScript {
    private final Random random = new Random();
    private BotState botState = BotState.IDLE;
    public Integer bankPreset = 1; 
    public int bucketsCollected = 0;
    private Area rituals = new Area.Rectangular(new Coordinate(1033, 1767, 1), new Coordinate(1045, 1783, 1)); 
    ResultSet<SceneObject> pedestalObject = SceneObjectQuery.newQuery().contains("Pedestal").inside(rituals).results(); 
    SceneObject pedestal = pedestalObject.first();

    enum BotState {
        IDLE,
        COLLECTING,
        BANKING,
        RITUALS
    }

    public vSAR(String scriptName, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(scriptName, scriptConfig, scriptDefinition);
    }

    @Override
    public boolean initialize() {
        try {
            super.initialize();
            this.sgc = new vSARGraphicsContext(getConsole(), this);
            println("Loaded vSlimeAndRituals!");

            subscribe(ChatMessageEvent.class, chatMessageEvent -> {
                if (chatMessageEvent.getMessage().contains("Your components do not all have the required")) {
                    println("[Failsafe] Out of resources to repair glyphs, stopping script.");
                    botState = BotState.IDLE;
                }
            }
        );

            subscribe(InventoryUpdateEvent.class, it -> {
                if (it.getInventoryId() == 93) { 
                    if ("Bucket of slime".equals(it.getNewItem().getName())) { 
                        bucketsCollected += it.getNewItem().getStackSize() - it.getOldItem().getStackSize();
                    }
                }
            });
            return true;
        } catch (Exception e) {
            println("Error during initialization: " + e.getMessage());
            return false;
        }
    }

    public boolean logoutOnNoNotepaper = false;
    public boolean worldhop = false;
    public boolean devmode = false;
    private long lastWorldHopTime = 0;

    public void logout() {
        println("[Failsafe] Logging out...");
        if (Client.getGameState() != Client.GameState.LOGGED_IN) {
            return;
        }
        LoginManager.setAutoLogin(false);
        MiniMenu.interact(ComponentAction.COMPONENT.getType(), 1, -1, 93913156);
        if (devmode) {
            println("Developer mode enabled, preventing logout.");
            return;
        }
        Execution.delayUntil(5000, () -> Client.getGameState() != Client.GameState.LOGGED_IN);
        println("[Failsafe] Logged out.");
    }

    @Override
    public void onLoop() {
        try {
            Player player = Client.getLocalPlayer();
            if (Client.getGameState() != Client.GameState.LOGGED_IN || player == null || botState == BotState.IDLE) {
                Execution.delay(random.nextLong(2500, 5500));
                return;
            }
            switch (botState) {
                case COLLECTING:
                    if (devmode) {
                        println("[DEV] Switching to collecting state.");
                    }
                    Execution.delay(handleSkilling(player));
                    return;
                case BANKING:
                    if (devmode) {
                        println("[DEV] Switching to banking state.");
                    }
                    Execution.delay(handleBanking(player));
                    return;
                case RITUALS:
                    if (devmode) {
                        println("[DEV] Switching to rituals state.");
                    }
                    Execution.delay(random.nextLong(450,900));
                    handleRituals();
                    return;
                default:
                   return;
            }
        } catch (Exception e) {
            if (devmode) {
                println("[DEV] Error during onLoop: " + e.getMessage());
            }
        }
    }

    private long handleBanking(Player player) {
        try {
            if (player.isMoving() || player.getAnimationId() != -1)
                return random.nextLong(1000, 2000);

            if (Bank.isOpen()) {
                Bank.loadPreset(bankPreset);
                println("[BankHandler] Bank preset loaded");
                botState = BotState.COLLECTING;
            } else {
                SceneObject sceneObject = SceneObjectQuery.newQuery().name("Bank chest").option("Use").results().nearest();
                if (sceneObject != null) {
                    sceneObject.interact("Use");
                } else {
                    println("[BankHandler] No bank chest found.");
                }
            }
            if (player.getCoordinate().getRegionId() != 13214) {
                boolean use = ActionBar.useTeleport("War's Retreat Teleport");
                if (use) {
                    println("[BankHandler] Teleported to War's Retreat");
                    Execution.delay(random.nextLong(1000, 2000));
                }
            }
            return random.nextLong(1000, 3000);
        } catch (Exception e) {
            if (devmode) {
                println("[DEV] Error during handleBanking: " + e.getMessage());
            }
            return random.nextLong(1000, 3000); 
        }
    }

    private long handleSkilling(Player player) {
        try {
            if (player.isMoving() || player.getAnimationId() != -1)
                return random.nextLong(1000, 2000);

                if (Backpack.isFull()) {
                    var magicNote = InventoryItemQuery.newQuery(93)
                    .name("Magic notepaper")
                    .or(InventoryItemQuery.newQuery(93).name("Enchanted notepaper"))
                    .results().first();
                    var bucketOfSlime = InventoryItemQuery.newQuery(93).ids(4286).results().first();
                    if (magicNote != null && bucketOfSlime != null) {
                       boolean itemSelected = MiniMenu.interact(SelectableAction.SELECTABLE_COMPONENT.getType(), 0, bucketOfSlime.getSlot(), 96534533);
                       println("[SlimeCollection] Item selected: " + itemSelected);
                       Execution.delay(random.nextInt(200, 300));
                       boolean notepaperSelected = MiniMenu.interact(SelectableAction.SELECT_COMPONENT_ITEM.getType(), 0, magicNote.getSlot(), 96534533);
                       println("[SlimeCollection] Notepaper selected: " + notepaperSelected);
                       Execution.delay(random.nextInt(200, 300));
                    } else if (logoutOnNoNotepaper) {
                        println("[SlimeCollection] No magic notepaper found, logging out.");
                        logout();
                    } else {
                        println("[SlimeCollection] No magic notepaper found, banking instead.");
                        botState = BotState.BANKING;
                        return random.nextLong(1000, 2000);
                    }
                    worldhopping(); // check for world hop at the end of the slime collection
                    return random.nextLong(1000, 2000);
                }

            switch (player.getCoordinate().getRegionId()) {
                case 14746:
                    handleRegion14746();
                    break;
                case 14646:
                case 14647:
                    handleRegion14646And14647();
                    break;
                default:
                    useEctophial();
                    break;
            }

            return random.nextLong(1000, 3000);
        } catch (Exception e) {
            if (devmode) {
                println("[DEV] Error during handleSkilling: " + e.getMessage());
            }
            return random.nextLong(1000, 3000); 
        }
    }

    private void handleRegion14746() {
        try {
            if (devmode) {
                println("[DEV] Handling region 14746...");
            }
            List<Coordinate> stairsCoordinates = List.of(
                new Coordinate(3689, 9887, 3),
                new Coordinate(3672, 9887, 2),
                new Coordinate(3684, 9887, 1)
            );
            List<Coordinate> agilityShortcut = List.of(
                new Coordinate(3670, 9888, 3)
            );
            Player player = Client.getLocalPlayer();
            if (player != null) {
                int playerZ = player.getCoordinate().getZ();
                if (Skills.AGILITY.getLevel() >= 58 && playerZ == 3) {
                    if (devmode) {
                        println("[DEV] Players agility level: " + Skills.AGILITY.getLevel());
                    }
                    Coordinate shortcutCoordinate = agilityShortcut.stream().filter(it -> it.getZ() == playerZ).findFirst().orElse(null);
                    if (shortcutCoordinate != null) {
                        SceneObject shortcut = SceneObjectQuery.newQuery().results().nearestTo(shortcutCoordinate);
                        if (shortcut != null && shortcut.getCoordinate().equals(shortcutCoordinate)) {
                            shortcut.interact("Jump-down");
                            if (devmode) {
                                println("[DEV] Used shortcut at " + shortcutCoordinate);
                            }
                        }
                    }
                } else {
                    Coordinate stairsCoordinate = stairsCoordinates.stream().filter(it -> it.getZ() == playerZ).findFirst().orElse(null);
                    if (stairsCoordinate != null) {
                        SceneObject stairs = SceneObjectQuery.newQuery().name("Stairs").results().nearestTo(stairsCoordinate);
                        if (stairs != null) {
                            if (stairs.getCoordinate().equals(stairsCoordinate)) {
                                stairs.interact("Climb-down");
                                if (devmode) {
                                    println("[DEV] Used stairs at " + stairsCoordinate);
                                }
                            } else {
                                if (devmode) {
                                    println("[DEV] Found stairs at different coordinate: " + stairs.getCoordinate());
                                }
                            }
                        } else {
                            if (devmode) {
                                println("[DEV] No stairs found at " + stairsCoordinate);
                            }
                        }
                    }
                }
            }

            SceneObject pool = SceneObjectQuery.newQuery().name("Pool of Slime").results().nearest();
            int playerZ = player.getCoordinate().getZ();
            if (pool != null && playerZ == pool.getCoordinate().getZ()) { 
                pool.interact("Use slime");
                println("[SlimeCollection] Gathered slime from pool.");
            }
        } catch (Exception e) {
            if (devmode) {
                println("[DEV] Error during handleRegion14746: " + e.getMessage());
            }
        }
    }

    private void handleRegion14646And14647() {
        try { 
            if (devmode) {
                println("[DEV] Looking for and interacting with a trapdoor...");
            }
            var trapdoors = SceneObjectQuery.newQuery().name("Trapdoor").results();
            var climbDownTrapdoor = trapdoors.stream().filter(it -> it.getOptions().contains("Climb-down")).findFirst().orElse(null);
            var openTrapdoor = trapdoors.stream().filter(it -> it.getOptions().contains("Open")).findFirst().orElse(null);

            if (climbDownTrapdoor != null) {
                boolean success = climbDownTrapdoor.interact("Climb-down");
                if (success) {
                if (devmode) {
                    println("[DEV] Climbed down trapdoor");
                }
                    Execution.delay(random.nextLong(1000, 2000));
                } else {
                    if (devmode) {
                        println("[DEV] Failed to climb down trapdoor");
                    }
                }
            } else if (openTrapdoor != null) {
                boolean success = openTrapdoor.interact("Open");
                if (success) {
                    if (devmode) {
                        println("[DEV] Opened trapdoor");
                    }
                    Execution.delay(random.nextLong(1000, 2000));
                } else {
                    if (devmode) {
                    println("[DEV] Failed to open trapdoor");
                    }
                }
            }
        } catch (Exception e) {
            if (devmode) {
                println("[DEV] Error during handleRegion14646And14647: " + e.getMessage());
            }
        }
    }

    private void useEctophial() {
        try {
            var ectoPhial = InventoryItemQuery.newQuery().name("Ectophial").results().first(); 
            if (ectoPhial != null) {
                boolean use = Backpack.interact("Ectophial");
                if (use) {
                        println("[SlimeCollection] Used ectophial");
                    Execution.delay(random.nextLong(1000, 2000));
                }
            } else {
                if (devmode) {
                    println("[DEV] Ectophial not found in inventory.");
                }
            }
        } catch (Exception e) {
            if (devmode) {
                println("[DEV] Error during useEctophial: " + e.getMessage());
            }
        }
    }


    public void worldhopping() {
        if (!worldhop) { // Check if worldhop is true
            return; 
        }
        long randomOffset = random.nextLong(TimeUnit.MINUTES.toMillis(10), TimeUnit.MINUTES.toMillis(30)); 
        if (System.currentTimeMillis() - lastWorldHopTime < TimeUnit.MINUTES.toMillis(200) + randomOffset) { 
            return; 
        }
        lastWorldHopTime = System.currentTimeMillis(); 
        WorldQuery worlds = WorldQuery.newQuery().members().mark();
              List<Integer> allowedWorlds = Arrays.asList(1, 2, 4, 5, 6, 9, 10, 12, 14, 16, 21, 22, 23, 24, 25, 26, 27, 28, 31, 32, 35, 36, 37, 39, 40, 42, 44, 45,
            46, 49, 50, 51, 53, 54, 56, 58, 59, 60, 62, 63, 64, 65, 67, 68, 69, 70, 71, 72, 73, 74, 76, 77, 78, 79, 82, 83,
            84, 85, 87, 88, 89, 91, 92, 96, 98, 99, 100, 103, 104, 105, 106, 116, 117, 119, 123, 124, 138, 139,
            140, 252, 257, 258, 259);
        
        World world = worlds.results().stream()
            .filter(w -> allowedWorlds.contains(w.getId())) // Filter by allowed worlds
            .findFirst() // Get the first matching world
            .orElse(null); // Handle case where no world is found

        if (world != null) {
            println("[Worldhop] Hoping to world: " + world);
            LoginManager.hopWorld(world);
            Execution.delay(8000);
        }
    }

    private void handleRituals() {
        EntityResultSet<Npc> npcsinArea = NpcQuery.newQuery().inside(rituals).results(); 
        LocalPlayer player = Client.getLocalPlayer();
        ResultSet<SceneObject> ritualStart = SceneObjectQuery.newQuery().name("Platform").option("Start ritual").results();
        int playerCurrentAnim = Client.getLocalPlayer().getAnimationId();

        if(VarManager.getVarbitValue(53292) == 0) {
            println("[Rituals] Checking Glyph Health...");
            if (npcsinArea.isEmpty()) {
                println("[Rituals] No NPCs found in the area.");
            } else {
                for(Npc npc: npcsinArea) {
                    if (devmode) {
                        println("[DEV] Checking NPC: " + npc.getName()); 
                    }
                    if(npc.getName().contains("depleted") && !player.isMoving()) {
                        println("[Rituals] Depleted Glyph: " + npc.getName());
                        println("[Rituals] Repairing: " + pedestal.interact("Repair all"));
                        if (!npcsinArea.stream().anyMatch(n -> n.getName().contains("depleted"))) {
                            println("[Rituals] No depleted glyphs detected, continuing with ritual.");
                            break;
                        } else {
                            println("[Rituals] Depleted glyphs detected, delaying.");
                            Execution.delay(random.nextLong(300, 450));
                            return;
                        } 
                    } 
                }
            }
        }
        if (playerCurrentAnim == -1 && VarManager.getVarbitValue(53292) == 0) {
            SceneObject interactRitual = ritualStart.first();
            if (interactRitual != null) {
                println("[Rituals] Interacted Ritual: " + interactRitual.interact("Start ritual"));
                Execution.delay(random.nextLong(3000,4500));
            }
        } else while (VarManager.getVarbitValue(53292) != 0) {
            println("[Rituals] Currently in ritual - delaying."); 
            Execution.delay(random.nextLong(3000,4500));
        }
       worldhopping(); // check for world hop at the end of the ritual
    }

    



    public BotState getBotState() {
        return botState;
    }

    public void setBotState(BotState botState) {
        this.botState = botState;
    }

    public void setBankPreset(Integer bankPreset) { 
        this.bankPreset = bankPreset; 
    }

    public Integer getBankPreset() { 
        return bankPreset; 
    }
}

