package net.botwithus;

import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.internal.scripts.ScriptDefinition;
import net.botwithus.rs3.game.Client;
import net.botwithus.rs3.game.actionbar.ActionBar;
import net.botwithus.rs3.game.hud.interfaces.Component;
import net.botwithus.rs3.game.hud.interfaces.Interfaces;
import net.botwithus.rs3.game.minimenu.MiniMenu;
import net.botwithus.rs3.game.minimenu.actions.ComponentAction;
import net.botwithus.rs3.game.queries.builders.characters.NpcQuery;
import net.botwithus.rs3.game.queries.builders.components.ComponentQuery;
import net.botwithus.rs3.game.queries.builders.objects.SceneObjectQuery;
import net.botwithus.rs3.game.scene.entities.characters.player.LocalPlayer;
import net.botwithus.rs3.game.scene.entities.object.SceneObject;
import net.botwithus.rs3.script.Execution;
import net.botwithus.rs3.script.LoopingScript;
import net.botwithus.rs3.script.config.ScriptConfig;
import net.botwithus.rs3.game.Item;
import net.botwithus.rs3.game.queries.builders.items.InventoryItemQuery;
import net.botwithus.api.game.hud.inventories.Backpack;
import net.botwithus.api.game.hud.inventories.Bank;
import net.botwithus.rs3.events.impl.ChatMessageEvent;
import net.botwithus.rs3.events.impl.ServerTickedEvent;
import net.botwithus.rs3.game.Area;
import net.botwithus.rs3.game.Coordinate;
import net.botwithus.rs3.game.queries.results.ResultSet;
import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.util.Regex;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class vInkMaker extends LoopingScript {

    private BotState botState = BotState.IDLE;
    private boolean someBool = true;
    private Random random = new Random();
    private String selectedInk;
    private String inkPrimary; // Added this line to define the variable
    private boolean inkInteracted = false; // Add this flag

    public String getSelectedInk() {
        return selectedInk;
    }

    public void setSelectedInk(String selectedInk) {
        this.selectedInk = selectedInk;
    }

    public String getInkPrimary() {

        if (selectedInk.equals("Powerful")) {
            inkPrimary = "Powerful necroplasm";
        }

        if (selectedInk.equals("Greater")) {
            inkPrimary = "Greater necroplasm";
        }
        if (selectedInk.equals("Regular")) {
            inkPrimary = "Lesser necroplasm";
        }
        return inkPrimary; // Use the defined variable
    }

    public void beginMakingInk() {
       

        getInkPrimary();

        Component validink = ComponentQuery.newQuery(1473).itemName(inkPrimary).option("Craft").results().first();
        println("Looking for necroplasm: " + inkPrimary);
        Execution.delay(random.nextLong(200,450));
        if (validink == null) {
            println("No necroplasm found: " + inkPrimary);
            setBotState(BotState.IDLE);
            return;
        }
        if (validink != null && !Interfaces.isOpen(1370) && !Interfaces.isOpen(1251)) {
            println("Found necroplasm: " + inkPrimary);
            Execution.delay(random.nextLong(2000,3000));
            boolean success = validink.interact("Craft");
            Execution.delay(random.nextLong(500,650));
            if (inkInteracted) {
                return; // Skip if ink has already been interacted with
            }
            if (success) {
                MiniMenu.interact(ComponentAction.DIALOGUE.getType(), 0, -1, 89784350);
            }
            println("Attempting Ink Crafting: " + success);
            Execution.delay(random.nextLong(300,350));
            if (success) {
                inkInteracted = true; // Set the flag to true after successful interaction
                Execution.delayUntil(() -> !Interfaces.isOpen(1370) && !Interfaces.isOpen(1251), () -> true, 10000);
                if (!Interfaces.isOpen(1370) && !Interfaces.isOpen(1251)) {
                    println("Interface closed, switching to banking state");
                    setBotState(BotState.BANKING);
                }
            }
        }
    }

    private void handleBanking() {
        // Implement your banking logic here
        println("Handling banking...");
        SceneObject bankbooth = SceneObjectQuery.newQuery().name("Bank booth").results().nearest();
        SceneObject bankchest = SceneObjectQuery.newQuery().name("Bank chest").results().nearest();
        if (bankbooth == null) {
            println("Bank Booth not found - searching for bank chest.");
        } else {
            println("Found Bank Booth.");
            println("Interacted bank: " + bankbooth.interact("Load Last Preset from"));
        }
        if (bankchest == null) {
            println("Bank Chest not found.");
        } else {
            println("Found Bank Chest");
            println("Interacted bank: " + bankchest.interact("Load Last Preset from"));
        }

        if (bankbooth == null && bankchest == null) {
            println("No bank options found. Stopping script.");
            setBotState(BotState.IDLE);
            return;
        }
        inkInteracted = false; // Reset the flag when switching to banking state
        setBotState(BotState.MAKING_INK);
    }

    enum BotState {
        IDLE,
        MAKING_INK,
        BANKING,
    }

    public vInkMaker(String s, ScriptConfig scriptConfig, ScriptDefinition scriptDefinition) {
        super(s, scriptConfig, scriptDefinition);
        this.sgc = new vInkMakerGraphicsContext(getConsole(), this);

    }

    public boolean initialize() {
        super.initialize();
        subscribe(ChatMessageEvent.class, chatMessageEvent -> {
        if (chatMessageEvent.getMessage().contains("You don't have the necessary")) {
            println("Out of resources. Stopping script.");
            setBotState(BotState.IDLE);
        }
        });
        return true;
    }

    @Override
    public void onLoop() {
        //Loops every 100ms by default, to change:
        //this.loopDelay = 500;
        LocalPlayer player = Client.getLocalPlayer();
        if (player == null || Client.getGameState() != Client.GameState.LOGGED_IN || botState == BotState.IDLE) {
            //wait some time so we dont immediately start on login.
            Execution.delay(random.nextLong(3000,7000));
            return;
        }
        switch (botState) {
            case IDLE -> {
                //do nothing
                println("We're idle!");
                Execution.delay(random.nextLong(1000,3000));
            }
            case MAKING_INK -> {
                beginMakingInk();
            }
            case BANKING -> {
                handleBanking();
            }

        }
    }

    
    public BotState getBotState() {
        return botState;
    }

    public void setBotState(BotState botState) {
        this.botState = botState;
    }



}