package net.botwithus;


import java.text.NumberFormat;

import net.botwithus.vSAR.BotState;
import net.botwithus.rs3.game.skills.Skills;
import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;

public class vSARGraphicsContext extends ScriptGraphicsContext {

    private vSAR script;
    public int TheivingXPStart;
    public int TheivingXPGained = 0;
    public int bankPresetIndex = 0; // Add this line to declare the variable

    public vSARGraphicsContext(ScriptConsole scriptConsole, vSAR script) {
        super(scriptConsole);
        this.script = script;
        this.TheivingXPStart = Skills.THIEVING.getSkill().getExperience();
    }

    @Override
    public void drawSettings() {
        if (ImGui.Begin("vSlimeAndRituals", ImGuiWindowFlag.None.getValue())) {
            if (ImGui.BeginTabBar("vSlimeAndRituals", ImGuiWindowFlag.None.getValue())) {
                if (ImGui.BeginTabItem("Slime Collecting", ImGuiWindowFlag.None.getValue())) {
            
                    ImGui.Text("Requirement: Ectophial - Start at Wars Retreat");
                    ImGui.Text("Optional: 58 Agility for Shortcut");
                    ImGui.Text("Optional: Magic Notepaper");
                    ImGui.Text("Current State: " + script.getBotState());
                    ImGui.Text("Buckets Collected: " + script.bucketsCollected);
                    
                    script.bankPreset = ImGui.InputInt("Preset #", script.bankPreset);
                  
                    if (ImGui.Button("Start")) {
                        script.setBotState(vSAR.BotState.COLLECTING);
                    }
                    ImGui.SameLine();
                    if (ImGui.Button("Stop")) {
                        script.setBotState(vSAR.BotState.IDLE);
                    }
                    

                    ImGui.EndTabItem();
                  
                }
                if (ImGui.BeginTabItem("Rituals", ImGuiWindowFlag.None.getValue())) {
                    ImGui.Text("Begin at Um Ritual Site");
                    ImGui.Text("Reminder: Script will not interact with disturbances");
                    ImGui.Text("Ensure you have enough resources to repair your glyphs");
                    ImGui.Text("Current State: " + script.getBotState());
                    if (ImGui.Button("Start")) {
                        script.setBotState(vSAR.BotState.RITUALS);
                    }
                    ImGui.SameLine();
                    if (ImGui.Button("Stop")) {
                        script.setBotState(vSAR.BotState.IDLE);
                    }
                    
            }
        }
            ImGui.EndTabBar();
            ImGui.End(); 
        } 
    }

    @Override
    public void drawOverlay() {
        super.drawOverlay();
    }
}