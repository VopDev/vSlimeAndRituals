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
import net.botwithus.rs3.game.minimenu.actions.SelectableAction;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
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
import net.botwithus.rs3.events.impl.ChatMessageEvent;

import java.util.List;
import java.util.Random;

public class vSAR extends LoopingScript {
    private final Random random = new Random();
    private BotState botState = BotState.IDLE;
    public Integer bankPreset = 1; // Change from int to Integer
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
            System.out.println("Loaded vSlimeAndRituals!");

            subscribe(ChatMessageEvent.class, chatMessageEvent -> {
                if (chatMessageEvent.getMessage().contains("Your components do not all have the required")) {
                    println("You seem to be out of resources, stopping script.");
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
            System.out.println("Error during initialization: " + e.getMessage());
            return false;
        }
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
                    Execution.delay(handleSkilling(player));
                    return;
                case BANKING:
                    Execution.delay(handleBanking(player));
                    return;
                case RITUALS:
                    Execution.delay(random.nextLong(450,900));
                    handleRituals();
                default:
                    System.out.println("Unexpected bot state, report to author!");
            }
            Execution.delay(random.nextLong(2000, 4000));
        } catch (Exception e) {
            System.out.println("Error during onLoop: " + e.getMessage());
        }
    }

    private long handleBanking(Player player) {
        try {
            if (player.isMoving() || player.getAnimationId() != -1)
                return random.nextLong(1000, 2000);

            if (Bank.isOpen()) {
                Bank.loadPreset(bankPreset);
                System.out.println("Bank preset loaded");
                botState = BotState.COLLECTING;
            } else {
                SceneObject sceneObject = SceneObjectQuery.newQuery().name("Bank chest").option("Use").results().nearest();
                if (sceneObject != null) {
                    sceneObject.interact("Use");
                } else {
                    System.out.println("No bank chest found.");
                }
            }
            if (player.getCoordinate().getRegionId() != 13214) {
                boolean use = ActionBar.useTeleport("War's Retreat Teleport");
                if (use) {
                    System.out.println("Teleported to War's Retreat");
                    Execution.delay(random.nextLong(1000, 2000));
                }
            }
            return random.nextLong(1000, 3000);
        } catch (Exception e) {
            System.out.println("Error during handleBanking: " + e.getMessage());
            return random.nextLong(1000, 3000); // Return a default delay on error
        }
    }

    private long handleSkilling(Player player) {
        try {
            if (player.isMoving() || player.getAnimationId() != -1)
                return random.nextLong(1000, 2000);

            if (Backpack.isFull()) {
                var magicNote = InventoryItemQuery.newQuery().name("Magic notepaper").results().first();
                var bucketOfSlime = InventoryItemQuery.newQuery().name("Bucket of slime").results().first();
                if (magicNote != null && bucketOfSlime != null) {
                    if (Backpack.interact("Bucket of slime")) {
                        System.out.println("Clicked bucket");
                        Execution.delay(500);
                        Backpack.interact("Magic notepaper");
                        System.out.println("Clicked magic note");
                        return random.nextLong(1000, 2000);
                    }
                    Execution.delay(500);
                } else {
                    botState = BotState.BANKING;
                    return random.nextLong(1000, 2000);
                }
                return random.nextLong(1000, 2000);
            }

            // Handling actions based on region ID
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
            System.out.println("Error during handleSkilling: " + e.getMessage());
            return random.nextLong(1000, 3000); 
        }
    }

    private void handleRegion14746() {
        try {
            System.out.println("Handling region 14746...");
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
                    Coordinate shortcutCoordinate = agilityShortcut.stream().filter(it -> it.getZ() == playerZ).findFirst().orElse(null);
                    if (shortcutCoordinate != null) {
                        SceneObject shortcut = SceneObjectQuery.newQuery().results().nearestTo(shortcutCoordinate);
                        if (shortcut != null && shortcut.getCoordinate().equals(shortcutCoordinate)) {
                            shortcut.interact("Jump-down");
                            System.out.println("Used shortcut at " + shortcutCoordinate);
                        }
                    }
                } else {
                    Coordinate stairsCoordinate = stairsCoordinates.stream().filter(it -> it.getZ() == playerZ).findFirst().orElse(null);
                    if (stairsCoordinate != null) {
                        SceneObject stairs = SceneObjectQuery.newQuery().name("Stairs").results().nearestTo(stairsCoordinate);
                        if (stairs != null) {
                            if (stairs.getCoordinate().equals(stairsCoordinate)) {
                                stairs.interact("Climb-down");
                                System.out.println("Used stairs at " + stairsCoordinate);
                            } else {
                                System.out.println("Found stairs at different coordinate: " + stairs.getCoordinate());
                            }
                        } else {
                            System.out.println("No stairs found at " + stairsCoordinate);
                        }
                    }
                }
            }

            SceneObject pool = SceneObjectQuery.newQuery().name("Pool of Slime").results().nearest();
            int playerZ = player.getCoordinate().getZ();
            if (pool != null && playerZ == pool.getCoordinate().getZ()) { 
                pool.interact("Use slime");
                System.out.println("Used slime");
            }
        } catch (Exception e) {
            System.out.println("Error during handleRegion14746: " + e.getMessage());
        }
    }

    private void handleRegion14646And14647() {
        try {
            System.out.println("Looking for and interacting with a trapdoor...");
            var trapdoors = SceneObjectQuery.newQuery().name("Trapdoor").results();
            var climbDownTrapdoor = trapdoors.stream().filter(it -> it.getOptions().contains("Climb-down")).findFirst().orElse(null);
            var openTrapdoor = trapdoors.stream().filter(it -> it.getOptions().contains("Open")).findFirst().orElse(null);

            if (climbDownTrapdoor != null) {
                boolean success = climbDownTrapdoor.interact("Climb-down");
                if (success) {
                    System.out.println("Climbed down trapdoor");
                    Execution.delay(random.nextLong(1000, 2000));
                } else {
                    System.out.println("Failed to climb down trapdoor");
                }
            } else if (openTrapdoor != null) {
                boolean success = openTrapdoor.interact("Open");
                if (success) {
                    System.out.println("Opened trapdoor");
                    Execution.delay(random.nextLong(1000, 2000));
                } else {
                    System.out.println("Failed to open trapdoor");
                }
            }
        } catch (Exception e) {
            System.out.println("Error during handleRegion14646And14647: " + e.getMessage());
        }
    }

    private void useEctophial() {
        try {
            var ectoPhial = InventoryItemQuery.newQuery().name("Ectophial").results().first(); 
            if (ectoPhial != null) {
                boolean use = Backpack.interact("Ectophial");
                if (use) {
                    System.out.println("Used ectophial");
                    Execution.delay(random.nextLong(1000, 2000));
                }
            } else {
                System.out.println("Ectophial not found in inventory.");
            }
        } catch (Exception e) {
            System.out.println("Error during useEctophial: " + e.getMessage());
        }
    }

    private void handleRituals() {
        EntityResultSet<Npc> npcsinArea = NpcQuery.newQuery().inside(rituals).results(); 
        LocalPlayer player = Client.getLocalPlayer();
        ResultSet<SceneObject> ritualStart = SceneObjectQuery.newQuery().name("Platform").option("Start ritual").results();
        int playerCurrentAnim = Client.getLocalPlayer().getAnimationId();

        if(VarManager.getVarbitValue(53292) == 0) {
            println("Checking Glyph Health...");
            if (npcsinArea.isEmpty()) {
                println("No NPCs found in the area.");
            } else {
                for(Npc npc: npcsinArea) {
                    println("Checking NPC: " + npc.getName()); 
                    if(npc.getName().contains("depleted") && !player.isMoving()) {
                        println("Depleted Glyph: " + npc.getName());
                        println("Repairing: " + pedestal.interact("Repair all"));
                        Execution.delay(random.nextLong(4000,4500));
                        break; 
                    } 
                }
            }
        }
        Execution.delay(random.nextLong(500,750));
        if (playerCurrentAnim == -1 && VarManager.getVarbitValue(53292) == 0) {
            SceneObject interactRitual = ritualStart.first();
            if (interactRitual != null) {
                println("Interacted Ritual: " + interactRitual.interact("Start ritual"));
                Execution.delay(random.nextLong(3000,4500));
            }
        } else while (VarManager.getVarbitValue(53292) != 0) {
            println("Currently in ritual - delaying."); 
            Execution.delay(random.nextLong(3000,4500));
        }
       
    }

    



    public BotState getBotState() {
        return botState;
    }

    public void setBotState(BotState botState) {
        this.botState = botState;
    }

    public void setBankPreset(Integer bankPreset) { // Change parameter type to Integer
        this.bankPreset = bankPreset; // Update the bank preset
    }

    public Integer getBankPreset() { // Change return type to Integer
        return bankPreset; // Return the current bank preset
    }
}

