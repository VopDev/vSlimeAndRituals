package net.botwithus;

import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;

public class vSARGraphicsContext extends ScriptGraphicsContext {

    private vSAR script;
    public int bankPresetIndex = 0; 

    public vSARGraphicsContext(ScriptConsole scriptConsole, vSAR script) {
        super(scriptConsole);
        this.script = script;
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

                    script.logoutOnNoNotepaper = ImGui.Checkbox("Logout on No Magic Notepaper", script.logoutOnNoNotepaper); 

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
                if (ImGui.BeginTabItem("Extra Settings", ImGuiWindowFlag.None.getValue())) {
                    script.worldhop = ImGui.Checkbox("Enable Worldhopping", script.worldhop);
                    script.devmode = ImGui.Checkbox("Developer Mode", script.devmode);
                
                    ImGui.EndTabItem();
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